package com.example.onlinetts.tts.engine

import android.media.AudioFormat
import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeechService
import android.util.Log
import com.example.onlinetts.data.repository.SettingsRepository
import com.example.onlinetts.tts.api.AudioQueryRequest
import com.example.onlinetts.tts.api.TtsApiResult
import com.example.onlinetts.tts.provider.TtsProviderFactory
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking

class OnlineTtsService : TextToSpeechService() {

    companion object {
        private const val TAG = "OnlineTtsService"
        private const val CHUNK_SIZE = 8192
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface OnlineTtsServiceEntryPoint {
        fun settingsRepository(): SettingsRepository
        fun ttsProviderFactory(): TtsProviderFactory
    }

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var ttsProviderFactory: TtsProviderFactory

    @Volatile
    private var isStopped = false
    private var currentJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            OnlineTtsServiceEntryPoint::class.java,
        )
        settingsRepository = entryPoint.settingsRepository()
        ttsProviderFactory = entryPoint.ttsProviderFactory()
    }

    override fun onIsLanguageAvailable(lang: String?, country: String?, variant: String?): Int {
        return if (lang == "jpn" || lang == "ja") {
            TextToSpeech.LANG_AVAILABLE
        } else {
            TextToSpeech.LANG_NOT_SUPPORTED
        }
    }

    override fun onGetLanguage(): Array<String> {
        return arrayOf("jpn", "JPN", "")
    }

    override fun onLoadLanguage(lang: String?, country: String?, variant: String?): Int {
        return onIsLanguageAvailable(lang, country, variant)
    }

    override fun onSynthesizeText(request: SynthesisRequest, callback: SynthesisCallback) {
        isStopped = false
        val text = request.charSequenceText?.toString() ?: request.text ?: return

        if (text.isBlank()) {
            callback.start(44100, AudioFormat.ENCODING_PCM_16BIT, 1)
            callback.done()
            return
        }

        runBlocking {
            try {
                val settings = settingsRepository.getSettings()
                val provider = ttsProviderFactory.create(settings.providerType)

                if (!provider.isConfigured()) {
                    Log.e(TAG, "Provider not configured")
                    callback.error()
                    return@runBlocking
                }

                val audioRequest = AudioQueryRequest(
                    text = text,
                    modelUuid = settings.speakerModelUuid,
                    styleId = settings.selectedSpeakerId,
                    voiceParams = settings.voiceParams,
                )

                when (val result = provider.synthesize(audioRequest)) {
                    is TtsApiResult.Success -> {
                        if (isStopped) return@runBlocking

                        val synthesis = result.data
                        val audioFormat = when (synthesis.bitsPerSample) {
                            8 -> AudioFormat.ENCODING_PCM_8BIT
                            16 -> AudioFormat.ENCODING_PCM_16BIT
                            else -> AudioFormat.ENCODING_PCM_16BIT
                        }

                        callback.start(synthesis.sampleRate, audioFormat, synthesis.channels)

                        val pcmData = synthesis.pcmData
                        var offset = 0
                        while (offset < pcmData.size && !isStopped) {
                            val bytesToWrite = minOf(CHUNK_SIZE, pcmData.size - offset)
                            callback.audioAvailable(pcmData, offset, bytesToWrite)
                            offset += bytesToWrite
                        }

                        if (!isStopped) {
                            callback.done()
                        }
                    }
                    is TtsApiResult.Error -> {
                        Log.e(TAG, "Synthesis failed: ${result.message}", result.cause)
                        callback.error()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Synthesis error", e)
                callback.error()
            }
        }
    }

    override fun onStop() {
        isStopped = true
        currentJob?.cancel()
    }
}
