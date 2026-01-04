package com.liquidglass.fluxhub.chat

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
    
    fun sendMessage(content: String) {
        if (content.isBlank() || apiKey.isBlank()) {
            error = if (apiKey.isBlank()) "请先配置 API Key" else null
            return
        }
        
        messages.add(ChatMessage("user", content))
        isLoading = true
        error = null
        
        viewModelScope.launch {
            try {
                val response = callApi()
                messages.add(ChatMessage("assistant", response))
            } catch (e: Exception) {
                error = e.message ?: "Unknown error"
            } finally {
                isLoading = false
            }
        }
    }
    
    private suspend fun callApi(): String = withContext(Dispatchers.IO) {
        val request = ChatRequest(
            model = model,
            messages = messages.toList(),
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
        
        val response = client.newCall(httpRequest).execute()
        val body = response.body?.string() ?: ""
        
        if (!response.isSuccessful) {
            throw Exception("API error: ${response.code} - $body")
        }
        
        val chatResponse = json.decodeFromString(ChatResponse.serializer(), body)
        chatResponse.choices.firstOrNull()?.message?.content ?: ""
    }
    
    fun clearError() {
        error = null
    }
}
