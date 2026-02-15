package com.example.onlinetts.tts.provider

import com.example.onlinetts.tts.api.SynthesisEvent
import com.example.onlinetts.tts.api.SynthesisResult
import com.example.onlinetts.tts.api.TtsApiResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

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

    fun synthesizeStreaming(
        text: String,
        voiceId: String,
        params: Map<String, Float>,
    ): Flow<SynthesisEvent> = flow {
        when (val result = synthesize(text, voiceId, params)) {
            is TtsApiResult.Success -> {
                val data = result.data
                emit(SynthesisEvent.Started(data.sampleRate, data.channels, data.bitsPerSample))
                emit(SynthesisEvent.Audio(data.pcmData))
                emit(SynthesisEvent.Done)
            }
            is TtsApiResult.Error -> {
                emit(SynthesisEvent.Error(result.message, result.cause))
            }
        }
    }
}
