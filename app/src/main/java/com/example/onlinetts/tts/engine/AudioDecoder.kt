package com.example.onlinetts.tts.engine

import android.media.MediaCodec
import android.media.MediaDataSource
import android.media.MediaExtractor
import android.media.MediaFormat
import com.example.onlinetts.tts.api.SynthesisResult
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object AudioDecoder {

    fun decode(audioData: ByteArray): SynthesisResult {
        if (audioData.size >= 4) {
            val header = String(audioData, 0, 4, Charsets.US_ASCII)
            if (header == "RIFF") {
                return parseWav(audioData)
            }
        }
        return decodeCompressed(audioData)
    }

    private fun decodeCompressed(audioData: ByteArray): SynthesisResult {
        val extractor = MediaExtractor()
        val dataSource = ByteArrayMediaDataSource(audioData)
        extractor.setDataSource(dataSource)

        require(extractor.trackCount > 0) { "No audio track found" }
        extractor.selectTrack(0)

        val format = extractor.getTrackFormat(0)
        val mime = format.getString(MediaFormat.KEY_MIME)
            ?: throw IllegalArgumentException("No MIME type in track")
        val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(format, null, null, 0)
        codec.start()

        val pcmOutput = ByteArrayOutputStream()
        val bufferInfo = MediaCodec.BufferInfo()
        var inputDone = false

        try {
            while (true) {
                if (!inputDone) {
                    val inputIndex = codec.dequeueInputBuffer(10_000)
                    if (inputIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputIndex)!!
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(
                                inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                            )
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(
                                inputIndex, 0, sampleSize, extractor.sampleTime, 0,
                            )
                            extractor.advance()
                        }
                    }
                }

                val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000)
                if (outputIndex >= 0) {
                    val outputBuffer = codec.getOutputBuffer(outputIndex)!!
                    val chunk = ByteArray(bufferInfo.size)
                    outputBuffer.get(chunk)
                    pcmOutput.write(chunk)
                    codec.releaseOutputBuffer(outputIndex, false)

                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        break
                    }
                }
            }
        } finally {
            codec.stop()
            codec.release()
            extractor.release()
        }

        return SynthesisResult(
            pcmData = pcmOutput.toByteArray(),
            sampleRate = sampleRate,
            channels = channels,
            bitsPerSample = 16,
        )
    }

    private fun parseWav(wavData: ByteArray): SynthesisResult {
        val buffer = ByteBuffer.wrap(wavData).order(ByteOrder.LITTLE_ENDIAN)

        val riff = readString(buffer, 4)
        require(riff == "RIFF") { "Not a RIFF file: $riff" }
        buffer.int
        val wave = readString(buffer, 4)
        require(wave == "WAVE") { "Not a WAVE file: $wave" }

        var sampleRate = 0
        var channels = 0
        var bitsPerSample = 0
        var pcmData: ByteArray? = null

        while (buffer.hasRemaining()) {
            val chunkId = readString(buffer, 4)
            val chunkSize = buffer.int

            when (chunkId) {
                "fmt " -> {
                    val audioFormat = buffer.short.toInt()
                    require(audioFormat == 1) { "Unsupported audio format: $audioFormat (expected PCM)" }
                    channels = buffer.short.toInt()
                    sampleRate = buffer.int
                    buffer.int
                    buffer.short
                    bitsPerSample = buffer.short.toInt()
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

    private class ByteArrayMediaDataSource(
        private val data: ByteArray,
    ) : MediaDataSource() {
        override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
            if (position >= data.size) return -1
            val remaining = (data.size - position).toInt()
            val bytesToRead = minOf(size, remaining)
            System.arraycopy(data, position.toInt(), buffer, offset, bytesToRead)
            return bytesToRead
        }

        override fun getSize(): Long = data.size.toLong()

        override fun close() {}
    }
}
