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
import com.composables.icons.lucide.Zap
import com.composables.icons.lucide.Save
import com.composables.icons.lucide.Check
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.capsule.ContinuousRoundedRectangle
import com.liquidglass.fluxhub.components.LiquidButton
import com.liquidglass.fluxhub.ui.theme.GlassTypography
import com.liquidglass.fluxhub.ui.theme.GlassTextStyles

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
    onNavigateToDisplay: () -> Unit = {},
    onNavigateToDynamicIsland: () -> Unit = {}
) {
    val glassOpacity = viewModel.glassOpacity
    val glassBlur = viewModel.glassBlur
    
    // 动态字体样式
    val textStyles = GlassTextStyles.create(
        colorMode = viewModel.textColorMode,
        shadowEnabled = viewModel.textShadowEnabled
    )
    
    var showAboutDialog by remember { mutableStateOf(false) }
    var showModelDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    
    // 备份相关 Launcher
    val context = androidx.compose.ui.platform.LocalContext.current
    
    fun saveToFile(uri: android.net.Uri, content: String) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { 
                it.write(content.toByteArray()) 
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { 
             viewModel.exportData { json ->
                 if (json != null) {
                     saveToFile(it, json)
                 }
             }
        }
    }
    
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.importData(it) { success -> /* Toast? */ } }
    }
    
    // Main Content
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
                SettingsTitle("设置", textStyles)
            }
        } else {
             Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)) {
                 SettingsTitle("设置", textStyles)
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
                SettingsGroup(title = "智能配置", textStyles = textStyles) {
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
                        onClick = onNavigateToProviders
                    )
                    SettingsCategoryItem(
                        icon = Lucide.Zap,
                        iconColor = Color.White,
                        iconBgColor = Color(0xFFFF2D55), // Pink
                        title = "默认模型",
                        subtitle = if (viewModel.defaultModel.isNotBlank()) viewModel.defaultModel else "使用当前配置",
                        backdrop = backdrop,
                        isLast = true,
                        onClick = { showModelDialog = true }
                    )
                }
            }
            
            // Group 2: Data & Storage
            item {
                SettingsGroup(title = "数据与存储", textStyles = textStyles) {
                    SettingsCategoryItem(
                        icon = Lucide.Save,
                        iconColor = Color.White,
                        iconBgColor = Color(0xFF5856D6), // Indigo
                        title = "数据备份与恢复",
                        subtitle = "导出或导入聊天记录",
                        backdrop = backdrop,
                        isFirst = true,
                        isLast = true,
                        onClick = { showBackupDialog = true }
                    )
                }
            }
            
            // Group 3: Appearance
            item {
                SettingsGroup(title = "个性化", textStyles = textStyles) {
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

            // Group 4: About
            item {
                SettingsGroup(title = "其他", textStyles = textStyles) {
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
        }
    }
    
    // Dialogs (Overlays)
    
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
                            "v1.0.6 · Liquid Glass",
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
    
    // Model Selection Dialog
    if (showModelDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showModelDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { ContinuousRoundedRectangle(24.dp) },
                        effects = { vibrancy(); blur(16.dp.toPx()) },
                        onDrawSurface = { drawRect(Color.White.copy(0.1f)) }
                    )
                    .padding(24.dp)
            ) {
                Column {
                    Text("默认模型", style = textStyles.titleMedium, color = Color.White)
                    Spacer(Modifier.height(16.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(viewModel.availableModels.size) { index ->
                            val model = viewModel.availableModels[index]
                            val isSelected = model == viewModel.defaultModel
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        viewModel.updateDefaultModel(model)
                                        showModelDialog = false
                                    }
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(model, color = Color.White, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                if (isSelected) Icon(Lucide.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                            HorizontalDivider(color = Color.White.copy(0.1f))
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    LiquidButton(onClick = { showModelDialog = false }, backdrop = backdrop, modifier = Modifier.fillMaxWidth().height(48.dp), isInteractive = true) {
                        Text("关闭", color = Color.White)
                    }
                }
            }
        }
    }
    
    // Backup Dialog
    if (showBackupDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showBackupDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { ContinuousRoundedRectangle(24.dp) },
                        effects = { vibrancy(); blur(16.dp.toPx()) },
                        onDrawSurface = { drawRect(Color.White.copy(0.1f)) }
                    )
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("数据备份", style = textStyles.titleMedium, color = Color.White)
                    Spacer(Modifier.height(8.dp))
                    Text("备份或恢复所有聊天记录和设置。", style = textStyles.bodyMedium, color = Color.White.copy(0.7f))
                    Spacer(Modifier.height(24.dp))
                    
                    LiquidButton(
                        onClick = { 
                            createDocumentLauncher.launch("fluxhub_backup_${System.currentTimeMillis()}.json")
                            showBackupDialog = false
                        }, 
                        backdrop = backdrop, 
                        modifier = Modifier.fillMaxWidth().height(48.dp), 
                        isInteractive = true,
                        tint = Color(0xFF34C759)
                    ) {
                        Icon(Lucide.Save, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("导出备份", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    LiquidButton(
                        onClick = { 
                            importLauncher.launch("application/json")
                            showBackupDialog = false
                        }, 
                        backdrop = backdrop, 
                        modifier = Modifier.fillMaxWidth().height(48.dp), 
                        isInteractive = true,
                        tint = Color(0xFF007AFF)
                    ) {
                        Icon(Lucide.Zap, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("导入恢复", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    Text("导入将覆盖现有数据", style = textStyles.caption, color = Color.White.copy(0.4f))
                }
            }
        }
    }
}

@Composable
private fun SettingsTitle(text: String, textStyles: GlassTextStyles) {
    Text(
        text = text,
        style = textStyles.title
    )
}

@Composable
private fun SettingsGroup(
    title: String,
    textStyles: GlassTextStyles,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Text(
            text = title.uppercase(),
            style = textStyles.label,
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
