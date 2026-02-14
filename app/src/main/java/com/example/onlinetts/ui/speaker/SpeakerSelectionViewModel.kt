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
    val uuid: String = "",
    val styles: List<Speaker> = emptyList(),
    val selectedStyleId: Int = 0,
    val speakerName: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
)

@HiltViewModel
class SpeakerSelectionViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val ttsProviderFactory: TtsProviderFactory,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SpeakerSelectionUiState())
    val uiState: StateFlow<SpeakerSelectionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = settingsRepository.settingsFlow.first()
            _uiState.value = _uiState.value.copy(
                uuid = settings.speakerModelUuid,
                selectedStyleId = settings.selectedSpeakerId,
            )
            if (settings.speakerModelUuid.isNotBlank()) {
                resolveUuid()
            }
        }
    }

    fun updateUuid(uuid: String) {
        _uiState.value = _uiState.value.copy(uuid = uuid, saved = false)
    }

    fun resolveUuid() {
        val uuid = _uiState.value.uuid.trim()
        if (uuid.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "UUID を入力してください")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, styles = emptyList())
            val settings = settingsRepository.settingsFlow.first()
            val provider = ttsProviderFactory.create(settings.providerType)

            when (val result = provider.getSpeakers()) {
                is TtsApiResult.Success -> {
                    val matched = result.data.filter { it.speakerUuid == uuid }
                    if (matched.isEmpty()) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "UUID に一致する話者が見つかりません",
                        )
                    } else {
                        val speakerName = matched.first().name
                        val preselected = matched.find { it.styleId == _uiState.value.selectedStyleId }
                        val selectedId = preselected?.styleId ?: matched.first().styleId
                        _uiState.value = _uiState.value.copy(
                            styles = matched,
                            speakerName = speakerName,
                            selectedStyleId = selectedId,
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

    fun selectStyle(styleId: Int) {
        _uiState.value = _uiState.value.copy(selectedStyleId = styleId, saved = false)
    }

    fun save() {
        val state = _uiState.value
        val selected = state.styles.find { it.styleId == state.selectedStyleId } ?: return
        val displayName = if (selected.styleName.isNotBlank()) {
            "${selected.name} (${selected.styleName})"
        } else {
            selected.name
        }
        viewModelScope.launch {
            settingsRepository.updateSpeaker(state.uuid.trim(), selected.styleId, displayName)
            _uiState.value = _uiState.value.copy(saved = true)
        }
    }
}
