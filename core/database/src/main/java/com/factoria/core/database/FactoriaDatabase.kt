package com.factoria.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.factoria.core.database.dao.*
import com.factoria.core.database.entity.*

@Database(
    entities = [
        ItemEntity::class,
        FluidEntity::class,
        RecipeEntity::class,
        RecipeIngredientEntity::class,
        RecipeProductEntity::class,
        EntityEntity::class,
        EntityCraftingCategoryEntity::class,
        ModuleEntity::class,
        ModuleLimitationEntity::class,
        QualityEntity::class,
        TechnologyEntity::class,
        TechnologyIngredientEntity::class,
        TechnologyPrerequisiteEntity::class,
        TechnologyUnlockEntity::class,
        SearchFtsEntity::class,
        ManifestEntity::class,
        SavedProfileEntity::class,
        ChecklistEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class FactoriaDatabase : RoomDatabase() {

    abstract fun itemDao(): ItemDao
    abstract fun fluidDao(): FluidDao
    abstract fun recipeDao(): RecipeDao
    abstract fun entityDao(): EntityDao
    abstract fun technologyDao(): TechnologyDao
    abstract fun searchDao(): SearchDao
    abstract fun manifestDao(): ManifestDao
    abstract fun savedProfileDao(): SavedProfileDao
    abstract fun checklistDao(): ChecklistDao

    companion object {
        private const val DB_NAME = "factoria_data_2.0.db"
        private const val ASSET_DB_PATH = "databases/factorio_data_2.0.db"

        @Volatile
        private var INSTANCE: FactoriaDatabase? = null

        fun getInstance(context: Context): FactoriaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FactoriaDatabase::class.java,
                    DB_NAME
                )
                    .createFromAsset(ASSET_DB_PATH)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun createInMemory(context: Context): FactoriaDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                FactoriaDatabase::class.java
            ).allowMainThreadQueries().build()
        }
    }
}
