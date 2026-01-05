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
        private val THEME_MODE = stringPreferencesKey("theme_mode") // system, light, dark
        private val WALLPAPER_URI = stringPreferencesKey("wallpaper_uri")
        private val GLASS_OPACITY = androidx.datastore.preferences.core.floatPreferencesKey("glass_opacity")
        private val GLASS_BLUR = androidx.datastore.preferences.core.floatPreferencesKey("glass_blur")
        private val AGREEMENT_ACCEPTED = androidx.datastore.preferences.core.booleanPreferencesKey("agreement_accepted")
    }
    
    val apiKey: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[API_KEY] ?: ""
    }
    
    val baseUrl: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[BASE_URL] ?: "https://api.openai.com/v1"
    }
    
    val model: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[MODEL] ?: ""
    }
    
    val currentConversationId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[CURRENT_CONVERSATION_ID]
    }
    
    val themeMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[THEME_MODE] ?: "system"
    }

    val wallpaperUri: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[WALLPAPER_URI]
    }

    val glassOpacity: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[GLASS_OPACITY] ?: 0.1f // Default really transparent
    }

    val glassBlur: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[GLASS_BLUR] ?: 16f
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
    
    suspend fun setThemeMode(value: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE] = value
        }
    }

    suspend fun setWallpaperUri(value: String?) {
        context.dataStore.edit { preferences ->
            if (value != null) {
                preferences[WALLPAPER_URI] = value
            } else {
                preferences.remove(WALLPAPER_URI)
            }
        }
    }

    suspend fun setGlassOpacity(value: Float) {
        context.dataStore.edit { preferences ->
            preferences[GLASS_OPACITY] = value
        }
    }

    suspend fun setGlassBlur(value: Float) {
        context.dataStore.edit { preferences ->
            preferences[GLASS_BLUR] = value
        }
    }

    val agreementAccepted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AGREEMENT_ACCEPTED] ?: false
    }

    suspend fun setAgreementAccepted(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AGREEMENT_ACCEPTED] = value
        }
    }
}
