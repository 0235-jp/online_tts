package com.example.onlinetts.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.onlinetts.data.model.TtsSettings
import com.example.onlinetts.data.model.VoiceParams
import com.example.onlinetts.tts.provider.TtsProviderType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TtsPreferencesDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private companion object {
        val PROVIDER_TYPE = stringPreferencesKey("provider_type")
        val SPEAKER_MODEL_UUID = stringPreferencesKey("speaker_model_uuid")
        val SELECTED_SPEAKER_ID = intPreferencesKey("selected_speaker_id")
        val SELECTED_SPEAKER_NAME = stringPreferencesKey("selected_speaker_name")
        val SPEAKING_RATE = floatPreferencesKey("speaking_rate")
        val PITCH = floatPreferencesKey("pitch")
        val VOLUME = floatPreferencesKey("volume")
        val EMOTIONAL_INTENSITY = floatPreferencesKey("emotional_intensity")
    }

    val settingsFlow: Flow<TtsSettings> = dataStore.data.map { prefs ->
        TtsSettings(
            providerType = prefs[PROVIDER_TYPE]?.let {
                try { TtsProviderType.valueOf(it) } catch (_: Exception) { null }
            } ?: TtsProviderType.AIVIS_CLOUD,
            speakerModelUuid = prefs[SPEAKER_MODEL_UUID] ?: "",
            selectedSpeakerId = prefs[SELECTED_SPEAKER_ID] ?: 0,
            selectedSpeakerName = prefs[SELECTED_SPEAKER_NAME] ?: "",
            voiceParams = VoiceParams(
                speakingRate = prefs[SPEAKING_RATE] ?: 1.0f,
                pitch = prefs[PITCH] ?: 0.0f,
                volume = prefs[VOLUME] ?: 1.0f,
                emotionalIntensity = prefs[EMOTIONAL_INTENSITY] ?: 1.0f,
            ),
        )
    }

    suspend fun updateProviderType(type: TtsProviderType) {
        dataStore.edit { it[PROVIDER_TYPE] = type.name }
    }

    suspend fun updateSpeaker(uuid: String, styleId: Int, styleName: String) {
        dataStore.edit {
            it[SPEAKER_MODEL_UUID] = uuid
            it[SELECTED_SPEAKER_ID] = styleId
            it[SELECTED_SPEAKER_NAME] = styleName
        }
    }

    suspend fun updateVoiceParams(params: VoiceParams) {
        dataStore.edit {
            it[SPEAKING_RATE] = params.speakingRate
            it[PITCH] = params.pitch
            it[VOLUME] = params.volume
            it[EMOTIONAL_INTENSITY] = params.emotionalIntensity
        }
    }
}
