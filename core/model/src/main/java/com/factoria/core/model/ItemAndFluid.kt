package com.factoria.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class TargetType {
    ITEM,
    FLUID
}

@Serializable
data class Item(
    val id: String,
    val name: String,
    val localizedName: String,
    val itemGroup: String,
    val subgroup: String,
    val stackSize: Int,
    val fuelValueJoules: Double = 0.0,
    val fuelCategory: String? = null,
    val fuelEmissionsMultiplier: Double = 1.0,
    val spoilageTimeSeconds: Double = 0.0,
    val iconPath: String? = null
) {
    val isFuel: Boolean get() = fuelValueJoules > 0.0
    val hasSpoilage: Boolean get() = spoilageTimeSeconds > 0.0
}

@Serializable
data class Fluid(
    val id: String,
    val name: String,
    val localizedName: String,
    val defaultTemp: Double,
    val maxTemp: Double? = null,
    val heatCapacityJoules: Double,
    val fuelValueJoules: Double = 0.0,
    val emissionsMultiplier: Double = 1.0,
    val iconPath: String? = null
)
