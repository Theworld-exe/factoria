package com.factoria.ui.blueprint

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.factoria.FactoriaApplication
import com.factoria.core.blueprint.BlueprintDecodeResult
import com.factoria.core.blueprint.BlueprintDecoder
import com.factoria.core.blueprint.BomCalculator
import com.factoria.core.blueprint.SampleBlueprints
import com.factoria.core.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class BlueprintUiState(
    val blueprintStringInput: String = "",
    val isDecoding: Boolean = false,
    val decodedWrapper: BlueprintStringWrapper? = null,
    val activeBlueprint: BlueprintData? = null,
    val bom: BillOfMaterials? = null,
    val errorMessage: String? = null,
    val knownEntities: Set<String> = emptySet(),
    val knownItems: Set<String> = emptySet()
)

class BlueprintViewModel : ViewModel() {
    private val repository = FactoriaApplication.instance.repository
    private val _uiState = MutableStateFlow(BlueprintUiState())
    val uiState: StateFlow<BlueprintUiState> = _uiState.asStateFlow()

    init {
        loadKnownIds()
    }

    private fun loadKnownIds() {
        viewModelScope.launch(Dispatchers.IO) {
            launch {
                repository.getEntities().collect { list ->
                    _uiState.value = _uiState.value.copy(knownEntities = list.map { it.id }.toSet())
                }
            }
            launch {
                repository.getItems().collect { list ->
                    _uiState.value = _uiState.value.copy(knownItems = list.map { it.id }.toSet())
                }
            }
        }
    }

    fun onInputChanged(input: String) {
        _uiState.value = _uiState.value.copy(blueprintStringInput = input)
    }

    fun decodeBlueprint(strOverride: String? = null) {
        val raw = strOverride ?: _uiState.value.blueprintStringInput
        if (raw.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter or paste a blueprint string.")
            return
        }

        _uiState.value = _uiState.value.copy(isDecoding = true, errorMessage = null)

        viewModelScope.launch(Dispatchers.Default) {
            val result = BlueprintDecoder.decode(raw)
            withContext(Dispatchers.Main) {
                when (result) {
                    is BlueprintDecodeResult.Success -> {
                        val wrapper = result.wrapper
                        val activeBp = wrapper.blueprint ?: wrapper.blueprintBook?.blueprints?.firstOrNull()?.blueprint
                        val bom = if (activeBp != null) {
                            BomCalculator.computeBOM(
                                activeBp,
                                _uiState.value.knownEntities,
                                _uiState.value.knownItems
                            )
                        } else null

                        _uiState.value = _uiState.value.copy(
                            isDecoding = false,
                            decodedWrapper = wrapper,
                            activeBlueprint = activeBp,
                            bom = bom,
                            errorMessage = null
                        )
                    }
                    is BlueprintDecodeResult.Failure -> {
                        _uiState.value = _uiState.value.copy(
                            isDecoding = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    fun selectBlueprintFromBook(bp: BlueprintData) {
        viewModelScope.launch(Dispatchers.Default) {
            val bom = BomCalculator.computeBOM(bp, _uiState.value.knownEntities, _uiState.value.knownItems)
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(
                    activeBlueprint = bp,
                    bom = bom
                )
            }
        }
    }

    fun loadSampleBlueprint() {
        val sample = SampleBlueprints.SOLAR_STARTER.blueprintString
        _uiState.value = _uiState.value.copy(blueprintStringInput = sample)
        decodeBlueprint(sample)
    }

    fun inspectPresetBlueprint(blueprintString: String) {
        _uiState.value = _uiState.value.copy(blueprintStringInput = blueprintString)
        decodeBlueprint(blueprintString)
    }
}
