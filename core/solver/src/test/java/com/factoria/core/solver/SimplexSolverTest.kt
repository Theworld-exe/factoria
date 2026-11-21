package com.factoria.core.solver

import com.factoria.core.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SimplexSolverTest {

    @Test
    fun testStandardLinearProgram() {
        val solver = SimplexSolver()
        // Minimize 2*x1 + 3*x2
        // Subject to:
        // x1 + x2 >= 4
        // 2*x1 + x2 >= 5
        // x1, x2 >= 0
        // Optimal is x1 = 4, x2 = 0 -> obj = 8
        val obj = doubleArrayOf(2.0, 3.0)
        val constraints = listOf(
            SimplexSolver.Constraint(doubleArrayOf(1.0, 1.0), SimplexSolver.ConstraintType.GREATER_THAN_OR_EQUAL, 4.0),
            SimplexSolver.Constraint(doubleArrayOf(2.0, 1.0), SimplexSolver.ConstraintType.GREATER_THAN_OR_EQUAL, 5.0)
        )

        val result = solver.solve(obj, constraints)
        assertEquals(SimplexSolver.Status.OPTIMAL, result.status)
        assertEquals(8.0, result.objectiveValue, 1e-4)
        assertEquals(4.0, result.variableValues[0], 1e-4)
        assertEquals(0.0, result.variableValues[1], 1e-4)
    }

    @Test
    fun testElectronicCircuitProduction() {
        val calc = ProductionCalculator()

        val ironPlate = Recipe(
            id = "iron-plate",
            name = "iron-plate",
            localizedName = "Iron plate",
            category = "smelting",
            craftingTime = 3.2,
            isHandCraftable = true,
            ingredients = listOf(RecipeIngredient(TargetType.ITEM, "iron-ore", 1.0)),
            products = listOf(RecipeProduct(TargetType.ITEM, "iron-plate", 1.0))
        )
        val copperPlate = Recipe(
            id = "copper-plate",
            name = "copper-plate",
            localizedName = "Copper plate",
            category = "smelting",
            craftingTime = 3.2,
            isHandCraftable = true,
            ingredients = listOf(RecipeIngredient(TargetType.ITEM, "copper-ore", 1.0)),
            products = listOf(RecipeProduct(TargetType.ITEM, "copper-plate", 1.0))
        )
        val copperCable = Recipe(
            id = "copper-cable",
            name = "copper-cable",
            localizedName = "Copper cable",
            category = "crafting",
            craftingTime = 0.5,
            isHandCraftable = true,
            ingredients = listOf(RecipeIngredient(TargetType.ITEM, "copper-plate", 1.0)),
            products = listOf(RecipeProduct(TargetType.ITEM, "copper-cable", 2.0))
        )
        val electronicCircuit = Recipe(
            id = "electronic-circuit",
            name = "electronic-circuit",
            localizedName = "Electronic circuit",
            category = "crafting",
            craftingTime = 0.5,
            isHandCraftable = true,
            ingredients = listOf(
                RecipeIngredient(TargetType.ITEM, "iron-plate", 1.0),
                RecipeIngredient(TargetType.ITEM, "copper-cable", 3.0)
            ),
            products = listOf(RecipeProduct(TargetType.ITEM, "electronic-circuit", 1.0))
        )

        val assembler1 = Entity(
            id = "assembling-machine-1",
            name = "assembling-machine-1",
            localizedName = "Assembling machine 1",
            type = EntityType.ASSEMBLING_MACHINE,
            craftingSpeed = 0.5,
            energyUsageWatts = 75000.0,
            craftingCategories = listOf("crafting")
        )
        val stoneFurnace = Entity(
            id = "stone-furnace",
            name = "stone-furnace",
            localizedName = "Stone furnace",
            type = EntityType.FURNACE,
            craftingSpeed = 1.0,
            energyUsageWatts = 90000.0,
            craftingCategories = listOf("smelting")
        )

        val problem = ProductionCalculator.ProblemDefinition(
            recipes = listOf(ironPlate, copperPlate, copperCable, electronicCircuit),
            entities = mapOf(
                assembler1.id to assembler1,
                stoneFurnace.id to stoneFurnace
            ),
            targets = listOf(ProductionTarget("electronic-circuit", TargetType.ITEM, 2.0)), // 2 circuits/sec
            defaultMachineForCategory = mapOf(
                "crafting" to "assembling-machine-1",
                "smelting" to "stone-furnace"
            )
        )

        val solution = calc.solve(problem)
        assertEquals(SolverStatus.OPTIMAL, solution.status)

        // 2 circuits/sec requires:
        // - 2 circuits/sec * 0.5s / 0.5 speed = 2.0 assembler 1s
        // - 6 copper cables/sec -> 3 crafts/sec * 0.5s / 0.5 speed = 3.0 assembler 1s
        // - 3 copper plates/sec -> 3 crafts/sec * 3.2s / 1.0 speed = 9.6 stone furnaces
        // - 2 iron plates/sec -> 2 crafts/sec * 3.2s / 1.0 speed = 6.4 stone furnaces
        val circuitRate = solution.recipeRates.find { it.recipeId == "electronic-circuit" }
        val cableRate = solution.recipeRates.find { it.recipeId == "copper-cable" }
        val ironPlateRate = solution.recipeRates.find { it.recipeId == "iron-plate" }
        val copperPlateRate = solution.recipeRates.find { it.recipeId == "copper-plate" }

        assertEquals(2.0, circuitRate?.craftsPerSecond ?: 0.0, 1e-4)
        assertEquals(2.0, circuitRate?.machinesRequired ?: 0.0, 1e-4)
        assertEquals(3.0, cableRate?.craftsPerSecond ?: 0.0, 1e-4)
        assertEquals(3.0, cableRate?.machinesRequired ?: 0.0, 1e-4)
        assertEquals(2.0, ironPlateRate?.craftsPerSecond ?: 0.0, 1e-4)
        assertEquals(6.4, ironPlateRate?.machinesRequired ?: 0.0, 1e-4)
        assertEquals(3.0, copperPlateRate?.craftsPerSecond ?: 0.0, 1e-4)
        assertEquals(9.6, copperPlateRate?.machinesRequired ?: 0.0, 1e-4)

        // Raw materials check
        assertEquals(2.0, solution.rawResources["iron-ore"] ?: 0.0, 1e-4)
        assertEquals(3.0, solution.rawResources["copper-ore"] ?: 0.0, 1e-4)
    }

    @Test
    fun testKovarexCycleResolution() {
        val calc = ProductionCalculator()

        val uProcessing = Recipe(
            id = "uranium-processing",
            name = "uranium-processing",
            localizedName = "Uranium processing",
            category = "centrifuging",
            craftingTime = 12.0,
            isHandCraftable = false,
            ingredients = listOf(RecipeIngredient(TargetType.ITEM, "uranium-ore", 10.0)),
            products = listOf(
                RecipeProduct(TargetType.ITEM, "uranium-235", 1.0, 1.0, 1.0, 0.007, 0.0),
                RecipeProduct(TargetType.ITEM, "uranium-238", 1.0, 1.0, 1.0, 0.993, 0.0)
            )
        )

        val kovarex = Recipe(
            id = "kovarex-enrichment-process",
            name = "kovarex-enrichment-process",
            localizedName = "Kovarex enrichment process",
            category = "centrifuging",
            craftingTime = 60.0,
            isHandCraftable = false,
            isCatalystLoop = true,
            ingredients = listOf(
                RecipeIngredient(TargetType.ITEM, "uranium-235", 40.0),
                RecipeIngredient(TargetType.ITEM, "uranium-238", 5.0)
            ),
            products = listOf(
                RecipeProduct(TargetType.ITEM, "uranium-235", 41.0, 41.0, 41.0, 1.0, 40.0),
                RecipeProduct(TargetType.ITEM, "uranium-238", 2.0, 2.0, 2.0, 1.0, 2.0)
            )
        )

        val centrifuge = Entity(
            id = "centrifuge",
            name = "centrifuge",
            localizedName = "Centrifuge",
            type = EntityType.ASSEMBLING_MACHINE,
            craftingSpeed = 1.0,
            energyUsageWatts = 350000.0,
            craftingCategories = listOf("centrifuging")
        )

        // Target: 1 U-235 per second
        val problem = ProductionCalculator.ProblemDefinition(
            recipes = listOf(uProcessing, kovarex),
            entities = mapOf(centrifuge.id to centrifuge),
            targets = listOf(ProductionTarget("uranium-235", TargetType.ITEM, 1.0)),
            defaultMachineForCategory = mapOf("centrifuging" to "centrifuge"),
            objective = SolverObjective.MIN_RAW_RESOURCES
        )

        val solution = calc.solve(problem)
        assertEquals(SolverStatus.OPTIMAL, solution.status)

        // With Kovarex, net gain per craft is 1 U-235 using 3 U-238.
        assertTrue(solution.recipeRates.any { it.recipeId == "kovarex-enrichment-process" })
        // Check startup catalyst buffer was registered
        assertTrue((solution.catalystStartupBuffers["uranium-235"] ?: 0.0) > 0.0)
    }

    @Test
    fun testInfeasibleLinearProgram() {
        val solver = SimplexSolver()
        // x1 <= 2 and x1 >= 5 -> infeasible
        val obj = doubleArrayOf(1.0)
        val constraints = listOf(
            SimplexSolver.Constraint(doubleArrayOf(1.0), SimplexSolver.ConstraintType.LESS_THAN_OR_EQUAL, 2.0),
            SimplexSolver.Constraint(doubleArrayOf(1.0), SimplexSolver.ConstraintType.GREATER_THAN_OR_EQUAL, 5.0)
        )
        val result = solver.solve(obj, constraints)
        assertEquals(SimplexSolver.Status.INFEASIBLE, result.status)
    }

    @Test
    fun testQualityAndModuleScaling() {
        val calc = ProductionCalculator()
        val recipe = Recipe(
            id = "iron-gear-wheel",
            name = "iron-gear-wheel",
            localizedName = "Iron gear wheel",
            category = "crafting",
            craftingTime = 0.5,
            isHandCraftable = true,
            ingredients = listOf(RecipeIngredient(TargetType.ITEM, "iron-plate", 2.0)),
            products = listOf(RecipeProduct(TargetType.ITEM, "iron-gear-wheel", 1.0))
        )
        val assembler3 = Entity(
            id = "assembling-machine-3",
            name = "assembling-machine-3",
            localizedName = "Assembling machine 3",
            type = EntityType.ASSEMBLING_MACHINE,
            craftingSpeed = 1.25,
            energyUsageWatts = 375_000.0,
            emissionsPerMinute = 2.0,
            moduleSlots = 4,
            craftingCategories = listOf("crafting")
        )
        val prodMod3 = Module(
            id = "productivity-module-3",
            name = "Productivity module 3",
            tier = 3,
            category = ModuleCategory.PRODUCTIVITY,
            speedBonus = -0.15,
            productivityBonus = 0.10,
            energyBonus = 0.80
        )

        val baseSolution = calc.solve(
            ProductionCalculator.ProblemDefinition(
                recipes = listOf(recipe),
                entities = mapOf(assembler3.id to assembler3),
                targets = listOf(ProductionTarget("iron-gear-wheel", TargetType.ITEM, 10.0)),
                machineConfigs = mapOf(
                    recipe.id to MachineConfiguration(recipe.id, assembler3.id, QualityTier.NORMAL)
                )
            )
        )

        val upgradedSolution = calc.solve(
            ProductionCalculator.ProblemDefinition(
                recipes = listOf(recipe),
                entities = mapOf(assembler3.id to assembler3),
                modules = mapOf(prodMod3.id to prodMod3),
                targets = listOf(ProductionTarget("iron-gear-wheel", TargetType.ITEM, 10.0)),
                machineConfigs = mapOf(
                    recipe.id to MachineConfiguration(
                        recipeId = recipe.id,
                        entityId = assembler3.id,
                        quality = QualityTier.RARE,
                        modules = listOf(prodMod3.id, prodMod3.id)
                    )
                )
            )
        )

        assertEquals(SolverStatus.OPTIMAL, baseSolution.status)
        assertEquals(SolverStatus.OPTIMAL, upgradedSolution.status)
        // With +20% productivity, raw iron-plate demand drops from 20/s to 20/1.2 = 16.6667/s
        assertEquals(20.0, baseSolution.rawResources["iron-plate"] ?: 0.0, 1e-4)
        assertEquals(20.0 / 1.2, upgradedSolution.rawResources["iron-plate"] ?: 0.0, 1e-4)
        // Rare quality (1.6x speed) with two -15% speed modules (0.7x) -> 1.12x net speed vs 1.0x
        assertTrue(upgradedSolution.recipeRates.first().machinesRequired < baseSolution.recipeRates.first().machinesRequired)
    }
}
