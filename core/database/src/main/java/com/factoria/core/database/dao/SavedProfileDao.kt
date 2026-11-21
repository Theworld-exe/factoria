package com.factoria.core.database.dao

import androidx.room.*
import com.factoria.core.database.entity.SavedProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedProfileDao {
    @Query("SELECT * FROM saved_profiles ORDER BY created_at DESC")
    fun getAllProfiles(): Flow<List<SavedProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: SavedProfileEntity): Long

    @Query("DELETE FROM saved_profiles WHERE id = :id")
    suspend fun deleteProfile(id: Long)
}
