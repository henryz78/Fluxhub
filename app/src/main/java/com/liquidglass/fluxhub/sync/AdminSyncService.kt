package com.liquidglass.fluxhub.sync

import android.content.Context
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

// 后端地址（硬编码）
private const val ADMIN_BASE_URL = "https://fluxhubadmin-production.up.railway.app"

/**
 * 登录/注册结果
 */
sealed class AuthResult {
    data class Success(val token: String, val userId: String, val username: String) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

/**
 * 与后端管理系统同步数据的服务
 */
class AdminSyncService(private val context: Context) {
    
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)  // Railway 冷启动可能需要 30 秒+
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    // SharedPreferences 用于持久化登录状态
    private val prefs = context.getSharedPreferences("fluxhub_auth", Context.MODE_PRIVATE)
    
    // 用户 Token（登录后设置，持久化）
    var authToken: String?
        get() = prefs.getString("auth_token", null)
        set(value) = prefs.edit().putString("auth_token", value).apply()
    
    // 当前用户 ID（持久化）
    var userId: String?
        get() = prefs.getString("user_id", null)
        set(value) = prefs.edit().putString("user_id", value).apply()
    
    // 当前用户名（持久化）
    var username: String?
        get() = prefs.getString("username", null)
        set(value) = prefs.edit().putString("username", value).apply()
    
    /**
     * 用户注册
     */
    suspend fun register(username: String, email: String, password: String, inviteCode: String = ""): AuthResult = withContext(Dispatchers.IO) {
        try {
            val body = json.encodeToString(
                RegisterRequest(username = username, email = email, password = password, inviteCode = inviteCode.ifBlank { null })
            )
            
            val request = Request.Builder()
                .url("$ADMIN_BASE_URL/api/user-auth/register")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: "{}"
            
            if (response.isSuccessful) {
                val result = json.decodeFromString<AuthResponse>(responseBody)
                authToken = result.token
                userId = result.user.id
                this@AdminSyncService.username = result.user.username
                Log.d(TAG, "Register success: ${result.user.username}")
                return@withContext AuthResult.Success(result.token, result.user.id, result.user.username)
            } else {
                val error = try {
                    json.decodeFromString<ErrorResponse>(responseBody).error
                } catch (e: Exception) { "注册失败" }
                Log.e(TAG, "Register failed: $error")
                return@withContext AuthResult.Error(error)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Register error", e)
            return@withContext AuthResult.Error("网络连接失败")
        }
    }
    
    /**
     * 用户登录
     */
    suspend fun login(username: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        var attempts = 0
        while (attempts < 2) {
            try {
                val body = json.encodeToString(
                    LoginRequest(username = username, password = password)
                )
                
                val request = Request.Builder()
                    .url("$ADMIN_BASE_URL/api/user-auth/login")
                    .post(body.toRequestBody("application/json".toMediaType()))
                    .build()
                
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: "{}"
                
                if (response.isSuccessful) {
                    val result = json.decodeFromString<AuthResponse>(responseBody)
                    authToken = result.token
                    userId = result.user.id
                    this@AdminSyncService.username = result.user.username
                    Log.d(TAG, "Login success: ${result.user.username}")
                    return@withContext AuthResult.Success(result.token, result.user.id, result.user.username)
                } else {
                    val error = try {
                        json.decodeFromString<ErrorResponse>(responseBody).error
                    } catch (e: Exception) { "登录失败" }
                    Log.e(TAG, "Login failed: $error")
                    return@withContext AuthResult.Error(error)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Login attempt ${attempts + 1} failed", e)
                attempts++
                if (attempts >= 2) {
                    return@withContext AuthResult.Error("网络连接失败")
                }
                // 简单的失败重试，针对冷启动可能断开连接的情况
                try { kotlinx.coroutines.delay(1000) } catch (e: Exception) {}
            }
        }
        return@withContext AuthResult.Error("网络连接失败")
    }
    
    /**
     * 验证 Token 是否有效
     */
    suspend fun verifyToken(): AuthResult = withContext(Dispatchers.IO) {
        val token = authToken ?: return@withContext AuthResult.Error("未登录")
        
        try {
            val request = Request.Builder()
                .url("$ADMIN_BASE_URL/api/user-auth/me")
                .header("Authorization", "Bearer $token")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: "{}"
            
            if (response.isSuccessful) {
                val user = json.decodeFromString<UserInfo>(responseBody)
                userId = user.id
                Log.d(TAG, "Token valid: ${user.username}")
                return@withContext AuthResult.Success(token, user.id, user.username)
            } else {
                val error = try {
                    json.decodeFromString<ErrorResponse>(responseBody).error
                } catch (e: Exception) { "验证失败" }
                Log.e(TAG, "Token invalid: $error")
                // 清除无效 Token
                authToken = null
                userId = null
                return@withContext AuthResult.Error(error)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Verify token error", e)
            return@withContext AuthResult.Error("网络连接失败")
        }
    }
    
    /**
     * 登出
     */
    fun logout() {
        authToken = null
        userId = null
        username = null
    }
    
    /**
     * 使用激活码续期
     */
    suspend fun renew(inviteCode: String): AuthResult = withContext(Dispatchers.IO) {
        val token = authToken ?: return@withContext AuthResult.Error("未登录")
        
        try {
            val body = json.encodeToString(RenewRequest(inviteCode = inviteCode))
            
            val request = Request.Builder()
                .url("$ADMIN_BASE_URL/api/user-auth/renew")
                .header("Authorization", "Bearer $token")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: "{}"
            
            if (response.isSuccessful) {
                // 续期成功，重新验证获取用户信息
                val user = verifyTokenInternal(token)
                if (user != null) {
                    Log.d(TAG, "Renew success: ${user.username}")
                    return@withContext AuthResult.Success(token, user.id, user.username)
                }
                return@withContext AuthResult.Success(token, userId ?: "", "")
            } else {
                val error = try {
                    json.decodeFromString<ErrorResponse>(responseBody).error
                } catch (e: Exception) { "续期失败" }
                Log.e(TAG, "Renew failed: $error")
                return@withContext AuthResult.Error(error)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Renew error", e)
            return@withContext AuthResult.Error("网络连接失败")
        }
    }
    
    private suspend fun verifyTokenInternal(token: String): UserInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$ADMIN_BASE_URL/api/user-auth/me")
                .header("Authorization", "Bearer $token")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                return@withContext json.decodeFromString<UserInfo>(response.body?.string() ?: "{}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Verify token internal error", e)
        }
        return@withContext null
    }
    
    /**
     * 同步服务商配置
     */
    suspend fun syncProviders(providers: List<ProviderSyncData>): Boolean = withContext(Dispatchers.IO) {
        val token = authToken ?: return@withContext false
        val uid = userId ?: return@withContext false
        
        try {
            val body = json.encodeToString(
                ProvidersSyncRequest(userId = uid, providers = providers)
            )
            
            val request = Request.Builder()
                .url("$ADMIN_BASE_URL/api/providers/sync")
                .header("Authorization", "Bearer $token")
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
        val token = authToken ?: return@withContext false
        val uid = userId ?: return@withContext false
        
        try {
            val body = json.encodeToString(
                ConversationsSyncRequest(userId = uid, conversations = conversations)
            )
            
            val request = Request.Builder()
                .url("$ADMIN_BASE_URL/api/conversations/sync")
                .header("Authorization", "Bearer $token")
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
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val inviteCode: String? = null
)

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class RenewRequest(
    val inviteCode: String
)

@Serializable
data class AuthResponse(
    val token: String,
    val user: UserInfo
)

@Serializable
data class UserInfo(
    val id: String,
    val username: String,
    val email: String = "",
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
