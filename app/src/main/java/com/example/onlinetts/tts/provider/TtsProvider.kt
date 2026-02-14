package com.example.onlinetts.tts.provider

import com.example.onlinetts.data.model.Speaker
import com.example.onlinetts.tts.api.AudioQueryRequest
import com.example.onlinetts.tts.api.SynthesisResult
import com.example.onlinetts.tts.api.TtsApiResult

interface TtsProvider {
    val type: TtsProviderType
    suspend fun synthesize(request: AudioQueryRequest): TtsApiResult<SynthesisResult>
    suspend fun getSpeakers(modelUuid: String): TtsApiResult<List<Speaker>>
    fun isConfigured(): Boolean
}
