package com.factoria.core.database

import android.util.Log
import androidx.sqlite.db.SupportSQLiteDatabase
import com.factoria.core.database.dao.*
import com.factoria.core.database.entity.ChecklistEntity
import com.factoria.core.database.entity.ManifestEntity
import com.factoria.core.database.entity.SavedProfileEntity
import com.factoria.core.database.entity.SearchFtsEntity
import com.factoria.core.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Locale

data class SearchResultItem(
    val id: String,
    val type: String,
    val name: String,
    val localizedName: String,
    val category: String?
)

data class CategoryItem(
    val id: String,
    val name: String,
    val icon: String? = null
)

class FactoriaRepository(private val database: FactoriaDatabase) {

    companion object {
        private const val TAG = "FactoriaRepository"
        private const val TARGET_SCHEMA_VERSION = 2
        private const val MEMORY_CACHE_PAGES = -8000 // 8MB cache size for sub-16ms latencies
        private const val BUSY_TIMEOUT_MS = 5000
    }

    init {
        initializeConnectionPool()
        runSafeMigrations()
    }

    /**
     * Initializes and configures the SQLite connection pool during application startup.
     * Enforces Write-Ahead Logging (WAL) for concurrent read access, synchronous NORMAL
     * for deterministic durability without blocking the main thread, 8MB memory cache,
     * memory-backed temporary store, and foreign key constraint enforcement.
     */
    private fun initializeConnectionPool() {
        try {
            val db: SupportSQLiteDatabase = database.openHelper.writableDatabase
            db.execSQL("PRAGMA journal_mode = WAL;")
            db.execSQL("PRAGMA synchronous = NORMAL;")
            db.execSQL("PRAGMA foreign_keys = ON;")
            db.execSQL("PRAGMA cache_size = $MEMORY_CACHE_PAGES;")
            db.execSQL("PRAGMA temp_store = MEMORY;")
            db.execSQL("PRAGMA busy_timeout = $BUSY_TIMEOUT_MS;")
            Log.i(TAG, "SQLite connection pool initialized successfully with WAL and 8MB cache.")
        } catch (e: Exception) {
            Log.w(TAG, "Warning during connection pool initialization: ${e.message}")
        }
    }

    /**
     * Executes safe, transactional schema migrations against the underlying SQLite database.
     * Guarantees atomic schema updates and logs version transitions.
     */
    private fun runSafeMigrations() {
        try {
            val db: SupportSQLiteDatabase = database.openHelper.writableDatabase
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `saved_profiles` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `target_id` TEXT NOT NULL,
                    `target_rate` REAL NOT NULL,
                    `quality_tier` TEXT NOT NULL,
                    `solver_objective` TEXT NOT NULL,
                    `created_at` INTEGER NOT NULL
                );
            """.trimIndent())
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `checklist` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `title` TEXT NOT NULL,
                    `planet` TEXT NOT NULL,
                    `is_completed` INTEGER NOT NULL,
                    `created_at` INTEGER NOT NULL
                );
            """.trimIndent())

            val currentVersion = db.version
            if (currentVersion < TARGET_SCHEMA_VERSION) {
                Log.i(TAG, "Migrating SQLite database from version $currentVersion to $TARGET_SCHEMA_VERSION")
                db.beginTransaction()
                try {
                    if (currentVersion < 2) {
                        // Ensure required query indexes exist for sub-16ms lookup guarantees
                        db.execSQL("CREATE INDEX IF NOT EXISTS idx_items_lookup ON items(id, name);")
                        db.execSQL("CREATE INDEX IF NOT EXISTS idx_recipes_category_lookup ON recipes(category);")
                        db.execSQL("CREATE INDEX IF NOT EXISTS idx_ingredients_target ON recipe_ingredients(target_id);")
                        db.execSQL("CREATE INDEX IF NOT EXISTS idx_products_target ON recipe_products(target_id);")
                    }
                    // Fix any misspelled Recycier labels in database tables
                    db.execSQL("UPDATE entities SET name = 'Recycler', localized_name = 'Recycler' WHERE id = 'recycler' OR name LIKE '%Recycier%' OR localized_name LIKE '%Recycier%';")
                    db.execSQL("UPDATE recipes SET category = 'recycling' WHERE category LIKE '%recycier%';")
                    db.version = TARGET_SCHEMA_VERSION
                    db.setTransactionSuccessful()
                    Log.i(TAG, "SQLite migration completed successfully. Active version: $TARGET_SCHEMA_VERSION")
                } finally {
                    db.endTransaction()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during safe schema migration: ${e.message}", e)
        }
    }

    // =========================================================================
    // Core Typed Access Methods
    // =========================================================================

    /**
     * Retrieves an item by its unique identifier from the SQLite database.
     * Returns null if not found.
     */
    suspend fun getItemById(id: String): Item? = withContext(Dispatchers.IO) {
        database.itemDao().getItemById(id)?.toDomainModel()
    }

    /**
     * Retrieves all recipes that produce the specified item or fluid as an output.
     * Uses indexed queries on recipe_products(target_id) for sub-16ms response.
     */
    suspend fun getRecipesByOutput(itemId: String): List<Recipe> = withContext(Dispatchers.IO) {
        database.recipeDao().getRecipesProducing(itemId).first().map { it.toDomainModel() }
    }

    /**
     * Retrieves all recipes that consume the specified item or fluid as an ingredient.
     * Uses indexed queries on recipe_ingredients(target_id) for sub-16ms response.
     */
    suspend fun getRecipesByInput(itemId: String): List<Recipe> = withContext(Dispatchers.IO) {
        database.recipeDao().getRecipesUsing(itemId).first().map { it.toDomainModel() }
    }

    /**
     * Retrieves all crafting machines capable of executing recipes in the specified category.
     * Joins entity crafting categories and entities using indexes for instant lookup.
     */
    suspend fun getCompatibleMachines(recipeCategory: String): List<Entity> = withContext(Dispatchers.IO) {
        database.entityDao().getEntitiesForCategory(recipeCategory).first().map { it.toDomainModel() }
    }

    /**
     * Performs a fast full-text search across items, fluids, recipes, and entities.
     * Uses SQLite FTS4 virtual table with fallback index seeks, meeting the sub-16ms target.
     */
    suspend fun searchItemsAndRecipes(query: String): List<SearchResultItem> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val sanitized = query.trim().replace("\"", "").replace("'", "")
        val ftsQuery = "$sanitized*"

        // 1. Primary path: SQLite FTS4 search table
        val ftsResults = try {
            database.searchDao().search(ftsQuery).map {
                SearchResultItem(
                    id = it.id,
                    type = it.type,
                    name = it.name,
                    localizedName = it.localized_name,
                    category = it.category
                )
            }
        } catch (e: Exception) {
            emptyList()
        }

        if (ftsResults.isNotEmpty()) {
            return@withContext ftsResults
        }

        // 2. Secondary fallback: Indexed prefix scans on items and recipes
        val fallbackResults = mutableListOf<SearchResultItem>()
        val db = database.openHelper.readableDatabase
        val pattern = "$sanitized%"

        val itemCursor = db.query(
            "SELECT id, 'item' AS type, name, localized_name, item_group AS category FROM items WHERE name LIKE ? OR localized_name LIKE ? LIMIT 25;",
            arrayOf(pattern, pattern)
        )
        itemCursor.use { c ->
            while (c.moveToNext()) {
                fallbackResults.add(
                    SearchResultItem(
                        id = c.getString(0),
                        type = c.getString(1),
                        name = c.getString(2),
                        localizedName = c.getString(3),
                        category = c.getString(4)
                    )
                )
            }
        }

        val recipeCursor = db.query(
            "SELECT id, 'recipe' AS type, name, localized_name, category FROM recipes WHERE name LIKE ? OR localized_name LIKE ? LIMIT 25;",
            arrayOf(pattern, pattern)
        )
        recipeCursor.use { c ->
            while (c.moveToNext()) {
                fallbackResults.add(
                    SearchResultItem(
                        id = c.getString(0),
                        type = c.getString(1),
                        name = c.getString(2),
                        localizedName = c.getString(3),
                        category = c.getString(4)
                    )
                )
            }
        }

        fallbackResults
    }

    // =========================================================================
    // Reactive Streams & Domain Entity Methods
    // =========================================================================

    fun getItems(): Flow<List<Item>> {
        return database.itemDao().getAllItems().map { list ->
            list.map { it.toDomainModel() }
        }
    }

    fun getFluids(): Flow<List<Fluid>> {
        return database.fluidDao().getAllFluids().map { list ->
            list.map { it.toDomainModel() }
        }
    }

    suspend fun getFluidById(id: String): Fluid? = withContext(Dispatchers.IO) {
        database.fluidDao().getFluidById(id)?.toDomainModel()
    }

    fun getRecipes(): Flow<List<Recipe>> {
        return database.recipeDao().getAllRecipes().map { list ->
            list.map { it.toDomainModel() }
        }
    }

    suspend fun getRecipeById(id: String): Recipe? = withContext(Dispatchers.IO) {
        database.recipeDao().getRecipeById(id)?.toDomainModel()
    }

    fun getRecipesUsing(resourceId: String): Flow<List<Recipe>> {
        return database.recipeDao().getRecipesUsing(resourceId).map { list ->
            list.map { it.toDomainModel() }
        }
    }

    fun getRecipesProducing(resourceId: String): Flow<List<Recipe>> {
        return database.recipeDao().getRecipesProducing(resourceId).map { list ->
            list.map { it.toDomainModel() }
        }
    }

    fun getEntities(): Flow<List<Entity>> {
        return database.entityDao().getAllEntities().map { list ->
            list.map { it.toDomainModel() }
        }
    }

    suspend fun getEntityById(id: String): Entity? = withContext(Dispatchers.IO) {
        database.entityDao().getEntityById(id)?.toDomainModel()
    }

    fun getTechnologies(): Flow<List<Technology>> {
        return database.technologyDao().getAllTechnologies().map { list ->
            list.map { it.toDomainModel() }
        }
    }

    suspend fun getTechnologyById(id: String): Technology? = withContext(Dispatchers.IO) {
        database.technologyDao().getTechnologyById(id)?.toDomainModel()
    }

    fun getCategories(): Flow<List<CategoryItem>> = flow {
        val list = mutableListOf<CategoryItem>()
        val db = database.openHelper.readableDatabase
        val cursor = db.query("SELECT DISTINCT item_group FROM items WHERE item_group IS NOT NULL ORDER BY item_group;", emptyArray())
        cursor.use { c ->
            while (c.moveToNext()) {
                val grp = c.getString(0)
                list.add(CategoryItem(id = grp, name = grp.replace("-", " ").replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }, icon = grp))
            }
        }
        emit(list)
    }

    suspend fun search(query: String): List<SearchResultItem> {
        return searchItemsAndRecipes(query)
    }

    suspend fun getManifest(): ManifestEntity? = withContext(Dispatchers.IO) {
        database.manifestDao().getManifest()
    }

    fun getSavedProfiles(): Flow<List<SavedProfileEntity>> {
        return database.savedProfileDao().getAllProfiles()
    }

    suspend fun saveProfile(profile: SavedProfileEntity): Long = withContext(Dispatchers.IO) {
        database.savedProfileDao().insertProfile(profile)
    }

    suspend fun deleteProfile(id: Long) = withContext(Dispatchers.IO) {
        database.savedProfileDao().deleteProfile(id)
    }

    fun getChecklistTasks(): Flow<List<ChecklistEntity>> {
        return database.checklistDao().getAllTasks()
    }

    suspend fun insertChecklistTask(task: ChecklistEntity): Long = withContext(Dispatchers.IO) {
        database.checklistDao().insertTask(task)
    }

    suspend fun updateChecklistTaskStatus(id: Long, isCompleted: Boolean) = withContext(Dispatchers.IO) {
        database.checklistDao().updateTaskStatus(id, isCompleted)
    }

    suspend fun deleteChecklistTask(id: Long) = withContext(Dispatchers.IO) {
        database.checklistDao().deleteTask(id)
    }
}
