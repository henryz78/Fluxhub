package com.liquidglass.fluxhub.chat

import com.liquidglass.fluxhub.data.AssistantEntity
import com.liquidglass.fluxhub.data.ConversationEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatConversationSelectorTest {

    @Test
    fun filterForAssistantReturnsOnlyAssistantConversations() {
        val assistant = AssistantEntity(id = "a1", name = "assistant")
        val matching = conversation(id = "c1", assistantId = "a1")
        val other = conversation(id = "c2", assistantId = "a2")

        assertEquals(
            listOf(matching),
            ChatConversationSelector.filterForAssistant(assistant, listOf(matching, other))
        )
    }

    @Test
    fun filterForAssistantReturnsAllConversationsWithoutAssistant() {
        val conversations = listOf(conversation(id = "c1"), conversation(id = "c2"))

        assertEquals(conversations, ChatConversationSelector.filterForAssistant(null, conversations))
    }

    @Test
    fun nextSelectionKeepsExistingCurrentConversation() {
        val selection = ChatConversationSelector.nextSelection(
            currentConversationId = "c1",
            conversations = listOf(conversation(id = "c1")),
            transientConversationIds = emptySet()
        )

        assertEquals(ConversationSelection.KeepCurrent, selection)
    }

    @Test
    fun nextSelectionKeepsTransientConversationEvenWhenMissingFromDatabase() {
        val selection = ChatConversationSelector.nextSelection(
            currentConversationId = "transient",
            conversations = emptyList(),
            transientConversationIds = setOf("transient")
        )

        assertEquals(ConversationSelection.KeepCurrent, selection)
    }

    @Test
    fun nextSelectionSwitchesToFirstConversationWhenCurrentIsMissing() {
        val selection = ChatConversationSelector.nextSelection(
            currentConversationId = "missing",
            conversations = listOf(conversation(id = "c1"), conversation(id = "c2")),
            transientConversationIds = emptySet()
        )

        assertEquals(ConversationSelection.SwitchTo("c1"), selection)
    }

    @Test
    fun nextSelectionCreatesNewConversationWhenListIsEmpty() {
        val selection = ChatConversationSelector.nextSelection(
            currentConversationId = null,
            conversations = emptyList(),
            transientConversationIds = emptySet()
        )

        assertEquals(ConversationSelection.CreateNew, selection)
    }

    private fun conversation(id: String, assistantId: String? = null): ConversationEntity {
        return ConversationEntity(
            id = id,
            title = id,
            assistantId = assistantId
        )
    }
}
