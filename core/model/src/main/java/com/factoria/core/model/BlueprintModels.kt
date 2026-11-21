package com.factoria.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BlueprintStringWrapper(
    val blueprint: BlueprintData? = null,
    @SerialName("blueprint_book")
    val blueprintBook: BlueprintBookData? = null,
    @SerialName("upgrade_planner")
    val upgradePlanner: UpgradePlannerData? = null,
    @SerialName("deconstruction_planner")
    val deconstructionPlanner: DeconstructionPlannerData? = null
)

@Serializable
data class BlueprintData(
    val item: String = "blueprint",
    val label: String? = null,
    val description: String? = null,
    val version: Long? = null,
    val icons: List<BlueprintIcon> = emptyList(),
    val entities: List<BlueprintEntity> = emptyList(),
    val tiles: List<BlueprintTile> = emptyList(),
    val wires: List<List<Long>>? = null // Factorio 2.0 wire connections format
)

@Serializable
data class BlueprintBookData(
    val item: String = "blueprint-book",
    val label: String? = null,
    val description: String? = null,
    val version: Long? = null,
    val icons: List<BlueprintIcon> = emptyList(),
    val blueprints: List<BlueprintBookEntry> = emptyList(),
    @SerialName("active_index")
    val activeIndex: Int = 0
)

@Serializable
data class BlueprintBookEntry(
    val index: Int,
    val blueprint: BlueprintData? = null,
    @SerialName("blueprint_book")
    val blueprintBook: BlueprintBookData? = null
)

@Serializable
data class UpgradePlannerData(
    val item: String = "upgrade-planner",
    val label: String? = null
)

@Serializable
data class DeconstructionPlannerData(
    val item: String = "deconstruction-planner",
    val label: String? = null
)

@Serializable
data class BlueprintIcon(
    val index: Int,
    val signal: BlueprintSignal
)

@Serializable
data class BlueprintSignal(
    val name: String,
    val type: String? = "item",
    val quality: String? = "normal"
)

@Serializable
data class Position(
    val x: Double,
    val y: Double
)

@Serializable
data class BlueprintEntity(
    @SerialName("entity_number")
    val entityNumber: Int,
    val name: String,
    val position: Position,
    val direction: Int = 0,
    val quality: String = "normal",
    val recipe: String? = null,
    val items: Map<String, Int>? = null, // module or cargo inventory items
    val type: String? = null,
    val tags: Map<String, String>? = null
)

@Serializable
data class BlueprintTile(
    val name: String,
    val position: Position
)

@Serializable
data class BillOfMaterials(
    val entityCounts: Map<String, Int> = emptyMap(),
    val moduleCounts: Map<String, Int> = emptyMap(),
    val tileCounts: Map<String, Int> = emptyMap(),
    val qualityEntityCounts: Map<Pair<String, String>, Int> = emptyMap(),
    val totalRawEstimate: Map<String, Double> = emptyMap(),
    val unknownEntities: List<String> = emptyList()
)
