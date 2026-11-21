package com.factoria.ui.enemies

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EnemiesUiState(
    val searchQuery: String = "",
    val selectedCategory: String = "All",
    val selectedPlanet: String = "All",
    val filteredEnemies: List<EnemyUnit> = EnemyDataset.allEnemies,
    val selectedEnemy: EnemyUnit? = null
)

class EnemiesViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(EnemiesUiState())
    val uiState: StateFlow<EnemiesUiState> = _uiState.asStateFlow()

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFilters()
    }

    fun selectCategory(category: String) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        applyFilters()
    }

    fun selectPlanet(planet: String) {
        _uiState.value = _uiState.value.copy(selectedPlanet = planet)
        applyFilters()
    }

    fun selectEnemy(enemy: EnemyUnit) {
        _uiState.value = _uiState.value.copy(selectedEnemy = enemy)
    }

    fun dismissDetail() {
        _uiState.value = _uiState.value.copy(selectedEnemy = null)
    }

    private fun applyFilters() {
        val state = _uiState.value
        val query = state.searchQuery.trim().lowercase()

        val filtered = EnemyDataset.allEnemies.filter { enemy ->
            val matchesCategory = state.selectedCategory == "All" || enemy.category.equals(state.selectedCategory, ignoreCase = true)
            val matchesPlanet = state.selectedPlanet == "All" || enemy.planet.equals(state.selectedPlanet, ignoreCase = true)
            val matchesSearch = query.isBlank() ||
                    enemy.name.lowercase().contains(query) ||
                    enemy.category.lowercase().contains(query) ||
                    enemy.planet.lowercase().contains(query) ||
                    enemy.description.lowercase().contains(query)

            matchesCategory && matchesPlanet && matchesSearch
        }

        _uiState.value = state.copy(filteredEnemies = filtered)
    }
}
