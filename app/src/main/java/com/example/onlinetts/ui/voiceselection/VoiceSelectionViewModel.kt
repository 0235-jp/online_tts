package com.example.onlinetts.ui.voiceselection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.onlinetts.data.repository.SettingsRepository
import com.example.onlinetts.tts.api.TtsApiResult
import com.example.onlinetts.tts.provider.TtsProviderFactory
import com.example.onlinetts.tts.provider.Voice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VoiceSelectionUiState(
    val query: String = "",
    val voices: List<Voice> = emptyList(),
    val selectedVoiceId: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
    val voiceSearchLabel: String = "",
)

@HiltViewModel
class VoiceSelectionViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val ttsProviderFactory: TtsProviderFactory,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VoiceSelectionUiState())
    val uiState: StateFlow<VoiceSelectionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = settingsRepository.settingsFlow.first()
            val provider = ttsProviderFactory.create(settings.providerType)
            val voiceId = settings.selectedVoiceId
            val query = extractQueryFromVoiceId(voiceId)
            _uiState.value = _uiState.value.copy(
                query = query,
                selectedVoiceId = voiceId,
                voiceSearchLabel = provider.voiceSearchLabel,
            )
            if (query.isNotBlank()) {
                search()
            }
        }
    }

    fun updateQuery(query: String) {
        _uiState.value = _uiState.value.copy(query = query, saved = false)
    }

    fun search() {
        val query = _uiState.value.query.trim()
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "検索クエリを入力してください")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, voices = emptyList())
            val settings = settingsRepository.settingsFlow.first()
            val provider = ttsProviderFactory.create(settings.providerType)

            when (val result = provider.searchVoices(query)) {
                is TtsApiResult.Success -> {
                    val voices = result.data
                    if (voices.isEmpty()) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "音声が見つかりません",
                        )
                    } else {
                        val preselected = voices.find { it.id == _uiState.value.selectedVoiceId }
                        val selectedId = preselected?.id ?: voices.first().id
                        _uiState.value = _uiState.value.copy(
                            voices = voices,
                            selectedVoiceId = selectedId,
                            isLoading = false,
                        )
                    }
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

    fun selectVoice(voiceId: String) {
        _uiState.value = _uiState.value.copy(selectedVoiceId = voiceId, saved = false)
    }

    fun save() {
        val state = _uiState.value
        val selected = state.voices.find { it.id == state.selectedVoiceId } ?: return
        viewModelScope.launch {
            settingsRepository.updateSelectedVoice(selected.id, selected.name)
            _uiState.value = _uiState.value.copy(saved = true)
        }
    }

    private fun extractQueryFromVoiceId(voiceId: String): String {
        if (voiceId.isBlank()) return ""
        val lastColon = voiceId.lastIndexOf(':')
        return if (lastColon > 0) voiceId.substring(0, lastColon) else voiceId
    }
}
