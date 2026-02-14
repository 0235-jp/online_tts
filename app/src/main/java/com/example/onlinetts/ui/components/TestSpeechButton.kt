package com.example.onlinetts.ui.components

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.onlinetts.tts.api.SynthesisResult
import com.example.onlinetts.tts.api.TtsApiResult
import com.example.onlinetts.tts.provider.TtsProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun TestSpeechButton(
    provider: TtsProvider?,
    text: String,
    voiceId: String,
    params: Map<String, Float>,
    modifier: Modifier = Modifier,
) {
    var isPlaying by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Button(
        onClick = {
            if (isPlaying || provider == null) return@Button
            isPlaying = true
            scope.launch {
                try {
                    when (val result = provider.synthesize(text, voiceId, params)) {
                        is TtsApiResult.Success -> playPcm(result.data)
                        is TtsApiResult.Error -> { /* handled silently */ }
                    }
                } finally {
                    isPlaying = false
                }
            }
        },
        enabled = provider != null && provider.isConfigured() && !isPlaying,
        modifier = modifier,
    ) {
        if (isPlaying) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text = "テスト再生")
    }
}

private suspend fun playPcm(result: SynthesisResult) = withContext(Dispatchers.IO) {
    val channelConfig = if (result.channels == 1) {
        AudioFormat.CHANNEL_OUT_MONO
    } else {
        AudioFormat.CHANNEL_OUT_STEREO
    }
    val audioFormat = when (result.bitsPerSample) {
        8 -> AudioFormat.ENCODING_PCM_8BIT
        16 -> AudioFormat.ENCODING_PCM_16BIT
        else -> AudioFormat.ENCODING_PCM_16BIT
    }

    val bufferSize = AudioTrack.getMinBufferSize(result.sampleRate, channelConfig, audioFormat)
    val audioTrack = AudioTrack.Builder()
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )
        .setAudioFormat(
            AudioFormat.Builder()
                .setSampleRate(result.sampleRate)
                .setChannelMask(channelConfig)
                .setEncoding(audioFormat)
                .build()
        )
        .setBufferSizeInBytes(maxOf(bufferSize, result.pcmData.size))
        .setTransferMode(AudioTrack.MODE_STATIC)
        .build()

    audioTrack.write(result.pcmData, 0, result.pcmData.size)
    audioTrack.play()

    val durationMs = (result.pcmData.size.toLong() * 1000) /
        (result.sampleRate.toLong() * result.channels * (result.bitsPerSample / 8))
    Thread.sleep(durationMs + 100)

    audioTrack.stop()
    audioTrack.release()
}
