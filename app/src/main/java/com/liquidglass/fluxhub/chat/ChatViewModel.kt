package com.liquidglass.fluxhub.chat

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

private const val TAG = "ChatViewModel"

@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean = false
)

@Serializable
data class ChatResponse(
    val choices: List<Choice> = emptyList()
)

@Serializable
data class Choice(
    val message: ChatMessage? = null
)

class ChatViewModel : ViewModel() {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
    
    val messages = mutableStateListOf<ChatMessage>()
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    
    // 配置
    var apiKey by mutableStateOf("")
    var baseUrl by mutableStateOf("https://api.openai.com/v1")
    var model by mutableStateOf("gpt-4o-mini")
    
    // Liquid Glass 风格开关
    var useLiquidGlass by mutableStateOf(false)
    
    fun sendMessage(content: String) {
        if (content.isBlank() || apiKey.isBlank()) {
            error = if (apiKey.isBlank()) "请先配置 API Key" else null
            Log.w(TAG, "sendMessage: apiKey is blank or content is blank")
            return
        }
        
        Log.d(TAG, "sendMessage: $content")
        messages.add(ChatMessage("user", content))
        isLoading = true
        error = null
        
        viewModelScope.launch {
            try {
                val response = callApi()
                Log.d(TAG, "API response received: ${response.take(100)}...")
                messages.add(ChatMessage("assistant", response))
            } catch (e: Exception) {
                Log.e(TAG, "API call failed", e)
                error = e.message ?: "Unknown error"
            } finally {
                isLoading = false
            }
        }
    }
    
    private suspend fun callApi(): String = withContext(Dispatchers.IO) {
        Log.d(TAG, "callApi: using baseUrl=$baseUrl, model=$model")
        
        val request = ChatRequest(
            model = model,
            messages = messages.toList(),
            stream = false
        )
        
        val requestBody = json.encodeToString(ChatRequest.serializer(), request)
        Log.d(TAG, "Request body: $requestBody")
        
        val httpRequest = Request.Builder()
            .url("$baseUrl/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()
        
        Log.d(TAG, "Sending request to: ${httpRequest.url}")
        
        val response = client.newCall(httpRequest).execute()
        val body = response.body?.string() ?: ""
        
        Log.d(TAG, "Response code: ${response.code}, body length: ${body.length}")
        
        if (!response.isSuccessful) {
            Log.e(TAG, "API error: ${response.code} - $body")
            throw Exception("API error: ${response.code} - $body")
        }
        
        val chatResponse = json.decodeFromString(ChatResponse.serializer(), body)
        chatResponse.choices.firstOrNull()?.message?.content ?: ""
    }
    
    fun clearError() {
        error = null
    }
}
