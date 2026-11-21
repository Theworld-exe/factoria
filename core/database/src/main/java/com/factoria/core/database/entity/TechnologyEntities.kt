package com.factoria.core.database.entity

import androidx.room.*
import com.factoria.core.model.SciencePackCost
import com.factoria.core.model.Technology

@Entity(tableName = "technologies")
data class TechnologyEntity(
    @PrimaryKey val id: String,
    val name: String,
    val localized_name: String,
    val unit_count: Int,
    val unit_time: Double,
    val level_formula: String? = null,
    val max_level: Int? = null,
    val icon_path: String? = null
)

@Entity(
    tableName = "technology_ingredients",
    primaryKeys = ["technology_id", "science_pack_id"]
)
data class TechnologyIngredientEntity(
    val technology_id: String,
    val science_pack_id: String,
    val amount: Int
)

@Entity(
    tableName = "technology_prerequisites",
    primaryKeys = ["technology_id", "prerequisite_id"],
    indices = [Index("technology_id"), Index("prerequisite_id")]
)
data class TechnologyPrerequisiteEntity(
    val technology_id: String,
    val prerequisite_id: String
)

@Entity(
    tableName = "technology_unlocks",
    primaryKeys = ["technology_id", "recipe_id"],
    indices = [Index("technology_id"), Index("recipe_id")]
)
data class TechnologyUnlockEntity(
    val technology_id: String,
    val recipe_id: String
)

data class TechnologyWithDetails(
    @Embedded val technology: TechnologyEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "technology_id"
    )
    val ingredients: List<TechnologyIngredientEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "technology_id"
    )
    val prerequisites: List<TechnologyPrerequisiteEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "technology_id"
    )
    val unlocks: List<TechnologyUnlockEntity>
) {
    fun toDomainModel(): Technology = Technology(
        id = technology.id,
        name = technology.name,
        localizedName = technology.localized_name,
        unitCount = technology.unit_count,
        unitTimeSeconds = technology.unit_time,
        levelFormula = technology.level_formula,
        maxLevel = technology.max_level,
        sciencePacks = ingredients.map { SciencePackCost(it.science_pack_id, it.amount) },
        prerequisiteIds = prerequisites.map { it.prerequisite_id },
        unlockedRecipeIds = unlocks.map { it.recipe_id },
        iconPath = technology.icon_path
    )
}
