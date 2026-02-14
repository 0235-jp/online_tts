package com.example.onlinetts.tts.aiviscloud

import com.example.onlinetts.tts.aiviscloud.model.AivisAudioQuery
import com.example.onlinetts.tts.aiviscloud.model.AivisSpeaker
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AivisCloudApiClient @Inject constructor(
    private val httpClient: HttpClient,
) {
    companion object {
        private const val BASE_URL = "https://api.aivis-project.com/v1"
    }

    suspend fun audioQuery(text: String, speakerId: Int, apiKey: String): AivisAudioQuery {
        return httpClient.post("$BASE_URL/audio_query") {
            bearerAuth(apiKey)
            parameter("text", text)
            parameter("speaker", speakerId)
        }.body()
    }

    suspend fun synthesis(audioQuery: AivisAudioQuery, speakerId: Int, apiKey: String): ByteArray {
        return httpClient.post("$BASE_URL/synthesis") {
            bearerAuth(apiKey)
            parameter("speaker", speakerId)
            contentType(ContentType.Application.Json)
            setBody(audioQuery)
        }.body()
    }

    suspend fun getSpeakers(apiKey: String): List<AivisSpeaker> {
        return httpClient.get("$BASE_URL/speakers") {
            bearerAuth(apiKey)
        }.body()
    }
}
