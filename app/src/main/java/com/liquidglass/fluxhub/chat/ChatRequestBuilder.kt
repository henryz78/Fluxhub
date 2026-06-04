package com.liquidglass.fluxhub.chat

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class ChatMessage(
    val role: String,
    val content: JsonElement? = null
)

object ChatRequestBuilder {
    private val imageMarkdownPattern = Regex("!\\[image\\]\\((.*?)\\)")

    fun buildMessages(
        history: List<UiMessage>,
        aiMessageId: String?,
        contextSize: Int,
        systemPrompt: String?,
        imageBase64Loader: (String) -> String?
    ): List<ChatMessage> {
        val baseMessages = history
            .filter {
                it.role == "user" ||
                    (it.role == "assistant" && !it.isStreaming && it.content.isNotBlank() && it.id != aiMessageId)
            }
            .takeLast(contextSize)

        val processedMessages = baseMessages.map { message ->
            ChatMessage(message.role, buildContentElement(message.content, imageBase64Loader))
        }.filter { it.content != null }

        val trimmedSystemPrompt = systemPrompt?.takeIf { it.isNotBlank() }
        return if (trimmedSystemPrompt != null) {
            listOf(ChatMessage("system", JsonPrimitive(trimmedSystemPrompt))) + processedMessages
        } else {
            processedMessages
        }
    }

    fun buildRequestJson(
        model: String,
        messages: List<ChatMessage>,
        stream: Boolean,
        temperature: Float,
        topP: Float,
        maxTokens: Int?,
        reasoningEffort: String?,
        includeStreamOptions: Boolean
    ): JsonObject {
        return buildJsonObject {
            put("model", model)
            put("messages", buildJsonArray {
                messages.forEach { message ->
                    add(buildJsonObject {
                        put("role", message.role)
                        put("content", message.content ?: JsonPrimitive(""))
                    })
                }
            })
            put("stream", stream)
            if (stream && includeStreamOptions) {
                put("stream_options", buildJsonObject {
                    put("include_usage", true)
                })
            }
            put("temperature", temperature)
            put("top_p", topP)
            maxTokens?.let { put("max_tokens", it) }
            reasoningEffort?.let { put("reasoning_effort", it) }
        }
    }

    fun shouldSendOpenAiOnlyOptions(effectiveBaseUrl: String): Boolean {
        return effectiveBaseUrl.contains("api.openai.com", ignoreCase = true)
    }

    fun reasoningEffortOrNull(effectiveBaseUrl: String, thinkingBudget: Int): String? {
        if (!shouldSendOpenAiOnlyOptions(effectiveBaseUrl) || thinkingBudget == 0) return null
        return when (thinkingBudget) {
            in 1..4096 -> "low"
            in 4097..16000 -> "medium"
            else -> "high"
        }
    }

    private fun buildContentElement(
        content: String,
        imageBase64Loader: (String) -> String?
    ): JsonElement {
        val imageMatch = imageMarkdownPattern.find(content)
        if (imageMatch != null) {
            val uriString = imageMatch.groupValues[1]
            val textContent = content.replace(imageMatch.value, "").trim()
            val base64 = imageBase64Loader(uriString)

            if (base64 != null) {
                return buildJsonArray {
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
            }
        }

        return parseJsonArrayOrText(content)
    }

    private fun parseJsonArrayOrText(content: String): JsonElement {
        val trimmed = content.trim()
        if (trimmed.startsWith("[")) {
            val parsed = runCatching {
                kotlinx.serialization.json.Json.parseToJsonElement(trimmed)
            }.getOrNull()
            if (parsed is JsonArray) {
                return parsed
            }
        }
        return JsonPrimitive(content)
    }
}
