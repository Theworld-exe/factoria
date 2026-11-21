package com.factoria.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class RateUnit(val label: String, val multiplierFromPerSecond: Double) {
    PER_SECOND("/s", 1.0),
    PER_MINUTE("/m", 60.0),
    BELTS_YELLOW("Yellow Belts (15/s)", 1.0 / 15.0),
    BELTS_RED("Red Belts (30/s)", 1.0 / 30.0),
    BELTS_BLUE("Blue Belts (45/s)", 1.0 / 45.0),
    BELTS_TURBO("Turbo Belts (60/s)", 1.0 / 60.0);

    fun toRatePerSecond(value: Double): Double = value / multiplierFromPerSecond
    fun fromRatePerSecond(ratePerSec: Double): Double = ratePerSec * multiplierFromPerSecond
}

@Serializable
data class ProductionTarget(
    val targetId: String,
    val type: TargetType,
    val ratePerSecond: Double
)

@Serializable
enum class SolverObjective(val displayName: String, val description: String) {
    MIN_MACHINES("Minimize Machines", "Minimizes total factory footprint and machine count"),
    MIN_RAW_RESOURCES("Minimize Raw Resources", "Conserves base ores, crude oil, and raw materials"),
    MIN_POWER("Minimize Power", "Reduces total energy consumption (MW)"),
    MIN_POLLUTION("Minimize Pollution", "Reduces emissions per minute to minimize biter aggression")
}

@Serializable
data class MachineConfiguration(
    val recipeId: String,
    val entityId: String,
    val quality: QualityTier = QualityTier.NORMAL,
    val modules: List<String> = emptyList(),
    val beaconCount: Int = 0,
    val beaconModuleId: String? = null,
    val beaconQuality: QualityTier = QualityTier.NORMAL
)

@Serializable
enum class SolverStatus {
    OPTIMAL,
    INFEASIBLE,
    UNBOUNDED,
    ERROR
}

@Serializable
data class RecipeRate(
    val recipeId: String,
    val recipeName: String,
    val craftsPerSecond: Double,
    val machinesRequired: Double,
    val roundedMachines: Int,
    val machineEntityId: String,
    val machineQuality: QualityTier,
    val powerWatts: Double,
    val pollutionPerMinute: Double,
    val effectiveCraftingSpeed: Double,
    val effectiveProductivityBonus: Double
)

@Serializable
data class ItemRate(
    val targetId: String,
    val targetName: String,
    val type: TargetType,
    val netRatePerSecond: Double,
    val isRawInput: Boolean,
    val isTarget: Boolean,
    val isSurplus: Boolean
)

@Serializable
data class SolverSolution(
    val status: SolverStatus,
    val recipeRates: List<RecipeRate> = emptyList(),
    val itemRates: List<ItemRate> = emptyList(),
    val totalPowerWatts: Double = 0.0,
    val totalPollutionPerMinute: Double = 0.0,
    val rawResources: Map<String, Double> = emptyMap(),
    val surplusProducts: Map<String, Double> = emptyMap(),
    val catalystStartupBuffers: Map<String, Double> = emptyMap(),
    val warnings: List<String> = emptyList(),
    val solvingTimeMs: Long = 0L
)
