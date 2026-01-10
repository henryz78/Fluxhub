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
import com.liquidglass.fluxhub.data.AssistantEntity
import com.liquidglass.fluxhub.data.ProviderEntity
import com.liquidglass.fluxhub.data.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import androidx.compose.runtime.snapshotFlow
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
import android.net.Uri
import com.liquidglass.fluxhub.utils.FileUtils
import kotlinx.serialization.json.*

private const val TAG = "ChatViewModel"

@Serializable
data class ChatMessage(
    val role: String,
    val content: JsonElement? = null
)

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean,
    val temperature: Float? = null,
    @kotlinx.serialization.SerialName("top_p")
    val topP: Float? = null,
    @kotlinx.serialization.SerialName("max_tokens")
    val maxTokens: Int? = null,
    @kotlinx.serialization.SerialName("reasoning_effort")
    val reasoningEffort: String? = null
)

// 移除硬编码的数据类，改用动态解析
@Serializable
data class APIError(val message: String? = null)

@Serializable
data class ChatResponse(
    val choices: JsonArray? = null,
    val error: APIError? = null
)

// 用于 UI 显示的消息 (支持消息分支 - 参考 RikkaHub)
data class UiMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: String,
    var content: String,
    var thinkingContent: String? = null,
    val isStreaming: Boolean = false,
    val model: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    // 消息分支支持
    val parentId: String? = null,  // 关联的父消息 ID（用于分支追踪）
    val versionIndex: Int = 0,     // 当前版本索引
    val totalVersions: Int = 1     // 总版本数
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
    
    private val json = Json { 
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true // 确保传过去的 stream: false 等默认值生效
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()
    
    private val database = AppDatabase.getDatabase(application)
    
    private val messageDao = database.messageDao()
    private val conversationDao = database.conversationDao()
    private val assistantDao = database.assistantDao()
    private val providerDao = database.providerDao()
    private val settingsRepository = SettingsRepository(application)
    
    val messages = mutableStateListOf<UiMessage>()
    val availableModels = mutableStateListOf<String>()
    val assistants = mutableStateListOf<AssistantEntity>()
    val providers = mutableStateListOf<ProviderEntity>()
    
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var showError by mutableStateOf(false)
        private set
    
    // 配置（从当前 Provider 或 DataStore 加载）
    var apiKey by mutableStateOf("")
    var baseUrl by mutableStateOf("https://api.openai.com/v1")
    var model by mutableStateOf("") // 默认为空，用户需要选择
    
    // 请求参数 (现在从当前助手读取)
    var temperature by mutableStateOf(0.7f)
    var topP by mutableStateOf(1.0f)
    var maxTokens by mutableStateOf<Int?>(null) // null = 使用模型默认值
    
    // 当前选中的图片 URI (Vision)
    var selectedImageUri by mutableStateOf<Uri?>(null)
    
    // 显示设置
    var themeMode by mutableStateOf("system") // system, light, dark
    var wallpaperUri by mutableStateOf<String?>(null)

    var glassOpacity by mutableStateOf(0.1f)
        private set

    var glassBlur by mutableStateOf(16f)
        private set
    
    // 当前助手
    var currentAssistant by mutableStateOf<AssistantEntity?>(null)
        private set
    
    // 当前服务商
    var currentProvider by mutableStateOf<ProviderEntity?>(null)
        private set

    // 当前会话
    var currentConversationId by mutableStateOf<String?>(null)
    private set
    var currentConversationTitle by mutableStateOf("新对话")
    private set
    
    // 会话列表
    val conversations = mutableStateListOf<ConversationEntity>()
    
    // 用户协议状态
    var agreementAccepted by mutableStateOf(true) // 默认 true 防止闪烁，实际值从 DataStore 加载
        private set
    
    // ========== 工具箱配置项（全局持久存储）==========
    var thinkingBudget by mutableStateOf(1024)
        private set
    var webSearchEnabled by mutableStateOf(false)
        private set
    var searchProvider by mutableStateOf(0)
        private set
    var streamEnabled by mutableStateOf(true)
        private set
    var contextSize by mutableStateOf(64)
        private set
    
    var hapticFeedbackEnabled by mutableStateOf(true)
        private set
    

    // 当前活跃的 EventSource (用于取消)
    private var currentEventSource: EventSource? = null
    
    // 生成任务 Job (参考 RikkaHub ChatService)
    private var generationJob: Job? = null
    
    // Flow 采集任务
    private var messagesJob: Job? = null
    private var conversationsJob: Job? = null
    
    // 模型列表获取任务（防抖）
    private var fetchModelsJob: Job? = null
    
    init {
        loadSettings()
        startConversationsCollection()
        startAssistantsCollection()
        startProvidersCollection()
        loadOrCreateConversation()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.apiKey.collect { 
                // 仅在没有当前 Provider 时接受全局配置更新，避免冲突
                if (currentProvider == null) {
                    apiKey = it 
                    if (it.isNotBlank() && baseUrl.isNotBlank()) fetchModels()
                }
            }
        }
        viewModelScope.launch {
            settingsRepository.baseUrl.collect { 
                if (currentProvider == null) {
                    baseUrl = it
                    if (it.isNotBlank() && apiKey.isNotBlank()) fetchModels()
                }
            }
        }
        viewModelScope.launch {
            settingsRepository.model.collect { model = it }
        }
        viewModelScope.launch {
            settingsRepository.themeMode.collect { themeMode = it }
        }
        viewModelScope.launch {
            settingsRepository.wallpaperUri.collect { wallpaperUri = it }
        }
        viewModelScope.launch {
            settingsRepository.glassOpacity.collect { glassOpacity = it }
        }
        viewModelScope.launch {
            settingsRepository.glassBlur.collect { glassBlur = it }
        }
        viewModelScope.launch {
            settingsRepository.agreementAccepted.collect { agreementAccepted = it }
        }
        // 加载工具箱配置项
        viewModelScope.launch {
            settingsRepository.thinkingBudget.collect { thinkingBudget = it }
        }
        viewModelScope.launch {
            settingsRepository.webSearchEnabled.collect { webSearchEnabled = it }
        }
        viewModelScope.launch {
            settingsRepository.searchProvider.collect { searchProvider = it }
        }
        viewModelScope.launch {
            settingsRepository.streamEnabled.collect { streamEnabled = it }
        }
        viewModelScope.launch {
            settingsRepository.contextSize.collect { contextSize = it }
        }
        viewModelScope.launch {
            settingsRepository.hapticFeedbackEnabled.collect { hapticFeedbackEnabled = it }
        }
    }
    
    fun updateHapticFeedbackEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setHapticFeedbackEnabled(enabled)
        }
    }
    
    fun acceptAgreement() {
        agreementAccepted = true
        viewModelScope.launch {
            settingsRepository.setAgreementAccepted(true)
        }
    }
    
    fun fetchModels() {
        if (apiKey.isBlank() || baseUrl.isBlank()) return
        
        // 取消之前的获取任务（防抖）
        fetchModelsJob?.cancel()
        
        fetchModelsJob = viewModelScope.launch {
            // 延迟 200ms 防抖，避免 apiKey 和 baseUrl 同时变化时多次调用
            kotlinx.coroutines.delay(200)
            
            try {
                val url = "$baseUrl/models"
                Log.d(TAG, "Fetching models from: $url")
                
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $apiKey")
                    .get()
                    .build()
                
                withContext(Dispatchers.IO) {
                    try {
                        client.newCall(request).execute().use { response ->
                            if (response.isSuccessful) {
                                val body = response.body?.string() ?: "{}"
                                try {
                                    val modelsResponse = json.decodeFromString(ModelsResponse.serializer(), body)
                                    val modelIds = modelsResponse.data.map { it.id }.sorted()
                                    
                                    withContext(Dispatchers.Main) {
                                        Log.d(TAG, "Fetched ${modelIds.size} models from $baseUrl")
                                        availableModels.clear()
                                        availableModels.addAll(modelIds)
                                        
                                        // 校验当前选中的模型是否有效
                                        if (model.isNotBlank() && !modelIds.contains(model)) {
                                            Log.w(TAG, "Current model $model not available in $baseUrl")
                                            // 注意：不要在这里清空 saveModel("")，用户可能只是暂时连不上，或者 Provider 没更新
                                            // 我们保持现状，但在 UI 上可以给个提示
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed to parse models from $baseUrl", e)
                                    withContext(Dispatchers.Main) {
                                        if (availableModels.isEmpty()) {
                                            showErrorMessage("模型列表解析失败")
                                        }
                                    }
                                }
                            } else {
                                Log.e(TAG, "Failed to fetch models: ${response.code} for $baseUrl")
                                withContext(Dispatchers.Main) {
                                    if (availableModels.isEmpty()) {
                                        showErrorMessage("获取模型列表失败: ${response.code}")
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Network error fetching models from $baseUrl", e)
                        withContext(Dispatchers.Main) {
                            if (availableModels.isEmpty()) {
                                showErrorMessage("网络错误，无法获取模型列表")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching models", e)
                availableModels.clear()
                if (model.isNotBlank()) saveModel("")
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

                // 获取当前 UI 中存在但数据库中尚未存储的消息（主要是 AI 占位符）
                val uiOnlyMessages = messages.filter { uiMsg ->
                    dbMessages.none { dbMsg -> dbMsg.id == uiMsg.id }
                }
                
                // 组合消息：保留所有 UI 独有的消息（占位符）
                val finalMessages = (dbMessages + uiOnlyMessages).sortedBy { it.timestamp }
                
                // 智能更新 messages 列表
                // 1. 处理删除或重排
                if (messages.size != finalMessages.size) {
                    messages.clear()
                    messages.addAll(finalMessages)
                } else {
                    // 2. 处理内容更新 (逐个替换)
                    finalMessages.forEachIndexed { index, newMessage ->
                        if (messages[index] != newMessage) {
                            messages[index] = newMessage
                        }
                    }
                }
                
                Log.d(TAG, "Messages synced: ${dbMessages.size} from DB, ${uiOnlyMessages.size} UI-only. Total: ${messages.size}")
            }
        }
    }
    
    fun createNewConversation(systemPrompt: String? = null, title: String = "新对话") {
        // 同步更新 ID 和 UI 状态，防止 sendMessage 竞争
        // Removed empty check to align with Rikkahub logic
        val newId = UUID.randomUUID().toString()
        currentConversationId = newId
        currentConversationTitle = title
        messages.clear()
        
        // 开启新消息采集
        startMessagesCollection(newId)
        
        viewModelScope.launch {
            val conversation = ConversationEntity(
                id = newId,
                title = title,
                assistantId = currentAssistant?.id
            )
            conversationDao.insertConversation(conversation)
            
            if (!systemPrompt.isNullOrBlank()) {
                messageDao.insertMessage(MessageEntity(
                    id = UUID.randomUUID().toString(),
                    conversationId = newId,
                    role = "system",
                    content = systemPrompt
                ))
            }
            
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

    fun renameConversation(conversationId: String, newTitle: String) {
        viewModelScope.launch {
            conversationDao.updateConversationTitle(conversationId, newTitle)
            // 如果是当前会话，更新 UI 中的标题
            if (conversationId == currentConversationId) {
                currentConversationTitle = newTitle
            }
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            messageDao.deleteMessage(messageId)
            // 自动从 UI 列表中移除 (DAO 会触发 collect)
        }
    }
    
    /**
     * 删除指定消息及其后续所有消息（用于编辑重发）
     */
    fun deleteMessageAndFollowing(messageId: String) {
        val messageIndex = messages.indexOfFirst { it.id == messageId }
        if (messageIndex == -1) return
        
        // 获取要删除的消息 ID 列表（从该消息开始到最后）
        val idsToDelete = messages.drop(messageIndex).map { it.id }
        
        viewModelScope.launch {
            idsToDelete.forEach { id ->
                messageDao.deleteMessage(id)
            }
        }
    }
    
    /**
     * 重新生成消息
     * 
     * @param messageId 消息 ID
     * 
     * 行为：
     * - 用户消息：删除该消息之后的所有消息，从该用户消息重新生成 AI 回复
     * - AI 消息：删除该 AI 消息及其后续所有消息，从上一条用户消息重新生成
     */
    fun regenerate(messageId: String) {
        val message = messages.find { it.id == messageId } ?: return
        val conversationId = currentConversationId ?: return
        
        // 取消当前生成任务
        cancelGeneration()
        
        generationJob = viewModelScope.launch {
            try {
                val messageIndex = messages.indexOfFirst { it.id == messageId }
                if (messageIndex < 0) return@launch
                
                if (message.role == "user") {
                    // 用户消息：删除该消息之后的所有消息，保留用户消息
                    val idsToDelete = messages.drop(messageIndex + 1).map { it.id }
                    idsToDelete.forEach { id ->
                        messageDao.deleteMessage(id)
                    }
                    messages.removeAll { idsToDelete.contains(it.id) }
                    // 从该用户消息重新生成 AI 回复
                    initiateAiResponse(conversationId)
                } else {
                    // AI 消息：删除该 AI 消息及其后续所有消息
                    val idsToDelete = messages.drop(messageIndex).map { it.id }
                    idsToDelete.forEach { id ->
                        messageDao.deleteMessage(id)
                    }
                    messages.removeAll { idsToDelete.contains(it.id) }
                    // 从上一条用户消息重新生成 AI 回复
                    initiateAiResponse(conversationId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "regenerate failed", e)
                showErrorMessage("重试失败: ${e.message}")
            }
        }
    }
    
    /**
     * 生成 AI 回复（带父消息 ID，用于消息分支）
     */
    private fun initiateAiResponseWithParent(conversationId: String, parentId: String) {
        val parentMessage = messages.find { it.id == parentId }
        val newVersionIndex = (parentMessage?.versionIndex ?: 0) + 1
        
        // 添加 AI 消息占位符
        val aiMessageId = UUID.randomUUID().toString()
        messages.add(UiMessage(
            id = aiMessageId,
            role = "assistant",
            content = "",
            thinkingContent = "",
            isStreaming = streamEnabled,
            model = model,
            parentId = parentId,
            versionIndex = newVersionIndex,
            totalVersions = newVersionIndex + 1
        ))
        
        isLoading = true
        clearError()
        
        if (streamEnabled) {
            callStreamingApiWithEventSource(aiMessageId, conversationId)
        } else {
            callNonStreamingApi(aiMessageId, conversationId)
        }
    }
    
    /**
     * 切换消息版本 (用于消息分支)
     */
    fun switchMessageVersion(messageId: String, direction: Int) {
        val messageIndex = messages.indexOfFirst { it.id == messageId }
        if (messageIndex < 0) return
        
        val currentMessage = messages[messageIndex]
        val newVersionIndex = (currentMessage.versionIndex + direction).coerceIn(0, currentMessage.totalVersions - 1)
        
        if (newVersionIndex != currentMessage.versionIndex) {
            // TODO: 从数据库加载对应版本的消息内容
            // 目前简化处理：只更新版本索引
            messages[messageIndex] = currentMessage.copy(versionIndex = newVersionIndex)
        }
    }
    
    /**
     * 取消当前生成任务 (参考 RikkaHub)
     */
    fun cancelGeneration() {
        currentEventSource?.cancel()
        currentEventSource = null
        generationJob?.cancel()
        generationJob = null
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



    private fun startConversationsCollection() {
        conversationsJob?.cancel()
        conversationsJob = viewModelScope.launch {
            combine(
                snapshotFlow { currentAssistant },
                conversationDao.getAllConversations()
            ) { assistant, allConversations ->
                if (assistant == null) {
                    allConversations
                } else {
                    allConversations.filter { it.assistantId == assistant.id }
                }
            }.collect { filteredList ->
                conversations.clear()
                conversations.addAll(filteredList)
                
                // 处理当前会话不存在的情况
                val currentIdMissing = currentConversationId != null && 
                    conversations.none { it.id == currentConversationId }
                val noConversations = conversations.isEmpty()
                
                if (currentIdMissing || noConversations) {
                    if (conversations.isNotEmpty()) {
                        // 如果有其他会话，切换到第一个
                        switchConversation(conversations.first().id)
                    } else {
                        // 会话列表为空，自动创建新对话（解决新用户首次打开的问题）
                        createNewConversation()
                    }
                }
            }
        }
    }
    
    // 助手 Flow 采集任务
    private var assistantsJob: Job? = null
    
    private fun startAssistantsCollection() {
        assistantsJob?.cancel()
        assistantsJob = viewModelScope.launch {
            assistantDao.getAllAssistants().collect { list ->
                if (list.isEmpty()) {
                    // 如果列表为空，创建一个默认助手
                    launch {
                        val defaultAssistant = AssistantEntity(
                            id = UUID.randomUUID().toString(),
                            name = "通用助手",
                            avatar = "🤖",
                            systemPrompt = "你是一个有用的 AI 助手。",
                            isDefault = true,
                            modelId = null, // 使用默认模型
                            temperature = 0.7f,
                            topP = 1.0f
                        )
                        assistantDao.insertAssistant(defaultAssistant)
                    }
                    // 清空当前列表并等待下一次数据发射
                    assistants.clear()
                } else {
                    assistants.clear()
                    assistants.addAll(list)
                    
                    // 如果没有当前助手，尝试加载默认助手
                    if (currentAssistant == null) {
                        currentAssistant = list.find { it.isDefault } ?: list.first()
                        applyAssistantSettings(currentAssistant!!)
                    }
                }
            }
        }
    }
    
    fun switchAssistant(assistant: AssistantEntity) {
        currentAssistant = assistant
        applyAssistantSettings(assistant)
    }
    
    private fun applyAssistantSettings(assistant: AssistantEntity) {
        temperature = assistant.temperature
        topP = assistant.topP
        maxTokens = assistant.maxTokens
        // 不再覆盖 model，让用户自行在顶栏选择
    }
    
    fun createAssistant(
        name: String,
        avatar: String? = null,
        systemPrompt: String = "",
        temperature: Float = 0.7f,
        topP: Float = 1.0f,
        maxTokens: Int? = null,
        modelId: String? = null
    ) {
        viewModelScope.launch {
            val assistant = AssistantEntity(
                id = UUID.randomUUID().toString(),
                name = name,
                avatar = avatar,
                systemPrompt = systemPrompt,
                temperature = temperature,
                topP = topP,
                maxTokens = maxTokens,
                modelId = modelId,
                isDefault = assistants.isEmpty() // 第一个助手为默认
            )
            assistantDao.insertAssistant(assistant)
        }
    }
    
    fun updateAssistant(assistant: AssistantEntity) {
        viewModelScope.launch {
            assistantDao.updateAssistant(assistant)
            if (currentAssistant?.id == assistant.id) {
                currentAssistant = assistant
                applyAssistantSettings(assistant)
            }
        }
    }
    
    fun deleteAssistant(assistantId: String) {
        viewModelScope.launch {
            assistantDao.deleteAssistant(assistantId)
            if (currentAssistant?.id == assistantId) {
                currentAssistant = assistants.firstOrNull { it.id != assistantId }
                currentAssistant?.let { applyAssistantSettings(it) }
            }
        }
    }
    
    // ========== Provider 管理 ==========
    
    private var providersJob: Job? = null
    
    private fun startProvidersCollection() {
        providersJob?.cancel()
        providersJob = viewModelScope.launch {
            providerDao.getAllProviders().collect { list ->
                providers.clear()
                providers.addAll(list)

                // 同步当前 Provider 的配置变更 (解决用户在其他页面修改配置后此处不更新的问题)
                currentProvider?.let { current ->
                    list.find { it.id == current.id }?.let { updated ->
                        if (updated.apiKey != current.apiKey || updated.baseUrl != current.baseUrl) {
                            Log.d(TAG, "Active provider config updated from DB")
                            currentProvider = updated
                            applyProviderSettings(updated)
                            fetchModels()
                        }
                    }
                }

                // 如果没有当前 Provider，尝试加载激活的 Provider
                if (currentProvider == null && list.isNotEmpty()) {
                    currentProvider = list.find { it.isActive } ?: list.first()
                    applyProviderSettings(currentProvider!!)
                    fetchModels() // 初始加载完成后抓取模型
                }
            }
        }
    }
    
    fun switchProvider(provider: ProviderEntity) {
        viewModelScope.launch {
            providerDao.deactivateAllProviders()
            providerDao.activateProvider(provider.id)
            currentProvider = provider.copy(isActive = true)
            applyProviderSettings(currentProvider!!)
            fetchModels() // 切换 Provider 后重新获取模型列表
        }
    }
    
    private fun applyProviderSettings(provider: ProviderEntity) {
        baseUrl = provider.baseUrl
        apiKey = provider.apiKey
    }
    
    fun createProvider(
        name: String,
        baseUrl: String,
        apiKey: String,
        icon: String? = null
    ) {
        viewModelScope.launch {
            val provider = ProviderEntity(
                id = UUID.randomUUID().toString(),
                name = name,
                baseUrl = baseUrl,
                apiKey = apiKey,
                icon = icon,
                isActive = providers.isEmpty() // 第一个为激活状态
            )
            providerDao.insertProvider(provider)
            if (provider.isActive) {
                currentProvider = provider
                applyProviderSettings(provider)
                fetchModels()
            }
        }
    }
    
    fun updateProvider(provider: ProviderEntity) {
        viewModelScope.launch {
            providerDao.updateProvider(provider)
            if (currentProvider?.id == provider.id) {
                currentProvider = provider
                applyProviderSettings(provider)
                fetchModels()
            }
        }
    }
    
    fun deleteProvider(providerId: String) {
        viewModelScope.launch {
            providerDao.deleteProvider(providerId)
            if (currentProvider?.id == providerId) {
                currentProvider = providers.firstOrNull { it.id != providerId }
                currentProvider?.let { 
                    applyProviderSettings(it)
                    fetchModels()
                }
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

    fun updateThemeMode(value: String) {
        themeMode = value
        viewModelScope.launch {
            settingsRepository.setThemeMode(value)
        }
    }

    fun updateWallpaperUri(value: String?) {
        wallpaperUri = value
        viewModelScope.launch {
            settingsRepository.setWallpaperUri(value)
        }
    }

    fun updateGlassOpacity(value: Float) {
        glassOpacity = value
        viewModelScope.launch {
            settingsRepository.setGlassOpacity(value)
        }
    }

    fun updateGlassBlur(value: Float) {
        glassBlur = value
        viewModelScope.launch {
            settingsRepository.setGlassBlur(value)
        }
    }
    
    // ========== 工具箱配置项更新方法 ==========
    
    fun updateThinkingBudget(value: Int) {
        thinkingBudget = value
        viewModelScope.launch {
            settingsRepository.setThinkingBudget(value)
        }
    }
    
    fun updateWebSearchEnabled(value: Boolean) {
        webSearchEnabled = value
        viewModelScope.launch {
            settingsRepository.setWebSearchEnabled(value)
        }
    }
    
    fun updateSearchProvider(value: Int) {
        searchProvider = value
        viewModelScope.launch {
            settingsRepository.setSearchProvider(value)
        }
    }
    
    fun updateStreamEnabled(value: Boolean) {
        streamEnabled = value
        viewModelScope.launch {
            settingsRepository.setStreamEnabled(value)
        }
    }
    
    fun updateContextSize(value: Int) {
        contextSize = value
        viewModelScope.launch {
            settingsRepository.setContextSize(value)
        }
    }
    
    fun sendMessage(content: String) {
        if (content.isBlank() && selectedImageUri == null) return
        
        val finalContent = if (selectedImageUri != null) {
            val uri = selectedImageUri
            selectedImageUri = null // 消费图片
            "![image]($uri)\n$content"
        } else {
            content
        }
        
        // 优先从当前 Provider 获取配置，确保使用最新值
        val effectiveApiKey = currentProvider?.apiKey ?: apiKey
        val effectiveBaseUrl = currentProvider?.baseUrl ?: baseUrl
        
        if (effectiveApiKey.isBlank()) {
            showErrorMessage("请先配置服务商")
            return
        }
        
        if (model.isBlank()) {
            showErrorMessage("请先选择模型")
            return
        }
        
        val conversationId = currentConversationId ?: return
        
        Log.d(TAG, "sendMessage: $finalContent")
        
        // 添加用户消息
        val userMessageId = UUID.randomUUID().toString()
        val userMessage = UiMessage(id = userMessageId, role = "user", content = finalContent)
        messages.add(userMessage)
        
        // 保存到数据库
        viewModelScope.launch {
            messageDao.insertMessage(MessageEntity(
                id = userMessageId,
                conversationId = conversationId,
                role = "user",
                content = finalContent
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
        
        // 发起 AI 请求
        initiateAiResponse(conversationId)
    }

    fun initiateAiResponse(conversationId: String) {
        // 强制在主线程执行 UI 更新，并确保状态不被之前的请求锁死
        viewModelScope.launch(Dispatchers.Main) {
            if (model.isBlank()) {
                showErrorMessage("请先选择模型")
                isLoading = false
                return@launch
            }

            // 关键：即使 isLoading 为 true，我们也强制重置并添加新气泡
            // 防止因为之前的请求挂起导致 UI 彻底没反应
            if (isLoading) {
                Log.w(TAG, "isLoading was true, forcing reset for new request")
                isLoading = false
            }
            
            val useStream = this@ChatViewModel.streamEnabled
            isLoading = true
            
            val aiMessageId = UUID.randomUUID().toString()
            val initialMessage = UiMessage(
                id = aiMessageId,
                role = "assistant",
                content = "",
                thinkingContent = "",
                isStreaming = useStream,
                model = model
            )
            
            messages.add(initialMessage)
            Log.d(TAG, "Added AI bubble: $aiMessageId, model=$model")
            
            clearError()
            
            // 发起真正的 API 调用
            if (useStream) {
                callStreamingApiWithEventSource(aiMessageId, conversationId)
            } else {
                callNonStreamingApi(aiMessageId, conversationId)
            }
            
            // 安全兜底：如果 30 秒还没反应，强制重置 loading 状态
            launch {
                delay(30000)
                if (isLoading) {
                    Log.w(TAG, "Request timeout, forcing isLoading = false")
                    isLoading = false
                }
            }
        }
    }
    
    fun stopStreaming() {
        cancelGeneration()
    }
    
    private fun callNonStreamingApi(aiMessageId: String, conversationId: String) {
        viewModelScope.launch {
            try {
                // 构建消息列表（共用逻辑）
                val requestMessages = buildApiMessages(messages, aiMessageId)
                
                // 根据 thinkingBudget 计算 reasoning_effort 级别
                val reasoningEffort = when (thinkingBudget) {
                    0 -> null // 关闭时不传此参数
                    in 1..4096 -> "low"
                    in 4097..16000 -> "medium"
                    else -> "high"
                }
                
                val requestJson = buildJsonObject {
                    put("model", model)
                    put("messages", buildJsonArray {
                        requestMessages.forEach { msg ->
                            add(buildJsonObject {
                                put("role", msg.role)
                                // 安全处理：如果是 JsonElement 则直接 put，否则转为 JsonPrimitive
                                val content = msg.content
                                if (content != null) {
                                    put("content", content)
                                } else {
                                    put("content", "")
                                }
                            })
                        }
                    })
                    put("stream", false)
                    temperature.let { put("temperature", it) }
                    topP.let { put("top_p", it) }
                    maxTokens?.let { put("max_tokens", it) }
                    reasoningEffort?.let { put("reasoning_effort", it) }
                }
                
                val requestBody = json.encodeToString(JsonObject.serializer(), requestJson)
                Log.d(TAG, "Request body (Non-Streaming): $requestBody")
                
                val effectiveBaseUrl = currentProvider?.baseUrl ?: baseUrl
                val effectiveApiKey = currentProvider?.apiKey ?: apiKey
                
                val request = Request.Builder()
                    .url("$effectiveBaseUrl/chat/completions")
                    .addHeader("Authorization", "Bearer $effectiveApiKey")
                    .post(requestBody.toRequestBody("application/json".toMediaType()))
                    .build()
                
                withContext(Dispatchers.IO) {
                    val response = client.newCall(request).execute()
                    val body = response.body?.string() ?: "{}"
                    Log.d(TAG, "Non-streaming response raw body: $body") // CRITICAL LOG
                    
                    if (response.isSuccessful) {
                        try {
                            val responseJson = json.parseToJsonElement(body).jsonObject
                            
                            // 检查 API 错误 (更全面的检测)
                            responseJson["error"]?.let { 
                                val errorObj = it.jsonObject
                                val errorMsg = errorObj["message"]?.jsonPrimitive?.contentOrNull 
                                    ?: errorObj["code"]?.jsonPrimitive?.contentOrNull 
                                    ?: "Unknown API Error"
                                throw Exception(errorMsg)
                            }
                            
                            val choices = responseJson["choices"]?.jsonArray
                            val firstChoice = choices?.getOrNull(0)?.jsonObject
                            val messageObj = firstChoice?.get("message")?.jsonObject ?: firstChoice?.get("delta")?.jsonObject
                            
                            // 深度提取内容 (参考 Rikkahub)
                            val contentElement = messageObj?.get("content") ?: firstChoice?.get("text")
                            val contentStr = when (contentElement) {
                                is JsonPrimitive -> contentElement.contentOrNull ?: ""
                                is JsonArray -> contentElement.mapNotNull { 
                                    it.jsonObject["text"]?.jsonPrimitive?.contentOrNull 
                                }.joinToString("")
                                else -> ""
                            }
                            
                            val reasoningElement = messageObj?.get("reasoning_content") ?: messageObj?.get("reasoning")
                            val reasoningStr = when (reasoningElement) {
                                is JsonPrimitive -> reasoningElement.contentOrNull ?: ""
                                else -> ""
                            }
                            
                            Log.d(TAG, "Parsed result - content length: ${contentStr.length}, reasoning length: ${reasoningStr.length}")
                            
                            withContext(Dispatchers.Main) {
                                isLoading = false
                                val index = messages.indexOfFirst { it.id == aiMessageId }
                                if (index >= 0) {
                                    val safeContent = if (contentStr.isEmpty() && reasoningStr.isEmpty()) "⚠️ API 未返回任何内容" else contentStr
                                    messages[index] = messages[index].copy(
                                        content = safeContent,
                                        thinkingContent = reasoningStr.takeIf { it.isNotEmpty() },
                                        isStreaming = false
                                    )
                                    
                                    // 保存到数据库
                                    messageDao.insertMessage(MessageEntity(
                                        id = aiMessageId,
                                        conversationId = conversationId,
                                        role = "assistant",
                                        content = safeContent,
                                        thinkingContent = reasoningStr.takeIf { it.isNotEmpty() },
                                        model = model
                                    ))
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Parsing error", e)
                            throw e 
                        }
                    } else {
                        throw Exception("HTTP ${response.code}: ${response.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Non-streaming request failed", e)
                isLoading = false
                val errorMsg = "请求失败: ${e.message}"
                showErrorMessage(errorMsg)
                
                // 绝不移除气泡，而是显示错误
                val index = messages.indexOfFirst { it.id == aiMessageId }
                if (index >= 0) {
                    messages[index] = messages[index].copy(
                        content = "⚠️ $errorMsg",
                        isStreaming = false
                    )
                }
            }
        }
    }
    
    private fun buildApiMessages(history: List<UiMessage>, aiMessageId: String? = null): List<ChatMessage> {
        Log.d(TAG, "Building API messages from history of size ${history.size}, excluding: $aiMessageId")
        val baseMessages = history
            // 关键：必须排除当前正在生成的(isStreaming)或者占位符消息
            // 否则 API 会因为 history 以 assistant 消息结尾而返回 400 错误
            .filter { 
                it.role == "user" || 
                (it.role == "assistant" && !it.isStreaming && it.content.isNotBlank() && it.id != aiMessageId)
            }
            .takeLast(contextSize) // 直接限制上下文数量

        val processedMsgs = baseMessages.map { message ->
            // 解析 Vision 图片 (Markdown: ![image](uri))
            val imageMatch = Regex("!\\[image\\]\\((.*?)\\)").find(message.content)
            val contentElement = if (imageMatch != null) {
                val uriStr = imageMatch.groupValues[1]
                val textContent = message.content.replace(imageMatch.value, "").trim()
                
                val base64 = try {
                        FileUtils.uriToBase64(getApplication(), Uri.parse(uriStr))
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load image from URI: $uriStr", e)
                    null
                }
                
                if (base64 != null) {
                    buildJsonArray {
                        add(buildJsonObject {
                            put("type", "text")
                            put("text", textContent)
                        })
                        add(buildJsonObject {
                            put("type", "image_url")
                            put("image_url", buildJsonObject {
                                put("url", "data:image/jpeg;base64,$base64")
                            })
                        })
                    }
                } else {
                    JsonPrimitive(message.content)
                }
            } else {
                // 如果 content 已经是 JSON 字符串，尝试解析，否则包装成 JsonPrimitive
                try {
                    if (message.content.trim().startsWith("[")) {
                        json.parseToJsonElement(message.content)
                    } else {
                        JsonPrimitive(message.content)
                    }
                } catch (e: Exception) {
                    JsonPrimitive(message.content)
                }
            }
            
            ChatMessage(message.role, contentElement)
        }
            
        // 过滤无效消息
        val finalMsgs = processedMsgs.filter { it.content != null }
        
        // 注入系统提示词
        val systemPrompt = currentAssistant?.systemPrompt?.takeIf { it.isNotBlank() }
        if (systemPrompt != null) {
            return listOf(ChatMessage("system", JsonPrimitive(systemPrompt))) + finalMsgs
        } else {
            return finalMsgs
        }
    }

    private fun callStreamingApiWithEventSource(aiMessageId: String, conversationId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "callStreamingApiWithEventSource: using baseUrl=$baseUrl, model=$model, contextSize=$contextSize")
                
                val messagesWithSystem = buildApiMessages(messages, aiMessageId)
                
                // 根据 thinkingBudget 计算 reasoning_effort 级别
                val reasoningEffort = when (thinkingBudget) {
                    0 -> null // 关闭时不传此参数
                    in 1..4096 -> "low"
                    in 4097..16000 -> "medium"
                    else -> "high"
                }
                
                val requestJson = buildJsonObject {
                    put("model", model)
                    put("messages", buildJsonArray {
                        messagesWithSystem.forEach { msg ->
                            add(buildJsonObject {
                                put("role", msg.role)
                                val content = msg.content
                                if (content != null) {
                                    put("content", content)
                                } else {
                                    put("content", "")
                                }
                            })
                        }
                    })
                    put("stream", true)
                    // 增加 stream_options 以获得更好的 API 兼容性 (OpenAI 标准)
                    put("stream_options", buildJsonObject {
                        put("include_usage", true)
                    })
                    temperature.let { put("temperature", it) }
                    topP.let { put("top_p", it) }
                    maxTokens?.let { put("max_tokens", it) }
                    reasoningEffort?.let { put("reasoning_effort", it) }
                }
                
                val requestBody = json.encodeToString(JsonObject.serializer(), requestJson)
                Log.d(TAG, "Request body (Streaming): $requestBody")
                
                // 优先从 currentProvider 获取配置
                val effectiveBaseUrl = currentProvider?.baseUrl ?: baseUrl
                val effectiveApiKey = currentProvider?.apiKey ?: apiKey
                
                val request = Request.Builder()
                    .url("$effectiveBaseUrl/chat/completions")
                    .addHeader("Authorization", "Bearer $effectiveApiKey")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "text/event-stream")
                    .post(requestBody.toRequestBody("application/json".toMediaType()))
                    .build()
                
                var fullContent = ""
                var fullThinkingContent = ""
                
                // ... (EventSourceListener setup continues)
        
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
                    // 动态解析 Streaming 响应
                    data.trim().split("\n").filter { it.isNotBlank() }.map { it.removePrefix("data: ") }.forEach { line ->
                        if (line == "[DONE]") return@forEach
                        
                        val chunkJson = json.parseToJsonElement(line).jsonObject
                        
                        // 检查错误
                        chunkJson["error"]?.let { 
                            val errorMsg = it.jsonObject["message"]?.jsonPrimitive?.contentOrNull ?: "Streaming Error"
                            Log.e(TAG, "Streaming API error: $errorMsg")
                            return@forEach
                        }
                        
                        val choices = chunkJson["choices"]?.jsonArray
                        val firstChoice = choices?.getOrNull(0)?.jsonObject
                        val deltaObj = firstChoice?.get("delta")?.jsonObject ?: firstChoice?.get("message")?.jsonObject
                        
                        val deltaContent = deltaObj?.get("content")?.jsonPrimitive?.contentOrNull
                        val deltaReasoning = deltaObj?.get("reasoning_content")?.jsonPrimitive?.contentOrNull
                            ?: deltaObj?.get("reasoning")?.jsonPrimitive?.contentOrNull
                        
                        if (!deltaReasoning.isNullOrEmpty()) {
                            fullThinkingContent += deltaReasoning
                            viewModelScope.launch {
                                val index = messages.indexOfFirst { it.id == aiMessageId }
                                if (index >= 0) {
                                    messages[index] = messages[index].copy(thinkingContent = fullThinkingContent)
                                }
                            }
                        }
                        
                        if (!deltaContent.isNullOrEmpty()) {
                            fullContent += deltaContent
                            viewModelScope.launch {
                                val index = messages.indexOfFirst { it.id == aiMessageId }
                                if (index >= 0) {
                                    messages[index] = messages[index].copy(content = fullContent)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse SSE chunk: ${data.take(200)}", e)
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
                // 特殊处理 stream was reset 错误：如果已经有内容生成，则视为成功
                if (t?.message?.contains("stream was reset", ignoreCase = true) == true && fullContent.isNotEmpty()) {
                    Log.w(TAG, "Stream reset ignored, treating as success (content length: ${fullContent.length})")
                    viewModelScope.launch {
                        isLoading = false
                        
                        val index = messages.indexOfFirst { it.id == aiMessageId }
                        if (index >= 0) {
                            messages[index] = messages[index].copy(isStreaming = false)
                        }
                        
                        // 保存到数据库
                        messageDao.insertMessage(MessageEntity(
                            id = aiMessageId,
                            conversationId = conversationId,
                            role = "assistant",
                            content = fullContent,
                            thinkingContent = fullThinkingContent.takeIf { it.isNotEmpty() },
                            model = model
                        ))
                    }
                    return
                }

                Log.e(TAG, "SSE onFailure: ${t?.message}, response: ${response?.code}")
                
                // 在 IO 线程/回调线程读取 response body，避免 NetworkOnMainThreadException
                val errorBody = try {
                    response?.body?.string()?.take(500) // 限制长度
                } catch (e: Exception) {
                    null
                }
                
                val errorMsg = t?.message ?: errorBody ?: "Unknown error"
                
                viewModelScope.launch {
                    isLoading = false
                    
                    val errorDetail = "请求失败: $errorMsg"
                    showErrorMessage(errorDetail)
                    
                    // 将错误信息写入气泡，而不是移除
                    val index = messages.indexOfFirst { it.id == aiMessageId }
                    if (index >= 0) {
                        messages[index] = messages[index].copy(
                            content = "⚠️ $errorDetail",
                            isStreaming = false
                        )
                    }
                }
            }
        }
        
                // 使用 EventSources 创建 SSE 连接
                currentEventSource = EventSources.createFactory(client).newEventSource(request, listener)
                Log.d(TAG, "SSE EventSource created successfully for $aiMessageId")
            } catch (e: Exception) {
                Log.e(TAG, "Streaming request setup failed", e)
                isLoading = false
                val errorMsg = "初始化流式请求失败: ${e.message}"
                showErrorMessage(errorMsg)
                
                // 绝不移除气泡，而是显示错误
                val index = messages.indexOfFirst { it.id == aiMessageId }
                if (index >= 0) {
                    messages[index] = messages[index].copy(
                        content = "⚠️ $errorMsg",
                        isStreaming = false
                    )
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
    
    fun clearError() {
        showError = false
        viewModelScope.launch {
            delay(300)
            error = null
        }
    }

    override fun onCleared() {
        super.onCleared()
        currentEventSource?.cancel()
    }
}
