package com.factoria.core.database.dao

import androidx.room.*
import com.factoria.core.database.entity.ChecklistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChecklistDao {
    @Query("SELECT * FROM checklist ORDER BY is_completed ASC, created_at DESC")
    fun getAllTasks(): Flow<List<ChecklistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: ChecklistEntity): Long

    @Query("UPDATE checklist SET is_completed = :isCompleted WHERE id = :id")
    suspend fun updateTaskStatus(id: Long, isCompleted: Boolean)

    @Query("DELETE FROM checklist WHERE id = :id")
    suspend fun deleteTask(id: Long)
}
