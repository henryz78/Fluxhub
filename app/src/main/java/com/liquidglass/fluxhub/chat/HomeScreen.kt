package com.liquidglass.fluxhub.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.composables.icons.lucide.*
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.capsule.ContinuousRoundedRectangle
import com.liquidglass.fluxhub.components.LiquidButton
import com.liquidglass.fluxhub.components.PersonaCard
import com.liquidglass.fluxhub.data.Personas
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.liquidglass.fluxhub.ui.theme.GlassTypography

/**
 * 首页 - 增强版
 */
@Composable
fun HomeScreen(
    backdrop: Backdrop,
    bottomPadding: PaddingValues = PaddingValues(0.dp),
    onNavigateToChat: () -> Unit,
    onNavigateToAssistantSelection: () -> Unit,
    onQuickPrompt: (String) -> Unit = { },
    viewModel: ChatViewModel = viewModel()
) {
    // 最近对话
    val recentConversations = viewModel.conversations.take(5)
    
    // 列表状态 (用于滚动)
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val scope = rememberCoroutineScope()
    
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

    // 统计弹窗状态
    var showStatsDialog by remember { mutableStateOf(false) }
    // 更新日志弹窗状态
    var showChangelogDialog by remember { mutableStateOf(false) }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(bottomPadding),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // 1. 动态问候头部 & 日期 & 版本号
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // 日期小标题
                    BasicText(
                        text = dateString.uppercase(),
                        style = GlassTypography.label
                    )
                    Spacer(Modifier.height(8.dp))
                    // 大标题问候
                    BasicText(
                        text = greeting,
                        style = GlassTypography.displayLarge
                    )
                    BasicText(
                        text = "准备好开始新的对话了吗？",
                        style = GlassTypography.bodyLarge.copy(color = Color.White.copy(alpha = 0.8f)),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                // 版本号 (右上角)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { ContinuousRoundedRectangle(12.dp) },
                            effects = { blur(10.dp.toPx()) },
                            onDrawSurface = { drawRect(Color.White.copy(alpha = 0.1f)) }
                        )
                        .clickable { showChangelogDialog = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "v1.0.5",
                        style = TextStyle(
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
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
                    // 主按钮：开启新对话
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
                                    drawRect(Color(0xFF007AFF).copy(alpha = 0.85f))
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
                                        effects = { blur(0f) },
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
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "开启探索旅程",
                                    style = TextStyle(
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 13.sp
                                    )
                                )
                            }
                        }
                    }
                    
                    // 右侧两个小卡片
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(140.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 随机一聊
                        QuickActionCard(
                            title = "随机一聊",
                            subtitle = "发现惊喜角色",
                            icon = Lucide.Sparkles,
                            color = Color(0xFFAF52DE),
                            backdrop = backdrop,
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            onClick = {
                                val randomPersona = Personas.all.random()
                                viewModel.createNewConversation(randomPersona.systemPrompt, randomPersona.name)
                                onNavigateToChat()
                            }
                        )
                        
                        // 今日统计
                        val todayStart = remember {
                            Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.timeInMillis
                        }
                        val todayConversations = viewModel.conversations.count { it.createdAt >= todayStart }
                        val totalConversations = viewModel.conversations.size
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .drawBackdrop(
                                    backdrop = backdrop,
                                    shape = { ContinuousRoundedRectangle(16.dp) },
                                    effects = {
                                        vibrancy()
                                        blur(8.dp.toPx())
                                    },
                                    onDrawSurface = {
                                        drawRect(Color(0xFF34C759).copy(alpha = 0.75f))
                                    }
                                )
                                .clickable { showStatsDialog = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "今日统计",
                                        style = TextStyle(
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        text = "今日$todayConversations · 共$totalConversations",
                                        style = TextStyle(
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontSize = 9.sp
                                        ),
                                        maxLines = 1
                                    )
                                }
                                Icon(
                                    Lucide.Zap,
                                    null,
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        
        item { Spacer(Modifier.height(24.dp)) }

        // 3. 灵动角色 (Liquid Personas) 轮播
        item {
            Column {
                PaddingLabel(text = "灵动角色", icon = Lucide.Sparkles)
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    items(Personas.all) { persona ->
                        PersonaCard(
                            persona = persona,
                            backdrop = backdrop,
                            onClick = {
                                viewModel.createNewConversation(persona.systemPrompt, persona.name)
                                onNavigateToChat()
                            },
                            modifier = Modifier.width(200.dp).height(130.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        // 4. 探索 (快捷提示词)
        item {
            Column {
                PaddingLabel(text = "探索更多", icon = Lucide.Zap)
                
                val prompts = listOf(
                    "帮我写一段 Python 代码", "解释量子纠缠",
                    "写一首关于春天的诗", "制定健身计划",
                    "翻译这段文字", "分析这个商业案例",
                    "推荐一部科幻电影", "如何制作拿铁咖啡"
                )
                
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(prompts) { prompt ->
                        QuickPromptChip(prompt, backdrop) { onQuickPrompt(prompt) }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        // 5. 最近会话
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
        }
    }

    // 统计弹窗
    if (showStatsDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showStatsDialog = false }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { ContinuousRoundedRectangle(28.dp) },
                        effects = {
                            vibrancy()
                            blur(20.dp.toPx())
                        },
                        onDrawSurface = {
                            drawRect(Color.White.copy(0.15f))
                        }
                    )
                    .padding(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Text(
                        "我的数据",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    
                    // 统计概览圆环（显示今日判比例）
                    val todayStart = remember {
                        Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                    }
                    val todayCount = viewModel.conversations.count { it.createdAt >= todayStart }
                    val totalCount = viewModel.conversations.size
                    val progressRatio = if (totalCount > 0) (todayCount.toFloat() / totalCount.coerceAtLeast(1)).coerceIn(0f, 1f) else 0f
                    
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { progressRatio },
                            modifier = Modifier.size(100.dp),
                            color = Color(0xFF34C759),
                            trackColor = Color.White.copy(alpha = 0.1f),
                            strokeWidth = 8.dp,
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "$todayCount",
                                style = TextStyle(
                                    color = Color.White,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                "今日会话",
                                style = TextStyle(
                                    color = Color.White.copy(0.6f),
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                    
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatCard(
                                icon = Lucide.MessageCircle,
                                label = "总会话",
                                value = "$totalCount",
                                backdrop = backdrop,
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                icon = Lucide.Zap,
                                label = "今日互动",
                                value = "$todayCount",
                                backdrop = backdrop,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatCard(
                                icon = Lucide.Bot,
                                label = "当前大脑",
                                value = viewModel.model.ifBlank { "AUTO" }.uppercase().take(6),
                                backdrop = backdrop,
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                icon = Lucide.Award,
                                label = "探索成就",
                                value = "初级向导",
                                backdrop = backdrop,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    LiquidButton(
                        onClick = { showStatsDialog = false },
                        backdrop = backdrop,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        isInteractive = true,
                        tint = Color(0xFF007AFF).copy(0.8f)
                    ) {
                        Text("关闭", color = Color.White, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }

    // 更新日志弹窗
    if (showChangelogDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showChangelogDialog = false }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { ContinuousRoundedRectangle(28.dp) },
                        effects = {
                            vibrancy()
                            blur(20.dp.toPx())
                        },
                        onDrawSurface = {
                            drawRect(Color.White.copy(0.15f))
                        }
                    )
                    .padding(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Column {
                        Text(
                            "版本更新 v1.0.5",
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            "2026-01-13",
                            style = TextStyle(
                                color = Color.White.copy(0.5f),
                                fontSize = 12.sp
                            )
                        )
                    }
                    
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        val updates = listOf(
                            "✨ 全新主页：随机一聊与数据仪表盘",
                            "🎨 视觉升级：毛玻璃质感与动态背景",
                            "💬 体验优化：聊天按钮尺寸与可见性调整",
                            "⚡ 性能提升：列表滚动更流畅"
                        )
                        updates.forEach { update ->
                            Row(verticalAlignment = Alignment.Top) {
                                Text(
                                    "• ",
                                    style = TextStyle(color = Color(0xFFAF52DE), fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    update,
                                    style = TextStyle(
                                        color = Color.White.copy(0.9f),
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp
                                    )
                                )
                            }
                        }
                    }
                    
                    LiquidButton(
                        onClick = { showChangelogDialog = false },
                        backdrop = backdrop,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        isInteractive = true,
                        tint = Color(0xFF007AFF).copy(0.8f)
                    ) {
                        Text("关闭", color = Color.White, fontWeight = FontWeight.Medium)
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
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(80.dp)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { ContinuousRoundedRectangle(16.dp) },
                effects = { blur(0f) },
                onDrawSurface = { drawRect(Color.White.copy(0.08f)) }
            )
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(icon, null, tint = Color.White.copy(0.6f), modifier = Modifier.size(16.dp))
            Column {
                Text(
                    value,
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
                Text(
                    label,
                    style = TextStyle(
                        color = Color.White.copy(0.5f),
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String? = null,
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
                shape = { ContinuousRoundedRectangle(16.dp) },
                effects = {
                    vibrancy()
                    blur(8.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(color.copy(alpha = 0.6f))
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        // 水平布局：图标在左，文字在右
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = TextStyle(
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 9.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
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
            style = GlassTypography.bodyMedium
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
