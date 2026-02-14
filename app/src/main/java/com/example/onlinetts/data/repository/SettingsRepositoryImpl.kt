package com.example.onlinetts.data.repository

import com.example.onlinetts.data.model.TtsSettings
import com.example.onlinetts.data.model.VoiceParams
import com.example.onlinetts.data.preferences.EncryptedPreferences
import com.example.onlinetts.data.preferences.TtsPreferencesDataStore
import com.example.onlinetts.tts.provider.TtsProviderType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val preferencesDataStore: TtsPreferencesDataStore,
    private val encryptedPreferences: EncryptedPreferences,
) : SettingsRepository {

    override val settingsFlow: Flow<TtsSettings> = preferencesDataStore.settingsFlow

    override suspend fun getSettings(): TtsSettings {
        return preferencesDataStore.settingsFlow.first()
    }

    override suspend fun updateProviderType(type: TtsProviderType) {
        preferencesDataStore.updateProviderType(type)
    }

    override suspend fun updateSelectedSpeaker(id: Int, name: String) {
        preferencesDataStore.updateSelectedSpeaker(id, name)
    }

    override suspend fun updateVoiceParams(params: VoiceParams) {
        preferencesDataStore.updateVoiceParams(params)
    }

    override fun getApiKey(providerType: TtsProviderType): String {
        return encryptedPreferences.getApiKey(providerType)
    }

    override fun setApiKey(providerType: TtsProviderType, apiKey: String) {
        encryptedPreferences.setApiKey(providerType, apiKey)
    }
}
