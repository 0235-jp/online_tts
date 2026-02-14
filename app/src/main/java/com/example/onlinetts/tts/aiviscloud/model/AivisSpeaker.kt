package com.example.onlinetts.tts.aiviscloud.model

import kotlinx.serialization.Serializable

@Serializable
data class AivisSpeaker(
    val name: String,
    val speaker_uuid: String = "",
    val styles: List<AivisStyle> = emptyList(),
    val version: String = "",
)

@Serializable
data class AivisStyle(
    val name: String,
    val id: Int,
)
