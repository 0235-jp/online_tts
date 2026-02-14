package com.example.onlinetts.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech

class SettingsEntryActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        when (intent?.action) {
            "android.speech.tts.engine.CHECK_TTS_DATA" -> {
                val result = Intent()
                result.putStringArrayListExtra(
                    TextToSpeech.Engine.EXTRA_AVAILABLE_VOICES,
                    arrayListOf("jpn-JPN"),
                )
                setResult(TextToSpeech.Engine.CHECK_VOICE_DATA_PASS, result)
                finish()
            }
            "android.speech.tts.engine.INSTALL_TTS_DATA" -> {
                // オンライン合成のため、インストール不要
                setResult(RESULT_OK)
                finish()
            }
            else -> {
                // 設定画面を開く
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
    }
}
