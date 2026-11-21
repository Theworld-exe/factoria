package com.factoria.core.database.sync

data class DatasetEntry(
    val id: String,
    val name: String,
    val version: String,
    val downloadUrl: String,
    val checksumSha256: String,
    val sizeBytes: Long,
    val releaseNotes: String
)

data class DatasetManifest(
    val schemaVersion: String,
    val publishedAt: String,
    val datasets: List<DatasetEntry>
)

data class ActiveDatabaseMetadata(
    val dataset: String,
    val sourceVersion: String,
    val sourceUrl: String,
    val contentChecksum: String,
    val fetchTimestamp: String,
    val schemaVersion: String,
    val itemsCount: Int,
    val recipesCount: Int,
    val machinesCount: Int,
    val categoriesCount: Int
)
