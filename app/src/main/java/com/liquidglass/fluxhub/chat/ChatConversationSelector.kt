package com.liquidglass.fluxhub.chat

import com.liquidglass.fluxhub.data.AssistantEntity
import com.liquidglass.fluxhub.data.ConversationEntity

sealed class ConversationSelection {
    object KeepCurrent : ConversationSelection()
    data class SwitchTo(val conversationId: String) : ConversationSelection()
    object CreateNew : ConversationSelection()
}

object ChatConversationSelector {
    fun filterForAssistant(
        assistant: AssistantEntity?,
        conversations: List<ConversationEntity>
    ): List<ConversationEntity> {
        return if (assistant == null) {
            conversations
        } else {
            conversations.filter { it.assistantId == assistant.id }
        }
    }

    fun nextSelection(
        currentConversationId: String?,
        conversations: List<ConversationEntity>,
        transientConversationIds: Set<String>
    ): ConversationSelection {
        val currentIdMissing = currentConversationId != null &&
            conversations.none { it.id == currentConversationId }
        val noConversations = conversations.isEmpty()

        if (!currentIdMissing && !noConversations) {
            return ConversationSelection.KeepCurrent
        }

        if (currentConversationId != null && transientConversationIds.contains(currentConversationId)) {
            return ConversationSelection.KeepCurrent
        }

        return conversations.firstOrNull()?.let { conversation ->
            ConversationSelection.SwitchTo(conversation.id)
        } ?: ConversationSelection.CreateNew
    }
}
