package com.liquidglass.fluxhub.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesForConversation(conversationId: String): Flow<List<MessageEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)
    
    @Update
    suspend fun updateMessage(message: MessageEntity)
    
    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesForConversation(conversationId: String)
    
    @Query("SELECT COUNT(*) FROM messages WHERE conversationId = :conversationId")
    suspend fun getMessageCountForConversation(conversationId: String): Int
    
    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: String)
    
    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getMessage(messageId: String): MessageEntity?
}

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>
    
    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getConversation(id: String): ConversationEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity)
    
    @Update
    suspend fun updateConversation(conversation: ConversationEntity)
    
    @Query("UPDATE conversations SET title = :title WHERE id = :id")
    suspend fun updateConversationTitle(id: String, title: String)

    @Query("UPDATE conversations SET updatedAt = :timestamp WHERE id = :id")
    suspend fun updateConversationTimestamp(id: String, timestamp: Long)
    
    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversation(id: String)
}

@Dao
interface AssistantDao {
    @Query("SELECT * FROM assistants ORDER BY createdAt DESC")
    fun getAllAssistants(): Flow<List<AssistantEntity>>
    
    @Query("SELECT * FROM assistants WHERE id = :id")
    suspend fun getAssistant(id: String): AssistantEntity?
    
    @Query("SELECT * FROM assistants WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultAssistant(): AssistantEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssistant(assistant: AssistantEntity)
    
    @Update
    suspend fun updateAssistant(assistant: AssistantEntity)
    
    @Query("DELETE FROM assistants WHERE id = :id")
    suspend fun deleteAssistant(id: String)
    
    @Query("UPDATE assistants SET isDefault = 0")
    suspend fun clearDefaultAssistant()
}

@Dao
interface ProviderDao {
    @Query("SELECT * FROM providers ORDER BY createdAt DESC")
    fun getAllProviders(): Flow<List<ProviderEntity>>
    
    @Query("SELECT * FROM providers WHERE id = :id")
    suspend fun getProvider(id: String): ProviderEntity?
    
    @Query("SELECT * FROM providers WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveProvider(): ProviderEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProvider(provider: ProviderEntity)
    
    @Update
    suspend fun updateProvider(provider: ProviderEntity)
    
    @Query("DELETE FROM providers WHERE id = :id")
    suspend fun deleteProvider(id: String)
    
    @Query("UPDATE providers SET isActive = 0")
    suspend fun deactivateAllProviders()
    
    @Query("UPDATE providers SET isActive = 1 WHERE id = :id")
    suspend fun activateProvider(id: String)
}
