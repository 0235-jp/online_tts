package com.example.onlinetts.tts.provider

data class VoiceParamSpec(
    val key: String,
    val label: String,
    val defaultValue: Float,
    val range: ClosedFloatingPointRange<Float>,
)
