package com.example.onlinetts.tts.aiviscloud

import com.example.onlinetts.tts.aiviscloud.model.AivisModelResponse
import com.example.onlinetts.tts.aiviscloud.model.AivisTtsRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AivisCloudApiClient @Inject constructor(
    private val httpClient: HttpClient,
) {
    companion object {
        private const val BASE_URL = "https://api.aivis-project.com/v1"
    }

    suspend fun synthesize(request: AivisTtsRequest, apiKey: String): ByteArray {
        return httpClient.post("$BASE_URL/tts/synthesize") {
            bearerAuth(apiKey)
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    fun synthesizeStreaming(request: AivisTtsRequest, apiKey: String): Flow<ByteArray> = flow {
        httpClient.preparePost("$BASE_URL/tts/synthesize") {
            bearerAuth(apiKey)
            contentType(ContentType.Application.Json)
            setBody(request)
        }.execute { response ->
            val channel = response.bodyAsChannel()
            val buffer = ByteArray(8192)
            while (!channel.isClosedForRead) {
                val bytesRead = channel.readAvailable(buffer)
                if (bytesRead > 0) {
                    emit(buffer.copyOf(bytesRead))
                }
            }
        }
    }

    suspend fun getModel(modelUuid: String, apiKey: String): AivisModelResponse {
        return httpClient.get("$BASE_URL/aivm-models/$modelUuid") {
            bearerAuth(apiKey)
        }.body()
    }
}
