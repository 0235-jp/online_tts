package com.example.onlinetts.tts.aiviscloud.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AivisModelResponse(
    val speakers: List<AivisSpeaker> = emptyList(),
)

@Serializable
data class AivisSpeaker(
    val name: String,
    @SerialName("aivm_speaker_uuid") val speakerUuid: String = "",
    val styles: List<AivisStyle> = emptyList(),
)

@Serializable
data class AivisStyle(
    val name: String,
    @SerialName("local_id") val localId: Int,
)
