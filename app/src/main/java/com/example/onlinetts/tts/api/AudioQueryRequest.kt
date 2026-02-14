package com.example.onlinetts.tts.api

import com.example.onlinetts.data.model.VoiceParams

data class AudioQueryRequest(
    val text: String,
    val speakerId: Int,
    val voiceParams: VoiceParams = VoiceParams(),
)
