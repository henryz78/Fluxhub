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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
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
import com.composables.icons.lucide.Bell
import androidx.compose.foundation.clickable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liquidglass.fluxhub.components.LiquidButton
import com.liquidglass.fluxhub.components.PersonaCard
import com.liquidglass.fluxhub.data.Personas

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
    // 最近对话（用于显示）
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
        contentPadding = PaddingValues(top = 32.dp, bottom = 100.dp) // 增加底部空间避免与导航栏重叠
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
        
        // 更新公告
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { ContinuousRoundedRectangle(16.dp) },
                        effects = {
                            vibrancy()
                            blur(4.dp.toPx())
                        },
                        onDrawSurface = {
                            drawRect(Color(0xFF34C759).copy(alpha = 0.2f))
                        }
                    )
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.Bell,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF34C759)
                        )
                        Spacer(Modifier.width(8.dp))
                        BasicText(
                            text = "更新公告",
                            style = TextStyle(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                    BasicText(
                        text = "v1.0.1 · 2026.01.05",
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    )
                    Spacer(Modifier.height(4.dp))
                    BasicText(
                        text = "• 新增顶栏助手快捷切换\n• 优化助手配置（移除模型绑定）\n• 对话记录按助手完全隔离\n• 简化显示设置界面\n• 新增首次启动用户协议",
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            lineHeight = 18.sp,
                            shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 4f)
                        )
                    )
                }
            }
        }
        
        // 灵动角色 (Liquid Personas) 轮播
        item {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp, top = 8.dp)
                ) {
                    Icon(
                        imageVector = Lucide.Sparkles,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.width(8.dp))
                    BasicText(
                        text = "灵动角色",
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.9f),
                            shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 4f)
                        )
                    )
                }
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(Personas.all) { persona ->
                        PersonaCard(
                            persona = persona,
                            backdrop = backdrop,
                            onClick = {
                                viewModel.createNewConversation(persona.systemPrompt, persona.name)
                                onNavigateToChat()
                            },
                            modifier = Modifier.width(280.dp).height(160.dp)
                        )
                    }
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
                            color = Color.White.copy(alpha = 0.9f),
                            shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 4f)
                        )
                    )
                }
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(quickPrompts) { prompt ->
                        LiquidButton(
                            onClick = { onQuickPrompt(prompt) },
                            backdrop = backdrop,
                            shape = { ContinuousRoundedRectangle(12.dp) },
                            tint = Color.White,
                            padding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            BasicText(
                                text = prompt,
                                style = TextStyle(
                                    fontSize = 13.sp,
                                    color = Color.White,
                                    shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 4f)
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
                                color = Color.White.copy(alpha = 0.9f),
                                shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 4f)
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
                                    style = TextStyle(
                                        shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 4f)
                                    ),
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
