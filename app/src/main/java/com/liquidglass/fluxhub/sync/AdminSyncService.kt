package com.liquidglass.fluxhub.sync

import android.content.Context
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

private const val TAG = "AdminSyncService"

/**
 * 登录/注册结果
 */
sealed class AuthResult {
    data class Success(val userId: String) : AuthResult()
    data class Disabled(val message: String = "账号已被禁用") : AuthResult()
    data class RegistrationClosed(val message: String = "注册已关闭") : AuthResult()
    data class NetworkError(val message: String = "网络错误") : AuthResult()
    object NoServer : AuthResult() // 未配置服务器
}

/**
 * 与后端管理系统同步数据的服务
 */
class AdminSyncService(private val context: Context) {
    
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    // 后端地址 - 部署后修改为实际地址
    var adminBaseUrl: String = ""
    
    // 设备唯一标识
    val deviceId: String by lazy {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
    }
    
    // 当前用户 ID (由后端返回)
    var userId: String? = null
        private set
    
    /**
     * 登录/注册用户（返回详细结果）
     */
    suspend fun authenticate(appVersion: String): AuthResult = withContext(Dispatchers.IO) {
        if (adminBaseUrl.isBlank()) {
            return@withContext AuthResult.NoServer
        }
        
        try {
            val body = json.encodeToString(
                UserSyncRequest(deviceId = deviceId, appVersion = appVersion)
            )
            
            val request = Request.Builder()
                .url("$adminBaseUrl/api/users/sync")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: "{}"
            
            when (response.code) {
                200 -> {
                    val result = json.decodeFromString<UserSyncResponse>(responseBody)
                    userId = result.userId
                    Log.d(TAG, "Auth success: $userId")
                    return@withContext AuthResult.Success(result.userId)
                }
                403 -> {
                    // 解析错误信息
                    val error = try {
                        json.decodeFromString<ErrorResponse>(responseBody).error
                    } catch (e: Exception) { "访问被拒绝" }
                    
                    return@withContext if (error.contains("禁用")) {
                        AuthResult.Disabled(error)
                    } else {
                        AuthResult.RegistrationClosed(error)
                    }
                }
                else -> {
                    Log.e(TAG, "Auth failed: ${response.code}")
                    return@withContext AuthResult.NetworkError("服务器错误: ${response.code}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Auth error", e)
            return@withContext AuthResult.NetworkError("网络连接失败: ${e.message}")
        }
    }
    
    /**
     * 同步服务商配置
     */
    suspend fun syncProviders(providers: List<ProviderSyncData>): Boolean = withContext(Dispatchers.IO) {
        if (adminBaseUrl.isBlank() || userId == null) return@withContext false
        
        try {
            val body = json.encodeToString(
                ProvidersSyncRequest(userId = userId!!, providers = providers)
            )
            
            val request = Request.Builder()
                .url("$adminBaseUrl/api/providers/sync")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            Log.d(TAG, "Providers synced: ${response.isSuccessful}")
            return@withContext response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Providers sync error", e)
        }
        false
    }
    
    /**
     * 同步对话记录
     */
    suspend fun syncConversations(conversations: List<ConversationSyncData>): Boolean = withContext(Dispatchers.IO) {
        if (adminBaseUrl.isBlank() || userId == null) return@withContext false
        
        try {
            val body = json.encodeToString(
                ConversationsSyncRequest(userId = userId!!, conversations = conversations)
            )
            
            val request = Request.Builder()
                .url("$adminBaseUrl/api/conversations/sync")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            Log.d(TAG, "Conversations synced: ${response.isSuccessful}")
            return@withContext response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Conversations sync error", e)
        }
        false
    }
}

// ========== 数据类 ==========

@Serializable
data class UserSyncRequest(
    val deviceId: String,
    val appVersion: String
)

@Serializable
data class UserSyncResponse(
    val userId: String,
    val isDisabled: Boolean = false
)

@Serializable
data class ErrorResponse(
    val error: String
)

@Serializable
data class ProviderSyncData(
    val id: String,
    val name: String,
    val baseUrl: String,
    val apiKey: String,
    val icon: String? = null,
    val isActive: Boolean = false
)

@Serializable
data class ProvidersSyncRequest(
    val userId: String,
    val providers: List<ProviderSyncData>
)

@Serializable
data class ConversationSyncData(
    val id: String,
    val title: String,
    val assistantId: String? = null,
    val isDeleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
    val messages: List<MessageSyncData> = emptyList()
)

@Serializable
data class MessageSyncData(
    val id: String,
    val role: String,
    val content: String,
    val thinkingContent: String? = null,
    val model: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class ConversationsSyncRequest(
    val userId: String,
    val conversations: List<ConversationSyncData>
)
