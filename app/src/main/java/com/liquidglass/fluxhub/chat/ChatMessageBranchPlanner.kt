package com.liquidglass.fluxhub.chat

data class MessageDeletePlan(
    val startIndex: Int,
    val idsToDelete: List<String>
)

data class MessageEditPlan(
    val messageIndex: Int,
    val idsToDelete: List<String>
)

object ChatMessageBranchPlanner {
    fun deleteMessageAndFollowing(
        messages: List<UiMessage>,
        messageId: String
    ): MessageDeletePlan? {
        val messageIndex = messages.indexOfFirst { it.id == messageId }
        if (messageIndex < 0) return null

        return MessageDeletePlan(
            startIndex = messageIndex,
            idsToDelete = messages.drop(messageIndex).map { it.id }
        )
    }

    fun editMessage(
        messages: List<UiMessage>,
        messageId: String
    ): MessageEditPlan? {
        val messageIndex = messages.indexOfFirst { it.id == messageId }
        if (messageIndex < 0) return null

        return MessageEditPlan(
            messageIndex = messageIndex,
            idsToDelete = messages.drop(messageIndex + 1).map { it.id }
        )
    }

    fun regenerateFrom(
        messages: List<UiMessage>,
        messageId: String
    ): MessageDeletePlan? {
        val messageIndex = messages.indexOfFirst { it.id == messageId }
        if (messageIndex < 0) return null

        val deleteStartIndex = if (messages[messageIndex].role == "user") {
            messageIndex + 1
        } else {
            messageIndex
        }

        return MessageDeletePlan(
            startIndex = deleteStartIndex,
            idsToDelete = messages.drop(deleteStartIndex).map { it.id }
        )
    }
}
