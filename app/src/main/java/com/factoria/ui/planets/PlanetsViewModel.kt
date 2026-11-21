package com.factoria.ui.planets

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlanetsUiState(
    val searchQuery: String = "",
    val filteredPlanets: List<PlanetInfo> = PlanetDataset.allPlanets,
    val selectedPlanet: PlanetInfo? = null
)

class PlanetsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PlanetsUiState())
    val uiState: StateFlow<PlanetsUiState> = _uiState.asStateFlow()

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFilters()
    }

    fun selectPlanet(planet: PlanetInfo) {
        _uiState.value = _uiState.value.copy(selectedPlanet = planet)
    }

    fun dismissDetail() {
        _uiState.value = _uiState.value.copy(selectedPlanet = null)
    }

    private fun applyFilters() {
        val state = _uiState.value
        val query = state.searchQuery.trim().lowercase()

        val filtered = PlanetDataset.allPlanets.filter { planet ->
            query.isBlank() ||
                    planet.name.lowercase().contains(query) ||
                    planet.subtitle.lowercase().contains(query) ||
                    planet.description.lowercase().contains(query) ||
                    planet.primaryResources.any { it.lowercase().contains(query) }
        }

        _uiState.value = state.copy(filteredPlanets = filtered)
    }
}