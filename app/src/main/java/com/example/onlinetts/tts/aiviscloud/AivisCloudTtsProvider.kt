package com.example.onlinetts.tts.aiviscloud

import com.example.onlinetts.data.preferences.EncryptedPreferences
import com.example.onlinetts.tts.aiviscloud.model.AivisTtsRequest
import com.example.onlinetts.tts.api.SynthesisResult
import com.example.onlinetts.tts.api.TtsApiResult
import com.example.onlinetts.tts.engine.WavParser
import com.example.onlinetts.tts.provider.TtsProvider
import com.example.onlinetts.tts.provider.TtsProviderType
import com.example.onlinetts.tts.provider.Voice
import com.example.onlinetts.tts.provider.VoiceParamSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AivisCloudTtsProvider @Inject constructor(
    private val apiClient: AivisCloudApiClient,
    private val encryptedPreferences: EncryptedPreferences,
) : TtsProvider {

    override val type = TtsProviderType.AIVIS_CLOUD

    override val voiceSearchLabel = "モデル UUID"

    override fun getSupportedParams(): List<VoiceParamSpec> = listOf(
        VoiceParamSpec("speaking_rate", "速度", 1.0f, 0.5f..2.0f),
        VoiceParamSpec("pitch", "ピッチ", 0.0f, -1.0f..1.0f),
        VoiceParamSpec("volume", "音量", 1.0f, 0.0f..2.0f),
        VoiceParamSpec("emotional_intensity", "抑揚", 1.0f, 0.0f..2.0f),
    )

    override suspend fun searchVoices(query: String): TtsApiResult<List<Voice>> {
        return try {
            val apiKey = encryptedPreferences.getApiKey(type)
            if (apiKey.isBlank()) {
                return TtsApiResult.Error("API キーが設定されていません")
            }

            val modelUuid = query.trim()
            val model = apiClient.getModel(modelUuid, apiKey)
            val voices = model.speakers.flatMap { speaker ->
                speaker.styles.map { style ->
                    val voiceId = "$modelUuid:${style.localId}"
                    val styleSuffix = if (style.name.isNotBlank()) " (${style.name})" else ""
                    Voice(
                        id = voiceId,
                        name = "${speaker.name}$styleSuffix",
                        description = "ID: ${style.localId}",
                    )
                }
            }
            TtsApiResult.Success(voices)
        } catch (e: Exception) {
            TtsApiResult.Error("話者情報の取得に失敗しました: ${e.message}", e)
        }
    }

    override suspend fun synthesize(
        text: String,
        voiceId: String,
        params: Map<String, Float>,
    ): TtsApiResult<SynthesisResult> {
        return try {
            val apiKey = encryptedPreferences.getApiKey(type)
            if (apiKey.isBlank()) {
                return TtsApiResult.Error("API キーが設定されていません")
            }

            val (modelUuid, styleId) = decodeVoiceId(voiceId)

            val ttsRequest = AivisTtsRequest(
                modelUuid = modelUuid,
                text = text,
                styleId = styleId,
                speakingRate = params["speaking_rate"] ?: 1.0f,
                pitch = params["pitch"] ?: 0.0f,
                volume = params["volume"] ?: 1.0f,
                emotionalIntensity = params["emotional_intensity"] ?: 1.0f,
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

    override fun isConfigured(): Boolean {
        return encryptedPreferences.getApiKey(type).isNotBlank()
    }

    private fun decodeVoiceId(voiceId: String): Pair<String, Int?> {
        val parts = voiceId.split(":")
        val modelUuid = parts.dropLast(1).joinToString(":")
        val styleId = parts.lastOrNull()?.toIntOrNull()
        return modelUuid to styleId
    }
}
