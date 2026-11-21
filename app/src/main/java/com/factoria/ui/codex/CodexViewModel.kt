package com.factoria.ui.codex

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.factoria.FactoriaApplication
import com.factoria.core.database.SearchResultItem
import com.factoria.core.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CodexUiState(
    val searchQuery: String = "",
    val selectedCategory: String = "All",
    val searchResults: List<SearchResultItem> = emptyList(),
    val items: List<Item> = emptyList(),
    val fluids: List<Fluid> = emptyList(),
    val recipes: List<Recipe> = emptyList(),
    val entities: List<Entity> = emptyList(),
    val technologies: List<Technology> = emptyList(),
    val selectedItem: Item? = null,
    val selectedRecipe: Recipe? = null,
    val selectedEntity: Entity? = null,
    val selectedTechnology: Technology? = null,
    val relatedUsedIn: List<Recipe> = emptyList(),
    val relatedProducedBy: List<Recipe> = emptyList(),
    val isLoading: Boolean = false
)

class CodexViewModel : ViewModel() {
    private val repository = FactoriaApplication.instance.repository
    private val _uiState = MutableStateFlow(CodexUiState())
    val uiState: StateFlow<CodexUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var detailJob: Job? = null

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true)

            launch {
                repository.getItems().collect { list ->
                    _uiState.value = _uiState.value.copy(items = list)
                }
            }
            launch {
                repository.getFluids().collect { list ->
                    _uiState.value = _uiState.value.copy(fluids = list)
                }
            }
            launch {
                repository.getRecipes().collect { list ->
                    _uiState.value = _uiState.value.copy(recipes = list)
                }
            }
            launch {
                repository.getEntities().collect { list ->
                    _uiState.value = _uiState.value.copy(entities = list)
                }
            }
            launch {
                repository.getTechnologies().collect { list ->
                    _uiState.value = _uiState.value.copy(technologies = list)
                }
            }

            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(searchResults = emptyList())
            return
        }

        searchJob = viewModelScope.launch(Dispatchers.IO) {
            val results = repository.search(query)
            _uiState.value = _uiState.value.copy(searchResults = results)
        }
    }

    fun selectCategory(category: String) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun selectItem(item: Item) {
        detailJob?.cancel()
        _uiState.value = _uiState.value.copy(
            selectedItem = item,
            relatedUsedIn = emptyList(),
            relatedProducedBy = emptyList()
        )
        detailJob = viewModelScope.launch(Dispatchers.IO) {
            launch {
                repository.getRecipesUsing(item.id).collect { usedIn ->
                    _uiState.value = _uiState.value.copy(relatedUsedIn = usedIn)
                }
            }
            launch {
                repository.getRecipesProducing(item.id).collect { prodBy ->
                    _uiState.value = _uiState.value.copy(relatedProducedBy = prodBy)
                }
            }
        }
    }

    fun selectRecipe(recipe: Recipe) {
        _uiState.value = _uiState.value.copy(selectedRecipe = recipe)
    }

    fun selectEntity(entity: Entity) {
        _uiState.value = _uiState.value.copy(selectedEntity = entity)
    }

    fun selectTechnology(tech: Technology) {
        _uiState.value = _uiState.value.copy(selectedTechnology = tech)
    }

    fun dismissDetail() {
        detailJob?.cancel()
        detailJob = null
        _uiState.value = _uiState.value.copy(
            selectedItem = null,
            selectedRecipe = null,
            selectedEntity = null,
            selectedTechnology = null,
            relatedUsedIn = emptyList(),
            relatedProducedBy = emptyList()
        )
    }
}
