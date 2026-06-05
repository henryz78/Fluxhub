package com.liquidglass.fluxhub.chat

import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatRequestBuilderTest {

    @Test
    fun buildMessagesFiltersTransientAssistantAndInjectsSystemPrompt() {
        val messages = ChatRequestBuilder.buildMessages(
            history = listOf(
                UiMessage(id = "u1", role = "user", content = "first"),
                UiMessage(id = "a1", role = "assistant", content = "done"),
                UiMessage(id = "pending", role = "assistant", content = "", isStreaming = true),
                UiMessage(id = "u2", role = "user", content = "next")
            ),
            aiMessageId = "pending",
            contextSize = 8,
            systemPrompt = "be concise",
            imageBase64Loader = { null }
        )

        assertEquals(listOf("system", "user", "assistant", "user"), messages.map { it.role })
        assertEquals("be concise", messages[0].content?.jsonPrimitive?.content)
        assertEquals("first", messages[1].content?.jsonPrimitive?.content)
        assertEquals("done", messages[2].content?.jsonPrimitive?.content)
        assertEquals("next", messages[3].content?.jsonPrimitive?.content)
    }

    @Test
    fun buildMessagesConvertsMarkdownImageToOpenAiVisionContent() {
        val messages = ChatRequestBuilder.buildMessages(
            history = listOf(
                UiMessage(id = "u1", role = "user", content = "![image](content://image/1)\nwhat is this?")
            ),
            aiMessageId = null,
            contextSize = 8,
            systemPrompt = null,
            imageBase64Loader = { uri ->
                assertEquals("content://image/1", uri)
                "base64-data"
            }
        )

        val messageContent = messages.single().content
        assertNotNull(messageContent)
        val content = messageContent!!.jsonArray
        assertEquals("text", content[0].jsonObject["type"]?.jsonPrimitive?.content)
        assertEquals("what is this?", content[0].jsonObject["text"]?.jsonPrimitive?.content)
        assertEquals("image_url", content[1].jsonObject["type"]?.jsonPrimitive?.content)
        assertEquals(
            "data:image/jpeg;base64,base64-data",
            content[1].jsonObject["image_url"]?.jsonObject?.get("url")?.jsonPrimitive?.content
        )
    }

    @Test
    fun buildRequestJsonAddsOpenAiOnlyOptionsOnlyWhenRequested() {
        val openAiReasoning = ChatRequestBuilder.reasoningEffortOrNull(
            effectiveBaseUrl = "https://api.openai.com/v1",
            thinkingBudget = 8192
        )
        val nonOpenAiReasoning = ChatRequestBuilder.reasoningEffortOrNull(
            effectiveBaseUrl = "https://example.com/v1",
            thinkingBudget = 8192
        )

        assertEquals("medium", openAiReasoning)
        assertNull(nonOpenAiReasoning)

        val request = ChatRequestBuilder.buildRequestJson(
            model = "gpt-test",
            messages = listOf(ChatMessage("user", kotlinx.serialization.json.JsonPrimitive("hello"))),
            stream = true,
            temperature = 0.7f,
            topP = 1.0f,
            maxTokens = null,
            reasoningEffort = openAiReasoning,
            includeStreamOptions = true
        )

        assertEquals("gpt-test", request["model"]?.jsonPrimitive?.content)
        assertTrue(request["stream"]?.jsonPrimitive?.boolean ?: false)
        assertEquals("medium", request["reasoning_effort"]?.jsonPrimitive?.content)
        assertTrue(request["stream_options"]?.jsonObject?.get("include_usage")?.jsonPrimitive?.boolean ?: false)

        val providerRequest = ChatRequestBuilder.buildRequestJson(
            model = "third-party",
            messages = listOf(ChatMessage("user", kotlinx.serialization.json.JsonPrimitive("hello"))),
            stream = true,
            temperature = 0.7f,
            topP = 1.0f,
            maxTokens = null,
            reasoningEffort = nonOpenAiReasoning,
            includeStreamOptions = false
        )

        assertFalse(providerRequest.containsKey("reasoning_effort"))
        assertFalse(providerRequest.containsKey("stream_options"))
    }
}
