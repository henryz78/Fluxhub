package com.liquidglass.fluxhub.data.repository

import com.liquidglass.fluxhub.data.local.AppDatabase
import com.liquidglass.fluxhub.data.model.Conversation
import com.liquidglass.fluxhub.data.model.Message
import com.liquidglass.fluxhub.data.model.Provider
import com.liquidglass.fluxhub.network.ChatApiClient
import com.liquidglass.fluxhub.network.ChatMessage
import kotlinx.coroutines.flow.Flow

class ChatRepository(private val database: AppDatabase) {
    
    private val messageDao = database.messageDao()
    private val conversationDao = database.conversationDao()
    private val providerDao = database.providerDao()
    
    // Messages
    fun getMessages(conversationId: Long): Flow<List<Message>> = 
        messageDao.getMessagesByConversation(conversationId)
    
    suspend fun insertMessage(message: Message): Long = 
        messageDao.insert(message)
    
    suspend fun updateMessage(message: Message) = 
        messageDao.update(message)
    
    suspend fun deleteMessage(message: Message) = 
        messageDao.delete(message)
    
    // Conversations
    fun getAllConversations(): Flow<List<Conversation>> = 
        conversationDao.getAllConversations()
    
    suspend fun getConversation(id: Long): Conversation? = 
        conversationDao.getById(id)
    
    suspend fun insertConversation(conversation: Conversation): Long = 
        conversationDao.insert(conversation)
    
    suspend fun updateConversation(conversation: Conversation) = 
        conversationDao.update(conversation)
    
    suspend fun deleteConversation(conversation: Conversation) {
        messageDao.deleteByConversation(conversation.id)
        conversationDao.delete(conversation)
    }
    
    // Providers
    fun getEnabledProviders(): Flow<List<Provider>> = 
        providerDao.getEnabledProviders()
    
    fun getAllProviders(): Flow<List<Provider>> = 
        providerDao.getAllProviders()
    
    suspend fun getProvider(id: Long): Provider? = 
        providerDao.getById(id)
    
    suspend fun insertProvider(provider: Provider): Long = 
        providerDao.insert(provider)
    
    suspend fun updateProvider(provider: Provider) = 
        providerDao.update(provider)
    
    suspend fun deleteProvider(provider: Provider) = 
        providerDao.delete(provider)
    
    // Chat API
    fun createApiClient(provider: Provider): ChatApiClient {
        return ChatApiClient(
            baseUrl = provider.baseUrl,
            apiKey = provider.apiKey
        )
    }
    
    fun streamChat(
        client: ChatApiClient,
        model: String,
        messages: List<Message>
    ): Flow<String> {
        val chatMessages = messages.map { 
            ChatMessage(role = it.role, content = it.content) 
        }
        return client.streamChat(model, chatMessages)
    }
}
