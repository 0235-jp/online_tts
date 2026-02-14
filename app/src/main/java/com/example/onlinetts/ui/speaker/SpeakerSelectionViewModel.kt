package com.example.onlinetts.ui.speaker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.onlinetts.data.model.Speaker
import com.example.onlinetts.data.repository.SettingsRepository
import com.example.onlinetts.tts.api.TtsApiResult
import com.example.onlinetts.tts.provider.TtsProviderFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SpeakerSelectionUiState(
    val speakers: List<Speaker> = emptyList(),
    val selectedSpeakerId: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class SpeakerSelectionViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val ttsProviderFactory: TtsProviderFactory,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SpeakerSelectionUiState())
    val uiState: StateFlow<SpeakerSelectionUiState> = _uiState.asStateFlow()

    init {
        loadSpeakers()
    }

    fun loadSpeakers() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val settings = settingsRepository.settingsFlow.first()
            _uiState.value = _uiState.value.copy(selectedSpeakerId = settings.selectedSpeakerId)

            val provider = ttsProviderFactory.create(settings.providerType)
            when (val result = provider.getSpeakers()) {
                is TtsApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        speakers = result.data,
                        isLoading = false,
                    )
                }
                is TtsApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = result.message,
                        isLoading = false,
                    )
                }
            }
        }
    }

    fun selectSpeaker(speaker: Speaker) {
        viewModelScope.launch {
            val displayName = if (speaker.styleName.isNotBlank()) {
                "${speaker.name} (${speaker.styleName})"
            } else {
                speaker.name
            }
            settingsRepository.updateSelectedSpeaker(speaker.styleId, displayName)
            _uiState.value = _uiState.value.copy(selectedSpeakerId = speaker.styleId)
        }
    }
}
