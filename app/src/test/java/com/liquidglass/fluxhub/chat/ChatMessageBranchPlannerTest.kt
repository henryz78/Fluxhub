package com.liquidglass.fluxhub.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatMessageBranchPlannerTest {

    @Test
    fun deleteMessageAndFollowingDeletesFromTargetMessage() {
        val plan = ChatMessageBranchPlanner.deleteMessageAndFollowing(
            messages = messages(),
            messageId = "a1"
        )

        requireNotNull(plan)
        assertEquals(1, plan.startIndex)
        assertEquals(listOf("a1", "u2", "a2"), plan.idsToDelete)
    }

    @Test
    fun editMessageDeletesOnlyFollowingMessages() {
        val plan = ChatMessageBranchPlanner.editMessage(
            messages = messages(),
            messageId = "u1"
        )

        requireNotNull(plan)
        assertEquals(0, plan.messageIndex)
        assertEquals(listOf("a1", "u2", "a2"), plan.idsToDelete)
    }

    @Test
    fun regenerateFromUserMessageKeepsTheUserMessage() {
        val plan = ChatMessageBranchPlanner.regenerateFrom(
            messages = messages(),
            messageId = "u2"
        )

        requireNotNull(plan)
        assertEquals(3, plan.startIndex)
        assertEquals(listOf("a2"), plan.idsToDelete)
    }

    @Test
    fun regenerateFromAssistantMessageDeletesAssistantAndFollowingMessages() {
        val plan = ChatMessageBranchPlanner.regenerateFrom(
            messages = messages(),
            messageId = "a1"
        )

        requireNotNull(plan)
        assertEquals(1, plan.startIndex)
        assertEquals(listOf("a1", "u2", "a2"), plan.idsToDelete)
    }

    @Test
    fun returnsNullWhenTargetMessageIsMissing() {
        assertNull(ChatMessageBranchPlanner.deleteMessageAndFollowing(messages(), "missing"))
        assertNull(ChatMessageBranchPlanner.editMessage(messages(), "missing"))
        assertNull(ChatMessageBranchPlanner.regenerateFrom(messages(), "missing"))
    }

    private fun messages(): List<UiMessage> {
        return listOf(
            UiMessage(id = "u1", role = "user", content = "one"),
            UiMessage(id = "a1", role = "assistant", content = "two"),
            UiMessage(id = "u2", role = "user", content = "three"),
            UiMessage(id = "a2", role = "assistant", content = "four")
        )
    }
}
