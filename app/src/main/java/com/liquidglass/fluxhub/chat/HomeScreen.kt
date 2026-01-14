package com.liquidglass.fluxhub.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.capsule.ContinuousRoundedRectangle
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MessageCircle
import com.composables.icons.lucide.Sparkles
import com.composables.icons.lucide.History
import com.composables.icons.lucide.Zap
import com.composables.icons.lucide.Settings
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Bot
import androidx.compose.foundation.clickable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liquidglass.fluxhub.components.LiquidButton
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 首页 - 增强版
 */
@Composable
fun HomeScreen(
    backdrop: Backdrop,
    bottomPadding: PaddingValues = PaddingValues(0.dp),
    onNavigateToChat: () -> Unit,
    onQuickPrompt: (String) -> Unit = { },
    viewModel: ChatViewModel = viewModel()
) {
    // 最近对话
    val recentConversations = viewModel.conversations.take(5)
    
    // 时间相关
    val calendar = remember { Calendar.getInstance() }
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 5..11 -> "早上好"
        in 12..18 -> "下午好"
        else -> "晚上好"
    }
    val dateFormat = SimpleDateFormat("M月d日 EEEE", Locale.CHINA)
    val dateString = dateFormat.format(calendar.time)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottomPadding),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // 1. 动态问候头部 & 日期
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp)
            ) {
                // 日期小标题
                BasicText(
                    text = dateString.uppercase(),
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.6f),
                        letterSpacing = 1.sp,
                        shadow = Shadow(color = Color.Black.copy(alpha = 0.3f), blurRadius = 2f)
                    )
                )
                Spacer(Modifier.height(8.dp))
                // 大标题问候
                BasicText(
                    text = greeting,
                    style = TextStyle(
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        shadow = Shadow(color = Color.Black.copy(alpha = 0.2f), blurRadius = 8f, offset = Offset(0f, 4f))
                    )
                )
                BasicText(
                    text = "准备好开始新的对话了吗？",
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.8f),
                        shadow = Shadow(color = Color.Black.copy(alpha = 0.3f), blurRadius = 4f)
                    ),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        
        // 2. 快捷操作网格 (Quick Actions)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 主按钮：开启新对话 (占据一半宽度，高度较大)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(140.dp)
                            .drawBackdrop(
                                backdrop = backdrop,
                                shape = { ContinuousRoundedRectangle(24.dp) },
                                effects = {
                                    vibrancy()
                                    blur(10.dp.toPx())
                                },
                                onDrawSurface = {
                                    drawRect(Color(0xFF007AFF).copy(alpha = 0.85f)) // 蓝色主色调
                                }
                            )
                            .clickable { 
                                viewModel.createNewConversation()
                                onNavigateToChat()
                            }
                            .padding(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .drawBackdrop(
                                        backdrop = backdrop,
                                        shape = { androidx.compose.foundation.shape.CircleShape },
                                        effects = { blur(0f) }, // clear
                                        onDrawSurface = { drawRect(Color.White.copy(alpha = 0.2f)) }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Lucide.Plus, null, tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                            
                            Column {
                                Text(
                                    text = "新对话",
                                    style = TextStyle(
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "立即开始",
                                    style = TextStyle(
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    }
                    
                    // 右侧两小块
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(140.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 助手中心
                        QuickActionCard(
                            title = "助手中心",
                            icon = Lucide.Bot,
                            color = Color(0xFFAF52DE), // 紫色
                            backdrop = backdrop,
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            onClick = {
                                // 导航到助手选择 (通过 ChatTab 的子页面)
                                viewModel.createNewConversation() // 先创建，然后... 
                                // 这里简化逻辑，先去聊天页
                                onNavigateToChat()
                                // 实际上应该触发 onNavigateToAssistantSelection，但这里回调未传递
                                // 暂由 ChatScreen 内部处理助手切换
                            }
                        )
                        
                        // 快捷提示
                        QuickActionCard(
                            title = "灵动角色",
                            icon = Lucide.Sparkles,
                            color = Color(0xFFFF9500), // 橙色
                            backdrop = backdrop,
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            onClick = {
                                // 同样，这里作为展示，简单跳到聊天
                                onNavigateToChat() 
                            }
                        )
                    }
                }
            }
        }
        
        item { Spacer(Modifier.height(24.dp)) }

        // 3. 最近会话 (横向卡片)
        if (recentConversations.isNotEmpty()) {
            item {
                Column {
                    PaddingLabel(text = "最近会话", icon = Lucide.History)
                    
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(recentConversations) { conversation ->
                            Box(
                                modifier = Modifier
                                    .width(200.dp)
                                    .height(110.dp)
                                    .drawBackdrop(
                                        backdrop = backdrop,
                                        shape = { ContinuousRoundedRectangle(20.dp) },
                                        effects = {
                                            vibrancy()
                                            blur(8.dp.toPx())
                                        },
                                        onDrawSurface = {
                                            drawRect(Color.White.copy(alpha = 0.12f))
                                        }
                                    )
                                    .clickable {
                                        viewModel.switchConversation(conversation.id)
                                        onNavigateToChat()
                                    }
                                    .padding(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Lucide.MessageCircle, 
                                            null, 
                                            tint = Color.White.copy(alpha = 0.7f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        // 简单的时间格式化
                                        val timeDiff = System.currentTimeMillis() - conversation.updatedAt
                                        val timeText = when {
                                            timeDiff < 60000 -> "刚刚"
                                            timeDiff < 3600000 -> "${timeDiff / 60000}分钟前"
                                            timeDiff < 86400000 -> "${timeDiff / 3600000}小时前"
                                            else -> "${timeDiff / 86400000}天前"
                                        }
                                        Text(
                                            text = timeText,
                                            style = TextStyle(
                                                color = Color.White.copy(alpha = 0.5f),
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                    
                                    Text(
                                        text = conversation.title,
                                        style = TextStyle(
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium,
                                            shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 4f)
                                        ),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }

        // 4. 探索 (快捷提示词)
        item {
            Column {
                PaddingLabel(text = "探索更多", icon = Lucide.Zap)
                
                // 瀑布流/网格布局 (简单模拟)
                val prompts = listOf(
                    "帮我写一段 Python 代码", "解释量子纠缠",
                    "写一首关于春天的诗", "制定健身计划",
                    "翻译这段文字", "分析这个商业案例"
                )
                
                // 第一行
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(prompts.take(3)) { prompt ->
                        QuickPromptChip(prompt, backdrop) { onQuickPrompt(prompt) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                // 第二行
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(prompts.takeLast(3)) { prompt ->
                        QuickPromptChip(prompt, backdrop) { onQuickPrompt(prompt) }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { ContinuousRoundedRectangle(20.dp) },
                effects = {
                    vibrancy()
                    blur(8.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(color.copy(alpha = 0.6f))
                }
            )
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(4.dp))
            Text(
                text = title,
                style = TextStyle(
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

@Composable
private fun PaddingLabel(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 24.dp, bottom = 12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = Color.White.copy(alpha = 0.7f)
        )
        Spacer(Modifier.width(8.dp))
        BasicText(
            text = text,
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.9f),
                shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 4f)
            )
        )
    }
}

@Composable
private fun QuickPromptChip(text: String, backdrop: Backdrop, onClick: () -> Unit) {
    LiquidButton(
        onClick = onClick,
        backdrop = backdrop,
        shape = { ContinuousRoundedRectangle(12.dp) },
        tint = Color.White.copy(alpha = 0.1f),
        padding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        BasicText(
            text = text,
            style = TextStyle(
                fontSize = 13.sp,
                color = Color.White,
                shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 3f)
            )
        )
    }
}
