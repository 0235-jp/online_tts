package com.example.onlinetts.tts.engine

import android.media.MediaCodec
import android.media.MediaFormat
import com.example.onlinetts.tts.api.SynthesisEvent
import java.io.ByteArrayOutputStream
import java.io.Closeable

class StreamingAudioDecoder : Closeable {

    private var codec: MediaCodec? = null
    private var codecErrored = false
    private var sampleRate = 0
    private var channels = 0
    private var headerParsed = false
    private var id3Skipped = false
    private val pending = ByteArrayOutputStream()

    fun feedChunk(mp3Bytes: ByteArray): List<SynthesisEvent> {
        if (codecErrored) return emptyList()

        val events = mutableListOf<SynthesisEvent>()
        pending.write(mp3Bytes)

        if (!id3Skipped) {
            if (!trySkipId3()) return events
        }

        if (!headerParsed) {
            val data = pending.toByteArray()
            val frameInfo = findFrame(data, 0) ?: return events
            sampleRate = frameInfo.sampleRate
            channels = frameInfo.channels
            headerParsed = true
            initCodec()
            events.add(SynthesisEvent.Started(sampleRate, channels))
        }

        events.addAll(feedCompleteFrames())
        return events
    }

    fun finish(): List<SynthesisEvent> {
        val events = mutableListOf<SynthesisEvent>()

        if (!headerParsed) {
            events.add(SynthesisEvent.Error("MP3 フォーマットを検出できませんでした"))
            events.add(SynthesisEvent.Done)
            return events
        }

        val codec = this.codec
        if (codec == null || codecErrored) {
            events.add(SynthesisEvent.Done)
            return events
        }

        events.addAll(feedCompleteFrames())

        try {
            val inputIndex = codec.dequeueInputBuffer(10_000)
            if (inputIndex >= 0) {
                codec.queueInputBuffer(
                    inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                )
            }
            events.addAll(drainOutput(waitForEos = true))
        } catch (e: Exception) {
            events.add(SynthesisEvent.Error("デコード終了処理に失敗しました: ${e.message}", e))
            codecErrored = true
        }

        events.add(SynthesisEvent.Done)
        return events
    }

    override fun close() {
        codec?.let {
            try {
                it.stop()
            } catch (_: Exception) {
            }
            try {
                it.release()
            } catch (_: Exception) {
            }
        }
        codec = null
    }

    private fun trySkipId3(): Boolean {
        val data = pending.toByteArray()
        if (data.size < 10) return false

        if (data[0] == 'I'.code.toByte() &&
            data[1] == 'D'.code.toByte() &&
            data[2] == '3'.code.toByte()
        ) {
            val size = (data[6].toInt() and 0x7F shl 21) or
                (data[7].toInt() and 0x7F shl 14) or
                (data[8].toInt() and 0x7F shl 7) or
                (data[9].toInt() and 0x7F)
            val totalId3Size = 10 + size
            if (data.size < totalId3Size) return false
            replaceBuffer(data.copyOfRange(totalId3Size, data.size))
        }

        id3Skipped = true
        return true
    }

    private fun feedCompleteFrames(): List<SynthesisEvent> {
        val events = mutableListOf<SynthesisEvent>()
        val codec = this.codec ?: return events
        if (codecErrored) return events

        var data = pending.toByteArray()
        var consumed = 0

        while (consumed < data.size - 3) {
            val frameInfo = findFrame(data, consumed) ?: break
            val frameEnd = frameInfo.offset + frameInfo.size
            if (frameEnd > data.size) break

            try {
                val inputIndex = codec.dequeueInputBuffer(5_000)
                if (inputIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputIndex)!!
                    inputBuffer.clear()
                    inputBuffer.put(data, frameInfo.offset, frameInfo.size)
                    codec.queueInputBuffer(inputIndex, 0, frameInfo.size, 0, 0)
                }
            } catch (e: Exception) {
                events.add(SynthesisEvent.Error("デコードエラー: ${e.message}", e))
                codecErrored = true
                break
            }

            consumed = frameEnd

            try {
                events.addAll(drainOutput(waitForEos = false))
            } catch (e: Exception) {
                events.add(SynthesisEvent.Error("デコード出力エラー: ${e.message}", e))
                codecErrored = true
                break
            }
        }

        if (consumed > 0) {
            data = pending.toByteArray()
            replaceBuffer(data.copyOfRange(consumed, data.size))
        }

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

    private fun replaceBuffer(data: ByteArray) {
        pending.reset()
        if (data.isNotEmpty()) {
            pending.write(data)
        }
    }

    private data class FrameInfo(
        val offset: Int,
        val size: Int,
        val sampleRate: Int,
        val channels: Int,
    )

    private fun findFrame(data: ByteArray, startOffset: Int): FrameInfo? {
        var i = startOffset
        while (i < data.size - 3) {
            val b0 = data[i].toInt() and 0xFF
            val b1 = data[i + 1].toInt() and 0xFF

            if (b0 != 0xFF || (b1 and 0xE0) != 0xE0) {
                i++
                continue
            }

            val versionBits = (b1 shr 3) and 0x03
            val layerBits = (b1 shr 1) and 0x03

            // reserved version or reserved layer → not a valid frame
            if (versionBits == 1 || layerBits == 0) {
                i++
                continue
            }

            val b2 = data[i + 2].toInt() and 0xFF
            val bitrateIndex = (b2 shr 4) and 0x0F
            val srIndex = (b2 shr 2) and 0x03
            val padding = (b2 shr 1) and 0x01

            // free or bad bitrate, reserved sample rate → not valid
            if (bitrateIndex == 0 || bitrateIndex == 15 || srIndex == 3) {
                i++
                continue
            }

            val b3 = data[i + 3].toInt() and 0xFF
            val channelMode = (b3 shr 6) and 0x03

            val sr = getSampleRate(versionBits, srIndex)
            val bitrate = getBitrate(versionBits, layerBits, bitrateIndex)

            if (sr == 0 || bitrate == 0) {
                i++
                continue
            }

            val frameSize = if (versionBits == 3) {
                // MPEG 1
                if (layerBits == 3) {
                    // Layer I
                    (12 * bitrate * 1000 / sr + padding) * 4
                } else {
                    // Layer II, III
                    144 * bitrate * 1000 / sr + padding
                }
            } else {
                // MPEG 2, 2.5
                if (layerBits == 3) {
                    // Layer I
                    (12 * bitrate * 1000 / sr + padding) * 4
                } else {
                    // Layer II, III
                    72 * bitrate * 1000 / sr + padding
                }
            }

            if (frameSize < 4) {
                i++
                continue
            }

            return FrameInfo(
                offset = i,
                size = frameSize,
                sampleRate = sr,
                channels = if (channelMode == 3) 1 else 2,
            )
        }
        return null
    }

    companion object {
        // MPEG 1, Layer I / II / III bitrates (kbps)
        private val BITRATES_V1 = arrayOf(
            // Layer I
            intArrayOf(0, 32, 64, 96, 128, 160, 192, 224, 256, 288, 320, 352, 384, 416, 448, 0),
            // Layer II
            intArrayOf(0, 32, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 384, 0),
            // Layer III
            intArrayOf(0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 0),
        )

        // MPEG 2/2.5, Layer I / II / III bitrates (kbps)
        private val BITRATES_V2 = arrayOf(
            // Layer I
            intArrayOf(0, 32, 48, 56, 64, 80, 96, 112, 128, 144, 160, 176, 192, 224, 256, 0),
            // Layer II, III
            intArrayOf(0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160, 0),
        )

        private val SAMPLE_RATES = arrayOf(
            intArrayOf(11025, 12000, 8000),  // MPEG 2.5
            intArrayOf(0, 0, 0),             // reserved
            intArrayOf(22050, 24000, 16000), // MPEG 2
            intArrayOf(44100, 48000, 32000), // MPEG 1
        )

        private fun getSampleRate(versionBits: Int, srIndex: Int): Int {
            return SAMPLE_RATES[versionBits][srIndex]
        }

        private fun getBitrate(versionBits: Int, layerBits: Int, bitrateIndex: Int): Int {
            // layerBits: 01=III, 10=II, 11=I → table index: I=0, II=1, III=2
            return if (versionBits == 3) {
                // MPEG 1
                val tableIndex = 3 - layerBits // 11→0(I), 10→1(II), 01→2(III)
                BITRATES_V1[tableIndex][bitrateIndex]
            } else {
                // MPEG 2/2.5
                if (layerBits == 3) {
                    BITRATES_V2[0][bitrateIndex] // Layer I
                } else {
                    BITRATES_V2[1][bitrateIndex] // Layer II, III
                }
            }
        }
    }
}
