package com.factoria.ui.calculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.factoria.FactoriaApplication
import com.factoria.core.database.entity.SavedProfileEntity
import com.factoria.core.designsystem.FactoriaSoundManager
import com.factoria.core.model.*
import com.factoria.core.solver.ProductionCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CalculatorUiState(
    val availableItems: List<Item> = emptyList(),
    val availableRecipes: List<Recipe> = emptyList(),
    val availableEntities: Map<String, Entity> = emptyMap(),
    val availableModules: Map<String, Module> = emptyMap(),
    val selectedTargetId: String = "electronic-circuit",
    val targetRateText: String = "15.0",
    val selectedUnit: RateUnit = RateUnit.PER_SECOND,
    val selectedObjective: SolverObjective = SolverObjective.MIN_MACHINES,
    val selectedQuality: QualityTier = QualityTier.NORMAL,
    val allowSurplus: Boolean = true,
    val solution: SolverSolution? = null,
    val isSolving: Boolean = false,
    val errorMessage: String? = null
)

class CalculatorViewModel : ViewModel() {
    private val repository = FactoriaApplication.instance.repository
    private val calculator = FactoriaApplication.instance.calculator

    private val _uiState = MutableStateFlow(CalculatorUiState())
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    val savedProfiles: StateFlow<List<SavedProfileEntity>> = repository.getSavedProfiles()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch(Dispatchers.IO) {
            launch {
                repository.getItems().collect { items ->
                    _uiState.value = _uiState.value.copy(availableItems = items)
                }
            }
            launch {
                repository.getRecipes().collect { recipes ->
                    _uiState.value = _uiState.value.copy(availableRecipes = recipes)
                }
            }
            launch {
                repository.getEntities().collect { entities ->
                    _uiState.value = _uiState.value.copy(availableEntities = entities.associateBy { it.id })
                }
            }
        }
    }

    fun setTargetId(targetId: String) {
        _uiState.value = _uiState.value.copy(selectedTargetId = targetId)
    }

    fun setTargetRate(rateText: String) {
        _uiState.value = _uiState.value.copy(targetRateText = rateText)
    }

    fun setUnit(unit: RateUnit) {
        _uiState.value = _uiState.value.copy(selectedUnit = unit)
    }

    fun setObjective(objective: SolverObjective) {
        _uiState.value = _uiState.value.copy(selectedObjective = objective)
    }

    fun setQuality(quality: QualityTier) {
        _uiState.value = _uiState.value.copy(selectedQuality = quality)
    }

    fun toggleAllowSurplus(allow: Boolean) {
        _uiState.value = _uiState.value.copy(allowSurplus = allow)
    }

    fun saveCurrentProfile(profileName: String) {
        val state = _uiState.value
        val rateValue = state.targetRateText.toDoubleOrNull() ?: 1.0
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveProfile(
                SavedProfileEntity(
                    name = profileName.ifBlank { "${state.selectedTargetId.replace("-", " ").replaceFirstChar { it.uppercase() }} Plan" },
                    target_id = state.selectedTargetId,
                    target_rate = rateValue,
                    quality_tier = state.selectedQuality.name,
                    solver_objective = state.selectedObjective.name
                )
            )
        }
    }

    fun loadProfile(profile: SavedProfileEntity) {
        _uiState.value = _uiState.value.copy(
            selectedTargetId = profile.target_id,
            targetRateText = profile.target_rate.toString(),
            selectedQuality = try { QualityTier.valueOf(profile.quality_tier) } catch (_: Exception) { QualityTier.NORMAL },
            selectedObjective = try { SolverObjective.valueOf(profile.solver_objective) } catch (_: Exception) { SolverObjective.MIN_MACHINES }
        )
        solve()
    }

    fun deleteSavedProfile(profileId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteProfile(profileId)
        }
    }

    fun solve() {
        val state = _uiState.value
        val rateValue = state.targetRateText.toDoubleOrNull()
        if (rateValue == null || rateValue <= 0.0) {
            _uiState.value = state.copy(errorMessage = "Please enter a valid positive number for rate.")
            return
        }

        val ratePerSecond = state.selectedUnit.toRatePerSecond(rateValue)

        _uiState.value = state.copy(isSolving = true, errorMessage = null)

        viewModelScope.launch(Dispatchers.Default) {
            try {
                val isFluid = state.selectedTargetId.contains("oil") ||
                        state.selectedTargetId.contains("water") ||
                        state.selectedTargetId.contains("gas") ||
                        state.selectedTargetId.contains("acid") ||
                        state.selectedTargetId.contains("lava")

                val target = ProductionTarget(
                    targetId = state.selectedTargetId,
                    type = if (isFluid) TargetType.FLUID else TargetType.ITEM,
                    ratePerSecond = ratePerSecond
                )

                val defaultMachines = mapOf(
                    "crafting" to "assembling-machine-3",
                    "advanced-crafting" to "assembling-machine-3",
                    "smelting" to "electric-furnace",
                    "chemistry" to "chemical-plant",
                    "oil-processing" to "oil-refinery",
                    "centrifuging" to "centrifuge",
                    "metallurgy" to "foundry",
                    "casting" to "foundry",
                    "electromagnetics" to "electromagnetic-plant",
                    "organic" to "biochamber",
                    "recycling" to "recycler"
                )

                val configs = state.availableRecipes.associate { recipe ->
                    recipe.id to MachineConfiguration(
                        recipeId = recipe.id,
                        entityId = defaultMachines[recipe.category] ?: "assembling-machine-3",
                        quality = state.selectedQuality
                    )
                }

                val problem = ProductionCalculator.ProblemDefinition(
                    recipes = state.availableRecipes,
                    entities = state.availableEntities,
                    modules = state.availableModules,
                    targets = listOf(target),
                    machineConfigs = configs,
                    defaultMachineForCategory = defaultMachines,
                    objective = state.selectedObjective,
                    allowSurplus = state.allowSurplus
                )

                val solution = calculator.solve(problem)

                withContext(Dispatchers.Main) {
                    if (solution.status == SolverStatus.OPTIMAL) {
                        FactoriaSoundManager.playSolveSuccess()
                    }
                    _uiState.value = _uiState.value.copy(
                        solution = solution,
                        isSolving = false,
                        errorMessage = if (solution.status != SolverStatus.OPTIMAL) {
                            "Unable to find optimal solution: ${solution.status}"
                        } else null
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isSolving = false,
                        errorMessage = "Error solving: ${e.message}"
                    )
                }
            }
        }
    }
}
