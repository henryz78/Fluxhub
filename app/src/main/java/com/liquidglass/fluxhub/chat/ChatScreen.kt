package com.liquidglass.fluxhub.chat

import android.util.Log
import androidx.compose.animation.*
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
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import com.liquidglass.fluxhub.ui.components.richtext.MarkdownBlock
import com.liquidglass.fluxhub.ui.components.message.MessageAvatar
import com.liquidglass.fluxhub.ui.components.message.MessageActionButtons
import kotlinx.coroutines.launch

private const val TAG = "ChatScreen"

@Composable
fun ChatScreen(
    backdrop: Backdrop,
    bottomPadding: PaddingValues,
    onNavigateToSettings: () -> Unit = {},
    viewModel: ChatViewModel = viewModel(),
    listState: LazyListState = rememberLazyListState()
) {
    var inputText by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // 检测键盘可见性
    val isKeyboardVisible = rememberIsKeyboardVisible()
    
    // 获取最后一条消息的内容（用于检测流式更新）
    val lastMessageContent = viewModel.messages.lastOrNull()?.content ?: ""
    val isStreaming = viewModel.messages.lastOrNull()?.isStreaming == true
    
    // 流式输出时自动滚动到底部（内容变化时）
    LaunchedEffect(lastMessageContent) {
        if (isStreaming && viewModel.messages.isNotEmpty()) {
            listState.scrollToItem(viewModel.messages.size - 1)
        }
    }
    
    // 新消息时滚动到底部
    LaunchedEffect(viewModel.messages.size) {
        if (viewModel.messages.isNotEmpty()) {
            listState.scrollToItem(viewModel.messages.size - 1)
        }
    }
    
    // 当键盘弹出时，延迟滚动到最后一条消息
    LaunchedEffect(isKeyboardVisible) {
        if (isKeyboardVisible && viewModel.messages.isNotEmpty()) {
            kotlinx.coroutines.delay(200)
            listState.animateScrollToItem(viewModel.messages.size - 1)
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
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ConversationDrawerContent(
                conversations = viewModel.conversations,
                currentConversationId = viewModel.currentConversationId,
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
                }
            )
        },
        gesturesEnabled = true
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
            bottomPadding = bottomPadding
        )
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
    bottomPadding: PaddingValues
) {
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
                IconButton(
                    onClick = onOpenDrawer
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "会话列表",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                // 会话标题
                BasicText(
                    text = viewModel.currentConversationTitle,
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                    maxLines = 1
                )
                
                // 新建会话按钮
                IconButton(
                    onClick = { viewModel.createNewConversation() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "新建会话",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
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
                    viewModel = viewModel
                )
            }
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

@Composable
private fun LiquidGlassChatBubble(
    message: UiMessage,
    backdrop: Backdrop,
    viewModel: ChatViewModel
) {
    val isUser = message.role == "user"
    val bubbleShape = ContinuousRoundedRectangle(20.dp)
    val tintColor = if (isUser) Color(0xFF007AFF) else Color(0xFF34C759)
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // 头像和元信息
        MessageAvatar(
            isUser = isUser,
            modelName = if (!isUser) viewModel.model else null,
            timestamp = message.timestamp
        )
        
        // 消息气泡
        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { bubbleShape },
                    effects = {
                        vibrancy()
                        blur(4f.dp.toPx())
                        lens(12f.dp.toPx(), 24f.dp.toPx())
                    },
                    highlight = { Highlight.Plain },
                    onDrawSurface = {
                        drawRect(tintColor.copy(alpha = 0.25f))
                    }
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (message.content.isEmpty() && message.isStreaming) {
                // 显示打字指示器
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .drawBackdrop(
                                    backdrop = backdrop,
                                    shape = { ContinuousCapsule },
                                    effects = { vibrancy() },
                                    onDrawSurface = {
                                        drawRect(Color.White.copy(alpha = 0.6f))
                                    }
                                )
                        )
                    }
                }
            } else {
                // 使用 CompositionLocalProvider 确保 Markdown 文本颜色为白色
                CompositionLocalProvider(
                    LocalContentColor provides Color.White,
                    LocalTextStyle provides TextStyle(
                        fontFamily = MaterialTheme.typography.bodyLarge.fontFamily,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        color = Color.White
                    )
                ) {
                    Column {
                        if (message.content.isNotEmpty()) {
                            MarkdownBlock(
                                content = message.content,
                                modifier = Modifier.wrapContentWidth()
                            )
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
        }
        
        // AI 消息显示操作按钮 (非流式时)
        if (!isUser && !message.isStreaming && message.content.isNotEmpty()) {
            MessageActionButtons(
                content = message.content,
                isUser = isUser,
                onRegenerate = {
                    // TODO: 实现重新生成功能
                }
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
    onSelectConversation: (String) -> Unit,
    onDeleteConversation: (String) -> Unit,
    onNewConversation: () -> Unit
) {
    ModalDrawerSheet(
        modifier = Modifier.width(280.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(16.dp)
        ) {
            // 标题
            Text(
                text = "会话列表",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // 新建会话按钮
            OutlinedButton(
                onClick = onNewConversation,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, "新建会话")
                Spacer(Modifier.width(8.dp))
                Text("新建会话")
            }
            
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            
            // 会话列表
            if (conversations.isEmpty()) {
                Text(
                    text = "暂无会话",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(conversations.size) { index ->
                        val conversation = conversations[index]
                        val isSelected = conversation.id == currentConversationId
                        
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectConversation(conversation.id) },
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = conversation.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                                
                                // 删除按钮
                                IconButton(
                                    onClick = { onDeleteConversation(conversation.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "删除",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                        modifier = Modifier.size(18.dp)
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
