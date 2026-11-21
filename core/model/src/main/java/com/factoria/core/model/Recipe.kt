package com.factoria.core.model

import kotlinx.serialization.Serializable

@Serializable
data class RecipeIngredient(
    val type: TargetType,
    val targetId: String,
    val amount: Double,
    val temperature: Double? = null,
    val minimumTemperature: Double? = null,
    val maximumTemperature: Double? = null
)

@Serializable
data class RecipeProduct(
    val type: TargetType,
    val targetId: String,
    val amount: Double,
    val amountMin: Double = amount,
    val amountMax: Double = amount,
    val probability: Double = 1.0,
    val catalystAmount: Double = 0.0,
    val temperature: Double? = null
) {
    /**
     * Mathematical expected yield per craft: E = probability * (min + max) / 2
     */
    val expectedYield: Double
        get() = probability * ((amountMin + amountMax) / 2.0)

    /**
     * Net yield accounting for internal recipe catalyst retention
     */
    val netExpectedYield: Double
        get() = expectedYield - catalystAmount
}

@Serializable
data class Recipe(
    val id: String,
    val name: String,
    val localizedName: String,
    val category: String,
    val craftingTime: Double,
    val isHandCraftable: Boolean,
    val isCatalystLoop: Boolean = false,
    val isRecycling: Boolean = false,
    val unlockedByTechId: String? = null,
    val subgroup: String? = null,
    val ingredients: List<RecipeIngredient> = emptyList(),
    val products: List<RecipeProduct> = emptyList()
) {
    /**
     * Returns true if this recipe contains cyclic catalyst loops (e.g. Kovarex enrichment)
     */
    val hasCatalysts: Boolean
        get() = isCatalystLoop || products.any { it.catalystAmount > 0.0 }
}
