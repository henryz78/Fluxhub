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
    onNavigateToApiConfig: () -> Unit = {},
    onNavigateToProviders: () -> Unit = {}
) {
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
                    onClick = onNavigateToProviders
                )
            }
            
            // 显示设置 (占位)
            item {
                SettingsCategoryCard(
                    icon = { Icon(Lucide.Palette, null, tint = Color(0xFF34C759), modifier = Modifier.size(24.dp)) },
                    title = "显示设置",
                    subtitle = "主题、字体大小等",
                    backdrop = backdrop,
                    onClick = { /* TODO */ }
                )
            }
            
            // 关于
            item {
                SettingsCategoryCard(
                    icon = { Icon(Lucide.Info, null, tint = Color(0xFFAF52DE), modifier = Modifier.size(24.dp)) },
                    title = "关于",
                    subtitle = "FluxHub v1.0 · Liquid Glass",
                    backdrop = backdrop,
                    onClick = { /* TODO */ }
                )
            }
            
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SettingsCategoryCard(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    badge: String? = null,
    backdrop: Backdrop,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { ContinuousRoundedRectangle(16.dp) },
                effects = { vibrancy(); blur(16.dp.toPx()) },
                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.3f)) }
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
