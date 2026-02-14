package com.example.onlinetts.tts.api

sealed class TtsApiResult<out T> {
    data class Success<T>(val data: T) : TtsApiResult<T>()
    data class Error(val message: String, val cause: Throwable? = null) : TtsApiResult<Nothing>()
}
