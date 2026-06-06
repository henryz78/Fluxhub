package com.liquidglass.fluxhub.chat

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatMessageSyncPlannerTest {

    @Test
    fun `keeps ui only messages that are not deleted`() {
        val dbMessages = listOf(
            message(id = "db-user", role = "user", timestamp = 10)
        )
        val currentMessages = dbMessages + message(
            id = "ui-ai-placeholder",
            role = "assistant",
            timestamp = 20,
            isStreaming = true
        )

        val merged = ChatMessageSyncPlanner.merge(
            dbMessages = dbMessages,
            currentMessages = currentMessages,
            deletedMessageIds = emptySet()
        )

        assertEquals(listOf("db-user", "ui-ai-placeholder"), merged.map { it.id })
    }

    @Test
    fun `does not keep ui only messages that were deleted`() {
        val dbMessages = listOf(message(id = "db-user", role = "user", timestamp = 10))
        val currentMessages = dbMessages + message(id = "deleted-ui", role = "assistant", timestamp = 20)

        val merged = ChatMessageSyncPlanner.merge(
            dbMessages = dbMessages,
            currentMessages = currentMessages,
            deletedMessageIds = setOf("deleted-ui")
        )

        assertEquals(listOf("db-user"), merged.map { it.id })
    }

    @Test
    fun `sorts database and ui only messages by timestamp`() {
        val dbMessages = listOf(
            message(id = "db-later", role = "assistant", timestamp = 30),
            message(id = "db-earlier", role = "user", timestamp = 10)
        )
        val currentMessages = dbMessages + message(id = "ui-middle", role = "assistant", timestamp = 20)

        val merged = ChatMessageSyncPlanner.merge(
            dbMessages = dbMessages,
            currentMessages = currentMessages,
            deletedMessageIds = emptySet()
        )

        assertEquals(listOf("db-earlier", "ui-middle", "db-later"), merged.map { it.id })
    }

    private fun message(
        id: String,
        role: String,
        timestamp: Long,
        isStreaming: Boolean = false
    ): UiMessage {
        return UiMessage(
            id = id,
            role = role,
            content = id,
            timestamp = timestamp,
            isStreaming = isStreaming
        )
    }
}
