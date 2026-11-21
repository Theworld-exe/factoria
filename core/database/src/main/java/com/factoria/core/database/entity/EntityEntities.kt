package com.factoria.core.database.entity

import androidx.room.*
import com.factoria.core.model.Entity as DomainEntity
import com.factoria.core.model.EntityType

@Entity(tableName = "entities")
data class EntityEntity(
    @PrimaryKey val id: String,
    val name: String,
    val localized_name: String,
    val type: String,
    val crafting_speed: Double = 1.0,
    val mining_speed: Double = 1.0,
    val energy_usage: Double = 0.0,
    val drain_energy: Double = 0.0,
    val emissions_per_minute: Double = 0.0,
    val module_slots: Int = 0,
    val pollution_multiplier: Double = 1.0,
    val health: Double = 100.0,
    val icon_path: String? = null
)

@Entity(
    tableName = "entity_crafting_categories",
    primaryKeys = ["entity_id", "category"],
    indices = [Index("category")]
)
data class EntityCraftingCategoryEntity(
    val entity_id: String,
    val category: String
)

data class EntityWithCategories(
    @Embedded val entity: EntityEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "entity_id"
    )
    val categories: List<EntityCraftingCategoryEntity>
) {
    fun toDomainModel(): DomainEntity = DomainEntity(
        id = entity.id,
        name = entity.name,
        localizedName = entity.localized_name,
        type = EntityType.fromPrototypeType(entity.type),
        craftingSpeed = entity.crafting_speed,
        miningSpeed = entity.mining_speed,
        energyUsageWatts = entity.energy_usage,
        drainWatts = entity.drain_energy,
        emissionsPerMinute = entity.emissions_per_minute,
        moduleSlots = entity.module_slots,
        pollutionMultiplier = entity.pollution_multiplier,
        health = entity.health,
        craftingCategories = categories.map { it.category },
        iconPath = entity.icon_path
    )
}
