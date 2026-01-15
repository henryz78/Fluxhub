package com.liquidglass.fluxhub.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
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
        private val GLASS_OPACITY = floatPreferencesKey("glass_opacity")
        private val GLASS_BLUR = floatPreferencesKey("glass_blur")
        private val AGREEMENT_ACCEPTED = booleanPreferencesKey("agreement_accepted")
        
        // 工具箱配置项（全局持久存储）
        private val THINKING_BUDGET = intPreferencesKey("thinking_budget")
        private val WEB_SEARCH_ENABLED = booleanPreferencesKey("web_search_enabled")
        private val SEARCH_PROVIDER = intPreferencesKey("search_provider")
        private val STREAM_ENABLED = booleanPreferencesKey("stream_enabled")
        private val CONTEXT_SIZE = intPreferencesKey("context_size")
        private val HAPTIC_FEEDBACK_ENABLED = booleanPreferencesKey("haptic_feedback_enabled")
        
        // 字体样式配置
        private val TEXT_COLOR_MODE = stringPreferencesKey("text_color_mode") // white, black
        private val TEXT_SHADOW_ENABLED = booleanPreferencesKey("text_shadow_enabled")
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
    
    // ========== 工具箱配置项 ==========
    
    val thinkingBudget: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[THINKING_BUDGET] ?: 1024 // 默认 1024 tokens
    }
    
    val webSearchEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[WEB_SEARCH_ENABLED] ?: false
    }
    
    val searchProvider: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[SEARCH_PROVIDER] ?: 0
    }
    
    val streamEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[STREAM_ENABLED] ?: true // 默认开启流式输出
    }
    
    val contextSize: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[CONTEXT_SIZE] ?: 64 // 默认 64 条消息
    }
    
    suspend fun setThinkingBudget(value: Int) {
        context.dataStore.edit { preferences ->
            preferences[THINKING_BUDGET] = value.coerceIn(0, 32768)
        }
    }
    
    suspend fun setWebSearchEnabled(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[WEB_SEARCH_ENABLED] = value
        }
    }
    
    suspend fun setSearchProvider(value: Int) {
        context.dataStore.edit { preferences ->
            preferences[SEARCH_PROVIDER] = value
        }
    }
    
    suspend fun setStreamEnabled(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[STREAM_ENABLED] = value
        }
    }
    
    val hapticFeedbackEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[HAPTIC_FEEDBACK_ENABLED] ?: true // 默认开启震动
    }

    suspend fun setHapticFeedbackEnabled(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HAPTIC_FEEDBACK_ENABLED] = value
        }
    }

    suspend fun setContextSize(value: Int) {
        context.dataStore.edit { preferences ->
            preferences[CONTEXT_SIZE] = value.coerceIn(1, 128)
        }
    }
    
    // ========== 字体样式配置 ==========
    
    val textColorMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[TEXT_COLOR_MODE] ?: "white" // 默认白色字体
    }
    
    val textShadowEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[TEXT_SHADOW_ENABLED] ?: true // 默认开启阴影
    }
    
    suspend fun setTextColorMode(value: String) {
        context.dataStore.edit { preferences ->
            preferences[TEXT_COLOR_MODE] = value
        }
    }
    
    suspend fun setTextShadowEnabled(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[TEXT_SHADOW_ENABLED] = value
        }
    }
}
