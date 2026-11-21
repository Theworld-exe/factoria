package com.factoria.core.database.entity

import androidx.room.*
import com.factoria.core.model.Module
import com.factoria.core.model.ModuleCategory
import com.factoria.core.model.Quality

@Entity(tableName = "modules")
data class ModuleEntity(
    @PrimaryKey val id: String,
    val name: String,
    val tier: Int,
    val category: String,
    val speed_bonus: Double = 0.0,
    val productivity_bonus: Double = 0.0,
    val energy_bonus: Double = 0.0,
    val pollution_bonus: Double = 0.0,
    val quality_bonus: Double = 0.0
) {
    fun toDomainModel(): Module = Module(
        id = id,
        name = name,
        tier = tier,
        category = when (category.lowercase()) {
            "productivity" -> ModuleCategory.PRODUCTIVITY
            "speed" -> ModuleCategory.SPEED
            "efficiency" -> ModuleCategory.EFFICIENCY
            "quality" -> ModuleCategory.QUALITY
            else -> ModuleCategory.SPEED
        },
        speedBonus = speed_bonus,
        productivityBonus = productivity_bonus,
        energyBonus = energy_bonus,
        pollutionBonus = pollution_bonus,
        qualityBonus = quality_bonus
    )
}

@Entity(
    tableName = "module_limitations",
    primaryKeys = ["module_id", "recipe_id"]
)
data class ModuleLimitationEntity(
    val module_id: String,
    val recipe_id: String
)

@Entity(tableName = "qualities")
data class QualityEntity(
    @PrimaryKey val id: String,
    val name: String,
    val tier: Int,
    val speed_modifier: Double,
    val health_modifier: Double,
    val module_effect_modifier: Double
) {
    fun toDomainModel(): Quality = Quality(
        id = id,
        name = name,
        tier = tier,
        speedModifier = speed_modifier,
        healthModifier = health_modifier,
        moduleEffectModifier = module_effect_modifier
    )
}
