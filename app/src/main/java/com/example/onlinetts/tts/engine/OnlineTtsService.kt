package com.example.onlinetts.tts.engine

import android.media.AudioFormat
import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeechService
import android.util.Log
import com.example.onlinetts.data.repository.SettingsRepository
import com.example.onlinetts.tts.api.SynthesisEvent
import com.example.onlinetts.tts.provider.TtsProviderFactory
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
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

                val flow = provider.synthesizeStreaming(
                    text = text,
                    voiceId = settings.selectedVoiceId,
                    params = settings.voiceParams,
                )

                flow.collect { event ->
                    if (isStopped) {
                        throw CancellationException("TTS stopped")
                    }
                    when (event) {
                        is SynthesisEvent.Started -> {
                            val audioFormat = when (event.bitsPerSample) {
                                8 -> AudioFormat.ENCODING_PCM_8BIT
                                16 -> AudioFormat.ENCODING_PCM_16BIT
                                else -> AudioFormat.ENCODING_PCM_16BIT
                            }
                            callback.start(event.sampleRate, audioFormat, event.channels)
                        }
                        is SynthesisEvent.Audio -> {
                            val pcmData = event.pcmData
                            var offset = 0
                            while (offset < pcmData.size && !isStopped) {
                                val bytesToWrite = minOf(CHUNK_SIZE, pcmData.size - offset)
                                callback.audioAvailable(pcmData, offset, bytesToWrite)
                                offset += bytesToWrite
                            }
                        }
                        is SynthesisEvent.Error -> {
                            Log.e(TAG, "Streaming synthesis error: ${event.message}", event.cause)
                            callback.error()
                        }
                        is SynthesisEvent.Done -> {
                            if (!isStopped) {
                                callback.done()
                            }
                        }
                    }
                }
            } catch (_: CancellationException) {
                Log.d(TAG, "Synthesis cancelled")
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
