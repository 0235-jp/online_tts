package com.example.onlinetts.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.onlinetts.data.model.TtsSettings
import com.example.onlinetts.tts.provider.TtsProviderType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TtsPreferencesDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
) {
    private companion object {
        val PROVIDER_TYPE = stringPreferencesKey("provider_type")
        val SELECTED_VOICE_ID = stringPreferencesKey("selected_voice_id")
        val SELECTED_VOICE_NAME = stringPreferencesKey("selected_voice_name")
        val VOICE_PARAMS = stringPreferencesKey("voice_params")
    }

    val settingsFlow: Flow<TtsSettings> = dataStore.data.map { prefs ->
        TtsSettings(
            providerType = prefs[PROVIDER_TYPE]?.let {
                try { TtsProviderType.valueOf(it) } catch (_: Exception) { null }
            } ?: TtsProviderType.AIVIS_CLOUD,
            selectedVoiceId = prefs[SELECTED_VOICE_ID] ?: "",
            selectedVoiceName = prefs[SELECTED_VOICE_NAME] ?: "",
            voiceParams = prefs[VOICE_PARAMS]?.let {
                try {
                    json.decodeFromString<Map<String, Float>>(it)
                } catch (_: Exception) {
                    emptyMap()
                }
            } ?: emptyMap(),
        )
    }

    suspend fun updateProviderType(type: TtsProviderType) {
        dataStore.edit { it[PROVIDER_TYPE] = type.name }
    }

    suspend fun updateSelectedVoice(voiceId: String, voiceName: String) {
        dataStore.edit {
            it[SELECTED_VOICE_ID] = voiceId
            it[SELECTED_VOICE_NAME] = voiceName
        }
    }

    suspend fun updateVoiceParams(params: Map<String, Float>) {
        dataStore.edit {
            it[VOICE_PARAMS] = json.encodeToString(params)
        }
    }
}
