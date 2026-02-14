package com.example.onlinetts.tts.api

data class SynthesisResult(
    val pcmData: ByteArray,
    val sampleRate: Int,
    val channels: Int,
    val bitsPerSample: Int,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SynthesisResult) return false
        return pcmData.contentEquals(other.pcmData) &&
            sampleRate == other.sampleRate &&
            channels == other.channels &&
            bitsPerSample == other.bitsPerSample
    }

    override fun hashCode(): Int {
        var result = pcmData.contentHashCode()
        result = 31 * result + sampleRate
        result = 31 * result + channels
        result = 31 * result + bitsPerSample
        return result
    }
}
