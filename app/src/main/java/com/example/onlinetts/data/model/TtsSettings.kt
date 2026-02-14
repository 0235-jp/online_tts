package com.example.onlinetts.data.model

import com.example.onlinetts.tts.provider.TtsProviderType
import kotlinx.serialization.Serializable

@Serializable
data class TtsSettings(
    val providerType: TtsProviderType = TtsProviderType.AIVIS_CLOUD,
    val selectedVoiceId: String = "",
    val selectedVoiceName: String = "",
    val voiceParams: Map<String, Float> = emptyMap(),
)
