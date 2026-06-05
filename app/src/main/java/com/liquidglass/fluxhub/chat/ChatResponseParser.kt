package com.liquidglass.fluxhub.chat

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class ChatCompletionResult(
    val content: String,
    val reasoningContent: String? = null
)

data class ChatStreamDelta(
    val content: String = "",
    val reasoningContent: String = "",
    val errorMessage: String? = null,
    val isDone: Boolean = false
)

class ChatApiException(message: String, cause: Throwable? = null) : Exception(message, cause)

object ChatResponseParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parseNonStreaming(body: String): ChatCompletionResult {
        val responseJson = json.parseToJsonElement(body).jsonObject
        responseJson["error"]?.let { throw ChatApiException(parseErrorMessage(it)) }

        val choices = responseJson["choices"]?.jsonArray
        val firstChoice = choices?.getOrNull(0)?.jsonObject
        val messageObject = firstChoice?.get("message")?.jsonObject ?: firstChoice?.get("delta")?.jsonObject

        val contentElement = messageObject?.get("content") ?: firstChoice?.get("text")
        val content = parseContent(contentElement)

        val reasoningElement = messageObject?.get("reasoning_content") ?: messageObject?.get("reasoning")
        val reasoningContent = (reasoningElement as? JsonPrimitive)?.contentOrNull.orEmpty()

        return ChatCompletionResult(
            content = content,
            reasoningContent = reasoningContent.takeIf { it.isNotEmpty() }
        )
    }

    fun parseStreamingEventData(data: String): List<ChatStreamDelta> {
        return data
            .trim()
            .lineSequence()
            .filter { it.isNotBlank() }
            .map { it.removePrefix("data: ").trim() }
            .mapNotNull { line ->
                if (line == "[DONE]") {
                    ChatStreamDelta(isDone = true)
                } else {
                    parseStreamingLine(line)
                }
            }
            .toList()
    }

    private fun parseStreamingLine(line: String): ChatStreamDelta {
        val chunkJson = json.parseToJsonElement(line).jsonObject

        chunkJson["error"]?.let {
            return ChatStreamDelta(errorMessage = parseErrorMessage(it))
        }

        val choices = chunkJson["choices"]?.jsonArray
        val firstChoice = choices?.getOrNull(0)?.jsonObject
        val deltaObject = firstChoice?.get("delta")?.jsonObject ?: firstChoice?.get("message")?.jsonObject

        val content = deltaObject?.get("content")?.jsonPrimitive?.contentOrNull.orEmpty()
        val reasoningContent = deltaObject?.get("reasoning_content")?.jsonPrimitive?.contentOrNull
            ?: deltaObject?.get("reasoning")?.jsonPrimitive?.contentOrNull
            ?: ""

        return ChatStreamDelta(
            content = content,
            reasoningContent = reasoningContent
        )
    }

    private fun parseContent(contentElement: JsonElement?): String {
        return when (contentElement) {
            is JsonPrimitive -> contentElement.contentOrNull ?: ""
            is JsonArray -> contentElement.mapNotNull {
                it.jsonObject["text"]?.jsonPrimitive?.contentOrNull
            }.joinToString("")
            else -> ""
        }
    }

    private fun parseErrorMessage(errorElement: JsonElement): String {
        val errorObject = errorElement as? JsonObject ?: return "Unknown API Error"
        return errorObject["message"]?.jsonPrimitive?.contentOrNull
            ?: errorObject["code"]?.jsonPrimitive?.contentOrNull
            ?: "Unknown API Error"
    }
}
