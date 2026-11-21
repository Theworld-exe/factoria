package com.factoria.core.model

import kotlinx.serialization.Serializable

@Serializable
data class SciencePackCost(
    val sciencePackId: String,
    val amount: Int
)

@Serializable
data class Technology(
    val id: String,
    val name: String,
    val localizedName: String,
    val unitCount: Int,
    val unitTimeSeconds: Double,
    val levelFormula: String? = null,
    val maxLevel: Int? = 1,
    val sciencePacks: List<SciencePackCost> = emptyList(),
    val prerequisiteIds: List<String> = emptyList(),
    val unlockedRecipeIds: List<String> = emptyList(),
    val iconPath: String? = null
) {
    val isInfinite: Boolean get() = levelFormula != null || maxLevel == null
    val totalSciencePacksPerUnit: Int get() = sciencePacks.sumOf { it.amount }
    val totalDirectPacks: Map<String, Long> get() = sciencePacks.associate { it.sciencePackId to (it.amount.toLong() * unitCount) }
}
