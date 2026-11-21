package com.factoria.ui.datapack

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.factoria.core.database.sync.ActiveDatabaseMetadata
import com.factoria.core.database.sync.DatabaseSyncManager
import com.factoria.core.database.sync.DatasetEntry
import com.factoria.core.database.sync.SyncState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DataPackViewModel(application: Application) : AndroidViewModel(application) {

    private val syncManager = DatabaseSyncManager(application)

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _activeMetadata = MutableStateFlow<ActiveDatabaseMetadata?>(null)
    val activeMetadata: StateFlow<ActiveDatabaseMetadata?> = _activeMetadata.asStateFlow()

    init {
        loadActiveMetadata()
    }

    fun loadActiveMetadata() {
        viewModelScope.launch {
            _activeMetadata.value = syncManager.getActiveMetadata()
        }
    }

    fun checkForUpdate(datasetId: String = "2.0") {
        viewModelScope.launch {
            _syncState.value = SyncState.CheckingForUpdates(datasetId)
            val result = syncManager.checkForUpdate(targetDatasetId = datasetId)
            _syncState.value = result
        }
    }

    fun applyUpdate(entry: DatasetEntry) {
        viewModelScope.launch {
            syncManager.downloadAndApplyUpdate(entry).collect { state ->
                _syncState.value = state
                if (state is SyncState.Success) {
                    loadActiveMetadata()
                }
            }
        }
    }

    fun resetSyncState() {
        _syncState.value = SyncState.Idle
    }
}
