package com.liquidglass.fluxhub.chat

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
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
    val message: ChatMessage? = null,
    val delta: Delta? = null
)

@Serializable
data class Delta(
    val content: String? = null
)

// 用于 UI 显示的消息，支持流式更新
data class UiMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: String,
    var content: String,
    val isStreaming: Boolean = false
)

class ChatViewModel : ViewModel() {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()
    
    val messages = mutableStateListOf<UiMessage>()
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var showError by mutableStateOf(false)
        private set
    
    // 配置
    var apiKey by mutableStateOf("")
    var baseUrl by mutableStateOf("https://api.openai.com/v1")
    var model by mutableStateOf("gpt-4o-mini")
    
    fun sendMessage(content: String) {
        if (content.isBlank()) return
        
        if (apiKey.isBlank()) {
            showErrorMessage("请先配置 API Key")
            Log.w(TAG, "sendMessage: apiKey is blank")
            return
        }
        
        Log.d(TAG, "sendMessage: $content")
        
        // 添加用户消息
        messages.add(UiMessage(role = "user", content = content))
        
        // 添加空的 AI 消息用于流式更新
        val aiMessageId = java.util.UUID.randomUUID().toString()
        messages.add(UiMessage(id = aiMessageId, role = "assistant", content = "", isStreaming = true))
        
        isLoading = true
        clearError()
        
        viewModelScope.launch {
            try {
                callStreamingApi(aiMessageId)
            } catch (e: Exception) {
                Log.e(TAG, "API call failed", e)
                showErrorMessage(e.message ?: "Unknown error")
                // 移除空的 AI 消息
                messages.removeAll { it.id == aiMessageId }
            } finally {
                isLoading = false
                // 更新消息状态
                val index = messages.indexOfFirst { it.id == aiMessageId }
                if (index >= 0) {
                    messages[index] = messages[index].copy(isStreaming = false)
                }
            }
        }
    }
    
    private fun showErrorMessage(message: String) {
        error = message
        showError = true
        
        // 3秒后自动关闭
        viewModelScope.launch {
            delay(3000)
            if (error == message) {
                showError = false
                delay(300)
                error = null
            }
        }
    }
    
    private suspend fun callStreamingApi(aiMessageId: String) {
        Log.d(TAG, "callStreamingApi: using baseUrl=$baseUrl, model=$model")
        
        // 构建请求消息（转换为 API 格式）
        val apiMessages = messages
            .filter { it.role == "user" || (it.role == "assistant" && it.content.isNotEmpty()) }
            .map { ChatMessage(it.role, it.content) }
        
        val request = ChatRequest(
            model = model,
            messages = apiMessages,
            stream = true
        )
        
        val requestBody = json.encodeToString(ChatRequest.serializer(), request)
        Log.d(TAG, "Request body: $requestBody")
        
        val httpRequest = Request.Builder()
            .url("$baseUrl/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "text/event-stream")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()
        
        Log.d(TAG, "Sending streaming request to: ${httpRequest.url}")
        
        // 使用协程 Flow 处理 SSE
        collectStreamingResponse(httpRequest, aiMessageId)
    }
    
    private suspend fun collectStreamingResponse(request: Request, aiMessageId: String) {
        val response = client.newCall(request).execute()
        
        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: ""
            Log.e(TAG, "API error: ${response.code} - $errorBody")
            throw Exception("API error: ${response.code}")
        }
        
        val body = response.body ?: throw Exception("Empty response body")
        val reader = body.source().buffer()
        
        var fullContent = ""
        
        while (!reader.exhausted()) {
            val line = reader.readUtf8Line() ?: break
            
            if (line.startsWith("data: ")) {
                val data = line.removePrefix("data: ").trim()
                
                if (data == "[DONE]") {
                    Log.d(TAG, "Stream completed")
                    break
                }
                
                try {
                    val chunk = json.decodeFromString(ChatResponse.serializer(), data)
                    val deltaContent = chunk.choices.firstOrNull()?.delta?.content
                    
                    if (!deltaContent.isNullOrEmpty()) {
                        fullContent += deltaContent
                        
                        // 更新 UI 中的消息
                        val index = messages.indexOfFirst { it.id == aiMessageId }
                        if (index >= 0) {
                            messages[index] = messages[index].copy(content = fullContent)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse chunk: $data", e)
                }
            }
        }
        
        response.close()
        Log.d(TAG, "Final content length: ${fullContent.length}")
    }
    
    fun clearError() {
        showError = false
        viewModelScope.launch {
            delay(300)
            error = null
        }
    }
}
