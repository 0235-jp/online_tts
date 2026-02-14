package com.example.onlinetts.tts.aiviscloud.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class AivisAudioQuery(
    val accent_phrases: List<JsonElement> = emptyList(),
    val speedScale: Float = 1.0f,
    val pitchScale: Float = 0.0f,
    val intonationScale: Float = 1.0f,
    val volumeScale: Float = 1.0f,
    val prePhonemeLength: Float = 0.1f,
    val postPhonemeLength: Float = 0.1f,
    val outputSamplingRate: Int = 44100,
    val outputStereo: Boolean = false,
    val kana: String? = null,
)
