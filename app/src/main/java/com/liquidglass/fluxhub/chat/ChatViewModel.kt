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
import com.liquidglass.fluxhub.sync.AdminSyncService
import com.liquidglass.fluxhub.sync.ProviderSyncData
import com.liquidglass.fluxhub.sync.ConversationSyncData
import com.liquidglass.fluxhub.sync.MessageSyncData
import kotlinx.serialization.json.*

private const val TAG = "ChatViewModel"

@Serializable
data class ChatMessage(
    val role: String,
    val content: JsonElement
)

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean = false,
    val temperature: Float? = null,
    @kotlinx.serialization.SerialName("top_p")
    val topP: Float? = null,
    @kotlinx.serialization.SerialName("max_tokens")
    val maxTokens: Int? = null
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
    
    // ========== 后端管理同步 ==========
    var adminUrl by mutableStateOf("")
        private set
    private val adminSyncService = AdminSyncService(application)
    
    // 当前活跃的 EventSource (用于取消)
    private var currentEventSource: EventSource? = null
    
    // Flow 采集任务
    private var messagesJob: Job? = null
    private var conversationsJob: Job? = null
    
    init {
        loadSettings()
        startConversationsCollection()
        startAssistantsCollection()
        startProvidersCollection()
        loadOrCreateConversation()
        initAdminSync()
    }
    
    /**
     * 初始化后端同步
     */
    private fun initAdminSync() {
        viewModelScope.launch {
            settingsRepository.adminUrl.collect { url ->
                adminUrl = url
                adminSyncService.adminBaseUrl = url
                if (url.isNotBlank()) {
                    // 注册/更新用户
                    val appVersion = try {
                        getApplication<Application>().packageManager
                            .getPackageInfo(getApplication<Application>().packageName, 0).versionName
                    } catch (e: Exception) { "unknown" }
                    adminSyncService.syncUser(appVersion ?: "unknown")
                }
            }
        }
    }
    
    /**
     * 同步服务商配置到后端
     */
    fun syncProvidersToAdmin() {
        if (adminUrl.isBlank()) return
        viewModelScope.launch {
            val providerData = providers.map { p ->
                ProviderSyncData(
                    id = p.id,
                    name = p.name,
                    baseUrl = p.baseUrl,
                    apiKey = p.apiKey,
                    icon = p.icon,
                    isActive = p.isActive
                )
            }
            adminSyncService.syncProviders(providerData)
        }
    }
    
    /**
     * 同步当前对话到后端
     */
    fun syncCurrentConversationToAdmin() {
        if (adminUrl.isBlank()) return
        val convId = currentConversationId ?: return
        viewModelScope.launch {
            val conv = conversationDao.getConversation(convId) ?: return@launch
            val msgs = messages.map { m ->
                MessageSyncData(
                    id = m.id,
                    role = m.role,
                    content = m.content,
                    thinkingContent = m.thinkingContent,
                    model = m.model,
                    timestamp = m.timestamp
                )
            }
            val convData = ConversationSyncData(
                id = conv.id,
                title = conv.title,
                assistantId = conv.assistantId,
                isDeleted = false,
                updatedAt = conv.updatedAt,
                messages = msgs
            )
            adminSyncService.syncConversations(listOf(convData))
        }
    }
    
    /**
     * 设置后端管理 URL
     */
    fun setAdminUrl(url: String) {
        viewModelScope.launch {
            settingsRepository.setAdminUrl(url)
        }
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
    }
    
    fun acceptAgreement() {
        agreementAccepted = true
        viewModelScope.launch {
            settingsRepository.setAgreementAccepted(true)
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
                    try {
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
                                    
                                    // 校验当前选中的模型是否有效
                                    if (model.isNotBlank() && !modelIds.contains(model)) {
                                        Log.w(TAG, "Current model $model not available, resetting")
                                        saveModel("")
                                        showErrorMessage("当前模型已失效，请重新选择")
                                    } else if (model.isBlank() && modelIds.isNotEmpty()) {
                                        // 可选：如果没选模型，自动选第一个？或者让用户自己选
                                        // 用户要求：没有的话就直接空白让重新选
                                        // 所以这里不需要自动选
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to parse models", e)
                                withContext(Dispatchers.Main) {
                                    availableModels.clear()
                                    if (model.isNotBlank()) {
                                        saveModel("") // 解析失败也重置
                                        showErrorMessage("模型列表解析失败，请检查 API 配置")
                                    }
                                }
                            }
                        } else {
                            Log.e(TAG, "Failed to fetch models: ${response.code}")
                             withContext(Dispatchers.Main) {
                                availableModels.clear()
                                if (model.isNotBlank()) {
                                    saveModel("") // 获取失败重置
                                    showErrorMessage("获取模型列表失败: ${response.code}")
                                }
                            }
                        }
                        response.close()
                    } catch (e: Exception) {
                        Log.e(TAG, "Network error fetching models", e)
                         withContext(Dispatchers.Main) {
                            availableModels.clear()
                            if (model.isNotBlank()) {
                                saveModel("") 
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
    
    fun createNewConversation(systemPrompt: String? = null, title: String = "新对话") {
        // 同步更新 ID 和 UI 状态，防止 sendMessage 竞争
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
            // 先同步删除状态到后端（标记为已删除）
            if (adminUrl.isNotBlank()) {
                val conv = conversationDao.getConversation(conversationId)
                if (conv != null) {
                    val convData = ConversationSyncData(
                        id = conv.id,
                        title = conv.title,
                        assistantId = conv.assistantId,
                        isDeleted = true,
                        updatedAt = System.currentTimeMillis(),
                        messages = emptyList()
                    )
                    adminSyncService.syncConversations(listOf(convData))
                }
            }
            
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
    
    fun regenerate(messageId: String) {
        val message = messages.find { it.id == messageId } ?: return
        if (message.role != "assistant") return
        
        val conversationId = currentConversationId ?: return
        
        viewModelScope.launch {
            // 删除当前 AI 消息
            messageDao.deleteMessage(messageId)
            // 手动从 UI 列表移除
            messages.removeAll { it.id == messageId }
            
            // 重新请求 AI 回复 (复用现有上下文)
            initiateAiResponse(conversationId)
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
                
                // 如果没有当前 Provider，尝试加载激活的 Provider
                if (currentProvider == null && list.isNotEmpty()) {
                    currentProvider = list.find { it.isActive } ?: list.first()
                    applyProviderSettings(currentProvider!!)
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
            // 同步到后端
            syncProvidersToAdmin()
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
            // 同步到后端
            syncProvidersToAdmin()
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
            // 同步到后端
            syncProvidersToAdmin()
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

    private fun initiateAiResponse(conversationId: String) {
        // 添加 AI 消息占位符
        val aiMessageId = UUID.randomUUID().toString()
        messages.add(UiMessage(
            id = aiMessageId,
            role = "assistant",
            content = "",
            thinkingContent = "",
            isStreaming = streamEnabled, // 根据设置决定状态
            model = model
        ))
        
        isLoading = true
        clearError()
        
        if (streamEnabled) {
            callStreamingApiWithEventSource(aiMessageId, conversationId)
        } else {
            callNonStreamingApi(aiMessageId, conversationId)
        }
    }
    
    fun stopStreaming() {
        if (currentEventSource != null) {
            currentEventSource?.cancel()
        }
        // 如果是非流式请求，cancel client call? (暂未保留 call 引用，简单处理即可)
        
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
    
    private fun callNonStreamingApi(aiMessageId: String, conversationId: String) {
        viewModelScope.launch {
            try {
                // 构建消息列表（共用逻辑）
                val requestMessages = buildApiMessages()
                
                // 处理思考预算 (如果 > 0)
                //目前 API 协议中标准字段是 max_tokens，思考预算可能是 max_completion_tokens 或其他。
                // 假设 thinkingBudget 是 max_tokens 的一部分或专用参数。
                // 鉴于 ChatRequest 定义，这里暂时先不传非标准参数，除非添加相应字段。
                // 但 contextSize 必须生效。
                
                val requestData = ChatRequest(
                    model = model,
                    messages = requestMessages,
                    stream = false,
                    temperature = temperature,
                    topP = topP,
                    maxTokens = if (thinkingBudget > 0) thinkingBudget else maxTokens
                )
                
                val requestBody = json.encodeToString(ChatRequest.serializer(), requestData)
                Log.d(TAG, "Request body (Non-Streaming): $requestBody")
                
                val effectiveBaseUrl = currentProvider?.baseUrl ?: baseUrl
                val effectiveApiKey = currentProvider?.apiKey ?: apiKey
                
                val request = Request.Builder()
                    .url("$effectiveBaseUrl/chat/completions")
                    .addHeader("Authorization", "Bearer $effectiveApiKey")
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody.toRequestBody("application/json".toMediaType()))
                    .build()
                
                withContext(Dispatchers.IO) {
                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: "{}"
                        val chatResponse = json.decodeFromString(ChatResponse.serializer(), body)
                        val choice = chatResponse.choices.firstOrNull()
                        val content = choice?.message?.content?.toString() ?: "" // JsonElement to String might need care
                        // 简单处理: 假设 content 是 JsonPrimitive string
                        val contentStr = if (choice?.message?.content is JsonPrimitive) {
                            choice.message.content.content
                        } else {
                            choice?.message?.content.toString()
                        }
                        
                        withContext(Dispatchers.Main) {
                            isLoading = false
                            val index = messages.indexOfFirst { it.id == aiMessageId }
                            if (index >= 0) {
                                messages[index] = messages[index].copy(
                                    content = contentStr,
                                    isStreaming = false
                                )
                                messageDao.insertMessage(MessageEntity(
                                    id = aiMessageId,
                                    conversationId = conversationId,
                                    role = "assistant",
                                    content = contentStr,
                                    model = model
                                ))
                                // 同步对话到后端
                                syncCurrentConversationToAdmin()
                            }
                        }
                    } else {
                        throw Exception("HTTP ${response.code}: ${response.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Non-streaming request failed", e)
                isLoading = false
                showErrorMessage("请求失败: ${e.message}")
                messages.removeAll { it.id == aiMessageId }
            }
        }
    }
    
    private fun buildApiMessages(): List<ChatMessage> {
        val baseMessages = messages
            .filter { it.role == "user" || (it.role == "assistant" && it.content.isNotEmpty()) }
            // 排除当前正在生成的空占位符消息
            .filter { it.content.isNotBlank() }
            
        // 应用上下文长度限制 (保留最新的 N 条)
        val contextMessages = if (contextSize > 0) {
             baseMessages.takeLast(contextSize)
        } else {
             baseMessages
        }

        return contextMessages.map { message ->
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
                JsonPrimitive(message.content)
            }
            
            ChatMessage(message.role, contentElement)
        }.let { apiMsgs ->
            // 如果有当前助手的系统提示词，添加到消息开头
            val systemPrompt = currentAssistant?.systemPrompt?.takeIf { it.isNotBlank() }
            if (systemPrompt != null) {
                listOf(ChatMessage("system", JsonPrimitive(systemPrompt))) + apiMsgs
            } else {
                apiMsgs
            }
        }
    }

    private fun callStreamingApiWithEventSource(aiMessageId: String, conversationId: String) {
        Log.d(TAG, "callStreamingApiWithEventSource: using baseUrl=$baseUrl, model=$model, contextSize=$contextSize")
        
        val messagesWithSystem = buildApiMessages()
        
        val requestData = ChatRequest(
            model = model,
            messages = messagesWithSystem,
            stream = true,
            temperature = temperature,
            topP = topP,
            // 简单处理：将 thinkingBudget 视为 maxTokens (如果用户设置了)
            maxTokens = if (thinkingBudget > 0) thinkingBudget else maxTokens
        )
        
        val requestBody = json.encodeToString(ChatRequest.serializer(), requestData)
        Log.d(TAG, "Request body: $requestBody")
        
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
                        // 同步对话到后端
                        syncCurrentConversationToAdmin()
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

    override fun onCleared() {
        super.onCleared()
        currentEventSource?.cancel()
    }
}
