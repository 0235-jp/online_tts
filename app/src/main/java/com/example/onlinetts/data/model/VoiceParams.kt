package com.example.onlinetts.data.model

import kotlinx.serialization.Serializable

@Serializable
data class VoiceParams(
    val speedScale: Float = 1.0f,
    val pitchScale: Float = 0.0f,
    val volumeScale: Float = 1.0f,
    val intonationScale: Float = 1.0f,
) {
    companion object {
        val SPEED_RANGE = 0.5f..2.0f
        val PITCH_RANGE = -0.15f..0.15f
        val VOLUME_RANGE = 0.0f..2.0f
        val INTONATION_RANGE = 0.0f..2.0f
    }
}
