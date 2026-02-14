package com.example.onlinetts.tts.aiviscloud

import com.example.onlinetts.data.model.Speaker
import com.example.onlinetts.data.preferences.EncryptedPreferences
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

            // 1. AudioQuery を取得
            val audioQuery = apiClient.audioQuery(request.text, request.speakerId, apiKey)

            // 2. パラメータを上書き
            val modifiedQuery = audioQuery.copy(
                speedScale = request.voiceParams.speedScale,
                pitchScale = request.voiceParams.pitchScale,
                volumeScale = request.voiceParams.volumeScale,
                intonationScale = request.voiceParams.intonationScale,
            )

            // 3. 音声合成
            val wavData = apiClient.synthesis(modifiedQuery, request.speakerId, apiKey)

            // 4. WAV → PCM 変換
            val result = WavParser.parse(wavData)
            TtsApiResult.Success(result)
        } catch (e: Exception) {
            TtsApiResult.Error("音声合成に失敗しました: ${e.message}", e)
        }
    }

    override suspend fun getSpeakers(): TtsApiResult<List<Speaker>> {
        return try {
            val apiKey = encryptedPreferences.getApiKey(type)
            if (apiKey.isBlank()) {
                return TtsApiResult.Error("API キーが設定されていません")
            }

            val speakers = apiClient.getSpeakers(apiKey)
            val result = speakers.flatMap { speaker ->
                speaker.styles.map { style ->
                    Speaker(
                        name = speaker.name,
                        speakerUuid = speaker.speaker_uuid,
                        styleId = style.id,
                        styleName = style.name,
                    )
                }
            }
            TtsApiResult.Success(result)
        } catch (e: Exception) {
            TtsApiResult.Error("話者一覧の取得に失敗しました: ${e.message}", e)
        }
    }

    override fun isConfigured(): Boolean {
        return encryptedPreferences.getApiKey(type).isNotBlank()
    }
}
