package com.liquidglass.fluxhub.chat

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.ClipEntry
import android.content.ClipData
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import com.liquidglass.fluxhub.components.LiquidButton
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.RoundedCornerShape
import com.liquidglass.fluxhub.ui.components.richtext.MarkdownBlock
import com.liquidglass.fluxhub.ui.components.richtext.ProvideHighlighter
import com.liquidglass.fluxhub.ui.components.message.MessageAvatar
import com.liquidglass.fluxhub.ui.components.message.MessageActionButtons
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.liquidglass.fluxhub.ui.components.message.MessageActionsSheet
import com.liquidglass.fluxhub.ui.components.message.ThinkingComponent
import com.composables.icons.lucide.*
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.drawBehind
import com.liquidglass.fluxhub.utils.ImeLazyListAutoScroller

private const val TAG = "ChatScreen"

@Composable
fun TypingIndicator(backdrop: Backdrop) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val infiniteTransition = rememberInfiniteTransition(label = "typing")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = index * 200, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "alpha"
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { ContinuousCapsule },
                        effects = { vibrancy() },
                        onDrawSurface = {
                            drawRect(Color.White.copy(alpha = alpha))
                        }
                    )
            )
        }
    }
}
@Composable
fun ChatScreen(
    backdrop: Backdrop,
    bottomPadding: PaddingValues,
    onNavigateToSettings: () -> Unit = {},
    viewModel: ChatViewModel = viewModel(),
    listState: LazyListState = rememberLazyListState(),
    drawerState: DrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
) {
    var inputText by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    
    // 追踪是否有顶部按钮正在被交互，以禁用侧边栏手势
    var isInteractingWithButtons by remember { mutableStateOf(false) }
    // 检测键盘可见性
    val isKeyboardVisible = rememberIsKeyboardVisible()
    
    // 获取流式消息的状态（用于检测流式更新）
    val isStreaming = viewModel.messages.any { it.isStreaming }

    // 智能触底判定扩展逻辑 (参考 RikkaHub)
    fun LazyListState.isAtBottom(): Boolean {
        val lastItem = layoutInfo.visibleItemsInfo.lastOrNull() ?: return true
        return lastItem.index >= layoutInfo.totalItemsCount - 2 &&
               (lastItem.offset + lastItem.size <= layoutInfo.viewportEndOffset + lastItem.size * 0.15 + 32)
    }

    // 自动跟随键盘滚动 (参考 RikkaHub)
    ImeLazyListAutoScroller(lazyListState = listState)

    // AI 正在说话或思考时的全时吸附滚动 (参考 RikkaHub)
    val loadingState by rememberUpdatedState(isStreaming || viewModel.isLoading)
    val messagesUpdated by rememberUpdatedState(viewModel.messages)
    
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }.collect { visibleItemsInfo ->
            if (!listState.isScrollInProgress && loadingState) {
                if (listState.isAtBottom() && messagesUpdated.isNotEmpty()) {
                    // 滚动到 lastIndex + 10 确保到达底部占位符 (参考 RikkaHub)
                    listState.requestScrollToItem(messagesUpdated.lastIndex + 10)
                }
            }
        }
    }
    
    // 新消息时滚动到底部
    LaunchedEffect(viewModel.messages.size) {
        if (viewModel.messages.isNotEmpty()) {
            kotlinx.coroutines.delay(50) // 等待布局完成
            listState.requestScrollToItem(viewModel.messages.lastIndex + 10)
        }
    }
    
    // 发送消息的处理函数
    val onSendMessage: () -> Unit = {
        if (inputText.isNotBlank()) {
            viewModel.sendMessage(inputText)
            inputText = ""
            keyboardController?.hide()
        }
    }
    
    ProvideHighlighter {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ConversationDrawerContent(
                    conversations = viewModel.conversations,
                    currentConversationId = viewModel.currentConversationId,
                    backdrop = backdrop,
                    onSelectConversation = { id ->
                        viewModel.switchConversation(id)
                        scope.launch { drawerState.close() }
                    },
                    onDeleteConversation = { id ->
                        viewModel.deleteConversation(id)
                    },
                    onNewConversation = {
                        viewModel.createNewConversation()
                        scope.launch { drawerState.close() }
                    },
                    onInteractionChanged = { isInteractingWithButtons = it }
                )
            },
            gesturesEnabled = !isInteractingWithButtons,
            modifier = Modifier.fillMaxSize()
        ) {
            LiquidGlassChatContent(
                viewModel = viewModel,
                inputText = inputText,
                onInputTextChange = { inputText = it },
                listState = listState,
                onNavigateToSettings = onNavigateToSettings,
                onOpenDrawer = { scope.launch { drawerState.open() } },
                onSend = onSendMessage,
                backdrop = backdrop,
                bottomPadding = bottomPadding,
                scope = scope,
                onInteractionChanged = { isInteractingWithButtons = it }
            )
        }
    }
}

@Composable
private fun LiquidGlassChatContent(
    viewModel: ChatViewModel,
    inputText: String,
    onInputTextChange: (String) -> Unit,
    listState: LazyListState,
    onNavigateToSettings: () -> Unit,
    onOpenDrawer: () -> Unit,
    onSend: () -> Unit,
    backdrop: Backdrop,
    bottomPadding: PaddingValues,
    scope: kotlinx.coroutines.CoroutineScope,
    onInteractionChanged: (Boolean) -> Unit
) {
    // 消息操作菜单状态
    var selectedMessageForMenu by remember { mutableStateOf<UiMessage?>(null) }
    val clipboardManager = LocalClipboard.current

    // 主内容区域
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(bottomPadding) // 避开底部导航栏
    ) {
        // Top Bar - 椭圆形胶囊状
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { ContinuousCapsule },
                        effects = {
                            vibrancy()
                            blur(4f.dp.toPx())
                            lens(16f.dp.toPx(), 32f.dp.toPx())
                        },
                        highlight = { Highlight.Plain },
                        onDrawSurface = {
                            drawRect(Color.White.copy(alpha = 0.15f))
                        }
                    )
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 菜单按钮（打开会话列表）
                LiquidButton(
                    onClick = onOpenDrawer,
                    backdrop = backdrop,
                    modifier = Modifier.size(44.dp),
                    isInteractive = true,
                    padding = PaddingValues(0.dp),
                    onPressed = onInteractionChanged
                ) {
                    Icon(
                        imageVector = Lucide.Menu,
                        contentDescription = "会话列表",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // 会话标题与模型信息
                Column(
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                ) {
                    BasicText(
                        text = viewModel.currentConversationTitle,
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1
                    )
                    if (viewModel.model.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val aiIcon = remember(viewModel.model) {
                                val name = viewModel.model.lowercase()
                                when {
                                    name.contains("gpt") || name.contains("openai") -> Lucide.Zap
                                    name.contains("claude") -> Lucide.Sparkles
                                    name.contains("gemini") -> Lucide.Star
                                    name.contains("deepseek") -> Lucide.Compass
                                    else -> Lucide.Bot
                                }
                            }
                            Icon(
                                imageVector = aiIcon,
                                contentDescription = null,
                                modifier = Modifier.size(10.dp),
                                tint = Color.White.copy(alpha = 0.6f)
                            )
                            Spacer(Modifier.width(4.dp))
                            BasicText(
                                text = viewModel.model,
                                style = TextStyle(
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 10.sp
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }
                
                // 新建会话按钮（右上角）
                LiquidButton(
                    onClick = { viewModel.createNewConversation() },
                    backdrop = backdrop,
                    modifier = Modifier.size(44.dp),
                    isInteractive = true,
                    padding = PaddingValues(0.dp),
                    onPressed = onInteractionChanged
                ) {
                    Icon(
                        imageVector = Lucide.Plus,
                        contentDescription = "新建会话",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        
        // Messages - 占据剩余空间
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(
                top = 8.dp, 
                bottom = 8.dp, 
                start = 8.dp, 
                end = 8.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(viewModel.messages, key = { it.id }) { message ->
                LiquidGlassChatBubble(
                    message = message,
                    backdrop = backdrop,
                    viewModel = viewModel,
                    onLongClick = { selectedMessageForMenu = message }
                )
            }
            
            // 底部占位符：确保可以正确滚动到最底部 (参考 RikkaHub)
            item("scroll_bottom_spacer") {
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                )
            }
        }

        // 消息长按菜单
        selectedMessageForMenu?.let { message ->
            MessageActionsSheet(
                content = message.content,
                isUser = message.role == "user",
                onDismiss = { selectedMessageForMenu = null },
                onCopy = {
                    scope.launch {
                        val clipData = ClipData.newPlainText("message", message.content)
                        clipboardManager.setClipEntry(ClipEntry(clipData))
                    }
                },
                onRegenerate = { viewModel.regenerate(message.id) },
                onDelete = { viewModel.deleteMessage(message.id) },
                onSpeak = { viewModel.speak(message.content) }
            )
        }

        
        // Error message with animation
        AnimatedVisibility(
            visible = viewModel.showError,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it }
        ) {
            viewModel.error?.let { errorMsg ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { ContinuousCapsule },
                            effects = {
                                vibrancy()
                                blur(4f.dp.toPx())
                            },
                            onDrawSurface = {
                                drawRect(Color(0xFFFF3B30).copy(alpha = 0.3f))
                            }
                        )
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    BasicText(
                        text = errorMsg,
                        style = TextStyle(Color.White, 14.sp)
                    )
                }
            }
        }
        
        // Input with glass effect
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding() // 保持键盘适配
        ) {
            LiquidGlassChatInputBar(
                text = inputText,
                onTextChange = onInputTextChange,
                onSend = onSend,
                onStop = { viewModel.stopStreaming() },
                isLoading = viewModel.isLoading,
                backdrop = backdrop
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun LiquidGlassChatBubble(
    message: UiMessage,
    backdrop: Backdrop,
    viewModel: ChatViewModel,
    onLongClick: () -> Unit
) {
    val isUser = message.role == "user"
    val bubbleShape = ContinuousRoundedRectangle(20.dp)
    val tintColor = if (isUser) Color(0xFF007AFF) else Color(0xFF34C759)
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .animateContentSize(), // 添加尺寸变化动画，让气泡生长更顺滑
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // 头像和元信息
        MessageAvatar(
            isUser = isUser,
            modelName = if (!isUser) (message.model ?: viewModel.model) else null,
            userName = viewModel.userName,
            userAvatar = viewModel.userAvatar,
            timestamp = message.timestamp
        )
        
        // 消息气泡
        Box(
            modifier = Modifier
                .then(
                    if (isUser) {
                        // User 气泡：包裹内容，限制最大宽度
                        Modifier.widthIn(max = 300.dp)
                    } else {
                        // AI 气泡：极致展开，不设硬编码宽度上限
                        Modifier.fillMaxWidth(0.95f)
                    }
                )
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { bubbleShape },
                    effects = {
                        vibrancy()
                        blur(6f.dp.toPx()) // 稍微增加模糊度以提升文字可读性
                        lens(12f.dp.toPx(), 24f.dp.toPx())
                    },
                    highlight = { Highlight.Plain },
                    onDrawSurface = {
                        // 提高 alpha 从 0.25 到 0.45，增强对比度
                        drawRect(tintColor.copy(alpha = 0.45f))
                    }
                )
                .drawBehind {
                    // 背景兜底：即使 backdrop 在长消息下失效，这里也能保证气泡可见
                    drawRoundRect(
                        color = tintColor.copy(alpha = 0.2f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                            x = with(this) { 20.dp.toPx() },
                            y = with(this) { 20.dp.toPx() }
                        )
                    )
                }
                .combinedClickable(
                    onClick = { /* 单击动作 */ },
                    onLongClick = onLongClick
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // 使用 CompositionLocalProvider 确保 Markdown 文本颜色为白色
            CompositionLocalProvider(
                LocalContentColor provides Color.White,
                LocalTextStyle provides TextStyle(
                    fontFamily = MaterialTheme.typography.bodyLarge.fontFamily,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color.Black.copy(alpha = 0.3f),
                        offset = androidx.compose.ui.geometry.Offset(0f, 2f),
                        blurRadius = 4f
                    )
                )
            ) {
                Column {
                    // 1. 如果有思考内容，则显示
                    if (!message.thinkingContent.isNullOrBlank()) {
                        ThinkingComponent(
                            content = message.thinkingContent!!,
                            isThinking = message.isStreaming && message.content.isEmpty(),
                            backdrop = backdrop,
                            shouldCollapse = message.content.isNotEmpty() // 当主内容出现时自动折叠
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    
                    // 2. 显示主案内容
                    if (message.content.isNotEmpty()) {
                        MarkdownBlock(
                            content = message.content,
                            style = LocalTextStyle.current
                        )
                    } else if (message.isStreaming && (message.thinkingContent.isNullOrBlank())) {
                        // 如果主内容为空且没有思考内容，显示打字指示器
                        TypingIndicator(backdrop = backdrop)
                    }
                    
                    // 流式输出时显示光标，放在 Markdown 下方
                    if (message.isStreaming && message.content.isNotEmpty()) {
                        BasicText(
                            text = "▌",
                            style = TextStyle(
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 16.sp
                            ),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
        
        // AI 消息显示操作按钮 (非流式时)
        if (!isUser && !message.isStreaming && message.content.isNotEmpty()) {
            MessageActionButtons(
                content = message.content,
                isUser = isUser,
                onRegenerate = { viewModel.regenerate(message.id) },
                onDelete = { viewModel.deleteMessage(message.id) },
                onSpeak = { viewModel.speak(message.content) }
            )
        }
    }
}

@Composable
private fun LiquidGlassChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    isLoading: Boolean,
    backdrop: Backdrop
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Input field with glass effect
        Box(
            modifier = Modifier
                .weight(1f)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { ContinuousRoundedRectangle(28.dp) },
                    effects = {
                        vibrancy()
                        blur(4f.dp.toPx())
                        lens(12f.dp.toPx(), 24f.dp.toPx())
                    },
                    highlight = { Highlight.Plain },
                    onDrawSurface = {
                        drawRect(Color.White.copy(alpha = 0.15f))
                    }
                )
                .heightIn(min = 56.dp, max = 150.dp)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                enabled = !isLoading,
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 16.sp
                ),
                cursorBrush = SolidColor(Color(0xFF007AFF)),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                decorationBox = { innerTextField ->
                    Box {
                        if (text.isEmpty()) {
                            BasicText(
                                text = "输入消息...",
                                style = TextStyle(
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 16.sp
                                )
                            )
                        }
                        innerTextField()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // Send/Stop button
        LiquidButton(
            onClick = if (isLoading) onStop else onSend,
            backdrop = backdrop,
            modifier = Modifier.size(56.dp),
            isInteractive = isLoading || text.isNotBlank(),
            tint = if (isLoading) Color(0xFFFF3B30) else if (text.isNotBlank()) Color(0xFF007AFF) else Color.Gray.copy(alpha = 0.5f)
        ) {
            Icon(
                imageVector = if (isLoading) Icons.Default.Stop else Icons.AutoMirrored.Filled.Send,
                contentDescription = if (isLoading) "停止" else "发送",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun ConversationDrawerContent(
    conversations: List<com.liquidglass.fluxhub.data.ConversationEntity>,
    currentConversationId: String?,
    backdrop: Backdrop,
    onSelectConversation: (String) -> Unit,
    onDeleteConversation: (String) -> Unit,
    onNewConversation: () -> Unit,
    onInteractionChanged: (Boolean) -> Unit = {}
) {
    ModalDrawerSheet(
        modifier = Modifier
            .width(320.dp)
            .fillMaxHeight()
            .padding(vertical = 24.dp, horizontal = 16.dp), // 增加上下左右间距，使其悬浮
        drawerContainerColor = Color.Transparent,
        drawerShape = RoundedCornerShape(28.dp) // 全圆角，形成药丸感
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(28.dp) },
                    effects = {
                        vibrancy()
                        blur(16f.dp.toPx())
                    },
                    onDrawSurface = {
                        drawRect(Color.Black.copy(alpha = 0.45f))
                    }
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 20.dp)
                    .statusBarsPadding()
            ) {
                // 顶部标题和关闭动作 (虽然抽屉通常划走，但这里可以加个按钮)
                Row(
                    modifier = Modifier.padding(vertical = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Lucide.MessageSquare,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "会话记录",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                    )
                }
                
                // 新建会话按钮 - 采用玻璃按钮样式
                LiquidButton(
                    onClick = onNewConversation,
                    backdrop = backdrop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    isInteractive = true,
                    tint = Color(0xFF007AFF),
                    onPressed = onInteractionChanged
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Lucide.Plus, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("开启新对话", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(Modifier.height(24.dp))
                
                Text(
                    text = "最近会话",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                // 会话列表
                if (conversations.isEmpty()) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            text = "空空如也",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.3f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(conversations.size) { index ->
                            val conversation = conversations[index]
                            val isSelected = conversation.id == currentConversationId
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .drawBackdrop(
                                        backdrop = backdrop,
                                        shape = { RoundedCornerShape(12.dp) },
                                        effects = { vibrancy() },
                                        onDrawSurface = {
                                            if (isSelected) {
                                                drawRect(Color.White.copy(alpha = 0.15f))
                                            } else {
                                                drawRect(Color.White.copy(alpha = 0.05f))
                                            }
                                        }
                                    )
                                    .clickable { onSelectConversation(conversation.id) }
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) Lucide.MessageCircle else Lucide.MessageSquare,
                                        contentDescription = null,
                                        tint = if (isSelected) Color(0xFF007AFF) else Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    BasicText(
                                        text = conversation.title,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f)
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    
                                    if (isSelected) {
                                        LiquidButton(
                                            onClick = { onDeleteConversation(conversation.id) },
                                            backdrop = backdrop,
                                            modifier = Modifier.size(32.dp),
                                            isInteractive = true,
                                            tint = Color(0xFFFF3B30),
                                            padding = PaddingValues(0.dp),
                                            onPressed = onInteractionChanged
                                        ) {
                                            Icon(
                                                imageVector = Lucide.Trash2,
                                                contentDescription = "删除",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
