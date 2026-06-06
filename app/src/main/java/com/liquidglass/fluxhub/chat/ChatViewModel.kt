package com.liquidglass.fluxhub.chat

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.liquidglass.fluxhub.data.AppDatabase
import com.liquidglass.fluxhub.data.ConversationEntity
import com.liquidglass.fluxhub.data.DataRepository
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
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.sse.EventSource
import java.util.UUID
import java.util.concurrent.TimeUnit
import android.net.Uri
import com.liquidglass.fluxhub.utils.FileUtils
import kotlinx.serialization.json.*

import java.util.Collections

private const val TAG = "ChatViewModel"

// 用于 UI 显示的消息 (支持消息分支 - 参考 RikkaHub)
// @Immutable 帮助 Compose 跳过不必要的重组
@androidx.compose.runtime.Immutable
data class UiMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: String,
    val content: String,  // 改为 val 配合 Immutable
    val thinkingContent: String? = null,  // 改为 val 配合 Immutable
    val isStreaming: Boolean = false,
    val model: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    // 消息分支支持
    val parentId: String? = null,  // 关联的父消息 ID（用于分支追踪）
    val versionIndex: Int = 0,     // 当前版本索引
    val totalVersions: Int = 1     // 总版本数
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
    private val chatApiClient = ChatApiClient(client)
    
    private val database = AppDatabase.getDatabase(application)
    
    private val messageDao = database.messageDao()
    private val conversationDao = database.conversationDao()
    private val assistantDao = database.assistantDao()
    private val providerDao = database.providerDao()
    private val settingsRepository = SettingsRepository(application)
    private val dataRepository = DataRepository(application)
    
    // UI State
    var messages = mutableStateListOf<UiMessage>()
    val availableModels = mutableStateListOf<String>()
    val assistants = mutableStateListOf<AssistantEntity>()
    val providers = mutableStateListOf<ProviderEntity>()
    
    var isLoading by mutableStateOf(false)
        private set
    var streamingTokenCount by mutableIntStateOf(0)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var showError by mutableStateOf(false)
        private set
    
    // 设置是否初始化完成 (用于防止壁纸闪烁)
    var isSettingsInitialized by mutableStateOf(false)
        private set
    
    // 配置（从当前 Provider 或 DataStore 加载）
    var apiKey by mutableStateOf("")
    var baseUrl by mutableStateOf("https://api.openai.com/v1")
    var model by mutableStateOf("") // 默认为空，用户需要选择
    var defaultModel by mutableStateOf("") // 全局默认模型
    
    // 请求参数 (现在从当前助手读取)
    var temperature by mutableStateOf(0.7f)
    var topP by mutableStateOf(1.0f)
    var maxTokens by mutableStateOf<Int?>(null) // null = 使用模型默认值
    
    // 记录已删除的消息 ID，防止 sync 逻辑将其“复活”
    private val deletedMessageIds = Collections.synchronizedSet(HashSet<String>())
    
    // 记录"临时会话" ID (尚未保存到数据库的会话)
    private val transientConversationIds = Collections.synchronizedSet(HashSet<String>())
    // 暂存系统提示词 (用于在第一条消息发送时写入)
    private var pendingSystemPrompt: String? = null
    
    // 当前选中的图片 URI (Vision)
    var selectedImageUri by mutableStateOf<Uri?>(null)

    fun stopGeneration() {
        currentEventSource?.cancel()
        isLoading = false
    }
    
    // 输入框文本（保存在 ViewModel 中，避免导航时丢失）
    var inputText by mutableStateOf("")
    
    // 编辑状态：正在编辑的消息 ID
    var editingMessageId by mutableStateOf<String?>(null)
        private set
    
    /**
     * 开始编辑消息
     */
    fun startEditingMessage(messageId: String, content: String) {
        editingMessageId = messageId
        inputText = content
    }
    
    /**
     * 取消编辑
     */
    fun cancelEditing() {
        editingMessageId = null
        inputText = ""
    }
    
    /**
     * 是否正在编辑
     */
    fun isEditing(): Boolean = editingMessageId != null
    
    // 显示设置
    var themeMode by mutableStateOf("system") // system, light, dark
    var wallpaperUri by mutableStateOf<String?>(null)

    var glassOpacity by mutableStateOf(0.1f)
        private set

    var glassBlur by mutableStateOf(16f)
        private set
    
    var glassColor by mutableStateOf("default") // default, 或十六进制颜色如 "FF007AFF"
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
    
    // ========== 灵动岛配置项 ==========
    var dynamicIslandEnabled by mutableStateOf(true)
        private set
    var loginNotificationMode by mutableStateOf("first") // "first" or "every"
        private set
    var dynamicIslandDuration by mutableStateOf(3) // 秒
        private set
    var showTokenCount by mutableStateOf(true)
        private set
    var showElapsedTime by mutableStateOf(true)
        private set
    
    // 触感反馈
    var hapticFeedbackEnabled by mutableStateOf(true)
        private set
    
    // ========== 字体样式配置 ==========
    var textColorMode by mutableStateOf("white") // white, black
        private set
    var textShadowEnabled by mutableStateOf(true)
        private set

    // 当前活跃的 EventSource (用于取消)
    private var currentEventSource: EventSource? = null
    
    // 生成任务 Job (参考 RikkaHub ChatService)
    private var generationJob: Job? = null
    private var requestTimeoutJob: Job? = null
    
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
        // 应用启动时总是创建新对话，避免加载历史消息导致的切换卡顿
        // 用户仍可通过侧边栏切换到历史对话
        createNewConversation()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            // 预加载关键视觉配置，防止 UI 闪烁 (FOUC)
            // 读取 wallaperUri 和 agreementAccepted 的初始值
            try {
                wallpaperUri = settingsRepository.wallpaperUri.first()
                agreementAccepted = settingsRepository.agreementAccepted.first()
                defaultModel = settingsRepository.defaultModel.first() // 预加载默认模型
                isSettingsInitialized = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to preload settings", e)
                isSettingsInitialized = true // 即使失败也允许渲染
            }

            // 启动实时监听
            launch {
                settingsRepository.apiKey.collect { 
                    // 仅在没有当前 Provider 时接受全局配置更新，避免冲突
                    if (currentProvider == null) {
                        apiKey = it 
                        if (it.isNotBlank() && baseUrl.isNotBlank()) fetchModels()
                    }
                }
            }
            launch {
                settingsRepository.baseUrl.collect { 
                    if (currentProvider == null) {
                        baseUrl = it
                        if (it.isNotBlank() && apiKey.isNotBlank()) fetchModels()
                    }
                }
            }
            launch {
                settingsRepository.model.collect { model = it }
            }
            launch {
                settingsRepository.defaultModel.collect { defaultModel = it }
            }
            launch {
                settingsRepository.themeMode.collect { themeMode = it }
            }
            launch {
                settingsRepository.wallpaperUri.collect { wallpaperUri = it }
            }
            launch {
                settingsRepository.glassOpacity.collect { glassOpacity = it }
            }
            launch {
                settingsRepository.glassBlur.collect { glassBlur = it }
            }
            launch {
                settingsRepository.glassColor.collect { glassColor = it }
            }
            launch {
                settingsRepository.agreementAccepted.collect { agreementAccepted = it }
            }
            // 加载工具箱配置项
            launch {
                settingsRepository.thinkingBudget.collect { thinkingBudget = it }
            }
            launch {
                settingsRepository.webSearchEnabled.collect { webSearchEnabled = it }
            }
            launch {
                settingsRepository.searchProvider.collect { searchProvider = it }
            }
            launch {
                settingsRepository.streamEnabled.collect { streamEnabled = it }
            }
            launch {
                settingsRepository.contextSize.collect { contextSize = it }
            }
            // 加载灵动岛配置项
            launch {
                settingsRepository.dynamicIslandEnabled.collect { dynamicIslandEnabled = it }
            }
            launch {
                settingsRepository.loginNotificationMode.collect { loginNotificationMode = it }
            }
            launch {
                settingsRepository.dynamicIslandDuration.collect { dynamicIslandDuration = it }
            }
            launch {
                settingsRepository.showTokenCount.collect { showTokenCount = it }
            }
            launch {
                settingsRepository.showElapsedTime.collect { showElapsedTime = it }
            }
            // 触感反馈
            launch {
                settingsRepository.hapticFeedbackEnabled.collect { hapticFeedbackEnabled = it }
            }
            // 加载字体样式配置
            launch {
                settingsRepository.textColorMode.collect { textColorMode = it }
            }
            launch {
                settingsRepository.textShadowEnabled.collect { textShadowEnabled = it }
            }
        }
    }
    
    fun updateHapticFeedbackEnabled(enabled: Boolean) {
        hapticFeedbackEnabled = enabled
        viewModelScope.launch {
            settingsRepository.setHapticFeedbackEnabled(enabled)
        }
    }
    
    fun updateTextColorMode(mode: String) {
        viewModelScope.launch {
            settingsRepository.setTextColorMode(mode)
        }
    }
    
    fun updateTextShadowEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setTextShadowEnabled(enabled)
        }
    }
    
    fun updateGlassColor(color: String) {
        viewModelScope.launch {
            settingsRepository.setGlassColor(color)
        }
    }
    
    fun updateDefaultModel(value: String) {
        defaultModel = value
        viewModelScope.launch {
            settingsRepository.setDefaultModel(value)
        }
    }
    
    fun updateGlassBlur(blur: Float) {
        glassBlur = blur
        viewModelScope.launch {
            settingsRepository.setGlassBlur(blur)
        }
    }
    
    fun exportData(onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                val json = dataRepository.exportData()
                onResult(json)
            } catch (e: Exception) {
                onResult(null)
            }
        }
    }
    
    fun importData(uri: android.net.Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = dataRepository.importData(uri)
            if (success) {
                // 重新加载数据
                currentConversationId?.let { switchConversation(it) }
            }
            onResult(success)
        }
    }
    
    fun updateGlassOpacity(opacity: Float) {
        glassOpacity = opacity
        viewModelScope.launch {
            settingsRepository.setGlassOpacity(opacity)
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
                Log.d(TAG, "Fetching models from: $baseUrl/models")

                val modelIds = chatApiClient.fetchModels(baseUrl, apiKey)

                Log.d(TAG, "Fetched ${modelIds.size} models from $baseUrl")
                availableModels.clear()
                availableModels.addAll(modelIds)

                // 校验当前选中的模型是否有效
                if (model.isNotBlank() && !modelIds.contains(model)) {
                    Log.w(TAG, "Current model $model not available in $baseUrl")
                    // 注意：不要在这里清空 saveModel("")，用户可能只是暂时连不上，或者 Provider 没更新
                    // 我们保持现状，但在 UI 上可以给个提示
                }
            } catch (e: ChatApiException) {
                Log.e(TAG, "Failed to fetch models from $baseUrl", e)
                if (availableModels.isEmpty()) {
                    val errorMessage = e.message ?: "获取模型列表失败"
                    showErrorMessage(
                        if (errorMessage.startsWith("HTTP ")) {
                            "获取模型列表失败: $errorMessage"
                        } else {
                            errorMessage
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network error fetching models from $baseUrl", e)
                if (availableModels.isEmpty()) {
                    showErrorMessage("网络错误，无法获取模型列表")
                }
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
            createNewConversation(showNotification = false)
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
                    dbMessages.none { dbMsg -> dbMsg.id == uiMsg.id } && !deletedMessageIds.contains(uiMsg.id)
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
    
    fun createNewConversation(systemPrompt: String? = null, title: String = "新对话", showNotification: Boolean = false) {
        // 同步更新 ID 和 UI 状态，防止 sendMessage 竞争
        val newId = UUID.randomUUID().toString()
        currentConversationId = newId
        currentConversationTitle = title
        messages.clear()
        
        // 应用默认模型 (如果有)
        if (defaultModel.isNotBlank()) {
            model = defaultModel
        }
        
        // 标记为临时会话
        transientConversationIds.add(newId)
        pendingSystemPrompt = systemPrompt
        
        // 开启新消息采集 (此时 DB 为空，所以 UI 也是空的)
        startMessagesCollection(newId)
        
        viewModelScope.launch {
            // 注意：我们不再立即插入 ConversationEntity，也不插入 systemPrompt
            // 而是等到用户发送第一条消息时才真正创建
            
            // 但是我们要保存这个 ID 到设置，以便下次打开尝试恢复(虽然没存DB会失败，但逻辑一致)
            settingsRepository.setCurrentConversationId(newId)
            // 显示新对话创建成功通知
            if (showNotification) {
                com.liquidglass.fluxhub.chat.ui.components.DynamicIslandController.showSuccess(
                    message = "新对话已创建",
                    avatar = "✨"
                )
            }
        }
    }
    
    fun switchConversation(conversationId: String) {
        if (conversationId == currentConversationId) return
        
        val previousConversationId = currentConversationId
        
        // 同步设置以提升响应速度
        currentConversationId = conversationId
        messages.clear()
        
        // 取消当前流式输出
        cancelGeneration()
        
        viewModelScope.launch {
            // 清理之前的会话
            if (previousConversationId != null) {
                if (transientConversationIds.contains(previousConversationId)) {
                    // 如果是临时会话且未转正，直接丢弃
                    Log.d(TAG, "Discarding transient conversation: $previousConversationId")
                    transientConversationIds.remove(previousConversationId)
                } else {
                    // 检查是否为空会话（已存在数据库但无消息）
                    val previousMessages = messageDao.getMessageCountForConversation(previousConversationId)
                    if (previousMessages == 0) {
                        Log.d(TAG, "Cleaning up empty conversation: $previousConversationId")
                        conversationDao.deleteConversation(previousConversationId)
                    }
                }
            }
            
            val conversation = conversationDao.getConversation(conversationId)
            if (conversation != null) {
                currentConversationTitle = conversation.title
                settingsRepository.setCurrentConversationId(conversationId)
                startMessagesCollection(conversationId)
            }
        }
    }
    
    fun deleteConversation(conversationId: String) {
        // 如果是当前会话，立即清空消息列表
        // 1. 提升响应速度
        // 2. 防止 sync 逻辑将已删除的消息误判为"尚未保存的UI消息"而保留
        if (conversationId == currentConversationId) {
            messages.clear()
        }

        viewModelScope.launch {
            // 删除消息
            messageDao.deleteMessagesForConversation(conversationId)
            // 删除会话
            conversationDao.deleteConversation(conversationId)
            
            // 移除手动 createNewConversation，交由 startConversationsCollection 监听处理
            // 显示删除成功通知
            com.liquidglass.fluxhub.chat.ui.components.DynamicIslandController.showSuccess(
                message = "对话已删除",
                avatar = "🗑️"
            )
        }
    }

    fun renameConversation(conversationId: String, newTitle: String) {
        viewModelScope.launch {
            conversationDao.updateConversationTitle(conversationId, newTitle)
            // 如果是当前会话，更新 UI 中的标题
            if (conversationId == currentConversationId) {
                currentConversationTitle = newTitle
            }
            // 显示重命名成功通知
            com.liquidglass.fluxhub.chat.ui.components.DynamicIslandController.showSuccess(
                message = "对话已重命名",
                avatar = "✏️"
            )
        }
    }

    fun deleteMessage(messageId: String) {
        // 记录已删除 ID，防止 sync 逻辑复活
        deletedMessageIds.add(messageId)
        
        // 立即从 UI 移除
        messages.removeAll { it.id == messageId }
        
        viewModelScope.launch {
            messageDao.deleteMessage(messageId)
        }
    }
    
    /**
     * 删除指定消息及其后续所有消息（用于编辑重发）
     */
    fun deleteMessageAndFollowing(messageId: String) {
        val plan = ChatMessageBranchPlanner.deleteMessageAndFollowing(messages, messageId)
        if (plan == null) {
            Log.w(TAG, "deleteMessageAndFollowing: message not found: $messageId")
            return
        }
        
        // 记录 IDs
        deletedMessageIds.addAll(plan.idsToDelete)
        
        Log.d(TAG, "deleteMessageAndFollowing: deleting ${plan.idsToDelete.size} messages starting from index ${plan.startIndex}")
        
        // 立即更新 UI 列表（移除从该索引开始的所有消息）
        val messagesToKeep = messages.take(plan.startIndex)
        messages.clear()
        messages.addAll(messagesToKeep)
        
        // 异步删除数据库记录
        viewModelScope.launch {
            plan.idsToDelete.forEach { id ->
                messageDao.deleteMessage(id)
            }
            Log.d(TAG, "deleteMessageAndFollowing: database deletion complete")
        }
    }
    
    /**
     * 处理消息编辑
     * 
     * @param newContent 编辑后的新内容
     * 
     * 行为：
     * 1. 找到正在编辑的消息
     * 2. 删除该消息之后的所有消息
     * 3. 更新该消息的内容
     * 4. 重新生成 AI 回复
     */
    fun handleMessageEdit(newContent: String) {
        val messageId = editingMessageId ?: return
        val conversationId = currentConversationId ?: return
        
        if (newContent.isBlank()) {
            cancelEditing()
            return
        }
        
        val plan = ChatMessageBranchPlanner.editMessage(messages, messageId)
        if (plan == null) {
            cancelEditing()
            return
        }
        
        Log.d(TAG, "handleMessageEdit: editing message at index ${plan.messageIndex}")
        
        // 取消当前生成任务
        cancelGeneration()
        
        // 1. 删除该消息之后的所有消息
        deletedMessageIds.addAll(plan.idsToDelete)
        
        // 2. 更新 UI 中的消息内容
        val originalMessage = messages[plan.messageIndex]
        messages[plan.messageIndex] = originalMessage.copy(content = newContent)
        
        // 3. 立即移除后续消息
        messages.removeAll { plan.idsToDelete.contains(it.id) }
        
        // 4. 清除编辑状态
        editingMessageId = null
        inputText = ""
        
        // 5. 异步更新数据库
        viewModelScope.launch {
            // 删除后续消息
            plan.idsToDelete.forEach { id ->
                messageDao.deleteMessage(id)
            }
            
            // 更新编辑的消息内容
            val existingMessage = messageDao.getMessage(messageId)
            if (existingMessage != null) {
                messageDao.updateMessage(existingMessage.copy(content = newContent))
            }
            
            // 更新会话时间
            conversationDao.updateConversationTimestamp(conversationId, System.currentTimeMillis())
        }
        
        // 6. 重新生成 AI 回复
        initiateAiResponse(conversationId)
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
        if (ChatMessageBranchPlanner.regenerateFrom(messages, messageId) == null) return
        val conversationId = currentConversationId ?: return
        
        // 取消当前生成任务
        cancelGeneration()
        
        generationJob = viewModelScope.launch {
            try {
                val plan = ChatMessageBranchPlanner.regenerateFrom(messages, messageId) ?: return@launch
                deletedMessageIds.addAll(plan.idsToDelete)

                plan.idsToDelete.forEach { id ->
                    messageDao.deleteMessage(id)
                }
                messages.removeAll { plan.idsToDelete.contains(it.id) }
                initiateAiResponse(conversationId)
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
        requestTimeoutJob?.cancel()
        requestTimeoutJob = null
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
                ChatConversationSelector.filterForAssistant(assistant, allConversations)
            }.collect { filteredList ->
                conversations.clear()
                conversations.addAll(filteredList)
                
                when (val selection = ChatConversationSelector.nextSelection(
                    currentConversationId = currentConversationId,
                    conversations = conversations,
                    transientConversationIds = transientConversationIds
                )) {
                    ConversationSelection.KeepCurrent -> {
                        if (currentConversationId != null && transientConversationIds.contains(currentConversationId)) {
                            Log.d(TAG, "Current conversation $currentConversationId is transient, staying put.")
                        }
                    }
                    is ConversationSelection.SwitchTo -> switchConversation(selection.conversationId)
                    ConversationSelection.CreateNew -> createNewConversation()
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
                        currentAssistant = ChatAssistantSelector.defaultAssistant(list)
                        currentAssistant?.let { applyAssistantSettings(it) }
                    }
                }
            }
        }
    }
    
    fun switchAssistant(assistant: AssistantEntity) {
        currentAssistant = assistant
        applyAssistantSettings(assistant)
        // 显示切换成功通知
        com.liquidglass.fluxhub.chat.ui.components.DynamicIslandController.showSuccess(
            message = "已切换助手",
            avatar = assistant.avatar ?: "🤖"
        )
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
            // 显示创建成功通知
            com.liquidglass.fluxhub.chat.ui.components.DynamicIslandController.showSuccess(
                message = "助手已创建",
                avatar = avatar ?: "🤖"
            )
        }
    }
    
    fun updateAssistant(assistant: AssistantEntity) {
        viewModelScope.launch {
            assistantDao.updateAssistant(assistant)
            if (currentAssistant?.id == assistant.id) {
                currentAssistant = assistant
                applyAssistantSettings(assistant)
            }
            // 显示更新成功通知
            com.liquidglass.fluxhub.chat.ui.components.DynamicIslandController.showSuccess(
                message = "助手已更新",
                avatar = assistant.avatar
            )
        }
    }
    
    fun deleteAssistant(assistantId: String) {
        viewModelScope.launch {
            assistantDao.deleteAssistant(assistantId)
            if (currentAssistant?.id == assistantId) {
                currentAssistant = ChatAssistantSelector.fallbackAfterDelete(assistantId, assistants)
                currentAssistant?.let { applyAssistantSettings(it) }
            }
            // 显示删除成功通知
            com.liquidglass.fluxhub.chat.ui.components.DynamicIslandController.showSuccess(
                message = "助手已删除",
                avatar = "🗑️"
            )
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
                ChatProviderSelector.updatedConfigurationProvider(currentProvider, list)?.let { updated ->
                    Log.d(TAG, "Active provider config updated from DB")
                    currentProvider = updated
                    applyProviderSettings(updated)
                    fetchModels()
                }

                // 如果没有当前 Provider，尝试加载激活的 Provider
                if (currentProvider == null && list.isNotEmpty()) {
                    currentProvider = ChatProviderSelector.defaultProvider(list)
                    currentProvider?.let { applyProviderSettings(it) }
                    fetchModels() // 初始加载完成后抓取模型
                }
            }
        }
    }
    
    fun switchProvider(provider: ProviderEntity) {
        viewModelScope.launch {
            providerDao.deactivateAllProviders()
            providerDao.activateProvider(provider.id)
            val activatedProvider = provider.copy(isActive = true)
            currentProvider = activatedProvider
            applyProviderSettings(activatedProvider)
            fetchModels() // 切换 Provider 后重新获取模型列表
            // 显示切换成功通知
            com.liquidglass.fluxhub.chat.ui.components.DynamicIslandController.showSuccess(
                message = "已切换服务商",
                avatar = "🔄"
            )
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
            // 显示创建成功通知
            com.liquidglass.fluxhub.chat.ui.components.DynamicIslandController.showSuccess(
                message = "服务商已创建",
                avatar = "➕"
            )
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
            // 显示更新成功通知
            com.liquidglass.fluxhub.chat.ui.components.DynamicIslandController.showSuccess(
                message = "服务商已更新",
                avatar = "🔧"
            )
        }
    }
    
    fun deleteProvider(providerId: String) {
        viewModelScope.launch {
            providerDao.deleteProvider(providerId)
            if (currentProvider?.id == providerId) {
                currentProvider = ChatProviderSelector.fallbackAfterDelete(providerId, providers)
                currentProvider?.let { 
                    applyProviderSettings(it)
                    fetchModels()
                }
            }
            // 显示删除成功通知
            com.liquidglass.fluxhub.chat.ui.components.DynamicIslandController.showSuccess(
                message = "服务商已删除",
                avatar = "🗑️"
            )
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

    
    // ========== 工具箱配置项更新方法 ==========

    
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

    private fun shouldSendOpenAiOnlyOptions(effectiveBaseUrl: String): Boolean {
        return ChatRequestBuilder.shouldSendOpenAiOnlyOptions(effectiveBaseUrl)
    }

    private fun reasoningEffortOrNull(effectiveBaseUrl: String): String? {
        return ChatRequestBuilder.reasoningEffortOrNull(effectiveBaseUrl, thinkingBudget)
    }

    private fun beginRequestTimeout() {
        requestTimeoutJob?.cancel()
        requestTimeoutJob = viewModelScope.launch {
            delay(30000)
            if (isLoading) {
                Log.w(TAG, "Request timeout, cancelling active generation")
                cancelGeneration()
                showErrorMessage("请求超时，请重试")
            }
        }
    }

    private fun finishGeneration() {
        requestTimeoutJob?.cancel()
        requestTimeoutJob = null
        isLoading = false
    }
    
    // ========== 灵动岛配置项更新方法 ==========
    
    fun updateDynamicIslandEnabled(value: Boolean) {
        dynamicIslandEnabled = value
        viewModelScope.launch {
            settingsRepository.setDynamicIslandEnabled(value)
        }
    }
    
    fun updateLoginNotificationMode(value: String) {
        loginNotificationMode = value
        viewModelScope.launch {
            settingsRepository.setLoginNotificationMode(value)
        }
    }
    
    fun updateDynamicIslandDuration(value: Int) {
        dynamicIslandDuration = value
        viewModelScope.launch {
            settingsRepository.setDynamicIslandDuration(value)
        }
    }
    
    fun updateShowTokenCount(value: Boolean) {
        showTokenCount = value
        viewModelScope.launch {
            settingsRepository.setShowTokenCount(value)
        }
    }
    
    fun updateShowElapsedTime(value: Boolean) {
        showElapsedTime = value
        viewModelScope.launch {
            settingsRepository.setShowElapsedTime(value)
        }
    }
    
    fun sendMessage(content: String) {
        val preparedInput = ChatInputPreparer.prepare(
            content = content,
            imageUri = selectedImageUri?.toString()
        ) ?: return

        if (preparedInput.hasImage) {
            selectedImageUri = null // 消费图片
        }
        
        // 优先从当前 Provider 获取配置，确保使用最新值
        val effectiveApiKey = currentProvider?.apiKey ?: apiKey
        
        if (effectiveApiKey.isBlank()) {
            showErrorMessage("请先配置服务商")
            return
        }
        
        if (model.isBlank()) {
            showErrorMessage("请先选择模型")
            return
        }
        
        val conversationId = currentConversationId ?: return
        
        Log.d(TAG, "sendMessage: user message queued, hasImage=${preparedInput.hasImage}, length=${preparedInput.plainText.length}")
        
        // 添加用户消息
        val userMessageId = UUID.randomUUID().toString()
        val userMessage = UiMessage(id = userMessageId, role = "user", content = preparedInput.finalContent)
        messages.add(userMessage)
        
        // 保存到数据库
        viewModelScope.launch {
            messageDao.insertMessage(MessageEntity(
                id = userMessageId,
                conversationId = conversationId,
                role = "user",
                content = preparedInput.finalContent
            ))
            
            // 检查是否是临时会话，如果是，现在立即"转正"
            if (transientConversationIds.contains(conversationId)) {
                Log.d(TAG, "Persisting transient conversation: $conversationId")
                transientConversationIds.remove(conversationId)
                
                val conversation = ConversationEntity(
                    id = conversationId,
                    title = preparedInput.title, // 用第一句话做标题
                    assistantId = currentAssistant?.id,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                conversationDao.insertConversation(conversation)
                currentConversationTitle = conversation.title
                
                // 如果有暂存的 System Prompt，也写入
                pendingSystemPrompt?.let { prompt ->
                    if (prompt.isNotBlank()) {
                         messageDao.insertMessage(MessageEntity(
                            id = UUID.randomUUID().toString(),
                            conversationId = conversationId,
                            role = "system",
                            content = prompt
                        ))
                    }
                    pendingSystemPrompt = null
                }
            } else {
                // 原有逻辑：更新会话时间和标题
                if (messages.size <= 1) { // 这里的 messages.size 可能不准确，因为 Collection 是异步的
                    val newTitle = preparedInput.title
                    conversationDao.updateConversationTitle(conversationId, newTitle)
                    currentConversationTitle = newTitle
                }
                // 更新时间
                conversationDao.updateConversationTimestamp(conversationId, System.currentTimeMillis())
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
                finishGeneration()
                return@launch
            }

            if (isLoading) {
                Log.w(TAG, "isLoading was true, cancelling previous request before starting a new one")
                cancelGeneration()
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
            beginRequestTimeout()
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
                val effectiveBaseUrl = currentProvider?.baseUrl ?: baseUrl
                val effectiveApiKey = currentProvider?.apiKey ?: apiKey
                val reasoningEffort = reasoningEffortOrNull(effectiveBaseUrl)
                
                val requestJson = ChatRequestBuilder.buildRequestJson(
                    model = model,
                    messages = requestMessages,
                    stream = false,
                    temperature = temperature,
                    topP = topP,
                    maxTokens = maxTokens,
                    reasoningEffort = reasoningEffort,
                    includeStreamOptions = false
                )
                
                val requestBody = json.encodeToString(JsonObject.serializer(), requestJson)
                Log.d(TAG, "Non-streaming request prepared, messages=${requestMessages.size}, bodyLength=${requestBody.length}")

                val parsed = chatApiClient.executeNonStreaming(
                    baseUrl = effectiveBaseUrl,
                    apiKey = effectiveApiKey,
                    requestBody = requestBody
                )
                val contentStr = parsed.content
                val reasoningStr = parsed.reasoningContent.orEmpty()

                Log.d(TAG, "Parsed result - content length: ${contentStr.length}, reasoning length: ${reasoningStr.length}")

                finishGeneration()
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
            } catch (e: Exception) {
                Log.e(TAG, "Non-streaming request failed", e)
                finishGeneration()
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
        return ChatRequestBuilder.buildMessages(
            history = history,
            aiMessageId = aiMessageId,
            contextSize = contextSize,
            systemPrompt = currentAssistant?.systemPrompt,
            imageBase64Loader = { uriString ->
                try {
                    FileUtils.uriToBase64(getApplication(), Uri.parse(uriString))
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load image from URI: $uriString", e)
                    null
                }
            }
        )
    }

    private fun callStreamingApiWithEventSource(aiMessageId: String, conversationId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "callStreamingApiWithEventSource: using baseUrl=$baseUrl, model=$model, contextSize=$contextSize")
                
                val messagesWithSystem = buildApiMessages(messages, aiMessageId)
                val effectiveBaseUrl = currentProvider?.baseUrl ?: baseUrl
                val effectiveApiKey = currentProvider?.apiKey ?: apiKey
                val reasoningEffort = reasoningEffortOrNull(effectiveBaseUrl)
                
                val requestJson = ChatRequestBuilder.buildRequestJson(
                    model = model,
                    messages = messagesWithSystem,
                    stream = true,
                    temperature = temperature,
                    topP = topP,
                    maxTokens = maxTokens,
                    reasoningEffort = reasoningEffort,
                    includeStreamOptions = shouldSendOpenAiOnlyOptions(effectiveBaseUrl)
                )
                
                val requestBody = json.encodeToString(JsonObject.serializer(), requestJson)
                Log.d(TAG, "Streaming request prepared, messages=${messagesWithSystem.size}, bodyLength=${requestBody.length}")
                
                var fullContent = ""
                var fullThinkingContent = ""
                var hasFinished = false
                
                // 性能优化：UI 更新节流（每 50ms 最多更新一次）
                var lastUiUpdateTime = 0L
                val uiUpdateThrottleMs = 50L
                var pendingUiUpdate = false

                fun completeStreamingResponse(cancelEventSource: Boolean = false) {
                    if (hasFinished) return
                    hasFinished = true

                    if (cancelEventSource) {
                        currentEventSource?.cancel()
                    }
                    currentEventSource = null

                    viewModelScope.launch {
                        finishGeneration()
                        
                        val index = messages.indexOfFirst { it.id == aiMessageId }
                        if (index >= 0) {
                            // 确保最终内容完整更新（节流可能跳过最后几个 chunk）
                            messages[index] = messages[index].copy(
                                content = fullContent,
                                thinkingContent = fullThinkingContent.takeIf { it.isNotEmpty() },
                                isStreaming = false
                            )
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
                }

                currentEventSource = chatApiClient.startStreaming(
                    baseUrl = effectiveBaseUrl,
                    apiKey = effectiveApiKey,
                    requestBody = requestBody,
                    callback = object : ChatStreamCallback {
                        override fun onOpen(responseCode: Int) {
                            Log.d(TAG, "SSE onOpen: $responseCode")
                            streamingTokenCount = 0 // 重置 token 计数
                        }

                        override fun onDelta(delta: ChatStreamDelta) {
                            if (hasFinished) return

                            delta.errorMessage?.let { errorMsg ->
                                Log.e(TAG, "Streaming API error: $errorMsg")
                                return
                            }

                            if (delta.reasoningContent.isNotEmpty()) {
                                fullThinkingContent += delta.reasoningContent
                                pendingUiUpdate = true
                            }

                            if (delta.content.isNotEmpty()) {
                                fullContent += delta.content

                                // 增加 token 计数（简化估计：每个 chunk 约等于其长度/4 个 token）
                                streamingTokenCount += (delta.content.length / 4).coerceAtLeast(1)

                                pendingUiUpdate = true
                            }

                            // 节流 UI 更新：最多每 50ms 更新一次
                            val now = System.currentTimeMillis()
                            if (pendingUiUpdate && (now - lastUiUpdateTime >= uiUpdateThrottleMs)) {
                                pendingUiUpdate = false
                                lastUiUpdateTime = now
                                viewModelScope.launch {
                                    val index = messages.indexOfFirst { it.id == aiMessageId }
                                    if (index >= 0) {
                                        messages[index] = messages[index].copy(
                                            content = fullContent,
                                            thinkingContent = fullThinkingContent.takeIf { it.isNotEmpty() }
                                        )
                                    }
                                }
                            }
                        }

                        override fun onDone() {
                            Log.d(TAG, "SSE stream completed with [DONE], content length: ${fullContent.length}")
                            completeStreamingResponse(cancelEventSource = true)
                        }

                        override fun onClosed() {
                            Log.d(TAG, "SSE onClosed, final content length: ${fullContent.length}")
                            completeStreamingResponse()
                        }

                        override fun onFailure(message: String, throwable: Throwable?, responseCode: Int?) {
                            currentEventSource = null
                            // 特殊处理 stream was reset 错误：如果已经有内容生成，则视为成功
                            if (message.contains("stream was reset", ignoreCase = true) && fullContent.isNotEmpty()) {
                                Log.w(TAG, "Stream reset ignored, treating as success (content length: ${fullContent.length})")
                                completeStreamingResponse()
                                return
                            }

                            if (hasFinished) return
                            hasFinished = true

                            Log.e(TAG, "SSE onFailure: $message, response: $responseCode", throwable)

                            viewModelScope.launch {
                                finishGeneration()

                                val errorDetail = "请求失败: $message"
                                showErrorMessage(errorDetail)

                                // 仅当消息为空时才显示错误占位符，否则保留已有内容
                                val index = messages.indexOfFirst { it.id == aiMessageId }
                                if (index >= 0) {
                                    val currentMsg = messages[index]
                                    if (currentMsg.content.isBlank()) {
                                        messages[index] = currentMsg.copy(
                                            content = "⚠️ 加载失败", // 简单提示，详细看弹窗
                                            isStreaming = false
                                        )
                                    } else {
                                        messages[index] = currentMsg.copy(isStreaming = false)
                                    }
                                }
                            }
                        }

                        override fun onParseError(error: Exception) {
                            Log.w(TAG, "Failed to parse SSE chunk", error)
                        }
                    }
                )
                Log.d(TAG, "SSE EventSource created successfully for $aiMessageId")
            } catch (e: Exception) {
                Log.e(TAG, "Streaming request setup failed", e)
                finishGeneration()
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

    // 保存图片到相册
    fun saveImageToGallery(imageUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val filename = "Fluxhub_${System.currentTimeMillis()}.jpg"
                
                // 1. 下载图片数据
                val request = Request.Builder().url(imageUrl).build()
                val response = client.newCall(request).execute()
                val bytes = response.body?.bytes() ?: throw Exception("Download failed")
                
                // 2. 写入 MediaStore
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        put(android.provider.MediaStore.Images.Media.IS_PENDING, 1)
                        put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Fluxhub")
                    }
                }
                
                val resolver = context.contentResolver
                val uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                
                uri?.let {
                    resolver.openOutputStream(it)?.use { stream ->
                        stream.write(bytes)
                    }
                    
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(android.provider.MediaStore.Images.Media.IS_PENDING, 0)
                        resolver.update(uri, contentValues, null, null)
                    }
                    
                    withContext(Dispatchers.Main) {
                        // 使用灵动岛通知替换 Toast
                        com.liquidglass.fluxhub.chat.ui.components.DynamicIslandController.showSuccess(
                            message = "图片已保存至相册",
                            avatar = "🖼️"
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    com.liquidglass.fluxhub.chat.ui.components.DynamicIslandController.showError(
                        message = "保存失败"
                    )
                }
            }
        }
    }
}
