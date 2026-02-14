package com.example.onlinetts.di

import com.example.onlinetts.tts.aiviscloud.AivisCloudTtsProvider
import com.example.onlinetts.tts.provider.TtsProvider
import com.example.onlinetts.tts.provider.TtsProviderType
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TtsModule {

    @Provides
    @Singleton
    fun provideTtsProviderMap(
        aivisCloudProvider: AivisCloudTtsProvider,
    ): Map<TtsProviderType, TtsProvider> {
        return mapOf(
            TtsProviderType.AIVIS_CLOUD to aivisCloudProvider,
        )
    }
}
