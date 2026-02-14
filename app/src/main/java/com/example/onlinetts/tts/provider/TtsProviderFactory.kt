package com.example.onlinetts.tts.provider

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TtsProviderFactory @Inject constructor(
    private val providers: Map<TtsProviderType, @JvmSuppressWildcards TtsProvider>,
) {
    fun create(type: TtsProviderType): TtsProvider {
        return providers[type] ?: throw IllegalArgumentException("Unknown provider type: $type")
    }
}
