package com.liquidglass.fluxhub.chat

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources

@Serializable
private data class ModelsResponse(
    val data: List<ModelResponse>
)

@Serializable
private data class ModelResponse(
    val id: String
)

interface ChatStreamCallback {
    fun onOpen(responseCode: Int)
    fun onDelta(delta: ChatStreamDelta)
    fun onDone()
    fun onClosed()
    fun onFailure(message: String, throwable: Throwable?, responseCode: Int?)
    fun onParseError(error: Exception)
}

class ChatApiClient(
    private val client: OkHttpClient
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchModels(
        baseUrl: String,
        apiKey: String
    ): List<String> = withContext(Dispatchers.IO) {
        val response = client.newCall(
            Request.Builder()
                .url("$baseUrl/models")
                .addHeader("Authorization", "Bearer $apiKey")
                .get()
                .build()
        ).execute()

        response.use {
            val body = it.body?.string() ?: "{}"
            if (!it.isSuccessful) {
                throw ChatApiException("HTTP ${it.code}: ${it.message}")
            }

            try {
                json.decodeFromString(ModelsResponse.serializer(), body)
                    .data
                    .map { model -> model.id }
                    .sorted()
            } catch (e: Exception) {
                throw ChatApiException("模型列表解析失败", e)
            }
        }
    }

    suspend fun executeNonStreaming(
        baseUrl: String,
        apiKey: String,
        requestBody: String
    ): ChatCompletionResult = withContext(Dispatchers.IO) {
        val response = client.newCall(
            Request.Builder()
                .url("$baseUrl/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()
        ).execute()

        response.use {
            val body = it.body?.string() ?: "{}"
            if (!it.isSuccessful) {
                throw ChatApiException("HTTP ${it.code}: ${it.message}")
            }
            ChatResponseParser.parseNonStreaming(body)
        }
    }

    fun startStreaming(
        baseUrl: String,
        apiKey: String,
        requestBody: String,
        callback: ChatStreamCallback
    ): EventSource {
        val request = Request.Builder()
            .url("$baseUrl/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "text/event-stream")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        val listener = object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) {
                callback.onOpen(response.code)
            }

            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                if (data == "[DONE]") {
                    callback.onDone()
                    return
                }

                try {
                    ChatResponseParser.parseStreamingEventData(data).forEach { delta ->
                        if (delta.isDone) {
                            callback.onDone()
                        } else {
                            callback.onDelta(delta)
                        }
                    }
                } catch (e: Exception) {
                    callback.onParseError(e)
                }
            }

            override fun onClosed(eventSource: EventSource) {
                callback.onClosed()
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                val errorBody = try {
                    response?.body?.string()?.take(500)
                } catch (e: Exception) {
                    null
                }
                callback.onFailure(
                    message = t?.message ?: errorBody ?: "Unknown error",
                    throwable = t,
                    responseCode = response?.code
                )
            }
        }

        return EventSources.createFactory(client).newEventSource(request, listener)
    }
}
