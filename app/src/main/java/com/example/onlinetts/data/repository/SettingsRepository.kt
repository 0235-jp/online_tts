package com.example.onlinetts.data.repository

import com.example.onlinetts.data.model.TtsSettings
import com.example.onlinetts.data.model.VoiceParams
import com.example.onlinetts.tts.provider.TtsProviderType
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settingsFlow: Flow<TtsSettings>
    suspend fun getSettings(): TtsSettings
    suspend fun updateProviderType(type: TtsProviderType)
    suspend fun updateSelectedSpeaker(id: Int, name: String)
    suspend fun updateVoiceParams(params: VoiceParams)
    fun getApiKey(providerType: TtsProviderType): String
    fun setApiKey(providerType: TtsProviderType, apiKey: String)
}
