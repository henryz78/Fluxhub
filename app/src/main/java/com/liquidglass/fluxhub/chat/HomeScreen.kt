package com.liquidglass.fluxhub.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.composables.icons.lucide.Lightbulb
import com.composables.icons.lucide.Zap
import androidx.compose.foundation.clickable
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * 首页 - 欢迎页面
 */
@Composable
fun HomeScreen(
    backdrop: Backdrop,
    bottomPadding: PaddingValues = PaddingValues(0.dp),
    onNavigateToChat: () -> Unit,
    onQuickPrompt: (String) -> Unit = { },
    viewModel: ChatViewModel = viewModel()
) {
    // 统计数据
    val conversationCount = viewModel.conversations.size
    val messageCount = viewModel.messages.size
    val recentConversations = viewModel.conversations.take(3)
    
    // 快捷提示词
    val quickPrompts = listOf(
        "帮我写一段代码",
        "解释一个概念",
        "帮我翻译",
        "写一篇文章"
    )
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottomPadding)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 32.dp, bottom = 16.dp)
    ) {
        // Logo 和欢迎语
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Lucide.Sparkles,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Color(0xFFFFD700)
                )
                Spacer(Modifier.height(12.dp))
                BasicText(
                    text = "FluxHub",
                    style = TextStyle(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                )
                Spacer(Modifier.height(4.dp))
                BasicText(
                    text = "与 AI 对话，探索无限可能",
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                )
            }
        }
        
        // 统计卡片
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 对话数量
                StatCard(
                    icon = Lucide.MessageCircle,
                    label = "对话",
                    value = "$conversationCount",
                    color = Color(0xFF007AFF),
                    backdrop = backdrop,
                    modifier = Modifier.weight(1f)
                )
                // 消息数量
                StatCard(
                    icon = Lucide.Zap,
                    label = "消息",
                    value = "$messageCount",
                    color = Color(0xFF34C759),
                    backdrop = backdrop,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        // 开始对话按钮
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { ContinuousRoundedRectangle(20.dp) },
                        effects = {
                            vibrancy()
                            blur(4.dp.toPx())
                        },
                        onDrawSurface = {
                            drawRect(Color(0xFF007AFF).copy(alpha = 0.3f))
                        }
                    )
                    .clickable { onNavigateToChat() }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Lucide.MessageCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.White
                    )
                    BasicText(
                        text = "开始新对话",
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    )
                }
            }
        }
        
        // 快捷提示词
        item {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Lucide.Lightbulb,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.width(8.dp))
                    BasicText(
                        text = "快捷提示",
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    )
                }
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(quickPrompts) { prompt ->
                        Box(
                            modifier = Modifier
                                .drawBackdrop(
                                    backdrop = backdrop,
                                    shape = { ContinuousRoundedRectangle(12.dp) },
                                    effects = {
                                        vibrancy()
                                        blur(4.dp.toPx())
                                    },
                                    onDrawSurface = {
                                        drawRect(Color.White.copy(alpha = 0.15f))
                                    }
                                )
                                .clickable { onQuickPrompt(prompt) }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            BasicText(
                                text = prompt,
                                style = TextStyle(
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }
        
        // 最近对话
        if (recentConversations.isNotEmpty()) {
            item {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.History,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                        Spacer(Modifier.width(8.dp))
                        BasicText(
                            text = "最近对话",
                            style = TextStyle(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        )
                    }
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        recentConversations.forEach { conversation ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .drawBackdrop(
                                        backdrop = backdrop,
                                        shape = { ContinuousRoundedRectangle(12.dp) },
                                        effects = {
                                            vibrancy()
                                            blur(4.dp.toPx())
                                        },
                                        onDrawSurface = {
                                            drawRect(Color.White.copy(alpha = 0.1f))
                                        }
                                    )
                                    .clickable { 
                                        viewModel.switchConversation(conversation.id)
                                        onNavigateToChat()
                                    }
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = conversation.title,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color,
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { ContinuousRoundedRectangle(16.dp) },
                effects = {
                    vibrancy()
                    blur(4.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(color.copy(alpha = 0.2f))
                }
            )
            .padding(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = color
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}
