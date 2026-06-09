package com.liquidglass.fluxhub.chat

data class MessageVersionSwitchPlan(
    val messageIndex: Int,
    val updatedMessage: UiMessage
)

object ChatMessageVersionPlanner {
    fun planSwitch(
        messages: List<UiMessage>,
        messageId: String,
        direction: Int
    ): MessageVersionSwitchPlan? {
        val messageIndex = messages.indexOfFirst { it.id == messageId }
        if (messageIndex < 0) return null

        val currentMessage = messages[messageIndex]
        if (currentMessage.totalVersions <= 0) return null

        val newVersionIndex = (currentMessage.versionIndex + direction)
            .coerceIn(0, currentMessage.totalVersions - 1)

        if (newVersionIndex == currentMessage.versionIndex) return null

        return MessageVersionSwitchPlan(
            messageIndex = messageIndex,
            updatedMessage = currentMessage.copy(versionIndex = newVersionIndex)
        )
    }
}
