package com.example.onlinetts.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.onlinetts.data.model.TtsSettings
import com.example.onlinetts.data.repository.SettingsRepository
import com.example.onlinetts.tts.provider.TtsProviderFactory
import com.example.onlinetts.tts.provider.TtsProviderType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: TtsSettings = TtsSettings(),
    val apiKey: String = "",
    val isApiKeyVisible: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val ttsProviderFactory: TtsProviderFactory,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                val apiKey = settingsRepository.getApiKey(settings.providerType)
                _uiState.value = _uiState.value.copy(settings = settings, apiKey = apiKey)
            }
        }
    }

    fun updateProviderType(type: TtsProviderType) {
        viewModelScope.launch {
            settingsRepository.updateProviderType(type)
            val apiKey = settingsRepository.getApiKey(type)
            _uiState.value = _uiState.value.copy(apiKey = apiKey)
        }
    }

    fun updateApiKey(apiKey: String) {
        _uiState.value = _uiState.value.copy(apiKey = apiKey)
    }

    fun saveApiKey() {
        val state = _uiState.value
        settingsRepository.setApiKey(state.settings.providerType, state.apiKey)
    }

    fun toggleApiKeyVisibility() {
        _uiState.value = _uiState.value.copy(isApiKeyVisible = !_uiState.value.isApiKeyVisible)
    }

    fun getProvider() = ttsProviderFactory.create(_uiState.value.settings.providerType)
}
