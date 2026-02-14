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

    fun updateSpeedScale(value: Float) {
        _voiceParams.value = _voiceParams.value.copy(speedScale = value)
        saveParams()
    }

    fun updatePitchScale(value: Float) {
        _voiceParams.value = _voiceParams.value.copy(pitchScale = value)
        saveParams()
    }

    fun updateVolumeScale(value: Float) {
        _voiceParams.value = _voiceParams.value.copy(volumeScale = value)
        saveParams()
    }

    fun updateIntonationScale(value: Float) {
        _voiceParams.value = _voiceParams.value.copy(intonationScale = value)
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
