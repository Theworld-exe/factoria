package com.factoria.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class QualityTier(val level: Int, val displayName: String, val colorHex: String) {
    NORMAL(0, "Normal", "#808080"),
    UNCOMMON(1, "Uncommon", "#2ECC71"),
    RARE(2, "Rare", "#3498DB"),
    EPIC(3, "Epic", "#9B59B6"),
    LEGENDARY(4, "Legendary", "#F39C12");

    val craftingSpeedModifier: Double get() = 1.0 + (level * 0.3)
    val healthModifier: Double get() = 1.0 + (level * 0.3)
    val moduleEffectModifier: Double get() = 1.0 + (level * 0.3)

    companion object {
        fun fromLevel(level: Int): QualityTier =
            entries.find { it.level == level } ?: NORMAL

        fun fromName(name: String?): QualityTier =
            if (name == null) NORMAL else entries.find { it.name.equals(name, ignoreCase = true) } ?: NORMAL
    }
}

@Serializable
data class Quality(
    val id: String,
    val name: String,
    val tier: Int,
    val speedModifier: Double,
    val healthModifier: Double,
    val moduleEffectModifier: Double
)

@Serializable
enum class ModuleCategory {
    SPEED,
    PRODUCTIVITY,
    EFFICIENCY,
    QUALITY
}

@Serializable
data class Module(
    val id: String,
    val name: String,
    val tier: Int,
    val category: ModuleCategory,
    val speedBonus: Double = 0.0,
    val productivityBonus: Double = 0.0,
    val energyBonus: Double = 0.0,
    val pollutionBonus: Double = 0.0,
    val qualityBonus: Double = 0.0
)

@Serializable
enum class EntityType {
    ASSEMBLING_MACHINE,
    FURNACE,
    ROCKET_SILO,
    MINING_DRILL,
    LAB,
    BEACON,
    BOILER,
    GENERATOR,
    REACTOR,
    TRANSPORT_BELT,
    UNDERGROUND_BELT,
    SPLITTER,
    INSERTER,
    PIPE,
    PUMP,
    STORAGE_TANK,
    LOCOMOTIVE,
    CARGO_WAGON,
    FLUID_WAGON,
    CONTAINER,
    OTHER;

    companion object {
        fun fromPrototypeType(raw: String): EntityType = when (raw.lowercase()) {
            "assembling-machine" -> ASSEMBLING_MACHINE
            "furnace" -> FURNACE
            "rocket-silo" -> ROCKET_SILO
            "mining-drill" -> MINING_DRILL
            "lab" -> LAB
            "beacon" -> BEACON
            "boiler" -> BOILER
            "generator" -> GENERATOR
            "reactor" -> REACTOR
            "transport-belt" -> TRANSPORT_BELT
            "underground-belt" -> UNDERGROUND_BELT
            "splitter" -> SPLITTER
            "inserter" -> INSERTER
            "pipe" -> PIPE
            "pump" -> PUMP
            "storage-tank" -> STORAGE_TANK
            "locomotive" -> LOCOMOTIVE
            "cargo-wagon" -> CARGO_WAGON
            "fluid-wagon" -> FLUID_WAGON
            "container", "logistic-container" -> CONTAINER
            else -> OTHER
        }
    }
}

@Serializable
data class Entity(
    val id: String,
    val name: String,
    val localizedName: String,
    val type: EntityType,
    val craftingSpeed: Double = 1.0,
    val miningSpeed: Double = 1.0,
    val energyUsageWatts: Double = 0.0,
    val drainWatts: Double = 0.0,
    val emissionsPerMinute: Double = 0.0,
    val moduleSlots: Int = 0,
    val pollutionMultiplier: Double = 1.0,
    val health: Double = 100.0,
    val craftingCategories: List<String> = emptyList(),
    val iconPath: String? = null
)
