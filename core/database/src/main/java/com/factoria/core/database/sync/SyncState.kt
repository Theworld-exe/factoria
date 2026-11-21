package com.factoria.core.database.sync

sealed interface SyncState {
    data object Idle : SyncState

    data class CheckingForUpdates(val datasetId: String) : SyncState

    data class UpdateAvailable(
        val currentVersion: String,
        val newVersion: String,
        val entry: DatasetEntry
    ) : SyncState

    data class UpToDate(
        val datasetId: String,
        val version: String
    ) : SyncState

    data class Downloading(
        val progressPercent: Int,
        val bytesDownloaded: Long,
        val totalBytes: Long
    ) : SyncState

    data object Validating : SyncState

    data object Applying : SyncState

    data class Success(
        val datasetId: String,
        val newVersion: String,
        val appliedAt: String,
        val message: String
    ) : SyncState

    data class Error(
        val message: String,
        val canRetry: Boolean = true,
        val rolledBack: Boolean = false
    ) : SyncState
}
