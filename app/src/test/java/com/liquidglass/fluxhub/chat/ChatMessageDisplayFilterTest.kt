package com.liquidglass.fluxhub.chat

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatMessageDisplayFilterTest {

    @Test
    fun `removes system messages from display list`() {
        val messages = listOf(
            UiMessage(id = "system", role = "system", content = "hidden"),
            UiMessage(id = "user", role = "user", content = "visible"),
            UiMessage(id = "assistant", role = "assistant", content = "also visible")
        )

        val visible = ChatMessageDisplayFilter.visibleMessages(messages)

        assertEquals(listOf("user", "assistant"), visible.map { it.id })
    }

    @Test
    fun `keeps visible messages in original order`() {
        val messages = listOf(
            UiMessage(id = "u1", role = "user", content = "first"),
            UiMessage(id = "a1", role = "assistant", content = "second"),
            UiMessage(id = "u2", role = "user", content = "third")
        )

        val visible = ChatMessageDisplayFilter.visibleMessages(messages)

        assertEquals(messages, visible)
    }
}
