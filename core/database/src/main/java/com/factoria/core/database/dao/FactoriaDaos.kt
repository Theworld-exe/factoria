package com.factoria.core.database.dao

import androidx.room.*
import com.factoria.core.database.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Query("SELECT * FROM items ORDER BY item_group, subgroup, name")
    fun getAllItems(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: String): ItemEntity?

    @Query("SELECT * FROM items WHERE item_group = :group")
    fun getItemsByGroup(group: String): Flow<List<ItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ItemEntity>)
}

@Dao
interface FluidDao {
    @Query("SELECT * FROM fluids ORDER BY name")
    fun getAllFluids(): Flow<List<FluidEntity>>

    @Query("SELECT * FROM fluids WHERE id = :id LIMIT 1")
    suspend fun getFluidById(id: String): FluidEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(fluids: List<FluidEntity>)
}

@Dao
interface RecipeDao {
    @Transaction
    @Query("SELECT * FROM recipes ORDER BY name")
    fun getAllRecipes(): Flow<List<RecipeWithDetails>>

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id LIMIT 1")
    suspend fun getRecipeById(id: String): RecipeWithDetails?

    @Transaction
    @Query("""
        SELECT * FROM recipes WHERE id IN (
            SELECT DISTINCT recipe_id FROM recipe_ingredients WHERE target_id = :resourceId
        )
    """)
    fun getRecipesUsing(resourceId: String): Flow<List<RecipeWithDetails>>

    @Transaction
    @Query("""
        SELECT * FROM recipes WHERE id IN (
            SELECT DISTINCT recipe_id FROM recipe_products WHERE target_id = :resourceId
        )
    """)
    fun getRecipesProducing(resourceId: String): Flow<List<RecipeWithDetails>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipes(recipes: List<RecipeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredients(ingredients: List<RecipeIngredientEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<RecipeProductEntity>)
}

@Dao
interface EntityDao {
    @Transaction
    @Query("SELECT * FROM entities ORDER BY name")
    fun getAllEntities(): Flow<List<EntityWithCategories>>

    @Transaction
    @Query("SELECT * FROM entities WHERE id = :id LIMIT 1")
    suspend fun getEntityById(id: String): EntityWithCategories?

    @Transaction
    @Query("""
        SELECT * FROM entities WHERE id IN (
            SELECT entity_id FROM entity_crafting_categories WHERE category = :category
        )
    """)
    fun getEntitiesForCategory(category: String): Flow<List<EntityWithCategories>>
}

@Dao
interface TechnologyDao {
    @Transaction
    @Query("SELECT * FROM technologies ORDER BY name")
    fun getAllTechnologies(): Flow<List<TechnologyWithDetails>>

    @Transaction
    @Query("SELECT * FROM technologies WHERE id = :id LIMIT 1")
    suspend fun getTechnologyById(id: String): TechnologyWithDetails?

    @Transaction
    @Query("""
        SELECT * FROM technologies WHERE id IN (
            SELECT technology_id FROM technology_prerequisites WHERE prerequisite_id = :techId
        )
    """)
    fun getTechnologiesDependentOn(techId: String): Flow<List<TechnologyWithDetails>>
}

@Dao
interface SearchDao {
    @Query("""
        SELECT rowid, * FROM search_index
        WHERE search_index MATCH :query
        LIMIT 50
    """)
    suspend fun search(query: String): List<SearchFtsEntity>

    @Query("""
        SELECT rowid, * FROM search_index
        WHERE type = :type AND search_index MATCH :query
        LIMIT 50
    """)
    suspend fun searchByType(query: String, type: String): List<SearchFtsEntity>
}

@Dao
interface ManifestDao {
    @Query("SELECT * FROM manifest WHERE id = 1 LIMIT 1")
    suspend fun getManifest(): ManifestEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(manifest: ManifestEntity)
}
