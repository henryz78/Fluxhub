package com.liquidglass.fluxhub.chat

import com.liquidglass.fluxhub.data.AssistantEntity

object ChatAssistantSelector {
    fun defaultAssistant(assistants: List<AssistantEntity>): AssistantEntity? {
        return assistants.find { it.isDefault } ?: assistants.firstOrNull()
    }

    fun fallbackAfterDelete(
        deletedAssistantId: String,
        assistants: List<AssistantEntity>
    ): AssistantEntity? {
        return assistants.firstOrNull { it.id != deletedAssistantId }
    }
}
