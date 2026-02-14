package com.example.onlinetts.tts.engine

import com.example.onlinetts.tts.api.SynthesisResult
import java.nio.ByteBuffer
import java.nio.ByteOrder

object WavParser {

    fun parse(wavData: ByteArray): SynthesisResult {
        val buffer = ByteBuffer.wrap(wavData).order(ByteOrder.LITTLE_ENDIAN)

        // RIFF header
        val riff = readString(buffer, 4)
        require(riff == "RIFF") { "Not a RIFF file: $riff" }
        buffer.int // file size
        val wave = readString(buffer, 4)
        require(wave == "WAVE") { "Not a WAVE file: $wave" }

        var sampleRate = 0
        var channels = 0
        var bitsPerSample = 0
        var pcmData: ByteArray? = null

        // Parse chunks
        while (buffer.hasRemaining()) {
            val chunkId = readString(buffer, 4)
            val chunkSize = buffer.int

            when (chunkId) {
                "fmt " -> {
                    val audioFormat = buffer.short.toInt()
                    require(audioFormat == 1) { "Unsupported audio format: $audioFormat (expected PCM)" }
                    channels = buffer.short.toInt()
                    sampleRate = buffer.int
                    buffer.int   // byte rate
                    buffer.short // block align
                    bitsPerSample = buffer.short.toInt()
                    // Skip extra fmt bytes if present
                    val extraBytes = chunkSize - 16
                    if (extraBytes > 0) {
                        buffer.position(buffer.position() + extraBytes)
                    }
                }
                "data" -> {
                    pcmData = ByteArray(chunkSize)
                    buffer.get(pcmData)
                }
                else -> {
                    // Skip unknown chunks
                    buffer.position(buffer.position() + chunkSize)
                }
            }
        }

        requireNotNull(pcmData) { "No data chunk found in WAV file" }
        require(sampleRate > 0) { "No fmt chunk found in WAV file" }

        return SynthesisResult(
            pcmData = pcmData,
            sampleRate = sampleRate,
            channels = channels,
            bitsPerSample = bitsPerSample,
        )
    }

    private fun readString(buffer: ByteBuffer, length: Int): String {
        val bytes = ByteArray(length)
        buffer.get(bytes)
        return String(bytes, Charsets.US_ASCII)
    }
}
