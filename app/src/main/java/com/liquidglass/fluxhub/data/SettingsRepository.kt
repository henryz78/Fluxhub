package com.liquidglass.fluxhub.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    
    companion object {
        private val API_KEY = stringPreferencesKey("api_key")
        private val BASE_URL = stringPreferencesKey("base_url")
        private val MODEL = stringPreferencesKey("model")
        private val CURRENT_CONVERSATION_ID = stringPreferencesKey("current_conversation_id")
    }
    
    val apiKey: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[API_KEY] ?: ""
    }
    
    val baseUrl: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[BASE_URL] ?: "https://api.openai.com/v1"
    }
    
    val model: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[MODEL] ?: "gpt-4o-mini"
    }
    
    val currentConversationId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[CURRENT_CONVERSATION_ID]
    }
    
    suspend fun setApiKey(value: String) {
        context.dataStore.edit { preferences ->
            preferences[API_KEY] = value
        }
    }
    
    suspend fun setBaseUrl(value: String) {
        context.dataStore.edit { preferences ->
            preferences[BASE_URL] = value
        }
    }
    
    suspend fun setModel(value: String) {
        context.dataStore.edit { preferences ->
            preferences[MODEL] = value
        }
    }
    
    suspend fun setCurrentConversationId(value: String?) {
        context.dataStore.edit { preferences ->
            if (value != null) {
                preferences[CURRENT_CONVERSATION_ID] = value
            } else {
                preferences.remove(CURRENT_CONVERSATION_ID)
            }
        }
    }
}
