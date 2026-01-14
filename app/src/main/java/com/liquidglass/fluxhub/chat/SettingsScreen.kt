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
 * 设置主页面 - 分类入口 (重构版)
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
    
    // About Dialog State
    var showAboutDialog by remember { mutableStateOf(false) }
    
    // About Dialog
    if (showAboutDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showAboutDialog = false }
        ) {
            Box(
                modifier = Modifier
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { ContinuousRoundedRectangle(24.dp) },
                        effects = { vibrancy(); blur(glassBlur.dp.toPx()) },
                        onDrawSurface = { drawRect(Color.White.copy(alpha = glassOpacity + 0.1f)) }
                    )
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Logo 区域
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .drawBackdrop(
                                backdrop = backdrop,
                                shape = { ContinuousRoundedRectangle(20.dp) },
                                effects = { vibrancy(); blur(20.dp.toPx()) },
                                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.2f)) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("💎", fontSize = 42.sp)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "FluxHub",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            style = TextStyle(
                                shadow = Shadow(color = Color.Black.copy(alpha = 0.3f), blurRadius = 4f)
                            )
                        )
                        Text(
                            "v1.0.4 · Liquid Glass",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    
                    Text(
                        "一款基于 Liquid Glass 设计语言的 AI 聊天应用，为您带来沉浸式的对话体验。",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.padding(horizontal = 8.dp),
                        style = TextStyle(lineHeight = 20.sp)
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    LiquidButton(
                        onClick = { showAboutDialog = false },
                        backdrop = backdrop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = { ContinuousRoundedRectangle(16.dp) },
                        isInteractive = true,
                        tint = Color(0xFF007AFF)
                    ) {
                        Text(
                            "关闭",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(bottomPadding)
    ) {
        // App Bar / Title
        if (!isTab) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LiquidButton(
                    onClick = onBack,
                    backdrop = backdrop,
                    modifier = Modifier.size(40.dp),
                    isInteractive = true,
                    padding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = Color.White)
                }
                Spacer(Modifier.width(16.dp))
                SettingsTitle("设置")
            }
        } else {
             Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)) {
                 SettingsTitle("设置")
             }
        }
        
        // Settings List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Group 1: AI Config
            item {
                SettingsGroup(title = "智能配置") {
                    SettingsCategoryItem(
                        icon = Lucide.User,
                        iconColor = Color.White,
                        iconBgColor = Color(0xFF007AFF), // Blue
                        title = "助手管理",
                        subtitle = "管理您的 AI 角色与预设",
                        badge = if (viewModel.assistants.isNotEmpty()) "${viewModel.assistants.size}" else null,
                        backdrop = backdrop,
                        isFirst = true,
                        onClick = onNavigateToAssistants
                    )
                    SettingsCategoryItem(
                        icon = Lucide.Key,
                        iconColor = Color.White,
                        iconBgColor = Color(0xFFFF9500), // Orange
                        title = "服务商管理",
                        subtitle = "配置 API Key 与模型端点",
                        badge = if (viewModel.providers.isNotEmpty()) "${viewModel.providers.size}" else null,
                        backdrop = backdrop,
                        isLast = true,
                        onClick = onNavigateToProviders
                    )
                }
            }
            
            // Group 2: Appearance
            item {
                SettingsGroup(title = "个性化") {
                    SettingsCategoryItem(
                        icon = Lucide.Palette,
                        iconColor = Color.White,
                        iconBgColor = Color(0xFF34C759), // Green
                        title = "显示设置",
                        subtitle = "壁纸、毛玻璃效果强度",
                        backdrop = backdrop,
                        isFirst = true,
                        isLast = true,
                        onClick = onNavigateToDisplay
                    )
                }
            }

            // Group 3: About
            item {
                SettingsGroup(title = "其他") {
                    SettingsCategoryItem(
                        icon = Lucide.Info,
                        iconColor = Color.White,
                        iconBgColor = Color(0xFFAF52DE), // Purple
                        title = "关于 FluxHub",
                        subtitle = "版本与开发者信息",
                        backdrop = backdrop,
                        isFirst = true,
                        isLast = true,
                        onClick = { showAboutDialog = true }
                    )
                }
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
                    onClick = { showAboutDialog = true }
                )
            }
            
            
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SettingsTitle(text: String) {
    Text(
        text = text,
        style = TextStyle(
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            shadow = Shadow(color = Color.Black.copy(alpha = 0.3f), blurRadius = 8f)
        )
    )
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Text(
            text = title.uppercase(),
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.5f),
                letterSpacing = 1.sp,
                shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 2f)
            ),
            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
        )
        content()
    }
}

@Composable
private fun SettingsCategoryItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    iconBgColor: Color,
    title: String,
    subtitle: String,
    badge: String? = null,
    backdrop: Backdrop,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    onClick: () -> Unit
) {
    val shape = when {
        isFirst && isLast -> ContinuousRoundedRectangle(20.dp)
        isFirst -> androidx.compose.foundation.shape.RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
        isLast -> androidx.compose.foundation.shape.RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
        else -> androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
    }
    
    val bottomSpacer = if (isLast) 0.dp else 1.dp

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { shape },
                    effects = { vibrancy(); blur(20.dp.toPx()) },
                    onDrawSurface = { drawRect(Color.White.copy(alpha = 0.1f)) }
                )
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Colored Icon Container
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { ContinuousRoundedRectangle(10.dp) },
                            effects = { blur(0f) }, // Solid color usually
                            onDrawSurface = { drawRect(iconBgColor) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
                }
                
                Spacer(Modifier.width(16.dp))
                
                // Texts
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                            shadow = Shadow(color = Color.Black.copy(alpha = 0.3f), blurRadius = 2f)
                        )
                    )
                    Text(
                        text = subtitle,
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    )
                }
                
                // Badge
                if (badge != null) {
                    Box(
                        modifier = Modifier
                            .drawBackdrop(
                                backdrop = backdrop,
                                shape = { androidx.compose.foundation.shape.CircleShape },
                                effects = { vibrancy() },
                                onDrawSurface = { drawRect(Color.Red) }
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badge,
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                }
                
                // Chevron
                Icon(
                    Lucide.ChevronRight,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        
        if (!isLast) {
             Spacer(Modifier.height(2.dp))
        }
    }
}
