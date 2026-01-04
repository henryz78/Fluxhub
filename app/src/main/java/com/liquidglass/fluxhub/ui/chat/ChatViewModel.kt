package com.liquidglass.fluxhub.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.liquidglass.fluxhub.data.local.AppDatabase
import com.liquidglass.fluxhub.data.model.Conversation
import com.liquidglass.fluxhub.data.model.Message
import com.liquidglass.fluxhub.data.model.Provider
import com.liquidglass.fluxhub.data.repository.ChatRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentConversation: Conversation? = null,
    val currentProvider: Provider? = null,
    val streamingContent: String = ""
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = AppDatabase.getDatabase(application)
    private val repository = ChatRepository(database)
    
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    
    private var currentConversationId: Long = 0
    
    val conversations = repository.getAllConversations()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    val providers = repository.getAllProviders()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    init {
        // 初始化默认提供商
        viewModelScope.launch {
            providers.first { it.isNotEmpty() }.let { list ->
                if (list.isEmpty()) {
                    // 添加默认 OpenAI 提供商
                    repository.insertProvider(
                        Provider(
                            name = "OpenAI",
                            baseUrl = "https://api.openai.com/v1",
                            apiKey = "",
                            defaultModel = "gpt-4o-mini"
                        )
                    )
                }
            }
        }
    }
    
    fun loadConversation(conversationId: Long) {
        currentConversationId = conversationId
        viewModelScope.launch {
            val conversation = repository.getConversation(conversationId)
            _uiState.update { it.copy(currentConversation = conversation) }
            
            repository.getMessages(conversationId).collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
    }
    
    fun createNewConversation(providerId: Long, modelName: String) {
        viewModelScope.launch {
            val provider = repository.getProvider(providerId)
            val conversation = Conversation(
                providerId = providerId,
                modelName = modelName.ifEmpty { provider?.defaultModel ?: "" }
            )
            val id = repository.insertConversation(conversation)
            _uiState.update { it.copy(currentProvider = provider) }
            loadConversation(id)
        }
    }
    
    fun setProvider(provider: Provider) {
        _uiState.update { it.copy(currentProvider = provider) }
    }
    
    fun sendMessage(content: String) {
        if (content.isBlank()) return
        
        viewModelScope.launch {
            val conversationId = currentConversationId
            val provider = _uiState.value.currentProvider
            
            if (provider == null || provider.apiKey.isBlank()) {
                _uiState.update { it.copy(error = "请先配置 API Key") }
                return@launch
            }
            
            // 如果没有对话，先创建一个
            if (conversationId == 0L) {
                createNewConversation(provider.id, provider.defaultModel)
                return@launch
            }
            
            // 添加用户消息
            val userMessage = Message(
                conversationId = conversationId,
                role = "user",
                content = content
            )
            repository.insertMessage(userMessage)
            
            // 创建助手消息占位
            val assistantMessage = Message(
                conversationId = conversationId,
                role = "assistant",
                content = "",
                isStreaming = true
            )
            val assistantId = repository.insertMessage(assistantMessage)
            
            _uiState.update { it.copy(isLoading = true, error = null, streamingContent = "") }
            
            try {
                val client = repository.createApiClient(provider)
                val messages = _uiState.value.messages
                val model = _uiState.value.currentConversation?.modelName ?: provider.defaultModel
                
                var fullContent = ""
                
                repository.streamChat(client, model, messages)
                    .catch { e ->
                        _uiState.update { it.copy(error = e.message ?: "Unknown error") }
                    }
                    .collect { chunk ->
                        fullContent += chunk
                        _uiState.update { it.copy(streamingContent = fullContent) }
                    }
                
                // 更新助手消息
                repository.updateMessage(
                    assistantMessage.copy(
                        id = assistantId,
                        content = fullContent,
                        isStreaming = false
                    )
                )
                
                // 更新对话标题（如果是第一条消息）
                if (_uiState.value.messages.size <= 2) {
                    val title = content.take(20) + if (content.length > 20) "..." else ""
                    repository.updateConversation(
                        _uiState.value.currentConversation?.copy(title = title) 
                            ?: return@launch
                    )
                }
                
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Unknown error") }
            } finally {
                _uiState.update { it.copy(isLoading = false, streamingContent = "") }
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun updateProvider(provider: Provider) {
        viewModelScope.launch {
            repository.updateProvider(provider)
            if (_uiState.value.currentProvider?.id == provider.id) {
                _uiState.update { it.copy(currentProvider = provider) }
            }
        }
    }
}
