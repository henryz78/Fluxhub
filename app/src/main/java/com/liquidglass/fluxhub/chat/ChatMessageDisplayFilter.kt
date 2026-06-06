package com.liquidglass.fluxhub.chat

object ChatMessageDisplayFilter {
    fun visibleMessages(messages: List<UiMessage>): List<UiMessage> {
        return messages.filter { it.role != "system" }
    }
}
