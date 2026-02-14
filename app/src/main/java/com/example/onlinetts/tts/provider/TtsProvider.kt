package com.example.onlinetts.tts.provider

import com.example.onlinetts.tts.api.SynthesisResult
import com.example.onlinetts.tts.api.TtsApiResult

interface TtsProvider {
    val type: TtsProviderType
    val voiceSearchLabel: String
    fun getSupportedParams(): List<VoiceParamSpec>
    suspend fun searchVoices(query: String): TtsApiResult<List<Voice>>
    suspend fun synthesize(
        text: String,
        voiceId: String,
        params: Map<String, Float>,
    ): TtsApiResult<SynthesisResult>
    fun isConfigured(): Boolean
}
