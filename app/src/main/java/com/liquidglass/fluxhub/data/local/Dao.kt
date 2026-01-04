package com.liquidglass.fluxhub.data.local

import androidx.room.*
import com.liquidglass.fluxhub.data.model.Conversation
import com.liquidglass.fluxhub.data.model.Message
import com.liquidglass.fluxhub.data.model.Provider
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesByConversation(conversationId: Long): Flow<List<Message>>
    
    @Insert
    suspend fun insert(message: Message): Long
    
    @Update
    suspend fun update(message: Message)
    
    @Delete
    suspend fun delete(message: Message)
    
    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteByConversation(conversationId: Long)
}

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun getAllConversations(): Flow<List<Conversation>>
    
    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getById(id: Long): Conversation?
    
    @Insert
    suspend fun insert(conversation: Conversation): Long
    
    @Update
    suspend fun update(conversation: Conversation)
    
    @Delete
    suspend fun delete(conversation: Conversation)
}

@Dao
interface ProviderDao {
    @Query("SELECT * FROM providers WHERE isEnabled = 1")
    fun getEnabledProviders(): Flow<List<Provider>>
    
    @Query("SELECT * FROM providers")
    fun getAllProviders(): Flow<List<Provider>>
    
    @Query("SELECT * FROM providers WHERE id = :id")
    suspend fun getById(id: Long): Provider?
    
    @Insert
    suspend fun insert(provider: Provider): Long
    
    @Update
    suspend fun update(provider: Provider)
    
    @Delete
    suspend fun delete(provider: Provider)
}
