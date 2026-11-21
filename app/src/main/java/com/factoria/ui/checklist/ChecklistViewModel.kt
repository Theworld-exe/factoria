package com.factoria.ui.checklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.factoria.FactoriaApplication
import com.factoria.core.database.entity.ChecklistEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ChecklistUiState(
    val selectedPlanet: String = "All",
    val newTaskTitle: String = "",
    val newTaskPlanet: String = "Nauvis"
)

class ChecklistViewModel : ViewModel() {
    private val repository = FactoriaApplication.instance.repository

    private val _uiState = MutableStateFlow(ChecklistUiState())
    val uiState: StateFlow<ChecklistUiState> = _uiState.asStateFlow()

    val tasks: StateFlow<List<ChecklistEntity>> = repository.getChecklistTasks()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setNewTaskTitle(title: String) {
        _uiState.value = _uiState.value.copy(newTaskTitle = title)
    }

    fun setNewTaskPlanet(planet: String) {
        _uiState.value = _uiState.value.copy(newTaskPlanet = planet)
    }

    fun selectPlanetFilter(planet: String) {
        _uiState.value = _uiState.value.copy(selectedPlanet = planet)
    }

    fun addTask() {
        val state = _uiState.value
        if (state.newTaskTitle.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            repository.insertChecklistTask(
                ChecklistEntity(
                    title = state.newTaskTitle.trim(),
                    planet = state.newTaskPlanet
                )
            )
            _uiState.value = state.copy(newTaskTitle = "")
        }
    }

    fun toggleTask(taskId: Long, currentCompleted: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateChecklistTaskStatus(taskId, !currentCompleted)
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteChecklistTask(taskId)
        }
    }
}
