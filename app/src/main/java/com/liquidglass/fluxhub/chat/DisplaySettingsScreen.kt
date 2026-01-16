package com.liquidglass.fluxhub.chat

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.effects.blur
import com.kyant.capsule.ContinuousRoundedRectangle
import com.liquidglass.fluxhub.components.LiquidButton
import com.liquidglass.fluxhub.components.LiquidSlider
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.liquidglass.fluxhub.ui.theme.GlassTextStyles

@Composable
fun DisplaySettingsScreen(
    onBack: () -> Unit,
    viewModel: ChatViewModel,
    backdrop: Backdrop,
    bottomPadding: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val wallpaperUri = viewModel.wallpaperUri
    val glassOpacity = viewModel.glassOpacity
    val glassBlur = viewModel.glassBlur
    
    // 动态字体样式
    val textStyles = GlassTextStyles.create(
        colorMode = viewModel.textColorMode,
        shadowEnabled = viewModel.textShadowEnabled
    )
    
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(it, flag)
                viewModel.updateWallpaperUri(it.toString())
            } catch (e: Exception) {
                // Fallback if persistence fails or already granted
                viewModel.updateWallpaperUri(it.toString())
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(bottomPadding)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                "显示设置",
                style = textStyles.title.copy(fontSize = 24.sp)
            )
        }
        
        Spacer(Modifier.height(32.dp))
        
        // Wallpaper Config
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { ContinuousRoundedRectangle(16.dp) },
                    effects = { vibrancy(); blur(glassBlur.dp.toPx()) },
                    onDrawSurface = { drawRect(Color.White.copy(alpha = glassOpacity)) }
                )
                .padding(16.dp)
        ) {
            Column {
                Text(
                    "背景壁纸",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(16.dp))
                
                // 预设壁纸选择
                Text(
                    "预设壁纸",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
                Spacer(Modifier.height(8.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 壁纸1: wallpaper_liquid
                    PresetWallpaperItem(
                        resourceId = com.liquidglass.fluxhub.R.drawable.wallpaper_liquid,
                        isSelected = wallpaperUri == null || wallpaperUri == "preset:wallpaper_liquid",
                        onClick = {
                            viewModel.updateWallpaperUri(null) // 使用 null 表示默认壁纸
                        }
                    )
                    
                    // 壁纸2: wallpaper_light
                    PresetWallpaperItem(
                        resourceId = com.liquidglass.fluxhub.R.drawable.wallpaper_light,
                        isSelected = wallpaperUri == "preset:wallpaper_light",
                        onClick = {
                            viewModel.updateWallpaperUri("preset:wallpaper_light")
                        }
                    )
                }
                
                Spacer(Modifier.height(16.dp))
                
                // 自定义壁纸
                Text(
                    "自定义壁纸",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
                Spacer(Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LiquidButton(
                        onClick = { launcher.launch("image/*") },
                        backdrop = backdrop,
                        modifier = Modifier.height(44.dp).weight(1f),
                        tint = Color(0xFF007AFF)
                    ) {
                        Icon(Icons.Default.Image, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("选择图片", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(Modifier.width(12.dp))
                    
                    if (wallpaperUri != null) {
                        LiquidButton(
                            onClick = { viewModel.updateWallpaperUri(null) },
                            backdrop = backdrop,
                            modifier = Modifier.height(44.dp),
                            tint = Color.Red.copy(alpha = 0.6f)
                        ) {
                            Text("恢复默认", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                if (wallpaperUri != null && !wallpaperUri.startsWith("preset:")) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "当前使用自定义壁纸",
                        style = TextStyle(fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f))
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        
        // Glass Color Config
        Text(
            "液态玻璃颜色",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { ContinuousRoundedRectangle(16.dp) },
                    effects = { vibrancy(); blur(glassBlur.dp.toPx()) },
                    onDrawSurface = { drawRect(Color.White.copy(alpha = glassOpacity)) }
                )
                .padding(16.dp)
        ) {
            Column {
                Text(
                    "选择毛玻璃色调",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(12.dp))
                
                // 颜色选项
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 默认（白色）
                    ColorOption(
                        color = Color.White,
                        label = "默认",
                        isSelected = viewModel.glassColor == "default",
                        onClick = { viewModel.updateGlassColor("default") }
                    )
                    // 蓝色
                    ColorOption(
                        color = Color(0xFF007AFF),
                        label = "蓝",
                        isSelected = viewModel.glassColor == "007AFF",
                        onClick = { viewModel.updateGlassColor("007AFF") }
                    )
                    // 紫色
                    ColorOption(
                        color = Color(0xFFAF52DE),
                        label = "紫",
                        isSelected = viewModel.glassColor == "AF52DE",
                        onClick = { viewModel.updateGlassColor("AF52DE") }
                    )
                    // 绿色
                    ColorOption(
                        color = Color(0xFF34C759),
                        label = "绿",
                        isSelected = viewModel.glassColor == "34C759",
                        onClick = { viewModel.updateGlassColor("34C759") }
                    )
                    // 橙色
                    ColorOption(
                        color = Color(0xFFFF9500),
                        label = "橙",
                        isSelected = viewModel.glassColor == "FF9500",
                        onClick = { viewModel.updateGlassColor("FF9500") }
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Interaction Config
        Text(
            "交互与反馈",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { ContinuousRoundedRectangle(16.dp) },
                    effects = { vibrancy(); blur(glassBlur.dp.toPx()) },
                    onDrawSurface = { drawRect(Color.White.copy(alpha = glassOpacity)) }
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "震动反馈",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "操作时提供触觉反馈体验",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
                
                com.liquidglass.fluxhub.components.LiquidToggle(
                    selected = { viewModel.hapticFeedbackEnabled },
                    onSelect = { 
                        viewModel.updateHapticFeedbackEnabled(it)
                        if (it) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    },
                    backdrop = backdrop
                )
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        // Text Style Config
        Text(
            "字体样式",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { ContinuousRoundedRectangle(16.dp) },
                    effects = { vibrancy(); blur(glassBlur.dp.toPx()) },
                    onDrawSurface = { drawRect(Color.White.copy(alpha = glassOpacity)) }
                )
                .padding(16.dp)
        ) {
            Column {
                // 字体颜色选择
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "字体颜色",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "根据壁纸选择合适的文字颜色",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 白色按钮
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(
                                    width = if (viewModel.textColorMode == "white") 3.dp else 1.dp,
                                    color = if (viewModel.textColorMode == "white") Color(0xFF007AFF) else Color.Black.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                                .clickable { viewModel.updateTextColorMode("white") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (viewModel.textColorMode == "white") {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "已选择",
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        
                        // 黑色按钮
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.Black)
                                .border(
                                    width = if (viewModel.textColorMode == "black") 3.dp else 1.dp,
                                    color = if (viewModel.textColorMode == "black") Color(0xFF007AFF) else Color.White.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                                .clickable { viewModel.updateTextColorMode("black") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (viewModel.textColorMode == "black") {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "已选择",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                // 阴影开关
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "文字阴影",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "增强文字在复杂背景下的可读性",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                    
                    com.liquidglass.fluxhub.components.LiquidToggle(
                        selected = { viewModel.textShadowEnabled },
                        onSelect = { viewModel.updateTextShadowEnabled(it) },
                        backdrop = backdrop
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetWallpaperItem(
    resourceId: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) Color(0xFF007AFF) else Color.White.copy(alpha = 0.3f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
    ) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(resourceId),
            contentDescription = "壁纸",
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "已选择",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun ColorOption(
    color: Color,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(color)
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.3f),
                    shape = CircleShape
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "已选择",
                    tint = if (color == Color.White) Color.Black else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = TextStyle(fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
        )
    }
}
