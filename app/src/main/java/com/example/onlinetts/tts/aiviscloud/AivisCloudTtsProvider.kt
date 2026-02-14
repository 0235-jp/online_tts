package com.example.onlinetts.tts.aiviscloud

import com.example.onlinetts.data.model.Speaker
import com.example.onlinetts.data.preferences.EncryptedPreferences
import com.example.onlinetts.tts.aiviscloud.model.AivisTtsRequest
import com.example.onlinetts.tts.api.AudioQueryRequest
import com.example.onlinetts.tts.api.SynthesisResult
import com.example.onlinetts.tts.api.TtsApiResult
import com.example.onlinetts.tts.engine.WavParser
import com.example.onlinetts.tts.provider.TtsProvider
import com.example.onlinetts.tts.provider.TtsProviderType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AivisCloudTtsProvider @Inject constructor(
    private val apiClient: AivisCloudApiClient,
    private val encryptedPreferences: EncryptedPreferences,
) : TtsProvider {

    override val type = TtsProviderType.AIVIS_CLOUD

    override suspend fun synthesize(request: AudioQueryRequest): TtsApiResult<SynthesisResult> {
        return try {
            val apiKey = encryptedPreferences.getApiKey(type)
            if (apiKey.isBlank()) {
                return TtsApiResult.Error("API キーが設定されていません")
            }

            val ttsRequest = AivisTtsRequest(
                modelUuid = request.modelUuid,
                text = request.text,
                speakerUuid = request.speakerUuid.ifBlank { null },
                styleId = request.styleId,
                speakingRate = request.voiceParams.speakingRate,
                pitch = request.voiceParams.pitch,
                volume = request.voiceParams.volume,
                emotionalIntensity = request.voiceParams.emotionalIntensity,
                outputFormat = "wav",
                outputSamplingRate = 44100,
            )

            val wavData = apiClient.synthesize(ttsRequest, apiKey)
            val result = WavParser.parse(wavData)
            TtsApiResult.Success(result)
        } catch (e: Exception) {
            TtsApiResult.Error("音声合成に失敗しました: ${e.message}", e)
        }
    }

    override suspend fun getSpeakers(modelUuid: String): TtsApiResult<List<Speaker>> {
        return try {
            val apiKey = encryptedPreferences.getApiKey(type)
            if (apiKey.isBlank()) {
                return TtsApiResult.Error("API キーが設定されていません")
            }

            val model = apiClient.getModel(modelUuid, apiKey)
            val result = model.speakers.flatMap { speaker ->
                speaker.styles.map { style ->
                    Speaker(
                        name = speaker.name,
                        speakerUuid = speaker.speakerUuid,
                        styleId = style.localId,
                        styleName = style.name,
                    )
                }
            }
            TtsApiResult.Success(result)
        } catch (e: Exception) {
            TtsApiResult.Error("話者情報の取得に失敗しました: ${e.message}", e)
        }
    }

    override fun isConfigured(): Boolean {
        return encryptedPreferences.getApiKey(type).isNotBlank()
    }
}
