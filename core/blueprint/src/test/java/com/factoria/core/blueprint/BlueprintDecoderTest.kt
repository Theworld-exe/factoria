package com.factoria.core.blueprint

import com.factoria.core.model.*
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.Deflater

class BlueprintDecoderTest {

    private fun encodeBlueprint(json: String): String {
        val bytes = json.toByteArray(Charsets.UTF_8)
        val deflater = Deflater()
        deflater.setInput(bytes)
        deflater.finish()

        val outputStream = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        while (!deflater.finished()) {
            val count = deflater.deflate(buffer)
            outputStream.write(buffer, 0, count)
        }
        deflater.end()
        val compressed = outputStream.toByteArray()
        val b64 = Base64.getEncoder().encodeToString(compressed)
        return "0$b64"
    }

    @Test
    fun testValidBlueprintDecoding() {
        val rawJson = """
            {
              "blueprint": {
                "item": "blueprint",
                "label": "Test Smelter",
                "version": 281479271743488,
                "entities": [
                  {
                    "entity_number": 1,
                    "name": "stone-furnace",
                    "position": { "x": 1.5, "y": 2.5 },
                    "direction": 0,
                    "quality": "normal"
                  },
                  {
                    "entity_number": 2,
                    "name": "inserter",
                    "position": { "x": 1.5, "y": 3.5 },
                    "direction": 4,
                    "quality": "rare"
                  }
                ],
                "tiles": [
                  {
                    "name": "concrete",
                    "position": { "x": 1, "y": 2 }
                  }
                ]
              }
            }
        """.trimIndent()

        val bpString = encodeBlueprint(rawJson)
        val result = BlueprintDecoder.decode(bpString)

        assertTrue("Blueprint decoding should succeed", result is BlueprintDecodeResult.Success)
        val success = result as BlueprintDecodeResult.Success
        val bp = success.wrapper.blueprint
        assertNotNull(bp)
        assertEquals("Test Smelter", bp?.label)
        assertEquals(2, bp?.entities?.size)
        assertEquals("stone-furnace", bp?.entities?.get(0)?.name)
        assertEquals("rare", bp?.entities?.get(1)?.quality)
        assertEquals(1, bp?.tiles?.size)

        // Test BOM calculation
        val bom = BomCalculator.computeBOM(
            blueprint = bp!!,
            knownEntityIds = setOf("stone-furnace", "inserter"),
            knownItemIds = setOf("concrete")
        )

        assertEquals(1, bom.entityCounts["stone-furnace"])
        assertEquals(1, bom.entityCounts["inserter"])
        assertEquals(1, bom.tileCounts["concrete"])
        assertEquals(1, bom.qualityEntityCounts[Pair("inserter", "rare")])
        assertEquals(0, bom.unknownEntities.size)
    }

    @Test
    fun testInvalidBlueprintString() {
        val result = BlueprintDecoder.decode("NotAValidBlueprintString")
        assertTrue(result is BlueprintDecodeResult.Failure)
    }

    @Test
    fun testEmptyBlueprintString() {
        val result = BlueprintDecoder.decode("")
        assertTrue(result is BlueprintDecodeResult.Failure)
        assertEquals(BlueprintError.EMPTY_STRING, (result as BlueprintDecodeResult.Failure).error)
    }

    @Test
    fun testUnknownEntitiesInBOM() {
        val bp = BlueprintData(
            label = "Modded Setup",
            entities = listOf(
                BlueprintEntity(entityNumber = 1, name = "vanilla-assembler", position = Position(0.0, 0.0)),
                BlueprintEntity(entityNumber = 2, name = "bob-greenhouse-mk2", position = Position(1.0, 1.0))
            )
        )

        val bom = BomCalculator.computeBOM(
            blueprint = bp,
            knownEntityIds = setOf("vanilla-assembler")
        )

        assertEquals(1, bom.unknownEntities.size)
        assertEquals("bob-greenhouse-mk2", bom.unknownEntities.first())
    }

    @Test
    fun testAllSampleBlueprintsDecodeAndComputeBom() {
        val allSamples = listOf(SampleBlueprints.SOLAR_STARTER) + SampleBlueprints.library
        for (entry in allSamples) {
            val result = BlueprintDecoder.decode(entry.blueprintString)
            assertTrue("Expected ${entry.id} to decode cleanly, got $result", result is BlueprintDecodeResult.Success)
            val bp = (result as BlueprintDecodeResult.Success).wrapper.blueprint
            assertNotNull("Expected blueprint payload for ${entry.id}", bp)
            val bom = BomCalculator.computeBOM(bp!!)
            assertTrue("Expected entities in ${entry.id}", bom.entityCounts.isNotEmpty())
        }
    }

    @Test
    fun testOversizedPayloadReturnsOversizedError() {
        val largeJson = """{"blueprint":{"item":"blueprint","label":"${"A".repeat(2048)}","entities":[]}}"""
        val encoded = encodeBlueprint(largeJson)
        val result = BlueprintDecoder.decode(encoded, maxUncompressedSizeBytes = 256)
        assertTrue(result is BlueprintDecodeResult.Failure)
        assertEquals(BlueprintError.OVERSIZED_PAYLOAD, (result as BlueprintDecodeResult.Failure).error)
    }

    @Test
    fun testBlueprintBookFlattening() {
        val bp1 = BlueprintData(label = "Inner 1")
        val bp2 = BlueprintData(label = "Nested 2")
        val wrapper = BlueprintStringWrapper(
            blueprintBook = BlueprintBookData(
                label = "Root Book",
                blueprints = listOf(
                    BlueprintBookEntry(index = 0, blueprint = bp1),
                    BlueprintBookEntry(
                        index = 1,
                        blueprintBook = BlueprintBookData(
                            label = "Sub Book",
                            blueprints = listOf(BlueprintBookEntry(index = 0, blueprint = bp2))
                        )
                    )
                )
            )
        )
        val extracted = BomCalculator.extractAllBlueprints(wrapper)
        assertEquals(2, extracted.size)
        assertEquals(listOf("Inner 1", "Nested 2"), extracted.map { it.label })
    }
}
