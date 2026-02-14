package com.example.onlinetts.tts.api

import com.example.onlinetts.data.model.VoiceParams

data class AudioQueryRequest(
    val text: String,
    val modelUuid: String,
    val speakerUuid: String = "",
    val styleId: Int? = null,
    val voiceParams: VoiceParams = VoiceParams(),
)
