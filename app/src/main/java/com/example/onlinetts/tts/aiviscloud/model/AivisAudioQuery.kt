package com.example.onlinetts.tts.aiviscloud.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AivisTtsRequest(
    @SerialName("model_uuid") val modelUuid: String,
    val text: String,
    @SerialName("speaker_uuid") val speakerUuid: String? = null,
    @SerialName("style_id") val styleId: Int? = null,
    @SerialName("use_ssml") val useSsml: Boolean = true,
    @SerialName("speaking_rate") val speakingRate: Float = 1.0f,
    val pitch: Float = 0.0f,
    val volume: Float = 1.0f,
    @SerialName("emotional_intensity") val emotionalIntensity: Float = 1.0f,
    @SerialName("output_format") val outputFormat: String = "wav",
    @SerialName("output_sampling_rate") val outputSamplingRate: Int = 44100,
)
