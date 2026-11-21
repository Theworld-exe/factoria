package com.factoria.ui.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.factoria.FactoriaApplication
import com.factoria.core.model.Recipe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RecipesUiState(
    val searchQuery: String = "",
    val selectedCategory: String = "All",
    val isLoading: Boolean = true,
    val allRecipes: List<Recipe> = emptyList(),
    val filteredRecipes: List<Recipe> = emptyList(),
    val selectedRecipe: Recipe? = null
)

class RecipesViewModel : ViewModel() {
    private val repository = FactoriaApplication.instance.repository

    private val _uiState = MutableStateFlow(RecipesUiState())
    val uiState: StateFlow<RecipesUiState> = _uiState.asStateFlow()

    init {
        loadRecipes()
    }

    private fun loadRecipes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.getRecipes().collect { recipes ->
                val state = _uiState.value
                _uiState.value = state.copy(
                    allRecipes = recipes,
                    isLoading = false
                )
                applyFilters()
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFilters()
    }

    fun selectCategory(category: String) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        applyFilters()
    }

    fun selectRecipe(recipe: Recipe) {
        _uiState.value = _uiState.value.copy(selectedRecipe = recipe)
    }

    fun dismissDetail() {
        _uiState.value = _uiState.value.copy(selectedRecipe = null)
    }

    private fun applyFilters() {
        val state = _uiState.value
        val query = state.searchQuery.trim().lowercase()

        val filtered = state.allRecipes.filter { recipe ->
            val matchesCategory = state.selectedCategory == "All" ||
                    recipe.category.equals(state.selectedCategory, ignoreCase = true) ||
                    (state.selectedCategory == "Oil" && recipe.category.contains("oil", ignoreCase = true))

            val matchesSearch = query.isBlank() ||
                    recipe.name.lowercase().contains(query) ||
                    recipe.localizedName.lowercase().contains(query) ||
                    recipe.category.lowercase().contains(query) ||
                    recipe.ingredients.any { it.targetId.lowercase().contains(query) } ||
                    recipe.products.any { it.targetId.lowercase().contains(query) }

            matchesCategory && matchesSearch
        }

        _uiState.value = state.copy(filteredRecipes = filtered)
    }
}
