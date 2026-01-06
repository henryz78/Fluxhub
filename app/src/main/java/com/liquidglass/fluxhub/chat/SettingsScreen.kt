package com.liquidglass.fluxhub.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.User
import com.composables.icons.lucide.Key
import com.composables.icons.lucide.Palette
import com.composables.icons.lucide.Info
import com.composables.icons.lucide.ChevronRight
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.capsule.ContinuousRoundedRectangle
import com.liquidglass.fluxhub.components.LiquidButton

/**
 * 设置主页面 - 分类入口
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: ChatViewModel,
    backdrop: Backdrop,
    isTab: Boolean = false,
    bottomPadding: PaddingValues = PaddingValues(0.dp),
    onNavigateToAssistants: () -> Unit = {},
    onNavigateToProviders: () -> Unit = {},
    onNavigateToDisplay: () -> Unit = {}
) {
    val glassOpacity = viewModel.glassOpacity
    val glassBlur = viewModel.glassBlur
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(bottomPadding)
    ) {
        // Top Bar (only if not tab)
        if (!isTab) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LiquidButton(
                    onClick = onBack,
                    backdrop = backdrop,
                    modifier = Modifier.size(44.dp),
                    isInteractive = true,
                    padding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = Color.White)
                }
                
                Spacer(Modifier.width(16.dp))
                
                Text(
                    "设置",
                    style = TextStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 4f)
                    )
                )
            }
        } else {
            Text(
                "设置",
                style = TextStyle(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 4f)
                ),
                modifier = Modifier.padding(24.dp)
            )
        }
        
        // 设置分类列表
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 助手管理
            item {
                SettingsCategoryCard(
                    icon = { Icon(Lucide.User, null, tint = Color(0xFF007AFF), modifier = Modifier.size(24.dp)) },
                    title = "助手管理",
                    subtitle = "创建和管理 AI 助手",
                    badge = if (viewModel.assistants.isNotEmpty()) "${viewModel.assistants.size}" else null,
                    backdrop = backdrop,
                    glassOpacity = glassOpacity,
                    glassBlur = glassBlur,
                    onClick = onNavigateToAssistants
                )
            }
            
            // 服务商管理
            item {
                SettingsCategoryCard(
                    icon = { Icon(Lucide.Key, null, tint = Color(0xFFFF9500), modifier = Modifier.size(24.dp)) },
                    title = "服务商管理",
                    subtitle = viewModel.currentProvider?.name ?: "未配置",
                    badge = if (viewModel.providers.isNotEmpty()) "${viewModel.providers.size}" else null,
                    backdrop = backdrop,
                    glassOpacity = glassOpacity,
                    glassBlur = glassBlur,
                    onClick = onNavigateToProviders
                )
            }
            
            // 显示设置
            item {
                SettingsCategoryCard(
                    icon = { Icon(Lucide.Palette, null, tint = Color(0xFF34C759), modifier = Modifier.size(24.dp)) },
                    title = "显示设置",
                    subtitle = "主题、壁纸与效果",
                    backdrop = backdrop,
                    glassOpacity = glassOpacity,
                    glassBlur = glassBlur,
                    onClick = onNavigateToDisplay
                )
            }
            
            // 关于
            item {
                SettingsCategoryCard(
                    icon = { Icon(Lucide.Info, null, tint = Color(0xFFAF52DE), modifier = Modifier.size(24.dp)) },
                    title = "关于",
                    subtitle = "FluxHub v1.0 · Liquid Glass",
                    backdrop = backdrop,
                    glassOpacity = glassOpacity,
                    glassBlur = glassBlur,
                    onClick = { /* TODO */ }
                )
            }
            
            // 账号管理
            item {
                val authState = viewModel.authState
                val authStatusText = when (authState) {
                    is AuthState.Checking -> "验证中..."
                    is AuthState.NoServer -> "未配置后端"
                    is AuthState.Authenticated -> "已绑定"
                    is AuthState.Blocked -> "账号被禁用"
                    is AuthState.Error -> "连接失败"
                }
                val authStatusColor = when (authState) {
                    is AuthState.Authenticated -> Color(0xFF34C759)
                    is AuthState.NoServer -> Color(0xFF8E8E93)
                    is AuthState.Checking -> Color(0xFF007AFF)
                    else -> Color(0xFFFF3B30)
                }
                
                AccountManagementCard(
                    viewModel = viewModel,
                    backdrop = backdrop,
                    glassOpacity = glassOpacity,
                    glassBlur = glassBlur,
                    statusText = authStatusText,
                    statusColor = authStatusColor
                )
            }
            
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun AccountManagementCard(
    viewModel: ChatViewModel,
    backdrop: Backdrop,
    glassOpacity: Float,
    glassBlur: Float,
    statusText: String,
    statusColor: Color
) {
    var showDialog by remember { mutableStateOf(false) }
    var adminUrlInput by remember { mutableStateOf(viewModel.adminUrl) }
    
    // 同步 adminUrl
    LaunchedEffect(viewModel.adminUrl) {
        adminUrlInput = viewModel.adminUrl
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { ContinuousRoundedRectangle(16.dp) },
                effects = { vibrancy(); blur(glassBlur.dp.toPx()) },
                onDrawSurface = { drawRect(Color.White.copy(alpha = glassOpacity)) }
            )
            .clickable { showDialog = true }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { ContinuousRoundedRectangle(12.dp) },
                        effects = { vibrancy() },
                        onDrawSurface = { drawRect(Color.White.copy(alpha = 0.15f)) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Lucide.User, null, tint = Color(0xFF5856D6), modifier = Modifier.size(24.dp))
            }
            
            Spacer(Modifier.width(16.dp))
            
            // Text
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "账号管理",
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 4f)
                    )
                )
                Text(
                    text = if (viewModel.adminUrl.isNotBlank()) statusText else "点击配置后端地址",
                    color = if (viewModel.adminUrl.isNotBlank()) statusColor else Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
            
            // Arrow
            Icon(
                Lucide.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
    
    // 配置对话框
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("账号管理") },
            text = {
                Column {
                    Text("后端地址", fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = adminUrlInput,
                        onValueChange = { adminUrlInput = it },
                        placeholder = { Text("https://your-admin.zeabur.app") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // 状态显示
                    val authState = viewModel.authState
                    when (authState) {
                        is AuthState.Authenticated -> {
                            Text("✅ 账号已绑定", color = Color(0xFF34C759))
                        }
                        is AuthState.NoServer -> {
                            Text("⚪ 未配置后端（离线模式）", color = Color.Gray)
                        }
                        is AuthState.Checking -> {
                            Text("🔄 正在验证...", color = Color(0xFF007AFF))
                        }
                        is AuthState.Blocked -> {
                            Text("⛔ ${authState.message}", color = Color(0xFFFF3B30))
                        }
                        is AuthState.Error -> {
                            Text("⚠️ ${authState.message}", color = Color(0xFFFF9500))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateAdminUrl(adminUrlInput)
                    showDialog = false
                }) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun SettingsCategoryCard(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    badge: String? = null,
    backdrop: Backdrop,
    glassOpacity: Float = 0.1f,
    glassBlur: Float = 16f,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { ContinuousRoundedRectangle(16.dp) },
                effects = { vibrancy(); blur(glassBlur.dp.toPx()) },
                onDrawSurface = { drawRect(Color.White.copy(alpha = glassOpacity)) }
            )
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { ContinuousRoundedRectangle(12.dp) },
                        effects = { vibrancy() },
                        onDrawSurface = { drawRect(Color.White.copy(alpha = 0.15f)) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
            
            Spacer(Modifier.width(16.dp))
            
            // Text
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 4f)
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
            
            // Badge
            if (badge != null) {
                Text(
                    text = badge,
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF34C759)
                    ),
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            
            // Arrow
            Icon(
                Lucide.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
