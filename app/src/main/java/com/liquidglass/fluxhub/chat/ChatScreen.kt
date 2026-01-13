package com.liquidglass.fluxhub.chat

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyListItemInfo
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
import androidx.compose.ui.window.Dialog
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import coil3.compose.AsyncImage
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.kyant.backdrop.highlight.Highlight
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import com.liquidglass.fluxhub.components.LiquidButton
import com.liquidglass.fluxhub.components.LiquidConfirmationDialog
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import com.liquidglass.fluxhub.ui.components.richtext.MarkdownBlock
import com.liquidglass.fluxhub.ui.components.richtext.ProvideHighlighter
import com.liquidglass.fluxhub.ui.components.message.MessageAvatar
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import com.liquidglass.fluxhub.ui.components.message.MessageActionButtons
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.liquidglass.fluxhub.ui.components.message.MessageActionsSheet
import com.liquidglass.fluxhub.ui.components.message.ThinkingComponent
import com.composables.icons.lucide.*
import com.composables.icons.lucide.ChevronRight
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
    onNavigateToAssistantSelection: () -> Unit = {},
    viewModel: ChatViewModel = viewModel(),
    listState: LazyListState = rememberLazyListState(),
    drawerState: DrawerState = rememberDrawerState(initialValue = DrawerValue.Closed),
    initialPrompt: String? = null,
    onPromptConsumed: () -> Unit = {}
) {
    // 使用 ViewModel 中的 inputText（导航时不会丢失）
    var inputText by remember { mutableStateOf(viewModel.inputText) }
    
    // 同步从 ViewModel 到本地（编辑消息时 ViewModel 会修改 inputText）
    LaunchedEffect(viewModel.inputText) {
        if (inputText != viewModel.inputText) {
            inputText = viewModel.inputText
        }
    }
    
    // 同步到 ViewModel（确保导航时保存）
    LaunchedEffect(inputText) {
        viewModel.inputText = inputText
    }
    
    // 消费初始提示词
    LaunchedEffect(initialPrompt) {
        if (!initialPrompt.isNullOrBlank()) {
            inputText = initialPrompt
            viewModel.inputText = initialPrompt
            onPromptConsumed()
        }
    }
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    
    // 追踪是否有顶部按钮正在被交互，以禁用侧边栏手势
    var isInteractingWithButtons by remember { mutableStateOf(false) }
    // 检测键盘可见性
    val isKeyboardVisible = rememberIsKeyboardVisible()
    // 获取流式消息的状态（用于检测流式更新）
    val isStreaming = viewModel.messages.any { it.isStreaming }
    val density = androidx.compose.ui.platform.LocalDensity.current

    // 底部标记 key (参考 RikkaHub)
    val ScrollBottomKey = "scroll_bottom_spacer"
    
    // 判断是否在底部 (完全对齐 RikkaHub ChatListNormal.isAtBottom)
    fun List<LazyListItemInfo>.isAtBottom(): Boolean {
        val lastItem = lastOrNull() ?: return true
        // 如果最后一个可见的是 spacer，认为在底部
        if (lastItem.key == ScrollBottomKey) {
            return true
        }
        // 如果最后一个可见消息接近视口底部
        val viewportEnd = listState.layoutInfo.viewportEndOffset
        return lastItem.offset + lastItem.size <= viewportEnd + lastItem.size * 0.15 + 32
    }

    // 官方设计：发送消息时自动滑动，AI 生成时跟随
    var isRecentScroll by remember { mutableStateOf(false) }
    
    // 追踪用户手动滚动，暂时暂停自动跟随
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            isRecentScroll = true
            kotlinx.coroutines.delay(2000)
            isRecentScroll = false
        }
    }
    
    // 获取最新状态用于自动滚动 (过滤系统消息以对齐列表索引)
    val loadingState by rememberUpdatedState(isStreaming || viewModel.isLoading)
    val messagesSnapshot by rememberUpdatedState(viewModel.messages.filter { it.role != "system" })
    
    // 自动滚动到底部 (精简版官方逻辑)
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }.collect { visibleItemsInfo ->
            // 只在加载中且用户最近没有手动大幅度滚动时自动跟随
            if (!listState.isScrollInProgress && !isRecentScroll && loadingState) {
                if (visibleItemsInfo.isAtBottom()) {
                    listState.requestScrollToItem(messagesSnapshot.size)
                }
            }
        }
    }
    

    // 强制跟随流式内容长度变化
    LaunchedEffect(loadingState) {
        if (loadingState) {
            snapshotFlow { messagesSnapshot.lastOrNull()?.content?.length ?: 0 }.collect {
                // 增加小延迟确保内容渲染后滚动
                if (!listState.isScrollInProgress && !isRecentScroll && listState.layoutInfo.visibleItemsInfo.isAtBottom()) {
                    listState.requestScrollToItem(messagesSnapshot.size)
                }
            }
        }
    }
    
    // 发送消息的处理函数
    val onSendMessage: () -> Unit = {
        if (inputText.isNotBlank()) {
            val textToSend = inputText
            inputText = "" // 立即清空输入框，避免视觉延迟
            
            // 如果正在编辑消息，调用编辑处理函数
            if (viewModel.isEditing()) {
                viewModel.handleMessageEdit(textToSend)
            } else {
                viewModel.sendMessage(textToSend)
            }
            
            // 发送后立即滚动到底部 (官方通常是立即到达底部)
            scope.launch {
                listState.scrollToItem(messagesSnapshot.size)
            }
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
                    onRenameConversation = { id, newTitle ->
                        viewModel.renameConversation(id, newTitle)
                    },
                    onNewConversation = {
                        viewModel.createNewConversation()
                        scope.launch { drawerState.close() }
                    },
                    onInteractionChanged = { isInteractingWithButtons = it },
                    assistants = viewModel.assistants,
                    currentAssistant = viewModel.currentAssistant,
                    onSwitchAssistant = { viewModel.switchAssistant(it) },
                    onNavigateToAssistantSelection = onNavigateToAssistantSelection
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
                onInteractionChanged = { isInteractingWithButtons = it },
                onImageSelected = { viewModel.selectedImageUri = it }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    onInteractionChanged: (Boolean) -> Unit,
    onImageSelected: (android.net.Uri?) -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        onImageSelected(uri)
    }
    
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        onImageSelected(uri)
    }
    
    // 拍照功能：使用 TakePicture + FileProvider
    val context = LocalContext.current
    var cameraOutputUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var cameraOutputFile by remember { mutableStateOf<java.io.File?>(null) }
    
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraOutputUri != null) {
            onImageSelected(cameraOutputUri)
        }
        // 如果失败，清理临时文件
        if (!success) {
            cameraOutputFile?.delete()
        }
        cameraOutputFile = null
        cameraOutputUri = null
    }
    
    // 相机权限请求
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // 权限已授予，启动相机
            try {
                cameraOutputFile = java.io.File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
                cameraOutputUri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    cameraOutputFile!!
                )
                cameraLauncher.launch(cameraOutputUri!!)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 消息操作菜单状态 (已移除)
    // clipboardManager 已移除
    // 模型选择器状态
    var showModelSelector by remember { mutableStateOf(false) }
    // 助手选择器状态
    var showAssistantSelector by remember { mutableStateOf(false) }
    // 上传选项弹窗状态
    var showUploadOptions by remember { mutableStateOf(false) }
    // 工具箱弹窗状态
    var showToolbox by remember { mutableStateOf(false) }

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
                            fontWeight = FontWeight.Medium,
                            shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 4f)
                        ),
                        maxLines = 1
                    )
                    // 模型选择器 - 始终显示
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { 
                                viewModel.fetchModels() // 打开时刷新模型列表
                                showModelSelector = true 
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
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
                                tint = Color.White.copy(alpha = 0.8f)
                            )
                            Spacer(Modifier.width(4.dp))
                            BasicText(
                                text = viewModel.model.ifBlank { "选择模型" },
                                style = TextStyle(
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 10.sp,
                                    shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 2f)
                                ),
                                maxLines = 1
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = Lucide.ChevronDown,
                                contentDescription = "切换模型",
                                modifier = Modifier.size(10.dp),
                                tint = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
                
                // 助手快捷切换按钮
                LiquidButton(
                    onClick = { showAssistantSelector = true },
                    backdrop = backdrop,
                    modifier = Modifier.size(44.dp),
                    isInteractive = true,
                    padding = PaddingValues(0.dp),
                    onPressed = onInteractionChanged
                ) {
                    Text(
                        text = viewModel.currentAssistant?.avatar ?: "🤖",
                        fontSize = 22.sp
                    )
                }
                
                Spacer(Modifier.width(8.dp))
                
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
        
        // 缓存过滤后的消息列表，避免每次重组都重新创建
        val displayMessages = remember { derivedStateOf { 
            viewModel.messages.filter { it.role != "system" }
        } }
        
        // 参考 RikkaHub: 判断是否在底部的辅助函数
        fun isAtBottom(): Boolean {
            val visibleItems = listState.layoutInfo.visibleItemsInfo
            val lastItem = visibleItems.lastOrNull() ?: return false
            // 如果最后一项是 spacer 或最后一条消息
            if (lastItem.key == "scroll_bottom_spacer") return true
            val lastMessageId = displayMessages.value.lastOrNull()?.id
            return lastItem.key == lastMessageId && 
                   (lastItem.offset + lastItem.size <= listState.layoutInfo.viewportEndOffset + lastItem.size * 0.15 + 32)
        }
        
        // 参考 RikkaHub: 使用 snapshotFlow 监听可见项变化，只在加载中且在底部时自动滚动
        val loadingState = viewModel.isLoading
        LaunchedEffect(listState, loadingState) {
            snapshotFlow { listState.layoutInfo.visibleItemsInfo }.collect { _ ->
                if (!listState.isScrollInProgress && loadingState) {
                    if (isAtBottom()) {
                        // 使用非动画滚动，减少性能开销
                        listState.requestScrollToItem(displayMessages.value.size + 1)
                    }
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
            // 缓存 model 避免每个气泡读取 ViewModel 导致级联重组
            val defaultModel = viewModel.model
            items(
                items = displayMessages.value, 
                key = { it.id },
                contentType = { it.role } // 帮助 Compose 复用相同类型的组件
            ) { message ->
                LiquidGlassChatBubble(
                    message = message,
                    backdrop = backdrop,
                    defaultModel = defaultModel,
                    onRegenerate = { viewModel.regenerate(message.id) },
                    onDelete = { viewModel.deleteMessage(message.id) },
                    onEdit = { 
                        // 仅加载内容到输入框，不立即删除消息
                        viewModel.startEditingMessage(message.id, message.content)
                    },
                    onSaveImage = { url -> viewModel.saveImageToGallery(url) },
                    hapticFeedbackEnabled = viewModel.hapticFeedbackEnabled
                )
            }
            
            // 底部占位符：确保可以正确滚动到最底部 (参考 RikkaHub)
            item("scroll_bottom_spacer") {
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .height(32.dp) // 保持适中距离
                )
            }
        }

        // 模型选择器底部弹窗
        // 模型选择器弹窗
        if (showModelSelector) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showModelSelector = false }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedCornerShape(24.dp) },
                            effects = { vibrancy(); blur(16.dp.toPx()) },
                            onDrawSurface = { drawRect(Color.White.copy(0.2f)) }
                        )
                        .padding(24.dp)
                ) {
                    Column {
                        // 标题行
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "选择模型",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = Color.White,
                                    shadow = Shadow(
                                        color = Color.Black.copy(alpha = 0.5f),
                                        blurRadius = 4f
                                    )
                                ),
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            LiquidButton(
                                onClick = { viewModel.fetchModels() },
                                backdrop = backdrop,
                                modifier = Modifier.size(40.dp),
                                isInteractive = true,
                                padding = PaddingValues(0.dp)
                            ) {
                                Icon(
                                    Lucide.RefreshCw, 
                                    contentDescription = "刷新", 
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // 搜索框
                        var modelSearchQuery by remember { mutableStateOf("") }
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Lucide.Search,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(18.dp)
                                )
                                BasicTextField(
                                    value = modelSearchQuery,
                                    onValueChange = { modelSearchQuery = it },
                                    textStyle = TextStyle(
                                        color = Color.White,
                                        fontSize = 14.sp
                                    ),
                                    singleLine = true,
                                    decorationBox = { innerTextField ->
                                        Box {
                                            if (modelSearchQuery.isEmpty()) {
                                                BasicText(
                                                    text = "搜索模型...",
                                                    style = TextStyle(
                                                        color = Color.White.copy(alpha = 0.4f),
                                                        fontSize = 14.sp
                                                    )
                                                )
                                            }
                                            innerTextField()
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                if (modelSearchQuery.isNotEmpty()) {
                                    Icon(
                                        Lucide.X,
                                        contentDescription = "清除",
                                        tint = Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clickable { modelSearchQuery = "" }
                                    )
                                }
                            }
                        }

                        // 模型列表（根据搜索过滤）
                        val filteredModels = remember(viewModel.availableModels, modelSearchQuery) {
                            if (modelSearchQuery.isBlank()) {
                                viewModel.availableModels
                            } else {
                                viewModel.availableModels.filter { 
                                    it.contains(modelSearchQuery, ignoreCase = true) 
                                }
                            }
                        }
                        
                        if (viewModel.availableModels.isEmpty()) {
                            Text(
                                text = "正在加载模型列表...",
                                color = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        } else if (filteredModels.isEmpty()) {
                            Text(
                                text = "未找到匹配的模型",
                                color = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 350.dp)
                            ) {
                                items(filteredModels) { modelName ->
                                    val isSelected = modelName == viewModel.model
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.saveModel(modelName)
                                                showModelSelector = false
                                            }
                                            .padding(vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = modelName,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.95f),
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                shadow = Shadow(
                                                    color = Color.Black.copy(alpha = 0.5f),
                                                    blurRadius = 4f
                                                )
                                            )

                                        )
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Lucide.Check,
                                                contentDescription = "已选择",
                                                tint = Color.White
                                            )
                                        }
                                    }
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                                }
                            }
                        }
                        
                        Spacer(Modifier.height(16.dp))
                        
                        Spacer(Modifier.height(16.dp))
                        
                        // 提示：可以在助手设置中调整参数
                        Text(
                            "提示：可以在助手设置中调整温度等参数。",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                        
                        Spacer(Modifier.height(16.dp))

                        
                        Spacer(Modifier.height(16.dp))
                        
                        // 关闭按钮
                        LiquidButton(
                            onClick = { showModelSelector = false },
                            backdrop = backdrop,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            isInteractive = true,
                            tint = Color(0xFF8E8E93).copy(alpha = 0.5f)
                        ) {
                            Text("关闭", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 助手选择器弹窗
        if (showAssistantSelector) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showAssistantSelector = false }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedCornerShape(24.dp) },
                            effects = { vibrancy(); blur(16.dp.toPx()) },
                            onDrawSurface = { drawRect(Color.White.copy(0.2f)) }
                        )
                        .padding(24.dp)
                ) {
                    Column {
                        // 标题行
                        Text(
                            text = "切换助手",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = Color.White,
                                shadow = Shadow(
                                    color = Color.Black.copy(alpha = 0.5f),
                                    blurRadius = 4f
                                )
                            ),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // 助手列表
                        if (viewModel.assistants.isEmpty()) {
                            Text(
                                text = "暂无助手，请先创建",
                                color = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 300.dp)
                            ) {
                                items(viewModel.assistants) { assistant ->
                                    val isSelected = assistant.id == viewModel.currentAssistant?.id
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                viewModel.switchAssistant(assistant)
                                                showAssistantSelector = false
                                            }
                                            .background(
                                                if (isSelected) Color(0xFF007AFF).copy(alpha = 0.2f)
                                                else Color.Transparent
                                            )
                                            .padding(vertical = 12.dp, horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // 助手头像
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color.White.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = assistant.avatar ?: "🤖",
                                                fontSize = 20.sp
                                            )
                                        }
                                        
                                        Spacer(Modifier.width(12.dp))
                                        
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                text = assistant.name,
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    color = Color.White,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    shadow = Shadow(
                                                        color = Color.Black.copy(alpha = 0.5f),
                                                        blurRadius = 4f
                                                    )
                                                )
                                            )
                                            if (assistant.systemPrompt.isNotBlank()) {
                                                Text(
                                                    text = assistant.systemPrompt.take(30) + if (assistant.systemPrompt.length > 30) "..." else "",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.White.copy(alpha = 0.5f),
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                        
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Lucide.Check,
                                                contentDescription = "已选择",
                                                tint = Color(0xFF34C759),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                                }
                            }
                        }
                        
                        Spacer(Modifier.height(16.dp))
                        
                        // 管理助手按钮
                        LiquidButton(
                            onClick = {
                                showAssistantSelector = false
                                onNavigateToSettings() // 跳转到设置页的助手管理
                            },
                            backdrop = backdrop,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            isInteractive = true,
                            tint = Color(0xFF007AFF).copy(alpha = 0.3f)
                        ) {
                            Icon(Lucide.Settings, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("管理助手", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
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
            Column(
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                 // 图片预览
                 if (viewModel.selectedImageUri != null) {
                     Box(
                         modifier = Modifier
                             .padding(horizontal = 16.dp, vertical = 4.dp)
                             .size(72.dp)
                     ) {
                         Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
                                .drawBackdrop(
                                    backdrop = backdrop,
                                    shape = { RoundedCornerShape(12.dp) },
                                    effects = { 
                                         vibrancy()
                                         blur(10.dp.toPx()) 
                                    },
                                    onDrawSurface = { drawRect(Color.White.copy(0.1f)) }
                                )
                         ) {
                             AsyncImage(
                                 model = viewModel.selectedImageUri,
                                 contentDescription = "Preview",
                                 contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                 modifier = Modifier.fillMaxSize()
                             )
                         }
                         
                         // 删除按钮
                         Icon(
                             imageVector = Lucide.X,
                             contentDescription = "Remove",
                             tint = Color.White,
                             modifier = Modifier
                                 .align(Alignment.TopEnd)
                                 .padding(6.dp)
                                 .size(20.dp)
                                 .clip(androidx.compose.foundation.shape.CircleShape)
                                 .background(Color.Black.copy(0.5f))
                                 .clickable { viewModel.selectedImageUri = null }
                                 .padding(3.dp)
                         )
                     }
                 }

                // 编辑指示器（正在编辑消息时显示）
                AnimatedVisibility(
                    visible = viewModel.isEditing(),
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .drawBackdrop(
                                backdrop = backdrop,
                                shape = { RoundedCornerShape(12.dp) },
                                effects = { vibrancy(); blur(8.dp.toPx()) },
                                onDrawSurface = { drawRect(Color(0xFFFF9500).copy(alpha = 0.3f)) }
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Lucide.PenLine,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                BasicText(
                                    text = "正在编辑消息",
                                    style = TextStyle(
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 2f)
                                    )
                                )
                            }
                            
                            // 取消按钮
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.cancelEditing() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                BasicText(
                                    text = "取消",
                                    style = TextStyle(
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }
                    }
                }

                LiquidGlassChatInputBar(
                    text = inputText,
                    onTextChange = onInputTextChange,
                    onSend = {
                        val isSendable = inputText.isNotBlank() || viewModel.selectedImageUri != null
                        if (isSendable && viewModel.hapticFeedbackEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        onSend()
                    },
                    onStop = { 
                        if (viewModel.hapticFeedbackEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        viewModel.stopStreaming() 
                    },
                    isLoading = viewModel.isLoading,
                    backdrop = backdrop,
                    onInteractionChanged = onInteractionChanged,
                    onPickImage = {
                        showUploadOptions = true
                    },
                    onOpenToolbox = {
                        showToolbox = true
                    }
                )
            }
        }
        
        // 上传选项弹窗
        if (showUploadOptions) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showUploadOptions = false }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedCornerShape(24.dp) },
                            effects = { vibrancy(); blur(16.dp.toPx()) },
                            onDrawSurface = { drawRect(Color.Black.copy(0.5f)) }
                        )
                        .padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "选择上传方式",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                shadow = Shadow(
                                    color = Color.Black.copy(alpha = 0.5f),
                                    blurRadius = 4f
                                )
                            ),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        // 选择图片
                        LiquidButton(
                            onClick = {
                                showUploadOptions = false
                                photoPicker.launch("image/*")
                            },
                            backdrop = backdrop,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            isInteractive = true,
                            tint = Color(0xFF007AFF).copy(alpha = 0.4f)
                        ) {
                            Icon(Lucide.Image, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("从相册选择图片", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        
                        // 选择文件
                        LiquidButton(
                            onClick = {
                                showUploadOptions = false
                                filePicker.launch("*/*")
                            },
                            backdrop = backdrop,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            isInteractive = true,
                            tint = Color(0xFF34C759).copy(alpha = 0.4f)
                        ) {
                            Icon(Lucide.File, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("选择文件", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        
                        // 拍照
                        LiquidButton(
                            onClick = {
                                showUploadOptions = false
                                // 检查并请求相机权限
                                val permission = android.Manifest.permission.CAMERA
                                if (androidx.core.content.ContextCompat.checkSelfPermission(context, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                    // 权限已授予，直接启动相机
                                    try {
                                        cameraOutputFile = java.io.File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
                                        cameraOutputUri = androidx.core.content.FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            cameraOutputFile!!
                                        )
                                        cameraLauncher.launch(cameraOutputUri!!)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                } else {
                                    // 请求权限
                                    cameraPermissionLauncher.launch(permission)
                                }
                            },
                            backdrop = backdrop,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            isInteractive = true,
                            tint = Color(0xFFFF9500).copy(alpha = 0.4f)
                        ) {
                            Icon(Lucide.Camera, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("拍照", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        
                        Spacer(Modifier.height(8.dp))
                        
                        // 取消按钮
                        LiquidButton(
                            onClick = { showUploadOptions = false },
                            backdrop = backdrop,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            isInteractive = true,
                            tint = Color(0xFF8E8E93).copy(alpha = 0.5f)
                        ) {
                            Text("取消", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        
        // 工具箱弹窗
        if (showToolbox) {
            ToolboxDialog(
                backdrop = backdrop,
                viewModel = viewModel,
                onDismiss = { showToolbox = false }
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun LiquidGlassChatBubble(
    message: UiMessage,
    backdrop: Backdrop,
    defaultModel: String,
    onRegenerate: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onSaveImage: (String) -> Unit,
    hapticFeedbackEnabled: Boolean
) {
    val isUser = message.role == "user"
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    var lastHapticTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(message.content) {
        if (!isUser && message.isStreaming && hapticFeedbackEnabled) {
            val now = System.currentTimeMillis()
            if (now - lastHapticTime > 50) { // 20Hz Limit
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                lastHapticTime = now
            }
        }
    }

    val bubbleShape = ContinuousRoundedRectangle(20.dp)
    val tintColor = if (isUser) Color(0xFF007AFF) else Color.White
    val keyboardController = LocalSoftwareKeyboardController.current
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // 角色标识
        Row(
            modifier = Modifier.padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = if (isUser) "你" else (message.model ?: defaultModel),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color.Black.copy(alpha = 0.5f),
                        blurRadius = 4f
                    )
                )
            )
        }
        
        // 消息气泡
        // 简化液态玻璃效果：只保留 vibrancy 和轻微模糊，不使用 lens
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
                        blur(2f.dp.toPx()) // 轻微模糊，减少 GPU 负载
                    },
                    highlight = { Highlight.Plain },
                    onDrawSurface = {
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
                .clickable { keyboardController?.hide() }
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
                        fontWeight = if (isUser) FontWeight.Medium else FontWeight.Normal
                    )
                ) {
                    // 使用 SelectionContainer 支持系统文本选择复制
                    androidx.compose.foundation.text.selection.SelectionContainer {
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
                    // 根据背景色决定文字颜色：用户(蓝底)->白字; AI(白底)->黑字
                    val textColor = if (isUser) Color.White else Color(0xFF1C1C1E)
                    // 为白字添加阴影以增强对比度
                    val textShadow = if (isUser) androidx.compose.ui.graphics.Shadow(
                        color = Color.Black.copy(alpha = 0.3f),
                        blurRadius = 2f,
                        offset = androidx.compose.ui.geometry.Offset(0f, 1f)
                    ) else null

                    // 解析并显示图片 (Vision 格式) - 使用 remember 缓存结果
                    val imageMatch = remember(message.content) {
                        Regex("^!\\[image\\]\\((.+?)\\)").find(message.content)
                    }
                    val imageUrl = imageMatch?.groupValues?.get(1)
                    // 缓存 textContent 避免每次重组时重新计算
                    val textContent = remember(message.content, imageUrl) {
                        if (imageUrl != null && imageMatch != null) {
                            message.content.substring(imageMatch.range.last + 1).trimStart()
                        } else message.content
                    }

                    // 图片预览状态
                    var showImagePreview by remember { mutableStateOf(false) }

                    if (imageUrl != null) {
                         AsyncImage(
                             model = imageUrl,
                             contentDescription = "Image",
                             modifier = Modifier
                                 .fillMaxWidth()
                                 .heightIn(max = 240.dp)
                                 .clip(RoundedCornerShape(12.dp))
                                 .clickable { showImagePreview = true },
                             contentScale = androidx.compose.ui.layout.ContentScale.Crop
                         )
                         Spacer(Modifier.height(8.dp))
                         
                         // 全屏图片预览
                         if (showImagePreview) {
                             var scale by remember { mutableFloatStateOf(1f) }
                             var offsetX by remember { mutableFloatStateOf(0f) }
                             var offsetY by remember { mutableFloatStateOf(0f) }
                             
                             androidx.compose.ui.window.Dialog(
                                 onDismissRequest = { showImagePreview = false },
                                 properties = androidx.compose.ui.window.DialogProperties(
                                     usePlatformDefaultWidth = false,
                                     decorFitsSystemWindows = false
                                 )
                             ) {
                                 Box(
                                     modifier = Modifier
                                         .fillMaxSize()
                                         .background(Color.Black),
                                     contentAlignment = Alignment.Center
                                 ) {
                                     // 可缩放的图片
                                     AsyncImage(
                                         model = imageUrl,
                                         contentDescription = "Full Image",
                                         modifier = Modifier
                                             .fillMaxWidth()
                                             .graphicsLayer(
                                                 scaleX = scale,
                                                 scaleY = scale,
                                                 translationX = offsetX,
                                                 translationY = offsetY
                                             )
                                             .pointerInput(Unit) {
                                                 detectTransformGestures { _, pan, zoom, _ ->
                                                     scale = (scale * zoom).coerceIn(1f, 5f)
                                                     if (scale > 1f) {
                                                         offsetX += pan.x
                                                         offsetY += pan.y
                                                     } else {
                                                         offsetX = 0f
                                                         offsetY = 0f
                                                     }
                                                 }
                                             },
                                         contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                     )
                                     
                                     // 下载按钮
                                     LiquidButton(
                                         onClick = { onSaveImage(imageUrl) },
                                         backdrop = backdrop,
                                         modifier = Modifier
                                             .align(Alignment.BottomCenter)
                                             .padding(bottom = 64.dp)
                                             .size(56.dp),
                                         isInteractive = true,
                                         tint = Color(0xFF007AFF).copy(alpha = 0.8f)
                                     ) {
                                         Icon(Lucide.Download, null, tint = Color.White)
                                     }
                                     
                                     // 关闭按钮
                                     LiquidButton(
                                         onClick = { showImagePreview = false },
                                         backdrop = backdrop,
                                         modifier = Modifier
                                             .align(Alignment.TopEnd)
                                             .padding(top = 48.dp, end = 24.dp)
                                             .size(44.dp),
                                         isInteractive = true,
                                         tint = Color.White.copy(alpha = 0.2f)
                                     ) {
                                         Icon(Lucide.X, null, tint = Color.White)
                                     }
                                 }
                             }
                         }
                    }

                    if (textContent.isNotEmpty()) {
                        MarkdownBlock(
                            content = textContent,
                            style = LocalTextStyle.current.copy(
                                color = textColor,
                                shadow = textShadow
                            )
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
                                color = textColor.copy(alpha = 0.7f),
                                fontSize = 16.sp,
                                shadow = textShadow
                            ),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                        }
                    }
                }
            }
        
        // 消息显示操作按钮 (用户和AI都显示，流式输出时不显示)
        if (!message.isStreaming && message.content.isNotEmpty()) {
            var showDeleteDialog by remember { mutableStateOf(false) }
            
            if (showDeleteDialog) {
                LiquidConfirmationDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    onConfirm = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    title = "删除消息",
                    message = "确定要删除这条消息吗？",
                    confirmText = "删除",
                    icon = Lucide.Trash2,
                    backdrop = backdrop
                )
            }
            
            MessageActionButtons(
                content = message.content,
                isUser = isUser,
                backdrop = backdrop,
                onRegenerate = if (isUser) null else onRegenerate,
                onEdit = if (isUser) onEdit else null,
                onDelete = { showDeleteDialog = true }
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
    backdrop: Backdrop,
    onInteractionChanged: (Boolean) -> Unit = {},
    onPickImage: () -> Unit = {},
    onOpenToolbox: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 左侧按钮组：+ 和工具箱
        LiquidButton(
            onClick = onPickImage,
            backdrop = backdrop,
            modifier = Modifier.size(44.dp),
            isInteractive = true,
            onPressed = onInteractionChanged,
            tint = Color(0xFF34C759).copy(alpha = 0.8f)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add",
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
        
        LiquidButton(
            onClick = onOpenToolbox,
            backdrop = backdrop,
            modifier = Modifier.size(44.dp),
            isInteractive = true,
            onPressed = onInteractionChanged,
            tint = Color(0xFF9B59B6).copy(alpha = 0.7f)
        ) {
            Icon(
                imageVector = Lucide.SlidersHorizontal,
                contentDescription = "工具箱",
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }

        // 输入框（带滚动条）
        val scrollState = rememberScrollState()
        Box(
            modifier = Modifier
                .weight(1f)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { ContinuousRoundedRectangle(24.dp) },
                    effects = {
                        vibrancy()
                        blur(4f.dp.toPx())
                        lens(12f.dp.toPx(), 24f.dp.toPx())
                    },
                    highlight = { Highlight.Plain },
                    onDrawSurface = {
                        // 增加 alpha 值使输入框在深色主题下更清晰
                        drawRect(Color.White.copy(alpha = 0.25f))
                    }
                )
                .heightIn(min = 44.dp, max = 160.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = onTextChange,
                    enabled = !isLoading,
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 4f)
                    ),
                    cursorBrush = SolidColor(Color(0xFF007AFF)),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() }),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .verticalScroll(scrollState),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (text.isEmpty()) {
                                BasicText(
                                    text = "输入消息...",
                                    style = TextStyle(
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 15.sp,
                                        lineHeight = 22.sp,
                                        shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 4f)
                                    )
                                )
                            }
                            innerTextField()
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                
                // 滚动条指示器（多行时显示）
                if (text.count { it == '\n' } > 1 || text.length > 80) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .fillMaxHeight()
                            .padding(vertical = 10.dp, horizontal = 2.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.5f))
                    )
                }
            }
        }
            
        // 发送/停止按钮
        LiquidButton(
            onClick = if (isLoading) onStop else onSend,
            backdrop = backdrop,
            modifier = Modifier.size(44.dp),
            isInteractive = isLoading || text.isNotBlank(),
            onPressed = onInteractionChanged,
            tint = if (isLoading) Color(0xFFFF3B30) else if (text.isNotBlank()) Color(0xFF007AFF) else Color.Gray.copy(alpha = 0.5f)
        ) {
            Icon(
                imageVector = if (isLoading) Icons.Default.Stop else Icons.AutoMirrored.Filled.Send,
                contentDescription = if (isLoading) "停止" else "发送",
                tint = Color.White,
                modifier = Modifier.size(30.dp)
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
    onRenameConversation: (String, String) -> Unit,
    onNewConversation: () -> Unit,
    onInteractionChanged: (Boolean) -> Unit = {},
    assistants: List<com.liquidglass.fluxhub.data.AssistantEntity> = emptyList(),
    currentAssistant: com.liquidglass.fluxhub.data.AssistantEntity? = null,
    onSwitchAssistant: (com.liquidglass.fluxhub.data.AssistantEntity) -> Unit = {},
    onNavigateToAssistantSelection: () -> Unit = {}
) {
    ModalDrawerSheet(
        modifier = Modifier
            .width(320.dp)
            .fillMaxHeight()
            .padding(top = 12.dp, bottom = 24.dp, start = 16.dp, end = 16.dp), // 调整间距，使其略微上移
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
                
                Spacer(Modifier.height(16.dp))
                
                // 搜索框
                var conversationSearchQuery by remember { mutableStateOf("") }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Lucide.Search,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                        BasicTextField(
                            value = conversationSearchQuery,
                            onValueChange = { conversationSearchQuery = it },
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 14.sp
                            ),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                Box {
                                    if (conversationSearchQuery.isEmpty()) {
                                        BasicText(
                                            text = "搜索对话...",
                                            style = TextStyle(
                                                color = Color.White.copy(alpha = 0.4f),
                                                fontSize = 14.sp
                                            )
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        if (conversationSearchQuery.isNotEmpty()) {
                            Icon(
                                Lucide.X,
                                contentDescription = "清除",
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable { conversationSearchQuery = "" }
                            )
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                Text(
                    text = "最近会话",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                // 过滤会话列表
                val filteredConversations = remember(conversations, conversationSearchQuery) {
                    if (conversationSearchQuery.isBlank()) {
                        conversations
                    } else {
                        conversations.filter { 
                            it.title.contains(conversationSearchQuery, ignoreCase = true) 
                        }
                    }
                }
                
                // 会话列表
                if (conversations.isEmpty()) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            text = "空空如也",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.3f)
                        )
                    }
                } else if (filteredConversations.isEmpty()) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            text = "未找到匹配的对话",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            count = filteredConversations.size,
                            key = { index -> filteredConversations[index].id }
                        ) { index ->
                            val conversation = filteredConversations[index]
                            val isSelected = conversation.id == currentConversationId
                            
                            // 重命名对话框状态
                            var showRenameDialog by remember { mutableStateOf(false) }
                            var renameText by remember { mutableStateOf(conversation.title) }

                            // 重命名对话框
                            if (showRenameDialog) {
                                androidx.compose.ui.window.Dialog(
                                    onDismissRequest = { showRenameDialog = false }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.85f)
                                            .clip(RoundedCornerShape(24.dp))
                                            .drawBackdrop(
                                                backdrop = backdrop,
                                                shape = { RoundedCornerShape(24.dp) },
                                                effects = { vibrancy(); blur(16.dp.toPx()) },
                                                onDrawSurface = { drawRect(Color.Black.copy(alpha = 0.6f)) }
                                            )
                                            .padding(24.dp)
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                "重命名对话",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp
                                            )
                                            Spacer(Modifier.height(20.dp))
                                            OutlinedTextField(
                                                value = renameText,
                                                onValueChange = { renameText = it },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = Color(0xFF007AFF),
                                                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                                    focusedTextColor = Color.White,
                                                    unfocusedTextColor = Color.White,
                                                    cursorColor = Color(0xFF007AFF)
                                                ),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            Spacer(Modifier.height(24.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                LiquidButton(
                                                    onClick = { showRenameDialog = false },
                                                    backdrop = backdrop,
                                                    modifier = Modifier.weight(1f).height(44.dp),
                                                    tint = Color.White.copy(alpha = 0.15f)
                                                ) {
                                                    Text("取消", color = Color.White, fontWeight = FontWeight.Medium)
                                                }
                                                LiquidButton(
                                                    onClick = {
                                                        if (renameText.isNotBlank()) {
                                                            onRenameConversation(conversation.id, renameText)
                                                        }
                                                        showRenameDialog = false
                                                    },
                                                    backdrop = backdrop,
                                                    modifier = Modifier.weight(1f).height(44.dp),
                                                    tint = Color(0xFF007AFF).copy(alpha = 0.8f)
                                                ) {
                                                    Text("确定", color = Color.White, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            
                            // 删除确认对话框
                            var showDeleteConfirmDialog by remember { mutableStateOf(false) }
                            if (showDeleteConfirmDialog) {
                                LiquidConfirmationDialog(
                                    onDismissRequest = { showDeleteConfirmDialog = false },
                                    onConfirm = {
                                        onDeleteConversation(conversation.id)
                                        showDeleteConfirmDialog = false
                                    },
                                    title = "删除对话",
                                    message = "确定要删除 \"${conversation.title}\" 吗？此操作无法撤销。",
                                    confirmText = "删除",
                                    icon = Lucide.Trash2,
                                    backdrop = backdrop
                                )
                            }
                            
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = {
                                    when (it) {
                                        SwipeToDismissBoxValue.EndToStart -> {
                                            showDeleteConfirmDialog = true
                                            false // Don't dismiss immediately, wait for dialog confirmation
                                        }
                                        SwipeToDismissBoxValue.StartToEnd -> {
                                            renameText = conversation.title
                                            showRenameDialog = true
                                            false // 不消费滑动，弹出对话框
                                        }
                                        else -> false
                                    }
                                }
                            )

                            SwipeToDismissBox(
                                state = dismissState,
                                enableDismissFromStartToEnd = true, // 启用左滑
                                backgroundContent = {
                                    val color = when (dismissState.dismissDirection) {
                                        SwipeToDismissBoxValue.EndToStart -> Color(0xFFFF3B30) // 删除红色
                                        SwipeToDismissBoxValue.StartToEnd -> Color(0xFF007AFF) // 重命名蓝色
                                        else -> Color.Transparent
                                    }
                                    val alignment = when (dismissState.dismissDirection) {
                                        SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                                        SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                                        else -> Alignment.Center
                                    }
                                    val icon = when (dismissState.dismissDirection) {
                                        SwipeToDismissBoxValue.EndToStart -> Lucide.Trash2
                                        SwipeToDismissBoxValue.StartToEnd -> Lucide.Pencil
                                        else -> Lucide.Trash2
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(vertical = 4.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(color)
                                            .padding(horizontal = 20.dp),
                                        contentAlignment = alignment
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = Color.White
                                        )
                                    }
                                },
                                content = {
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
                                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.9f),
                                                    shadow = androidx.compose.ui.graphics.Shadow(
                                                        color = Color.Black.copy(alpha = 0.5f),
                                                        blurRadius = 4f
                                                    )
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            )

                        }
                    }
                }
                }
            }
        }
    }

// ========== 工具箱弹窗 ==========

private sealed class ToolboxPage {
    object List : ToolboxPage()
    object ThinkingBudget : ToolboxPage()
    object WebSearch : ToolboxPage()
    object StreamOutput : ToolboxPage()
    object ContextSize : ToolboxPage()
}

@Composable
private fun ToolboxDialog(
    backdrop: Backdrop,
    viewModel: ChatViewModel,
    onDismiss: () -> Unit
) {
    var currentPage by remember { mutableStateOf<ToolboxPage>(ToolboxPage.List) }
    
    androidx.compose.ui.window.Dialog(
        onDismissRequest = {
            if (currentPage == ToolboxPage.List) {
                onDismiss()
            } else {
                currentPage = ToolboxPage.List
            }
        },
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.7f)
                .clip(RoundedCornerShape(28.dp))
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(28.dp) },
                    effects = { vibrancy(); blur(20.dp.toPx()) },
                    onDrawSurface = { drawRect(Color.Black.copy(0.6f)) }
                )
        ) {
            AnimatedContent(
                targetState = currentPage,
                transitionSpec = {
                    if (targetState == ToolboxPage.List) {
                        slideInHorizontally { -it } + fadeIn() togetherWith 
                            slideOutHorizontally { it } + fadeOut()
                    } else {
                        slideInHorizontally { it } + fadeIn() togetherWith 
                            slideOutHorizontally { -it } + fadeOut()
                    }
                },
                label = "toolbox_page"
            ) { page ->
                when (page) {
                    ToolboxPage.List -> ToolboxListPage(
                        viewModel = viewModel,
                        backdrop = backdrop,
                        onNavigate = { currentPage = it },
                        onDismiss = onDismiss
                    )
                    ToolboxPage.ThinkingBudget -> ToolboxThinkingBudgetPage(
                        viewModel = viewModel,
                        backdrop = backdrop,
                        onBack = { currentPage = ToolboxPage.List }
                    )
                    ToolboxPage.WebSearch -> ToolboxWebSearchPage(
                        viewModel = viewModel,
                        backdrop = backdrop,
                        onBack = { currentPage = ToolboxPage.List }
                    )
                    ToolboxPage.StreamOutput -> ToolboxStreamOutputPage(
                        viewModel = viewModel,
                        backdrop = backdrop,
                        onBack = { currentPage = ToolboxPage.List }
                    )
                    ToolboxPage.ContextSize -> ToolboxContextSizePage(
                        viewModel = viewModel,
                        backdrop = backdrop,
                        onBack = { currentPage = ToolboxPage.List }
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolboxListPage(
    viewModel: ChatViewModel,
    backdrop: Backdrop,
    onNavigate: (ToolboxPage) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // 标题栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "快捷配置",
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 4f)
                )
            )
            LiquidButton(
                onClick = onDismiss,
                backdrop = backdrop,
                modifier = Modifier.size(44.dp),
                isInteractive = true,
                tint = Color(0xFF1C1C1E).copy(alpha = 0.4f)
            ) {
                Icon(
                    imageVector = Lucide.X,
                    contentDescription = "关闭",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        // 配置项列表
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 思考预算
            ToolboxListItem(
                title = "思考预算",
                value = if (viewModel.thinkingBudget == 0) "关闭" else "${viewModel.thinkingBudget} tokens",
                backdrop = backdrop,
                onClick = { onNavigate(ToolboxPage.ThinkingBudget) }
            )
            
            // 网络搜索
            ToolboxListItem(
                title = "网络搜索",
                value = if (viewModel.webSearchEnabled) "已开启" else "已关闭",
                backdrop = backdrop,
                onClick = { onNavigate(ToolboxPage.WebSearch) }
            )
            
            // 流式输出
            ToolboxListItem(
                title = "流式输出",
                value = if (viewModel.streamEnabled) "已开启" else "已关闭",
                backdrop = backdrop,
                onClick = { onNavigate(ToolboxPage.StreamOutput) }
            )
            
            // 上下文长度
            ToolboxListItem(
                title = "上下文长度",
                value = "${viewModel.contextSize} 条消息",
                backdrop = backdrop,
                onClick = { onNavigate(ToolboxPage.ContextSize) }
            )
        }
    }
}

@Composable
private fun ToolboxListItem(
    title: String,
    value: String,
    backdrop: Backdrop,
    onClick: () -> Unit
) {
    LiquidButton(
        onClick = onClick,
        backdrop = backdrop,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        isInteractive = true,
        tint = Color.White.copy(alpha = 0.2f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                style = TextStyle(
                    shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 4f)
                )
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = value,
                    color = Color.White, // 增加可见度
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    style = TextStyle(
                        shadow = Shadow(color = Color.Black.copy(alpha = 0.6f), blurRadius = 4f)
                    )
                )
                Icon(
                    imageVector = Lucide.ChevronRight,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.8f), // 增加箭头可见度
                    modifier = Modifier.size(18.dp) // 稍微增大箭头
                )
            }
        }
    }
}

@Composable
private fun ToolboxDetailHeader(
    title: String,
    backdrop: Backdrop,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LiquidButton(
            onClick = onBack,
            backdrop = backdrop,
            modifier = Modifier.size(44.dp),
            isInteractive = true,
            tint = Color(0xFF1C1C1E).copy(alpha = 0.4f)
        ) {
            Icon(
                imageVector = Lucide.ChevronLeft,
                contentDescription = "返回",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(
                color = Color.White,
                fontWeight = FontWeight.Bold,
                shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 4f)
            )
        )
    }
}

@Composable
private fun ToolboxThinkingBudgetPage(
    viewModel: ChatViewModel,
    backdrop: Backdrop,
    onBack: () -> Unit
) {
    // 根据 thinkingBudget 值确定当前级别
    val currentLevel = when (viewModel.thinkingBudget) {
        0 -> "off"
        in 1..4096 -> "low"
        in 4097..16000 -> "medium"
        else -> "high"
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        ToolboxDetailHeader(title = "深度思考", backdrop = backdrop, onBack = onBack)
        
        // 当前状态显示
        val statusText = when (currentLevel) {
            "off" -> "已关闭"
            "low" -> "轻度思考"
            "medium" -> "中度思考"
            else -> "深度思考"
        }
        Text(
            text = statusText,
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 24.dp)
        )
        
        // 级别选项卡片
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 关闭
            ReasoningLevelCard(
                title = "关闭",
                description = "不使用深度思考，直接生成回复",
                isSelected = currentLevel == "off",
                backdrop = backdrop,
                onClick = { viewModel.updateThinkingBudget(0) }
            )
            
            // 轻度
            ReasoningLevelCard(
                title = "轻度",
                description = "简单推理，适合一般问答",
                isSelected = currentLevel == "low",
                backdrop = backdrop,
                onClick = { viewModel.updateThinkingBudget(1024) }
            )
            
            // 中度
            ReasoningLevelCard(
                title = "中度",
                description = "较深入推理，适合复杂问题",
                isSelected = currentLevel == "medium",
                backdrop = backdrop,
                onClick = { viewModel.updateThinkingBudget(8192) }
            )
            
            // 深度
            ReasoningLevelCard(
                title = "深度",
                description = "最强推理能力，适合数学、编程等",
                isSelected = currentLevel == "high",
                backdrop = backdrop,
                onClick = { viewModel.updateThinkingBudget(32000) }
            )
        }
        
        // 黄色警告提示
        Spacer(Modifier.height(16.dp))
        Text(
            text = "⚠️ 部分模型不支持此功能",
            color = Color(0xFFFFCC00),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            style = TextStyle(
                shadow = Shadow(color = Color.Black.copy(alpha = 0.6f), blurRadius = 4f)
            )
        )
        
        // 说明文字
        Spacer(Modifier.height(12.dp))
        Text(
            text = "深度思考让 AI 在回复前进行推理。需要模型支持此功能（如 o1、DeepSeek R1 等）。",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 13.sp,
            lineHeight = 20.sp,
            style = TextStyle(
                shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 4f)
            )
        )
    }
}

@Composable
private fun ReasoningLevelCard(
    title: String,
    description: String,
    isSelected: Boolean,
    backdrop: Backdrop,
    onClick: () -> Unit
) {
    LiquidButton(
        onClick = onClick,
        backdrop = backdrop,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        isInteractive = true,
        tint = if (isSelected) Color(0xFF007AFF).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.35f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    style = LocalTextStyle.current.copy(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.6f),
                            blurRadius = 4f
                        )
                    )
                )
                Text(
                    text = description,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    style = LocalTextStyle.current.copy(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.6f),
                            blurRadius = 4f
                        )
                    )
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Lucide.Check,
                    contentDescription = null,
                    tint = Color(0xFF34C759),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ToolboxWebSearchPage(
    viewModel: ChatViewModel,
    backdrop: Backdrop,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        ToolboxDetailHeader(title = "网络搜索", backdrop = backdrop, onBack = onBack)
        
        // 开关
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "启用网络搜索",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                style = TextStyle(
                    shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 4f)
                )
            )
            com.liquidglass.fluxhub.components.LiquidToggle(
                selected = { viewModel.webSearchEnabled },
                onSelect = { viewModel.updateWebSearchEnabled(it) },
                backdrop = backdrop,
                modifier = Modifier.size(width = 64.dp, height = 36.dp)
            )
        }
        
        // 说明文字
        Text(
            text = "启用后，AI 将能够搜索互联网获取最新信息来回答您的问题。这对于查询实时数据、新闻和不在 AI 训练数据中的内容特别有用。",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 13.sp,
            lineHeight = 20.sp,
            style = TextStyle(
                shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 4f)
            )
        )
        
        Spacer(Modifier.height(16.dp))
        
        // API 支持提示
        Text(
            text = "⚠️ 注意：此功能需要 API 提供商支持。部分 API（如 OpenAI 标准接口）可能不支持网络搜索，开启后可能无效果。请确认您的服务商是否支持此功能。",
            color = Color(0xFFFFCC00).copy(alpha = 0.8f),
            fontSize = 12.sp,
            lineHeight = 18.sp,
            style = TextStyle(
                shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 4f)
            )
        )
    }
}

@Composable
private fun ToolboxStreamOutputPage(
    viewModel: ChatViewModel,
    backdrop: Backdrop,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        ToolboxDetailHeader(title = "流式输出", backdrop = backdrop, onBack = onBack)
        
        // 开关
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "启用流式输出",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                style = TextStyle(
                    shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 4f)
                )
            )
            com.liquidglass.fluxhub.components.LiquidToggle(
                selected = { viewModel.streamEnabled },
                onSelect = { viewModel.updateStreamEnabled(it) },
                backdrop = backdrop,
                modifier = Modifier.size(width = 64.dp, height = 36.dp)
            )
        }
        
        // 说明文字
        Text(
            text = "流式输出会逐字显示 AI 的回复，让您无需等待完整回复即可开始阅读。关闭后，将在 AI 完成全部思考后一次性显示完整回复。",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 13.sp,
            lineHeight = 20.sp,
            style = TextStyle(
                shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 4f)
            )
        )
    }
}

@Composable
private fun ToolboxContextSizePage(
    viewModel: ChatViewModel,
    backdrop: Backdrop,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        ToolboxDetailHeader(title = "上下文长度", backdrop = backdrop, onBack = onBack)
        
        // 当前值显示
        Text(
            text = "${viewModel.contextSize} 条消息",
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 24.dp)
        )
        
        // 滑块 - 带黄色边框和警告提示
        var sliderValue by remember { mutableFloatStateOf(viewModel.contextSize.toFloat()) }
        LaunchedEffect(viewModel.contextSize) {
            if (sliderValue.toInt() != viewModel.contextSize) {
                sliderValue = viewModel.contextSize.toFloat()
            }
        }
        
        // 黄色边框包裹滑块
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 2.dp,
                    color = Color(0xFFFFCC00).copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(12.dp)
        ) {
            Column {
                com.liquidglass.fluxhub.components.LiquidSlider(
                    value = { sliderValue },
                    onValueChange = { 
                        sliderValue = it
                        viewModel.updateContextSize(it.toInt()) 
                    },
                    valueRange = 1f..128f,
                    visibilityThreshold = 4f,
                    backdrop = backdrop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "⚠️ 此拖动条有 Bug，拖动可能卡顿，推荐使用下方快捷档位",
                    color = Color(0xFFFFCC00),
                    fontSize = 12.sp,
                    style = TextStyle(
                        shadow = Shadow(color = Color.Black.copy(alpha = 0.6f), blurRadius = 4f)
                    )
                )
            }
        }
        
        // 快捷档位按钮
        Spacer(Modifier.height(24.dp))
        Text(
            text = "快捷档位",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp,
            style = TextStyle(
                shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 4f)
            ),
            modifier = Modifier.padding(bottom = 12.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val presets = listOf(8, 16, 32, 64, 96, 128)
            items(presets) { value ->
                LiquidButton(
                    onClick = { viewModel.updateContextSize(value) },
                    backdrop = backdrop,
                    modifier = Modifier.height(40.dp),
                    isInteractive = true,
                    tint = if (viewModel.contextSize == value) Color(0xFF007AFF).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "$value",
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        style = TextStyle(
                            shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 4f)
                        )
                    )
                }
            }
        }
        
        // 说明文字
        Spacer(Modifier.height(24.dp))
        Text(
            text = "上下文长度决定 AI 能「记住」多少条之前的对话消息。较大的值能让 AI 保持更好的对话连贯性，但会消耗更多 token 配额。",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 13.sp,
            lineHeight = 20.sp,
            style = TextStyle(
                shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 4f)
            )
        )
    }
}
