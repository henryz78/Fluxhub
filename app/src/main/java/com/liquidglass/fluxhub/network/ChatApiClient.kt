package com.liquidglass.fluxhub.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

class ChatApiClient(
    private val baseUrl: String,
    private val apiKey: String
) {
    private val json = Json { 
        ignoreUnknownKeys = true 
        isLenient = true
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    fun streamChat(
        model: String,
        messages: List<ChatMessage>
    ): Flow<String> = callbackFlow {
        val request = ChatRequest(
            model = model,
            messages = messages,
            stream = true
        )
        
        val requestBody = json.encodeToString(ChatRequest.serializer(), request)
            .toRequestBody("application/json".toMediaType())
        
        val httpRequest = Request.Builder()
            .url("$baseUrl/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()
        
        val eventSourceFactory = EventSources.createFactory(client)
        
        val listener = object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                if (data == "[DONE]") {
                    close()
                    return
                }
                
                try {
                    val response = json.decodeFromString(ChatResponse.serializer(), data)
                    val content = response.choices.firstOrNull()?.delta?.content
                    if (content != null) {
                        trySend(content)
                    }
                } catch (e: Exception) {
                    // Ignore parsing errors for partial data
                }
            }
            
            override fun onFailure(eventSource: EventSource, t: Throwable?, response: okhttp3.Response?) {
                close(t ?: Exception("Unknown error"))
            }
            
            override fun onClosed(eventSource: EventSource) {
                close()
            }
        }
        
        val eventSource = eventSourceFactory.newEventSource(httpRequest, listener)
        
        awaitClose {
            eventSource.cancel()
        }
    }.flowOn(Dispatchers.IO)
    
    suspend fun chat(
        model: String,
        messages: List<ChatMessage>
    ): String = suspendCancellableCoroutine { continuation ->
        val request = ChatRequest(
            model = model,
            messages = messages,
            stream = false
        )
        
        val requestBody = json.encodeToString(ChatRequest.serializer(), request)
            .toRequestBody("application/json".toMediaType())
        
        val httpRequest = Request.Builder()
            .url("$baseUrl/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()
        
        val call = client.newCall(httpRequest)
        
        continuation.invokeOnCancellation {
            call.cancel()
        }
        
        try {
            val response = call.execute()
            val body = response.body?.string() ?: ""
            
            if (!response.isSuccessful) {
                val error = try {
                    json.decodeFromString(ErrorResponse.serializer(), body)
                } catch (e: Exception) { null }
                
                continuation.resumeWithException(
                    Exception(error?.error?.message ?: "API error: ${response.code}")
                )
                return@suspendCancellableCoroutine
            }
            
            val chatResponse = json.decodeFromString(ChatResponse.serializer(), body)
            val content = chatResponse.choices.firstOrNull()?.message?.content ?: ""
            continuation.resume(content)
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }
}
