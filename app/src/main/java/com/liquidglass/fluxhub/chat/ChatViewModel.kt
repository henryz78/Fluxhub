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
import com.liquidglass.fluxhub.utils.TTSHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
    val content: String? = null,
    val reasoning_content: String? = null
)

// 用于 UI 显示的消息
data class UiMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: String,
    var content: String,
    var thinkingContent: String? = null,
    val isStreaming: Boolean = false,
    val model: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class ModelsResponse(
    val data: List<Model>
)

@Serializable
data class Model(
    val id: String
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
    val availableModels = mutableStateListOf<String>()
    
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var showError by mutableStateOf(false)
        private set
    
    // 配置（从 DataStore 加载）
    var apiKey by mutableStateOf("")
    var baseUrl by mutableStateOf("https://api.openai.com/v1")
    var model by mutableStateOf("") // 默认为空，用户需要选择
    
    // 用户配置
    var userName by mutableStateOf("你")
    var userAvatar by mutableStateOf("")

    // 当前会话
    var currentConversationId by mutableStateOf<String?>(null)
    private set
    var currentConversationTitle by mutableStateOf("新对话")
    private set
    
    // 会话列表
    val conversations = mutableStateListOf<ConversationEntity>()
    
    // TTS 辅助工具
    private val ttsHelper = TTSHelper(application)
    
    // 当前活跃的 EventSource (用于取消)
    private var currentEventSource: EventSource? = null
    
    // Flow 采集任务
    private var messagesJob: Job? = null
    private var conversationsJob: Job? = null
    
    init {
        loadSettings()
        startConversationsCollection()
        loadOrCreateConversation()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.apiKey.collect { 
                apiKey = it 
                if (it.isNotBlank() && baseUrl.isNotBlank()) fetchModels()
            }
        }
        viewModelScope.launch {
            settingsRepository.baseUrl.collect { 
                baseUrl = it
                if (it.isNotBlank() && apiKey.isNotBlank()) fetchModels()
            }
        }
        viewModelScope.launch {
            settingsRepository.model.collect { model = it }
        }
        viewModelScope.launch {
            settingsRepository.userName.collect { userName = it }
        }
        viewModelScope.launch {
            settingsRepository.userAvatar.collect { userAvatar = it }
        }
    }
    
    fun fetchModels() {
        if (apiKey.isBlank() || baseUrl.isBlank()) return
        
        viewModelScope.launch {
            try {
                val url = "$baseUrl/models"
                Log.d(TAG, "Fetching models from: $url")
                
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $apiKey")
                    .get()
                    .build()
                
                withContext(Dispatchers.IO) {
                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: "{}"
                        try {
                            val modelsResponse = json.decodeFromString(ModelsResponse.serializer(), body)
                            val modelIds = modelsResponse.data.map { it.id }.sorted()
                            withContext(Dispatchers.Main) {
                                availableModels.clear()
                                availableModels.addAll(modelIds)
                                Log.d(TAG, "Fetched ${modelIds.size} models")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to parse models", e)
                        }
                    } else {
                        Log.e(TAG, "Failed to fetch models: ${response.code}")
                    }
                    response.close()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching models", e)
            }
        }
    }

    private fun loadOrCreateConversation() {
        viewModelScope.launch {
            val savedId = settingsRepository.currentConversationId.first()
            if (savedId != null) {
                val conversation = conversationDao.getConversation(savedId)
                if (conversation != null) {
                    currentConversationId = savedId
                    currentConversationTitle = conversation.title
                    startMessagesCollection(savedId)
                    return@launch
                }
            }
            createNewConversation()
        }
    }
    
    private fun startMessagesCollection(conversationId: String) {
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            messageDao.getMessagesForConversation(conversationId).collect { entities ->
                // 获取当前正在流式输出的消息（如果有）
                val currentStreamingMessage = messages.find { it.isStreaming }
                
                val dbMessages = entities.map { entity ->
                    UiMessage(
                        id = entity.id,
                        role = entity.role,
                        content = entity.content,
                        thinkingContent = entity.thinkingContent,
                        isStreaming = false,
                        model = entity.model,
                        timestamp = entity.timestamp
                    )
                }
                
                // 组合消息：如果流式消息不在数据库中，则手动加入
                val finalMessages = if (currentStreamingMessage != null && dbMessages.none { it.id == currentStreamingMessage.id }) {
                    (dbMessages + currentStreamingMessage).sortedBy { it.timestamp }
                } else {
                    dbMessages
                }
                
                // 增量更新 messages 列表，避免频繁 clear 导致 UI 闪烁或状态丢失
                if (messages.size == finalMessages.size) {
                    finalMessages.forEachIndexed { index, newMessage ->
                        if (messages[index] != newMessage) {
                            messages[index] = newMessage
                        }
                    }
                } else {
                    messages.clear()
                    messages.addAll(finalMessages)
                }
                
                Log.d(TAG, "Messages updated: ${messages.size} total (Streaming present: ${currentStreamingMessage != null})")
            }
        }
    }
    
    fun createNewConversation() {
        // 同步更新 ID 和 UI 状态，防止 sendMessage 竞争
        val newId = UUID.randomUUID().toString()
        currentConversationId = newId
        currentConversationTitle = "新对话"
        messages.clear()
        
        // 开启新消息采集
        startMessagesCollection(newId)
        
        viewModelScope.launch {
            val conversation = ConversationEntity(
                id = newId,
                title = "新对话"
            )
            conversationDao.insertConversation(conversation)
            settingsRepository.setCurrentConversationId(newId)
        }
    }
    
    fun switchConversation(conversationId: String) {
        if (conversationId == currentConversationId) return
        
        // 同步设置以提升响应速度
        currentConversationId = conversationId
        messages.clear()
        
        // 取消当前流式输出
        currentEventSource?.cancel()
        isLoading = false
        
        viewModelScope.launch {
            val conversation = conversationDao.getConversation(conversationId)
            if (conversation != null) {
                currentConversationTitle = conversation.title
                settingsRepository.setCurrentConversationId(conversationId)
                startMessagesCollection(conversationId)
            }
        }
    }
    
    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            // 删除消息
            messageDao.deleteMessagesForConversation(conversationId)
            // 删除会话
            conversationDao.deleteConversation(conversationId)
            
            if (conversationId == currentConversationId) {
                createNewConversation()
            }
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            messageDao.deleteMessage(messageId)
            // 自动从 UI 列表中移除 (DAO 会触发 collect)
        }
    }
    
    fun regenerate(messageId: String) {
        val messageIndex = messages.indexOfFirst { it.id == messageId }
        if (messageIndex == -1) return
        
        val message = messages[messageIndex]
        if (message.role != "assistant") return
        
        // 查找前一个用户的消息
        var userMessage: UiMessage? = null
        for (i in messageIndex - 1 downTo 0) {
            if (messages[i].role == "user") {
                userMessage = messages[i]
                break
            }
        }
        
        if (userMessage != null) {
            viewModelScope.launch {
                // 删除当前 AI 消息
                messageDao.deleteMessage(messageId)
                // 重新发送前一个用户消息内容
                sendMessage(userMessage.content)
            }
        }
    }

    fun saveUserName(value: String) {
        userName = value
        viewModelScope.launch {
            settingsRepository.setUserName(value)
        }
    }

    fun saveUserAvatar(value: String) {
        userAvatar = value
        viewModelScope.launch {
            settingsRepository.setUserAvatar(value)
        }
    }

    private fun startConversationsCollection() {
        conversationsJob?.cancel()
        conversationsJob = viewModelScope.launch {
            conversationDao.getAllConversations().collect { list ->
                conversations.clear()
                conversations.addAll(list)
            }
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
            return
        }
        
        if (model.isBlank()) {
            showErrorMessage("请先选择模型")
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
            
            // 始终更新会话状态
            val currentConv = conversationDao.getConversation(conversationId)
            if (messages.size <= 1) {
                val newTitle = content.take(50)
                conversationDao.updateConversation(
                    ConversationEntity(
                        id = conversationId,
                        title = newTitle,
                        createdAt = currentConv?.createdAt ?: System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                )
                currentConversationTitle = newTitle
            } else {
                currentConv?.let {
                    conversationDao.updateConversation(it.copy(updatedAt = System.currentTimeMillis()))
                }
            }
        }
        
        // 添加 AI 消息占位符
        val aiMessageId = UUID.randomUUID().toString()
        messages.add(UiMessage(
            id = aiMessageId,
            role = "assistant",
            content = "",
            thinkingContent = "",
            isStreaming = true,
            model = model
        ))
        
        isLoading = true
        clearError()
        
        // 使用 OkHttp EventSources 进行流式请求
        callStreamingApiWithEventSource(aiMessageId, conversationId)
    }
    
    fun stopStreaming() {
        currentEventSource?.cancel()
        isLoading = false
        
        // 标记最后一条消息为非流式
        val lastMessage = messages.lastOrNull()
        if (lastMessage?.isStreaming == true) {
            val index = messages.indexOfLast { it.isStreaming }
            if (index >= 0) {
                messages[index] = messages[index].copy(isStreaming = false)
            }
        }
    }
    
    private fun callStreamingApiWithEventSource(aiMessageId: String, conversationId: String) {
        Log.d(TAG, "callStreamingApiWithEventSource: using baseUrl=$baseUrl, model=$model")
        
        val apiMessages = messages
            .filter { it.role == "user" || (it.role == "assistant" && it.content.isNotEmpty()) }
            .map { ChatMessage(it.role, it.content) }
        
        val requestData = ChatRequest(
            model = model,
            messages = apiMessages,
            stream = true
        )
        
        val requestBody = json.encodeToString(ChatRequest.serializer(), requestData)
        Log.d(TAG, "Request body: $requestBody")
        
        val request = Request.Builder()
            .url("$baseUrl/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "text/event-stream")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()
        
        var fullContent = ""
        var fullThinkingContent = ""
        
        val listener = object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) {
                Log.d(TAG, "SSE onOpen: ${response.code}")
            }
            
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                if (data == "[DONE]") {
                    Log.d(TAG, "SSE stream completed with [DONE], content length: ${fullContent.length}")
                    
                    // 收到 [DONE] 时主动完成处理
                    viewModelScope.launch {
                        isLoading = false
                        
                        val index = messages.indexOfFirst { it.id == aiMessageId }
                        if (index >= 0) {
                            messages[index] = messages[index].copy(isStreaming = false)
                        }
                        
                        // 保存到数据库
                        if (fullContent.isNotEmpty()) {
                            messageDao.insertMessage(MessageEntity(
                                id = aiMessageId,
                                conversationId = conversationId,
                                role = "assistant",
                                content = fullContent,
                                thinkingContent = fullThinkingContent.takeIf { it.isNotEmpty() },
                                model = model
                            ))
                        } else {
                            if (index >= 0) {
                                messages[index] = messages[index].copy(content = "⚠️ 无内容返回")
                            }
                        }
                    }
                    
                    // 取消连接
                    eventSource.cancel()
                    return
                }
                
                Log.d(TAG, "SSE onEvent: ${data.take(100)}")
                
                try {
                    // 处理可能的多行 JSON（有些 API 会在一个 event 中返回多个 JSON）
                    data.trim().split("\n").filter { it.isNotBlank() }.forEach { line ->
                        val chunk = json.decodeFromString(ChatResponse.serializer(), line)
                        val delta = chunk.choices.firstOrNull()?.delta
                        val deltaContent = delta?.content
                        val deltaReasoning = delta?.reasoning_content
                        
                        if (!deltaReasoning.isNullOrEmpty()) {
                            fullThinkingContent += deltaReasoning
                            
                            // 更新 UI (使用 copy 触发重绘)
                            viewModelScope.launch {
                                val index = messages.indexOfFirst { it.id == aiMessageId }
                                if (index >= 0) {
                                    messages[index] = messages[index].copy(thinkingContent = fullThinkingContent)
                                }
                            }
                        }
                        
                        if (!deltaContent.isNullOrEmpty()) {
                            fullContent += deltaContent
                            
                            // 更新 UI (使用 copy 触发重绘)
                            viewModelScope.launch {
                                val index = messages.indexOfFirst { it.id == aiMessageId }
                                if (index >= 0) {
                                    messages[index] = messages[index].copy(content = fullContent)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse SSE chunk: ${data.take(100)}", e)
                }
            }
            
            override fun onClosed(eventSource: EventSource) {
                Log.d(TAG, "SSE onClosed, final content length: ${fullContent.length}")
                
                viewModelScope.launch {
                    isLoading = false
                    
                    val index = messages.indexOfFirst { it.id == aiMessageId }
                    if (index >= 0) {
                        messages[index] = messages[index].copy(isStreaming = false)
                    }
                    
                    // 保存到数据库
                    if (fullContent.isNotEmpty()) {
                        messageDao.insertMessage(MessageEntity(
                            id = aiMessageId,
                            conversationId = conversationId,
                            role = "assistant",
                            content = fullContent,
                            thinkingContent = fullThinkingContent.takeIf { it.isNotEmpty() },
                            model = model
                        ))
                    } else {
                        // 如果没有内容，显示错误
                        if (index >= 0) {
                            messages[index] = messages[index].copy(content = "⚠️ 无内容返回")
                        }
                    }
                }
            }
            
            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                Log.e(TAG, "SSE onFailure: ${t?.message}, response: ${response?.code}")
                
                viewModelScope.launch {
                    isLoading = false
                    
                    val errorMsg = t?.message ?: response?.body?.string()?.take(200) ?: "Unknown error"
                    showErrorMessage("请求失败: $errorMsg")
                    
                    // 移除空的 AI 消息
                    messages.removeAll { it.id == aiMessageId }
                }
            }
        }
        
        // 使用 EventSources 创建 SSE 连接
        currentEventSource = EventSources.createFactory(client).newEventSource(request, listener)
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
    
    fun clearError() {
        showError = false
        viewModelScope.launch {
            delay(300)
            error = null
        }
    }
    
    fun speak(text: String) {
        ttsHelper.speak(text)
    }

    fun stopSpeaking() {
        ttsHelper.stop()
    }

    override fun onCleared() {
        super.onCleared()
        currentEventSource?.cancel()
        ttsHelper.release()
    }
}
