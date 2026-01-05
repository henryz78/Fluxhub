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
import com.kyant.capsule.ContinuousRoundedRectangle
import com.liquidglass.fluxhub.components.LiquidButton

@Composable
fun DisplaySettingsScreen(
    onBack: () -> Unit,
    viewModel: ChatViewModel,
    backdrop: Backdrop,
    bottomPadding: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current
    val themeMode = viewModel.themeMode
    val wallpaperUri = viewModel.wallpaperUri
    
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
                style = TextStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 4f)
                )
            )
        }
        
        Spacer(Modifier.height(32.dp))
        
        // Theme Config
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { ContinuousRoundedRectangle(16.dp) },
                    effects = { vibrancy() },
                    onDrawSurface = { drawRect(Color.White.copy(alpha = 0.1f)) }
                )
                .padding(16.dp)
        ) {
            Column {
                Text(
                    "主题模式",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeOption(
                        title = "跟随系统",
                        isSelected = themeMode == "system",
                        onClick = { viewModel.updateThemeMode("system") },
                        backdrop = backdrop,
                        modifier = Modifier.weight(1f)
                    )
                    ThemeOption(
                        title = "浅色",
                        isSelected = themeMode == "light",
                        onClick = { viewModel.updateThemeMode("light") },
                        backdrop = backdrop,
                        modifier = Modifier.weight(1f)
                    )
                    ThemeOption(
                        title = "深色",
                        isSelected = themeMode == "dark",
                        onClick = { viewModel.updateThemeMode("dark") },
                        backdrop = backdrop,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Wallpaper Config
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { ContinuousRoundedRectangle(16.dp) },
                    effects = { vibrancy() },
                    onDrawSurface = { drawRect(Color.White.copy(alpha = 0.1f)) }
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
                            modifier = Modifier.height(44.dp), // .weight(1f) optional
                            tint = Color.Red.copy(alpha = 0.6f)
                        ) {
                            Text("恢复默认", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                if (wallpaperUri != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "当前使用自定义壁纸",
                        style = TextStyle(fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f))
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeOption(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    LiquidButton(
        onClick = onClick,
        backdrop = backdrop,
        modifier = modifier.height(40.dp),
        tint = if (isSelected) Color(0xFF007AFF) else Color.White.copy(alpha = 0.1f)
    ) {
        if (isSelected) {
            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(
            title,
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 13.sp
        )
    }
}
