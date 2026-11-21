package com.factoria.core.solver

import com.factoria.core.model.*
import kotlin.math.ceil
import kotlin.math.max

class ProductionCalculator(
    private val solver: SimplexSolver = SimplexSolver()
) {

    data class ProblemDefinition(
        val recipes: List<Recipe>,
        val entities: Map<String, Entity>, // key is entity id
        val modules: Map<String, Module> = emptyMap(),
        val targets: List<ProductionTarget>,
        val machineConfigs: Map<String, MachineConfiguration> = emptyMap(), // recipeId -> config
        val defaultMachineForCategory: Map<String, String> = emptyMap(), // category -> entityId
        val objective: SolverObjective = SolverObjective.MIN_MACHINES,
        val allowSurplus: Boolean = true
    )

    fun solve(problem: ProblemDefinition): SolverSolution {
        val startTime = System.currentTimeMillis()

        if (problem.targets.isEmpty() || problem.recipes.isEmpty()) {
            return SolverSolution(
                status = SolverStatus.OPTIMAL,
                solvingTimeMs = System.currentTimeMillis() - startTime
            )
        }

        // 1. Identify all distinct items and fluids along with their types
        val allResources = linkedSetOf<String>()
        val resourceTypes = mutableMapOf<String, TargetType>()
        val producedBy = mutableMapOf<String, MutableList<String>>() // resource -> list of recipeIds

        for (recipe in problem.recipes) {
            for (p in recipe.products) {
                allResources.add(p.targetId)
                resourceTypes.putIfAbsent(p.targetId, p.type)
                producedBy.getOrPut(p.targetId) { mutableListOf() }.add(recipe.id)
            }
            for (i in recipe.ingredients) {
                allResources.add(i.targetId)
                resourceTypes.putIfAbsent(i.targetId, i.type)
            }
        }

        for (t in problem.targets) {
            allResources.add(t.targetId)
            resourceTypes.putIfAbsent(t.targetId, t.type)
        }

        // Distinguish raw resources (those with no recipes producing them in our active set)
        val rawResources = allResources.filter { res ->
            producedBy[res].isNullOrEmpty()
        }.toSet()

        // Resources that must be produced by recipes (intermediates and targets)
        val intermediateAndTargets = allResources.filter { !rawResources.contains(it) }

        val numRecipes = problem.recipes.size

        // Calculate machine params per recipe
        val effectiveSpeed = DoubleArray(numRecipes)
        val prodBonus = DoubleArray(numRecipes)
        val energyUsagePerSec = DoubleArray(numRecipes)
        val pollutionPerMin = DoubleArray(numRecipes)
        val machineType = Array(numRecipes) { "" }
        val machineQuality = Array(numRecipes) { QualityTier.NORMAL }

        for ((rIdx, recipe) in problem.recipes.withIndex()) {
            val config = problem.machineConfigs[recipe.id]
            val entityId = config?.entityId
                ?: problem.defaultMachineForCategory[recipe.category]
                ?: problem.entities.values.firstOrNull { it.craftingCategories.contains(recipe.category) }?.id

            val entity = entityId?.let { problem.entities[it] }

            var speed = 1.0
            var prod = 0.0
            var energy = 100_000.0 // 100 kW default
            var poll = 1.0

            if (entity != null) {
                machineType[rIdx] = entity.id
                val q = config?.quality ?: QualityTier.NORMAL
                machineQuality[rIdx] = q

                var speedMultiplier = 1.0
                if (config?.modules != null) {
                    val modSpeed = config.modules.mapNotNull { problem.modules[it] }.sumOf { it.speedBonus }
                    speedMultiplier += modSpeed
                }

                val prodMultiplier = config?.modules?.mapNotNull { problem.modules[it] }?.sumOf { it.productivityBonus } ?: 0.0
                val energyMultiplier = 1.0 + (config?.modules?.mapNotNull { problem.modules[it] }?.sumOf { it.energyBonus } ?: 0.0)
                val pollMultiplier = 1.0 + (config?.modules?.mapNotNull { problem.modules[it] }?.sumOf { it.pollutionBonus } ?: 0.0)

                // Beacon bonus
                if (config != null && config.beaconCount > 0 && config.beaconModuleId != null) {
                    val bMod = problem.modules[config.beaconModuleId]
                    if (bMod != null) {
                        val beaconEfficiency = 1.5 * config.beaconQuality.moduleEffectModifier
                        speedMultiplier += config.beaconCount * beaconEfficiency * bMod.speedBonus
                    }
                }

                speed = entity.craftingSpeed * q.craftingSpeedModifier * max(0.2, speedMultiplier)
                prod = prodMultiplier
                energy = entity.energyUsageWatts * max(0.2, energyMultiplier)
                poll = entity.emissionsPerMinute * max(0.2, pollMultiplier)
            } else {
                machineType[rIdx] = "hand"
            }

            effectiveSpeed[rIdx] = speed
            prodBonus[rIdx] = prod
            energyUsagePerSec[rIdx] = energy
            pollutionPerMin[rIdx] = poll
        }

        // Objective vector over recipes (variables: x_0 .. x_{numRecipes-1})
        val objective = DoubleArray(numRecipes)
        for (j in 0 until numRecipes) {
            val recipe = problem.recipes[j]
            val time = recipe.craftingTime
            val speed = effectiveSpeed[j]
            val machinesPerCraft = time / speed

            objective[j] = when (problem.objective) {
                SolverObjective.MIN_MACHINES -> machinesPerCraft
                SolverObjective.MIN_POWER -> energyUsagePerSec[j] * machinesPerCraft
                SolverObjective.MIN_POLLUTION -> pollutionPerMin[j] * machinesPerCraft
                SolverObjective.MIN_RAW_RESOURCES -> {
                    // Cost is sum of raw ingredients consumed
                    var rawCost = 0.0
                    for (ing in recipe.ingredients) {
                        if (rawResources.contains(ing.targetId)) {
                            rawCost += ing.amount
                        }
                    }
                    if (rawCost > 0.0) rawCost else 0.001 * machinesPerCraft
                }
            }
        }

        // Constraints for intermediate and target resources:
        // sum_j A_ij * x_j >= target_i
        val targetMap = mutableMapOf<String, Double>()
        for (t in problem.targets) {
            targetMap[t.targetId] = (targetMap[t.targetId] ?: 0.0) + t.ratePerSecond
        }
        val constraints = mutableListOf<SimplexSolver.Constraint>()

        for (resId in intermediateAndTargets) {
            val row = DoubleArray(numRecipes)

            for (j in 0 until numRecipes) {
                val recipe = problem.recipes[j]
                var net = 0.0

                for (prod in recipe.products) {
                    if (prod.targetId == resId) {
                        val baseAmount = prod.expectedYield
                        val catalyst = prod.catalystAmount
                        val extraFromProd = (baseAmount - catalyst).coerceAtLeast(0.0) * prodBonus[j]
                        net += (baseAmount + extraFromProd)
                    }
                }

                for (ing in recipe.ingredients) {
                    if (ing.targetId == resId) {
                        net -= ing.amount
                    }
                }

                row[j] = net
            }

            val targetDemand = targetMap[resId] ?: 0.0
            val constraintType = if (problem.allowSurplus) {
                SimplexSolver.ConstraintType.GREATER_THAN_OR_EQUAL
            } else {
                SimplexSolver.ConstraintType.EQUAL
            }

            constraints.add(
                SimplexSolver.Constraint(
                    coefficients = row,
                    type = constraintType,
                    rhs = targetDemand
                )
            )
        }

        // Solve using Simplex
        val lpResult = solver.solve(objective, constraints)

        if (lpResult.status != SimplexSolver.Status.OPTIMAL) {
            return SolverSolution(
                status = when (lpResult.status) {
                    SimplexSolver.Status.INFEASIBLE -> SolverStatus.INFEASIBLE
                    SimplexSolver.Status.UNBOUNDED -> SolverStatus.UNBOUNDED
                    else -> SolverStatus.ERROR
                },
                warnings = listOf("Solver returned status: ${lpResult.status}"),
                solvingTimeMs = System.currentTimeMillis() - startTime
            )
        }

        // Process solution
        val recipeRates = mutableListOf<RecipeRate>()
        var totalPower = 0.0
        var totalPollution = 0.0
        val catalystBuffers = mutableMapOf<String, Double>()

        for (j in 0 until numRecipes) {
            val rate = lpResult.variableValues[j]
            if (rate > 1e-6) {
                val recipe = problem.recipes[j]
                val time = recipe.craftingTime
                val speed = effectiveSpeed[j]
                val machinesReq = (rate * time) / speed
                val rounded = ceil(machinesReq).toInt()

                val power = machinesReq * energyUsagePerSec[j]
                val pollution = machinesReq * pollutionPerMin[j]
                totalPower += power
                totalPollution += pollution

                for (ing in recipe.ingredients) {
                    val matchingProd = recipe.products.find { it.targetId == ing.targetId && it.catalystAmount > 0.0 }
                    if (matchingProd != null) {
                        val buffer = matchingProd.catalystAmount * rounded
                        catalystBuffers[ing.targetId] = (catalystBuffers[ing.targetId] ?: 0.0) + buffer
                    }
                }

                recipeRates.add(
                    RecipeRate(
                        recipeId = recipe.id,
                        recipeName = recipe.localizedName,
                        craftsPerSecond = rate,
                        machinesRequired = machinesReq,
                        roundedMachines = rounded,
                        machineEntityId = machineType[j],
                        machineQuality = machineQuality[j],
                        powerWatts = power,
                        pollutionPerMinute = pollution,
                        effectiveCraftingSpeed = speed,
                        effectiveProductivityBonus = prodBonus[j]
                    )
                )
            }
        }

        // Compute net production/consumption for all resources
        val netBalance = mutableMapOf<String, Double>()
        for (j in 0 until numRecipes) {
            val rate = lpResult.variableValues[j]
            if (rate <= 1e-6) continue
            val recipe = problem.recipes[j]

            for (prod in recipe.products) {
                val baseAmount = prod.expectedYield
                val catalyst = prod.catalystAmount
                val extraFromProd = (baseAmount - catalyst).coerceAtLeast(0.0) * prodBonus[j]
                val totalProduced = (baseAmount + extraFromProd) * rate
                netBalance[prod.targetId] = (netBalance[prod.targetId] ?: 0.0) + totalProduced
            }

            for (ing in recipe.ingredients) {
                val totalConsumed = ing.amount * rate
                netBalance[ing.targetId] = (netBalance[ing.targetId] ?: 0.0) - totalConsumed
            }
        }

        val rawUsage = mutableMapOf<String, Double>()
        for (raw in rawResources) {
            val balance = netBalance[raw] ?: 0.0
            if (balance < -1e-6) {
                rawUsage[raw] = -balance
            }
        }

        val surplus = mutableMapOf<String, Double>()
        val itemRates = mutableListOf<ItemRate>()

        for (resId in allResources) {
            val balance = netBalance[resId] ?: 0.0
            val isRaw = rawResources.contains(resId)
            val isTarget = targetMap.containsKey(resId)
            val targetVal = targetMap[resId] ?: 0.0

            if (balance > targetVal + 1e-6) {
                surplus[resId] = balance - targetVal
            }

            if (isRaw || isTarget || surplus.containsKey(resId)) {
                val resolvedType = resourceTypes[resId] ?: if (
                    resId.contains("oil") || resId.contains("water") || resId.contains("gas") ||
                    resId.contains("acid") || resId.contains("lava") || resId.contains("molten")
                ) TargetType.FLUID else TargetType.ITEM
                itemRates.add(
                    ItemRate(
                        targetId = resId,
                        targetName = cleanLocalizedName(resId),
                        type = resolvedType,
                        netRatePerSecond = balance,
                        isRawInput = isRaw,
                        isTarget = isTarget,
                        isSurplus = surplus.containsKey(resId)
                    )
                )
            }
        }

        return SolverSolution(
            status = SolverStatus.OPTIMAL,
            recipeRates = recipeRates,
            itemRates = itemRates,
            totalPowerWatts = totalPower,
            totalPollutionPerMinute = totalPollution,
            rawResources = rawUsage,
            surplusProducts = surplus,
            catalystStartupBuffers = catalystBuffers,
            solvingTimeMs = System.currentTimeMillis() - startTime
        )
    }

    private fun cleanLocalizedName(name: String): String {
        return name.split("-", "_").joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
    }
}
