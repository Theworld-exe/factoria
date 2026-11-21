package com.factoria.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "checklist")
data class ChecklistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val planet: String = "Nauvis",
    val is_completed: Boolean = false,
    val created_at: Long = System.currentTimeMillis()
)
