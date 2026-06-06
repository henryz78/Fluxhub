package com.liquidglass.fluxhub.chat

object ChatMessageSyncPlanner {
    fun merge(
        dbMessages: List<UiMessage>,
        currentMessages: List<UiMessage>,
        deletedMessageIds: Set<String>
    ): List<UiMessage> {
        val dbMessageIds = dbMessages.mapTo(mutableSetOf()) { it.id }
        val uiOnlyMessages = currentMessages.filter { message ->
            message.id !in dbMessageIds && message.id !in deletedMessageIds
        }

        return (dbMessages + uiOnlyMessages).sortedBy { it.timestamp }
    }
}
