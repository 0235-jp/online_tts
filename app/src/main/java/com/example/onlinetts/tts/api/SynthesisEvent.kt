package com.example.onlinetts.tts.api

sealed class SynthesisEvent {
    data class Started(
        val sampleRate: Int,
        val channels: Int,
        val bitsPerSample: Int = 16,
    ) : SynthesisEvent()

    data class Audio(val pcmData: ByteArray) : SynthesisEvent()

    data class Error(
        val message: String,
        val cause: Throwable? = null,
    ) : SynthesisEvent()

    data object Done : SynthesisEvent()
}
