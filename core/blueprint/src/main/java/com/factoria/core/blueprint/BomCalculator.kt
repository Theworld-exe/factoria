package com.factoria.core.blueprint

import com.factoria.core.model.*

object BomCalculator {

    /**
     * Compute the Bill of Materials for a single blueprint.
     */
    fun computeBOM(
        blueprint: BlueprintData,
        knownEntityIds: Set<String> = emptySet(),
        knownItemIds: Set<String> = emptySet()
    ): BillOfMaterials {
        val entityCounts = mutableMapOf<String, Int>()
        val moduleCounts = mutableMapOf<String, Int>()
        val tileCounts = mutableMapOf<String, Int>()
        val qualityEntityCounts = mutableMapOf<Pair<String, String>, Int>()
        val unknownEntities = mutableSetOf<String>()

        for (entity in blueprint.entities) {
            val name = entity.name
            val quality = entity.quality

            entityCounts[name] = (entityCounts[name] ?: 0) + 1
            val key = Pair(name, quality)
            qualityEntityCounts[key] = (qualityEntityCounts[key] ?: 0) + 1

            if (knownEntityIds.isNotEmpty() && !knownEntityIds.contains(name) && !knownItemIds.contains(name)) {
                unknownEntities.add(name)
            }

            // Extract modules placed inside entity
            entity.items?.forEach { (itemName, count) ->
                moduleCounts[itemName] = (moduleCounts[itemName] ?: 0) + count
            }
        }

        for (tile in blueprint.tiles) {
            tileCounts[tile.name] = (tileCounts[tile.name] ?: 0) + 1
        }

        return BillOfMaterials(
            entityCounts = entityCounts,
            moduleCounts = moduleCounts,
            tileCounts = tileCounts,
            qualityEntityCounts = qualityEntityCounts,
            unknownEntities = unknownEntities.toList()
        )
    }

    /**
     * Flatten a blueprint book or blueprint into a list of all contained blueprints.
     */
    fun extractAllBlueprints(wrapper: BlueprintStringWrapper): List<BlueprintData> {
        val result = mutableListOf<BlueprintData>()
        wrapper.blueprint?.let { result.add(it) }
        wrapper.blueprintBook?.let { extractFromBook(it, result) }
        return result
    }

    private fun extractFromBook(book: BlueprintBookData, acc: MutableList<BlueprintData>) {
        for (entry in book.blueprints) {
            entry.blueprint?.let { acc.add(it) }
            entry.blueprintBook?.let { extractFromBook(it, acc) }
        }
    }
}
