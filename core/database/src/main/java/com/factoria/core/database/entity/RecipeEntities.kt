package com.factoria.core.database.entity

import androidx.room.*
import com.factoria.core.model.Recipe
import com.factoria.core.model.RecipeIngredient
import com.factoria.core.model.RecipeProduct
import com.factoria.core.model.TargetType

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val localized_name: String,
    val category: String,
    val crafting_time: Double,
    val is_craftable_in_hand: Int,
    val is_catalyst_loop: Int = 0,
    val is_recycling: Int = 0,
    val unlocked_by_tech_id: String? = null,
    val subgroup: String? = null
)

@Entity(
    tableName = "recipe_ingredients",
    indices = [Index("recipe_id"), Index("target_id")]
)
data class RecipeIngredientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipe_id: String,
    val type: String, // 'item' or 'fluid'
    val target_id: String,
    val amount: Double,
    val temperature: Double? = null,
    val minimum_temperature: Double? = null,
    val maximum_temperature: Double? = null
) {
    fun toDomainModel(): RecipeIngredient = RecipeIngredient(
        type = if (type == "fluid") TargetType.FLUID else TargetType.ITEM,
        targetId = target_id,
        amount = amount,
        temperature = temperature,
        minimumTemperature = minimum_temperature,
        maximumTemperature = maximum_temperature
    )
}

@Entity(
    tableName = "recipe_products",
    indices = [Index("recipe_id"), Index("target_id")]
)
data class RecipeProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipe_id: String,
    val type: String,
    val target_id: String,
    val amount: Double?,
    val amount_min: Double?,
    val amount_max: Double?,
    val probability: Double = 1.0,
    val catalyst_amount: Double = 0.0,
    val temperature: Double? = null
) {
    fun toDomainModel(): RecipeProduct {
        val finalAmount = amount ?: (((amount_min ?: 1.0) + (amount_max ?: 1.0)) / 2.0)
        return RecipeProduct(
            type = if (type == "fluid") TargetType.FLUID else TargetType.ITEM,
            targetId = target_id,
            amount = finalAmount,
            amountMin = amount_min ?: finalAmount,
            amountMax = amount_max ?: finalAmount,
            probability = probability,
            catalystAmount = catalyst_amount,
            temperature = temperature
        )
    }
}

data class RecipeWithDetails(
    @Embedded val recipe: RecipeEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "recipe_id"
    )
    val ingredients: List<RecipeIngredientEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "recipe_id"
    )
    val products: List<RecipeProductEntity>
) {
    fun toDomainModel(): Recipe = Recipe(
        id = recipe.id,
        name = recipe.name,
        localizedName = recipe.localized_name,
        category = recipe.category,
        craftingTime = recipe.crafting_time,
        isHandCraftable = recipe.is_craftable_in_hand == 1,
        isCatalystLoop = recipe.is_catalyst_loop == 1,
        isRecycling = recipe.is_recycling == 1,
        unlockedByTechId = recipe.unlocked_by_tech_id,
        subgroup = recipe.subgroup,
        ingredients = ingredients.map { it.toDomainModel() },
        products = products.map { it.toDomainModel() }
    )
}
