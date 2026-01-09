package com.liquidglass.fluxhub.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
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

@Composable
fun DynamicIslandSettingsScreen(
    onBack: () -> Unit,
    viewModel: ChatViewModel,
    backdrop: Backdrop,
    bottomPadding: PaddingValues = PaddingValues(0.dp)
) {
    // 从 ViewModel 获取设置
    val dynamicIslandEnabled = viewModel.dynamicIslandEnabled
    val loginNotificationMode = viewModel.loginNotificationMode
    val dynamicIslandDuration = viewModel.dynamicIslandDuration
    val showTokenCount = viewModel.showTokenCount
    val showElapsedTime = viewModel.showElapsedTime
    val glassOpacity = viewModel.glassOpacity
    val glassBlur = viewModel.glassBlur

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
                "灵动岛设置",
                style = TextStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 4f)
                )
            )
        }
        
        Spacer(Modifier.height(32.dp))
        
        // 启用灵动岛
        SettingsCard(backdrop, glassOpacity, glassBlur) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("启用灵动岛", color = Color.White, fontWeight = FontWeight.Bold)
                    Text(
                        "在聊天时显示灵动岛状态指示器",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
                Switch(
                    checked = dynamicIslandEnabled,
                    onCheckedChange = { viewModel.updateDynamicIslandEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF34C759),
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color.Gray.copy(alpha = 0.4f)
                    )
                )
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        // 登录成功通知模式
        SettingsCard(backdrop, glassOpacity, glassBlur) {
            Column {
                Text(
                    "登录成功通知",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 首次触发
                    SelectableOption(
                        selected = loginNotificationMode == "first",
                        onClick = { viewModel.updateLoginNotificationMode("first") },
                        label = "仅首次",
                        description = "只在首次进入时显示",
                        backdrop = backdrop,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // 每次触发
                    SelectableOption(
                        selected = loginNotificationMode == "every",
                        onClick = { viewModel.updateLoginNotificationMode("every") },
                        label = "每次启动",
                        description = "每次打开应用都显示",
                        backdrop = backdrop,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        // 显示时长
        SettingsCard(backdrop, glassOpacity, glassBlur) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("显示时长", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("${dynamicIslandDuration}秒", color = Color(0xFF007AFF), fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                LiquidSlider(
                    value = { dynamicIslandDuration.toFloat() },
                    onValueChange = { viewModel.updateDynamicIslandDuration(it.toInt()) },
                    valueRange = 1f..10f,
                    visibilityThreshold = 0.5f,
                    backdrop = backdrop,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "完成/失败动画显示的时长",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        // 显示选项
        SettingsCard(backdrop, glassOpacity, glassBlur) {
            Column {
                Text(
                    "显示内容",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(12.dp))
                
                // Token 计数
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("显示 Token 计数", color = Color.White)
                    Switch(
                        checked = showTokenCount,
                        onCheckedChange = { viewModel.updateShowTokenCount(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF34C759),
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color.Gray.copy(alpha = 0.4f)
                        )
                    )
                }
                
                Spacer(Modifier.height(8.dp))
                
                // 耗时
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("显示耗时", color = Color.White)
                    Switch(
                        checked = showElapsedTime,
                        onCheckedChange = { viewModel.updateShowElapsedTime(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF34C759),
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color.Gray.copy(alpha = 0.4f)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    backdrop: Backdrop,
    glassOpacity: Float,
    glassBlur: Float,
    content: @Composable ColumnScope.() -> Unit
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
            .padding(16.dp)
    ) {
        Column(content = content)
    }
}

@Composable
private fun SelectableOption(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    description: String,
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { ContinuousRoundedRectangle(12.dp) },
                effects = { vibrancy(); blur(8f.dp.toPx()) },
                onDrawSurface = {
                    drawRect(
                        if (selected) Color(0xFF34C759).copy(alpha = 0.3f)
                        else Color.White.copy(alpha = 0.1f)
                    )
                }
            )
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    description,
                    style = TextStyle(fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                )
            }
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color(0xFF34C759),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
