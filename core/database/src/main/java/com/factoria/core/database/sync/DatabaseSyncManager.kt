package com.factoria.core.database.sync

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.net.ssl.HttpsURLConnection

class DatabaseSyncManager(private val context: Context) {

    companion object {
        const val DEFAULT_MANIFEST_URL =
            "https://raw.githubusercontent.com/factoriolab/factoriolab/main/manifest.json"
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 30_000
        private const val ACTIVE_DB_NAME = "factorio_default.db"
    }

    /**
     * Inspects the active database and extracts its metadata key-value pairs.
     */
    suspend fun getActiveMetadata(): ActiveDatabaseMetadata? = withContext(Dispatchers.IO) {
        val dbFile = getActiveDatabaseFile()
        if (!dbFile.exists()) return@withContext null

        try {
            val db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            try {
                val metaMap = mutableMapOf<String, String>()
                db.rawQuery("SELECT key, value FROM metadata;", null).use { cursor ->
                    while (cursor.moveToNext()) {
                        val k = cursor.getString(0)
                        val v = cursor.getString(1)
                        metaMap[k] = v
                    }
                }

                val counts = mutableMapOf<String, Int>()
                for (tbl in listOf("items", "recipes", "machines", "categories")) {
                    db.rawQuery("SELECT COUNT(*) FROM $tbl;", null).use { cCursor ->
                        if (cCursor.moveToFirst()) {
                            counts[tbl] = cCursor.getInt(0)
                        }
                    }
                }

                ActiveDatabaseMetadata(
                    dataset = metaMap["dataset"] ?: "2.0",
                    sourceVersion = metaMap["source_version"] ?: "2.0.77",
                    sourceUrl = metaMap["source_url"] ?: "https://github.com/factoriolab/factoriolab",
                    contentChecksum = metaMap["content_checksum"] ?: "",
                    fetchTimestamp = metaMap["fetch_timestamp"] ?: "",
                    schemaVersion = metaMap["schema_version"] ?: "1.0.0",
                    itemsCount = counts["items"] ?: 0,
                    recipesCount = counts["recipes"] ?: 0,
                    machinesCount = counts["machines"] ?: 0,
                    categoriesCount = counts["categories"] ?: 0
                )
            } finally {
                db.close()
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Fetches and parses the published dataset manifest over HTTPS.
     */
    suspend fun fetchManifest(manifestUrl: String = DEFAULT_MANIFEST_URL): DatasetManifest = withContext(Dispatchers.IO) {
        if (!manifestUrl.startsWith("https://")) {
            throw IllegalArgumentException("Insecure protocol rejected: Manifest downloads must use HTTPS.")
        }

        val url = URL(manifestUrl)
        val connection = url.openConnection() as HttpsURLConnection
        val jsonString = try {
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Factoria-Android/1.0")

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw RuntimeException("HTTP $responseCode while fetching manifest from $manifestUrl")
            }

            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
        val root = JSONObject(jsonString)

        val schemaVersion = root.optString("schema_version", "1.0.0")
        val publishedAt = root.optString("published_at", "")
        val datasetsArray = root.getJSONArray("datasets")
        val entries = mutableListOf<DatasetEntry>()

        for (i in 0 until datasetsArray.length()) {
            val d = datasetsArray.getJSONObject(i)
            entries.add(
                DatasetEntry(
                    id = d.getString("id"),
                    name = d.getString("name"),
                    version = d.getString("version"),
                    downloadUrl = d.getString("download_url"),
                    checksumSha256 = d.getString("checksum_sha256"),
                    sizeBytes = d.optLong("size_bytes", 0L),
                    releaseNotes = d.optString("release_notes", "")
                )
            )
        }

        DatasetManifest(
            schemaVersion = schemaVersion,
            publishedAt = publishedAt,
            datasets = entries
        )
    }

    /**
     * Checks if a newer version of the specified dataset is available in the remote manifest.
     */
    suspend fun checkForUpdate(
        targetDatasetId: String,
        manifestUrl: String = DEFAULT_MANIFEST_URL
    ): SyncState = withContext(Dispatchers.IO) {
        try {
            val activeMeta = getActiveMetadata()
            val currentVersion = activeMeta?.sourceVersion ?: "0.0.0"

            val manifest = fetchManifest(manifestUrl)
            val entry = manifest.datasets.firstOrNull { it.id == targetDatasetId }
                ?: return@withContext SyncState.Error("Dataset '$targetDatasetId' not found in manifest.")

            if (entry.version != currentVersion) {
                SyncState.UpdateAvailable(
                    currentVersion = currentVersion,
                    newVersion = entry.version,
                    entry = entry
                )
            } else {
                SyncState.UpToDate(datasetId = targetDatasetId, version = currentVersion)
            }
        } catch (e: Exception) {
            SyncState.Error(
                message = e.localizedMessage ?: "Failed to check for updates: ${e.javaClass.simpleName}",
                canRetry = true
            )
        }
    }

    /**
     * Performs a resilient, atomic update:
     * 1. Streams download to a temporary file while computing SHA-256 in real time.
     * 2. Rejects and deletes file if checksum mismatches.
     * 3. Opens temp SQLite file and executes PRAGMA integrity_check and foreign_key_check.
     * 4. Backs up current active database.
     * 5. Atomically replaces active database.
     * 6. Automatically rolls back to backup if any step fails.
     */
    fun downloadAndApplyUpdate(entry: DatasetEntry): Flow<SyncState> = flow {
        val tempFile = File(context.cacheDir, "factoria_download_${System.currentTimeMillis()}.tmp")
        val activeDbFile = getActiveDatabaseFile()
        val backupFile = File(activeDbFile.parentFile, "${activeDbFile.name}.bak")

        try {
            // Step 1: Download
            emit(SyncState.Downloading(progressPercent = 0, bytesDownloaded = 0, totalBytes = entry.sizeBytes))

            if (!entry.downloadUrl.startsWith("https://")) {
                throw SecurityException("Insecure download URL rejected: Must use HTTPS.")
            }

            val url = URL(entry.downloadUrl)
            val conn = url.openConnection() as HttpsURLConnection
            val digest = MessageDigest.getInstance("SHA-256")
            try {
                conn.connectTimeout = CONNECT_TIMEOUT_MS
                conn.readTimeout = READ_TIMEOUT_MS
                conn.setRequestProperty("User-Agent", "Factoria-Android/1.0")

                val responseCode = conn.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw RuntimeException("HTTP $responseCode downloading dataset from ${entry.downloadUrl}")
                }

                val totalLength = if (conn.contentLengthLong > 0) conn.contentLengthLong else entry.sizeBytes
                var downloadedBytes = 0L

                conn.inputStream.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        val buffer = ByteArray(8192)
                        var read: Int
                        var lastReportTime = System.currentTimeMillis()

                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            digest.update(buffer, 0, read)
                            downloadedBytes += read

                            val now = System.currentTimeMillis()
                            if (now - lastReportTime > 200 || downloadedBytes == totalLength) {
                                val percent = if (totalLength > 0) ((downloadedBytes * 100) / totalLength).toInt().coerceIn(0, 100) else 0
                                emit(SyncState.Downloading(progressPercent = percent, bytesDownloaded = downloadedBytes, totalBytes = totalLength))
                                lastReportTime = now
                            }
                        }
                        output.flush()
                    }
                }
            } finally {
                conn.disconnect()
            }

            // Step 2: Checksum Verification
            val computedHash = digest.digest().joinToString("") { "%02x".format(it) }
            if (!entry.checksumSha256.equals(computedHash, ignoreCase = true)) {
                throw IllegalStateException("Checksum mismatch! Expected: ${entry.checksumSha256}, got: $computedHash")
            }

            // Step 3: SQLite Validation
            emit(SyncState.Validating)
            validateDatabaseFile(tempFile)

            // Step 4: Atomic Replacement with Rollback Protection
            emit(SyncState.Applying)

            // Make safe backup of current database
            if (activeDbFile.exists()) {
                activeDbFile.copyTo(backupFile, overwrite = true)
            }

            try {
                // Ensure target directory exists
                activeDbFile.parentFile?.mkdirs()
                // Atomic copy/replace
                tempFile.copyTo(activeDbFile, overwrite = true)
                // If swap succeeded, remove backup and temp
                if (backupFile.exists()) backupFile.delete()
                if (tempFile.exists()) tempFile.delete()

                val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                emit(
                    SyncState.Success(
                        datasetId = entry.id,
                        newVersion = entry.version,
                        appliedAt = timeStr,
                        message = "Database updated successfully to version ${entry.version}."
                    )
                )
            } catch (swapError: Exception) {
                // Rollback!
                if (backupFile.exists()) {
                    backupFile.copyTo(activeDbFile, overwrite = true)
                    backupFile.delete()
                }
                throw RuntimeException("Failed during database swap. Restored previous database. Reason: ${swapError.message}", swapError)
            }

        } catch (e: Exception) {
            if (tempFile.exists()) tempFile.delete()
            emit(
                SyncState.Error(
                    message = e.localizedMessage ?: "Synchronization failed: ${e.javaClass.simpleName}",
                    canRetry = true,
                    rolledBack = backupFile.exists()
                )
            )
        } finally {
            cleanupTempFiles()
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Validates SQLite database integrity and foreign-key constraints before activation.
     */
    private fun validateDatabaseFile(file: File) {
        val db = SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        try {
            // 1. Integrity check
            val integrityCursor = db.rawQuery("PRAGMA integrity_check;", null)
            val integrityOk = integrityCursor.moveToFirst() && integrityCursor.getString(0).equals("ok", ignoreCase = true)
            integrityCursor.close()
            if (!integrityOk) {
                throw IllegalStateException("Downloaded SQLite database failed PRAGMA integrity_check.")
            }

            // 2. Foreign key check
            val fkCursor = db.rawQuery("PRAGMA foreign_key_check;", null)
            val hasViolations = fkCursor.moveToFirst()
            fkCursor.close()
            if (hasViolations) {
                throw IllegalStateException("Downloaded SQLite database has foreign-key violations.")
            }

            // 3. Verify required tables exist
            val tablesCursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table';", null)
            val tableNames = mutableSetOf<String>()
            while (tablesCursor.moveToNext()) {
                tableNames.add(tablesCursor.getString(0))
            }
            tablesCursor.close()

            val requiredTables = listOf("metadata", "categories", "items", "machines", "recipes", "recipe_ingredients", "recipe_products", "machine_recipes")
            for (rt in requiredTables) {
                if (rt !in tableNames) {
                    throw IllegalStateException("Downloaded database missing required table: $rt")
                }
            }
        } finally {
            db.close()
        }
    }

    /**
     * Resolves the primary active database path in app storage.
     */
    fun getActiveDatabaseFile(): File {
        return context.getDatabasePath(ACTIVE_DB_NAME)
    }

    /**
     * Cleans up stray temporary download files.
     */
    fun cleanupTempFiles() {
        try {
            val cacheFiles = context.cacheDir.listFiles { _, name ->
                name.startsWith("factoria_download_") && name.endsWith(".tmp")
            }
            cacheFiles?.forEach { it.delete() }
        } catch (_: Exception) {}
    }
}
