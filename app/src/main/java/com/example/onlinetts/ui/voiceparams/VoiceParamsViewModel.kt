package com.example.onlinetts.ui.voiceparams

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.onlinetts.data.model.VoiceParams
import com.example.onlinetts.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VoiceParamsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _voiceParams = MutableStateFlow(VoiceParams())
    val voiceParams: StateFlow<VoiceParams> = _voiceParams.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                _voiceParams.value = settings.voiceParams
            }
        }
    }

    fun updateSpeakingRate(value: Float) {
        _voiceParams.value = _voiceParams.value.copy(speakingRate = value)
        saveParams()
    }

    fun updatePitch(value: Float) {
        _voiceParams.value = _voiceParams.value.copy(pitch = value)
        saveParams()
    }

    fun updateVolume(value: Float) {
        _voiceParams.value = _voiceParams.value.copy(volume = value)
        saveParams()
    }

    fun updateEmotionalIntensity(value: Float) {
        _voiceParams.value = _voiceParams.value.copy(emotionalIntensity = value)
        saveParams()
    }

    fun resetToDefaults() {
        _voiceParams.value = VoiceParams()
        saveParams()
    }

    private fun saveParams() {
        viewModelScope.launch {
            settingsRepository.updateVoiceParams(_voiceParams.value)
        }
    }
}
