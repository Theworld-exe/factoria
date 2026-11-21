package com.factoria.ui.techtree

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.factoria.FactoriaApplication
import com.factoria.core.model.Technology
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TechTreeUiState(
    val technologies: List<Technology> = emptyList(),
    val selectedTech: Technology? = null,
    val cumulativePacks: Map<String, Long> = emptyMap(),
    val searchQuery: String = ""
)

class TechTreeViewModel : ViewModel() {
    private val repository = FactoriaApplication.instance.repository
    private val _uiState = MutableStateFlow(TechTreeUiState())
    val uiState: StateFlow<TechTreeUiState> = _uiState.asStateFlow()

    init {
        loadTechnologies()
    }

    private fun loadTechnologies() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getTechnologies().collect { list ->
                _uiState.value = _uiState.value.copy(technologies = list)
                if (list.isNotEmpty() && _uiState.value.selectedTech == null) {
                    selectTechnology(list.first())
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun selectTechnology(tech: Technology) {
        val allTechs = _uiState.value.technologies.associateBy { it.id }
        val cumulative = calculateCumulativePacks(tech, allTechs)
        _uiState.value = _uiState.value.copy(
            selectedTech = tech,
            cumulativePacks = cumulative
        )
    }

    /**
     * Recursively traverses all prerequisites to sum total science pack costs.
     */
    private fun calculateCumulativePacks(
        target: Technology,
        allTechs: Map<String, Technology>
    ): Map<String, Long> {
        val visited = mutableSetOf<String>()
        val total = mutableMapOf<String, Long>()

        fun dfs(t: Technology) {
            if (visited.contains(t.id)) return
            visited.add(t.id)

            // Recurse into prerequisites first
            for (pId in t.prerequisiteIds) {
                allTechs[pId]?.let { dfs(it) }
            }

            // Add current technology cost
            for (pack in t.sciencePacks) {
                val packCost = pack.amount.toLong() * t.unitCount
                total[pack.sciencePackId] = (total[pack.sciencePackId] ?: 0L) + packCost
            }
        }

        dfs(target)
        return total
    }
}
