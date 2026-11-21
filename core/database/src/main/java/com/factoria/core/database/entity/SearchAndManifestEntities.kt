package com.factoria.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

@Fts4
@Entity(tableName = "search_index")
data class SearchFtsEntity(
    @PrimaryKey @ColumnInfo(name = "rowid") val rowid: Int = 0,
    val id: String,
    val type: String, // 'item', 'fluid', 'recipe', 'entity', 'technology'
    val name: String,
    val localized_name: String,
    val category: String?
)

@Entity(tableName = "manifest")
data class ManifestEntity(
    @PrimaryKey val id: Int = 1,
    val factorio_version: String,
    val space_age_enabled: Int,
    val schema_version: Int,
    val generated_at: String,
    val data_hash: String
)