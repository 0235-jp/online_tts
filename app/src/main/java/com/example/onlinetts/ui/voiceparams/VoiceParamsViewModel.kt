package com.example.onlinetts.ui.voiceparams

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.onlinetts.data.repository.SettingsRepository
import com.example.onlinetts.tts.provider.TtsProviderFactory
import com.example.onlinetts.tts.provider.VoiceParamSpec
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VoiceParamsUiState(
    val specs: List<VoiceParamSpec> = emptyList(),
    val values: Map<String, Float> = emptyMap(),
)

@HiltViewModel
class VoiceParamsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val ttsProviderFactory: TtsProviderFactory,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VoiceParamsUiState())
    val uiState: StateFlow<VoiceParamsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = settingsRepository.settingsFlow.first()
            val provider = ttsProviderFactory.create(settings.providerType)
            val specs = provider.getSupportedParams()
            val defaults = specs.associate { it.key to it.defaultValue }
            val values = defaults + settings.voiceParams
            _uiState.value = VoiceParamsUiState(specs = specs, values = values)
        }
    }

    fun updateParam(key: String, value: Float) {
        val newValues = _uiState.value.values + (key to value)
        _uiState.value = _uiState.value.copy(values = newValues)
        saveParams(newValues)
    }

    fun resetToDefaults() {
        val defaults = _uiState.value.specs.associate { it.key to it.defaultValue }
        _uiState.value = _uiState.value.copy(values = defaults)
        saveParams(defaults)
    }

    private fun saveParams(params: Map<String, Float>) {
        viewModelScope.launch {
            settingsRepository.updateVoiceParams(params)
        }
    }
}
