package com.example.onlinetts.tts.engine

import android.media.MediaCodec
import android.media.MediaFormat
import com.example.onlinetts.tts.api.SynthesisEvent
import java.io.Closeable

class StreamingAudioDecoder : Closeable {

    private var codec: MediaCodec? = null
    private var sampleRate = 0
    private var channels = 0
    private var headerParsed = false
    private var id3Skipped = false
    private val pending = ByteArrayBuffer()

    fun feedChunk(mp3Bytes: ByteArray): List<SynthesisEvent> {
        val events = mutableListOf<SynthesisEvent>()
        pending.append(mp3Bytes)

        if (!id3Skipped) {
            if (!trySkipId3()) return events
        }

        if (!headerParsed) {
            if (!tryParseFrameHeader()) return events
            headerParsed = true
            initCodec()
            events.add(SynthesisEvent.Started(sampleRate, channels))
        }

        val data = pending.drain()
        if (data.isNotEmpty()) {
            events.addAll(feedToCodec(data))
        }

        return events
    }

    fun finish(): List<SynthesisEvent> {
        val events = mutableListOf<SynthesisEvent>()

        if (!headerParsed) {
            events.add(SynthesisEvent.Error("MP3 フォーマットを検出できませんでした"))
            events.add(SynthesisEvent.Done)
            return events
        }

        val codec = this.codec ?: run {
            events.add(SynthesisEvent.Done)
            return events
        }

        val remaining = pending.drain()
        if (remaining.isNotEmpty()) {
            events.addAll(feedToCodec(remaining))
        }

        val inputIndex = codec.dequeueInputBuffer(10_000)
        if (inputIndex >= 0) {
            codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
        }

        events.addAll(drainOutput(waitForEos = true))
        events.add(SynthesisEvent.Done)
        return events
    }

    override fun close() {
        codec?.let {
            try {
                it.stop()
            } catch (_: Exception) {
            }
            it.release()
        }
        codec = null
    }

    private fun trySkipId3(): Boolean {
        val data = pending.peek()
        if (data.size < 10) return false

        if (data[0].toInt().toChar() == 'I' &&
            data[1].toInt().toChar() == 'D' &&
            data[2].toInt().toChar() == '3'
        ) {
            val size = (data[6].toInt() and 0x7F shl 21) or
                (data[7].toInt() and 0x7F shl 14) or
                (data[8].toInt() and 0x7F shl 7) or
                (data[9].toInt() and 0x7F)
            val totalId3Size = 10 + size
            if (data.size < totalId3Size) return false
            pending.skip(totalId3Size)
        }

        id3Skipped = true
        return true
    }

    private fun tryParseFrameHeader(): Boolean {
        val data = pending.peek()
        if (data.size < 4) return false

        for (i in 0 until data.size - 3) {
            val b0 = data[i].toInt() and 0xFF
            val b1 = data[i + 1].toInt() and 0xFF
            if (b0 == 0xFF && (b1 and 0xE0) == 0xE0) {
                val b2 = data[i + 2].toInt() and 0xFF
                val b3 = data[i + 3].toInt() and 0xFF

                val versionBits = (b1 shr 3) and 0x03
                val srIndex = (b2 shr 2) and 0x03
                val channelMode = (b3 shr 6) and 0x03

                sampleRate = getMp3SampleRate(versionBits, srIndex)
                channels = if (channelMode == 3) 1 else 2

                if (i > 0) {
                    pending.skip(i)
                }
                return true
            }
        }
        return false
    }

    private fun getMp3SampleRate(versionBits: Int, srIndex: Int): Int {
        val rates = arrayOf(
            intArrayOf(11025, 12000, 8000),  // MPEG 2.5
            intArrayOf(44100, 48000, 32000), // reserved → fallback to v1
            intArrayOf(22050, 24000, 16000), // MPEG 2
            intArrayOf(44100, 48000, 32000), // MPEG 1
        )
        return if (srIndex < 3) rates[versionBits][srIndex] else 44100
    }

    private fun initCodec() {
        val format = MediaFormat.createAudioFormat(
            MediaFormat.MIMETYPE_AUDIO_MPEG,
            sampleRate,
            channels,
        )
        val c = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_AUDIO_MPEG)
        c.configure(format, null, null, 0)
        c.start()
        codec = c
    }

    private fun feedToCodec(data: ByteArray): List<SynthesisEvent> {
        val events = mutableListOf<SynthesisEvent>()
        val codec = this.codec ?: return events
        var offset = 0

        while (offset < data.size) {
            val inputIndex = codec.dequeueInputBuffer(1_000)
            if (inputIndex >= 0) {
                val inputBuffer = codec.getInputBuffer(inputIndex)!!
                val bytesToWrite = minOf(data.size - offset, inputBuffer.remaining())
                inputBuffer.put(data, offset, bytesToWrite)
                codec.queueInputBuffer(inputIndex, 0, bytesToWrite, 0, 0)
                offset += bytesToWrite
            }
            events.addAll(drainOutput(waitForEos = false))
        }

        events.addAll(drainOutput(waitForEos = false))
        return events
    }

    private fun drainOutput(waitForEos: Boolean): List<SynthesisEvent> {
        val events = mutableListOf<SynthesisEvent>()
        val codec = this.codec ?: return events
        val bufferInfo = MediaCodec.BufferInfo()
        val timeout = if (waitForEos) 10_000L else 0L

        while (true) {
            val outputIndex = codec.dequeueOutputBuffer(bufferInfo, timeout)
            if (outputIndex >= 0) {
                if (bufferInfo.size > 0) {
                    val outputBuffer = codec.getOutputBuffer(outputIndex)!!
                    val chunk = ByteArray(bufferInfo.size)
                    outputBuffer.get(chunk)
                    events.add(SynthesisEvent.Audio(chunk))
                }
                codec.releaseOutputBuffer(outputIndex, false)
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
            } else {
                break
            }
        }
        return events
    }

    private class ByteArrayBuffer {
        private var data = ByteArray(0)

        fun append(bytes: ByteArray) {
            data = data + bytes
        }

        fun peek(): ByteArray = data

        fun skip(count: Int) {
            data = if (count >= data.size) ByteArray(0) else data.copyOfRange(count, data.size)
        }

        fun drain(): ByteArray {
            val result = data
            data = ByteArray(0)
            return result
        }
    }
}
