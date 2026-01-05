package com.liquidglass.fluxhub.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val conversationId: String,
    val role: String,
    val content: String,
    val thinkingContent: String? = null,
    val model: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val assistantId: String? = null, // 绑定的助手
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 助手实体 - 每个助手有独立的提示词和参数配置
 */
@Entity(tableName = "assistants")
data class AssistantEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val avatar: String? = null, // emoji 或图片 URI
    val systemPrompt: String = "",
    val temperature: Float = 0.7f,
    val topP: Float = 1.0f,
    val maxTokens: Int? = null,
    val modelId: String? = null, // 指定模型（可选）
    val createdAt: Long = System.currentTimeMillis(),
    val isDefault: Boolean = false // 默认助手
)

/**
 * 服务商实体 - 支持多个 API Provider
 */
@Entity(tableName = "providers")
data class ProviderEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val baseUrl: String,
    val apiKey: String,
    val icon: String? = null, // emoji 或图片 URI
    val isActive: Boolean = false, // 当前激活的 Provider
    val createdAt: Long = System.currentTimeMillis()
)
