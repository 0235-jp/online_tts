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
        val SPEED_SCALE = floatPreferencesKey("speed_scale")
        val PITCH_SCALE = floatPreferencesKey("pitch_scale")
        val VOLUME_SCALE = floatPreferencesKey("volume_scale")
        val INTONATION_SCALE = floatPreferencesKey("intonation_scale")
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
                speedScale = prefs[SPEED_SCALE] ?: 1.0f,
                pitchScale = prefs[PITCH_SCALE] ?: 0.0f,
                volumeScale = prefs[VOLUME_SCALE] ?: 1.0f,
                intonationScale = prefs[INTONATION_SCALE] ?: 1.0f,
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
            it[SPEED_SCALE] = params.speedScale
            it[PITCH_SCALE] = params.pitchScale
            it[VOLUME_SCALE] = params.volumeScale
            it[INTONATION_SCALE] = params.intonationScale
        }
    }
}
