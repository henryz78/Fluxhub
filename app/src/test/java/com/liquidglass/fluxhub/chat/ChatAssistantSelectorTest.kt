package com.liquidglass.fluxhub.chat

import com.liquidglass.fluxhub.data.AssistantEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatAssistantSelectorTest {

    @Test
    fun defaultAssistantPrefersDefaultAssistant() {
        val first = assistant(id = "a1")
        val default = assistant(id = "a2", isDefault = true)

        assertEquals(default, ChatAssistantSelector.defaultAssistant(listOf(first, default)))
    }

    @Test
    fun defaultAssistantFallsBackToFirstAssistant() {
        val first = assistant(id = "a1")
        val second = assistant(id = "a2")

        assertEquals(first, ChatAssistantSelector.defaultAssistant(listOf(first, second)))
    }

    @Test
    fun fallbackAfterDeleteSkipsDeletedAssistant() {
        val deleted = assistant(id = "a1")
        val fallback = assistant(id = "a2")

        assertEquals(fallback, ChatAssistantSelector.fallbackAfterDelete("a1", listOf(deleted, fallback)))
    }

    @Test
    fun fallbackAfterDeleteReturnsNullWhenNoAssistantRemains() {
        assertNull(ChatAssistantSelector.fallbackAfterDelete("a1", listOf(assistant(id = "a1"))))
    }

    private fun assistant(id: String, isDefault: Boolean = false): AssistantEntity {
        return AssistantEntity(
            id = id,
            name = id,
            isDefault = isDefault
        )
    }
}
