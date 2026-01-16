package com.liquidglass.fluxhub.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.liquidglass.fluxhub.chat.UiMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader

@Serializable
data class BackupData(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val conversations: List<ConversationEntity>,
    val messages: List<MessageEntity>,
    val assistants: List<AssistantEntity>,
    val providers: List<ProviderEntity>,
    // 包含部分关键设置
    val settings: AppSettingsBackup? = null
)

@Serializable
data class AppSettingsBackup(
    val apiKey: String? = null,
    val baseUrl: String? = null,
    val model: String? = null,
    val defaultModel: String? = null,
    val thinkingBudget: Int? = null
)

class DataRepository(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val settingsRepository = SettingsRepository(context)
    
    // 宽松的 JSON 解析器
    private val json = Json { 
        ignoreUnknownKeys = true 
        prettyPrint = true
        encodeDefaults = true
    }

    suspend fun exportData(): String = withContext(Dispatchers.IO) {
        val conversations = database.conversationDao().getAllConversationsSync()
        val messages = database.messageDao().getAllMessagesSync()
        val assistants = database.assistantDao().getAllAssistantsSync()
        val providers = database.providerDao().getAllProvidersSync()
        
        // 导出设置
        val settings = AppSettingsBackup(
            apiKey = settingsRepository.apiKey.first(),
            baseUrl = settingsRepository.baseUrl.first(),
            model = settingsRepository.model.first(),
            defaultModel = settingsRepository.defaultModel.first(),
            thinkingBudget = settingsRepository.thinkingBudget.first()
        )
        
        val backupData = BackupData(
            conversations = conversations,
            messages = messages,
            assistants = assistants,
            providers = providers,
            settings = settings
        )
        
        return@withContext json.encodeToString(backupData)
    }
    
    suspend fun importData(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val content = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).readText()
            } ?: return@withContext false
            
            val backupData = json.decodeFromString<BackupData>(content)
            
            // 恢复数据 (策略: 删除旧数据，插入备份数据? 还是合并? 
            // 简单起见，且为了避免冲突，通常建议"覆盖"或"清理后导入"。
            // 这里采用清除旧数据策略，确保一致性)
            
            database.runInTransaction {
                // 清理
                database.messageDao().deleteAllMessages()
                database.conversationDao().deleteAllConversations()
                database.assistantDao().deleteAllAssistants()
                database.providerDao().deleteAllProviders()
                
                // 插入
                database.assistantDao().insertAssistants(backupData.assistants)
                database.providerDao().insertProviders(backupData.providers)
                database.conversationDao().insertConversations(backupData.conversations)
                database.messageDao().insertMessages(backupData.messages)
            }
            
            // 恢复设置
            backupData.settings?.let { s ->
                s.apiKey?.let { settingsRepository.setApiKey(it) }
                s.baseUrl?.let { settingsRepository.setBaseUrl(it) }
                s.model?.let { settingsRepository.setModel(it) }
                s.defaultModel?.let { settingsRepository.setDefaultModel(it) }
                s.thinkingBudget?.let { settingsRepository.setThinkingBudget(it) }
            }
            
            return@withContext true
        } catch (e: Exception) {
            Log.e("DataRepository", "Import failed", e)
            return@withContext false
        }
    }
}
