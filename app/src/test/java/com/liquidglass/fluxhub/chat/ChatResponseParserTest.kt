package com.liquidglass.fluxhub.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatResponseParserTest {

    @Test
    fun parseNonStreamingReadsContentAndReasoning() {
        val result = ChatResponseParser.parseNonStreaming(
            """
            {
              "choices": [
                {
                  "message": {
                    "content": "final answer",
                    "reasoning_content": "thinking"
                  }
                }
              ]
            }
            """.trimIndent()
        )

        assertEquals("final answer", result.content)
        assertEquals("thinking", result.reasoningContent)
    }

    @Test
    fun parseNonStreamingReadsArrayTextContent() {
        val result = ChatResponseParser.parseNonStreaming(
            """
            {
              "choices": [
                {
                  "message": {
                    "content": [
                      { "type": "text", "text": "part one" },
                      { "type": "text", "text": " part two" }
                    ]
                  }
                }
              ]
            }
            """.trimIndent()
        )

        assertEquals("part one part two", result.content)
        assertNull(result.reasoningContent)
    }

    @Test(expected = ChatApiException::class)
    fun parseNonStreamingThrowsApiError() {
        ChatResponseParser.parseNonStreaming(
            """
            {
              "error": {
                "message": "bad request"
              }
            }
            """.trimIndent()
        )
    }

    @Test
    fun parseStreamingEventDataReadsDeltaAndDone() {
        val deltas = ChatResponseParser.parseStreamingEventData(
            """
            data: {"choices":[{"delta":{"reasoning_content":"think"}}]}
            data: {"choices":[{"delta":{"content":"hello"}}]}
            data: [DONE]
            """.trimIndent()
        )

        assertEquals("think", deltas[0].reasoningContent)
        assertEquals("hello", deltas[1].content)
        assertTrue(deltas[2].isDone)
    }

    @Test
    fun parseStreamingEventDataReadsErrorMessage() {
        val deltas = ChatResponseParser.parseStreamingEventData(
            """
            data: {"error":{"message":"stream failed"}}
            """.trimIndent()
        )

        assertEquals("stream failed", deltas.single().errorMessage)
    }
}
