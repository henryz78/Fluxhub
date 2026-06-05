package com.liquidglass.fluxhub.chat

import com.liquidglass.fluxhub.data.ProviderEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatProviderSelectorTest {

    @Test
    fun defaultProviderPrefersActiveProvider() {
        val inactive = provider(id = "p1", isActive = false)
        val active = provider(id = "p2", isActive = true)

        assertEquals(active, ChatProviderSelector.defaultProvider(listOf(inactive, active)))
    }

    @Test
    fun defaultProviderFallsBackToFirstProvider() {
        val first = provider(id = "p1")
        val second = provider(id = "p2")

        assertEquals(first, ChatProviderSelector.defaultProvider(listOf(first, second)))
    }

    @Test
    fun updatedConfigurationProviderReturnsChangedProvider() {
        val current = provider(id = "p1", apiKey = "old")
        val updated = provider(id = "p1", apiKey = "new")

        assertEquals(updated, ChatProviderSelector.updatedConfigurationProvider(current, listOf(updated)))
    }

    @Test
    fun updatedConfigurationProviderIgnoresUnchangedProvider() {
        val current = provider(id = "p1", apiKey = "same")
        val unchanged = provider(id = "p1", apiKey = "same")

        assertNull(ChatProviderSelector.updatedConfigurationProvider(current, listOf(unchanged)))
    }

    @Test
    fun fallbackAfterDeleteSkipsDeletedProvider() {
        val deleted = provider(id = "p1")
        val fallback = provider(id = "p2")

        assertEquals(fallback, ChatProviderSelector.fallbackAfterDelete("p1", listOf(deleted, fallback)))
    }

    @Test
    fun fallbackAfterDeleteReturnsNullWhenNoProviderRemains() {
        assertNull(ChatProviderSelector.fallbackAfterDelete("p1", listOf(provider(id = "p1"))))
    }

    private fun provider(
        id: String,
        apiKey: String = "key",
        isActive: Boolean = false
    ): ProviderEntity {
        return ProviderEntity(
            id = id,
            name = id,
            baseUrl = "https://example.com",
            apiKey = apiKey,
            isActive = isActive
        )
    }
}
