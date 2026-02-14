package com.example.onlinetts.tts.provider

import kotlinx.serialization.Serializable

@Serializable
enum class TtsProviderType(val displayName: String) {
    AIVIS_CLOUD("Aivis Cloud"),
}
