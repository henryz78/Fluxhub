package com.liquidglass.fluxhub.chat

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.liquidglass.fluxhub.data.AppDatabase
import com.liquidglass.fluxhub.data.ConversationEntity
import com.liquidglass.fluxhub.data.MessageEntity
import com.liquidglass.fluxhub.data.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID
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

// 用于 UI 显示的消息
data class UiMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: String,
    var content: String,
    val isStreaming: Boolean = false
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()
    
    private val database = AppDatabase.getDatabase(application)
    private val messageDao = database.messageDao()
    private val conversationDao = database.conversationDao()
    private val settingsRepository = SettingsRepository(application)
    
    val messages = mutableStateListOf<UiMessage>()
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var showError by mutableStateOf(false)
        private set
    
    // 配置（从 DataStore 加载）
    var apiKey by mutableStateOf("")
    var baseUrl by mutableStateOf("https://api.openai.com/v1")
    var model by mutableStateOf("gpt-4o-mini")
    
    // 当前会话
    var currentConversationId by mutableStateOf<String?>(null)
        private set
    
    init {
        loadSettings()
        loadOrCreateConversation()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.apiKey.collect { apiKey = it }
        }
        viewModelScope.launch {
            settingsRepository.baseUrl.collect { baseUrl = it }
        }
        viewModelScope.launch {
            settingsRepository.model.collect { model = it }
        }
    }
    
    private fun loadOrCreateConversation() {
        viewModelScope.launch {
            val savedId = settingsRepository.currentConversationId.first()
            if (savedId != null) {
                val conversation = conversationDao.getConversation(savedId)
                if (conversation != null) {
                    currentConversationId = savedId
                    loadMessages(savedId)
                    return@launch
                }
            }
            // 创建新会话
            createNewConversation()
        }
    }
    
    private suspend fun loadMessages(conversationId: String) {
        messageDao.getMessagesForConversation(conversationId).collect { entities ->
            messages.clear()
            messages.addAll(entities.map { entity ->
                UiMessage(
                    id = entity.id,
                    role = entity.role,
                    content = entity.content,
                    isStreaming = false
                )
            })
        }
    }
    
    fun createNewConversation() {
        viewModelScope.launch {
            val newId = UUID.randomUUID().toString()
            val conversation = ConversationEntity(
                id = newId,
                title = "新对话"
            )
            conversationDao.insertConversation(conversation)
            currentConversationId = newId
            settingsRepository.setCurrentConversationId(newId)
            messages.clear()
        }
    }
    
    fun saveApiKey(value: String) {
        apiKey = value
        viewModelScope.launch {
            settingsRepository.setApiKey(value)
        }
    }
    
    fun saveBaseUrl(value: String) {
        baseUrl = value
        viewModelScope.launch {
            settingsRepository.setBaseUrl(value)
        }
    }
    
    fun saveModel(value: String) {
        model = value
        viewModelScope.launch {
            settingsRepository.setModel(value)
        }
    }
    
    fun sendMessage(content: String) {
        if (content.isBlank()) return
        
        if (apiKey.isBlank()) {
            showErrorMessage("请先配置 API Key")
            Log.w(TAG, "sendMessage: apiKey is blank")
            return
        }
        
        val conversationId = currentConversationId ?: return
        
        Log.d(TAG, "sendMessage: $content")
        
        // 添加用户消息
        val userMessageId = UUID.randomUUID().toString()
        val userMessage = UiMessage(id = userMessageId, role = "user", content = content)
        messages.add(userMessage)
        
        // 保存到数据库
        viewModelScope.launch {
            messageDao.insertMessage(MessageEntity(
                id = userMessageId,
                conversationId = conversationId,
                role = "user",
                content = content
            ))
            // 更新会话标题（使用第一条消息）
            if (messages.size == 1) {
                conversationDao.updateConversation(
                    ConversationEntity(
                        id = conversationId,
                        title = content.take(30),
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
        
        // 添加空的 AI 消息用于流式更新
        val aiMessageId = UUID.randomUUID().toString()
        messages.add(UiMessage(id = aiMessageId, role = "assistant", content = "", isStreaming = true))
        
        isLoading = true
        clearError()
        
        viewModelScope.launch {
            try {
                callStreamingApi(aiMessageId, conversationId)
            } catch (e: Exception) {
                Log.e(TAG, "API call failed", e)
                showErrorMessage(e.message ?: "Unknown error")
                messages.removeAll { it.id == aiMessageId }
            } finally {
                isLoading = false
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
        
        viewModelScope.launch {
            delay(3000)
            if (error == message) {
                showError = false
                delay(300)
                error = null
            }
        }
    }
    
    private suspend fun callStreamingApi(aiMessageId: String, conversationId: String) {
        Log.d(TAG, "callStreamingApi: using baseUrl=$baseUrl, model=$model")
        
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
        
        collectStreamingResponse(httpRequest, aiMessageId, conversationId)
    }
    
    private suspend fun collectStreamingResponse(request: Request, aiMessageId: String, conversationId: String) = withContext(Dispatchers.IO) {
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
                        
                        withContext(Dispatchers.Main) {
                            val index = messages.indexOfFirst { it.id == aiMessageId }
                            if (index >= 0) {
                                messages[index] = messages[index].copy(content = fullContent)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse chunk: $data", e)
                }
            }
        }
        
        response.close()
        Log.d(TAG, "Final content length: ${fullContent.length}")
        
        // 保存 AI 回复到数据库
        if (fullContent.isNotEmpty()) {
            messageDao.insertMessage(MessageEntity(
                id = aiMessageId,
                conversationId = conversationId,
                role = "assistant",
                content = fullContent
            ))
        }
    }
    
    fun clearError() {
        showError = false
        viewModelScope.launch {
            delay(300)
            error = null
        }
    }
}
