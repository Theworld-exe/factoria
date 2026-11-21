package com.factoria.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_profiles")
data class SavedProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val target_id: String,
    val target_rate: Double,
    val quality_tier: String,
    val solver_objective: String,
    val created_at: Long = System.currentTimeMillis()
)
