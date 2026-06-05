package com.liquidglass.fluxhub.chat

import com.liquidglass.fluxhub.data.ProviderEntity

object ChatProviderSelector {
    fun defaultProvider(providers: List<ProviderEntity>): ProviderEntity? {
        return providers.find { it.isActive } ?: providers.firstOrNull()
    }

    fun updatedConfigurationProvider(
        currentProvider: ProviderEntity?,
        providers: List<ProviderEntity>
    ): ProviderEntity? {
        if (currentProvider == null) return null

        val updated = providers.find { it.id == currentProvider.id } ?: return null
        return if (updated.apiKey != currentProvider.apiKey || updated.baseUrl != currentProvider.baseUrl) {
            updated
        } else {
            null
        }
    }

    fun fallbackAfterDelete(
        deletedProviderId: String,
        providers: List<ProviderEntity>
    ): ProviderEntity? {
        return providers.firstOrNull { it.id != deletedProviderId }
    }
}
