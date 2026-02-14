package com.example.onlinetts.data.model

import kotlinx.serialization.Serializable

@Serializable
data class VoiceParams(
    val speakingRate: Float = 1.0f,
    val pitch: Float = 0.0f,
    val volume: Float = 1.0f,
    val emotionalIntensity: Float = 1.0f,
) {
    companion object {
        val SPEAKING_RATE_RANGE = 0.5f..2.0f
        val PITCH_RANGE = -1.0f..1.0f
        val VOLUME_RANGE = 0.0f..2.0f
        val EMOTIONAL_INTENSITY_RANGE = 0.0f..2.0f
    }
}
