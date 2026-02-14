package com.example.onlinetts.ui

import android.app.Activity
import android.os.Bundle
import android.speech.tts.TextToSpeech

class GetSampleTextActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val result = android.content.Intent()
        result.putExtra(TextToSpeech.Engine.EXTRA_SAMPLE_TEXT, "これは、オンライン音声合成エンジンのサンプルテキストです。")
        setResult(RESULT_OK, result)
        finish()
    }
}
