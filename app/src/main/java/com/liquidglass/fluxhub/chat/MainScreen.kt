package com.liquidglass.fluxhub.chat

import android.graphics.BitmapFactory
import android.graphics.Rect
import android.view.ViewTreeObserver
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.liquidglass.fluxhub.R
import com.liquidglass.fluxhub.components.LiquidBottomTab
import com.liquidglass.fluxhub.components.LiquidBottomTabs
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.effects.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun MainScreen(
    viewModel: ChatViewModel = viewModel()
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    // 默认打开首页 (Tab 0)
    var selectedTab by remember { mutableIntStateOf(0) }
    
    val backgroundBitmap = remember(viewModel.wallpaperUri) {
        if (viewModel.wallpaperUri != null) {
            try {
                val uri = android.net.Uri.parse(viewModel.wallpaperUri)
                val stream = context.contentResolver.openInputStream(uri)
                BitmapFactory.decodeStream(stream)
            } catch (e: Exception) {
                BitmapFactory.decodeResource(context.resources, R.drawable.wallpaper_light)
            }
        } else {
            BitmapFactory.decodeResource(context.resources, R.drawable.wallpaper_light)
        }
    }
    
    val backdrop = rememberLayerBackdrop()
    
    // 监听键盘可见性
    val isKeyboardVisible = rememberIsKeyboardVisible()
    
    // 为 ChatScreen 提升 drawerState
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // 将 listState 提升到 MainScreen 级别，保持滚动位置
    val chatListState = rememberLazyListState()
    
    // 快捷提示词状态
    var pendingPrompt by remember { mutableStateOf<String?>(null) }
    
    // 保持各页面的子页面状态 (Moved to top scope for access in Bottom Bar)
    var chatSubPage by remember { mutableStateOf<String?>(null) }
    var settingsSubPage by remember { mutableStateOf<String?>(null) }
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // 背景图片（共享）
        if (backgroundBitmap != null) {
            Image(
                bitmap = backgroundBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .layerBackdrop(backdrop)
                    .fillMaxSize()
            )
        }
        
        // 内容区域
        Box(modifier = Modifier.fillMaxSize()) {
            // 使用 WindowInsets 计算底部 Padding，实现与键盘的完美物理同步
            // 当键盘高度 < 80dp 时，Padding 补偿剩余高度
            // 当键盘高度 >= 80dp 时，Padding 为 0 (由内部 imePadding 接管)
            // 这样总高度始终 >= 80dp (键盘收起时) 或 = 键盘高度 (键盘弹出时)
            val density = LocalDensity.current
            val imeHeight = WindowInsets.ime.getBottom(density)
            val imeHeightDp = with(density) { imeHeight.toDp() }
            val bottomPadding = (100.dp - imeHeightDp).coerceAtLeast(0.dp) // 增加到100dp避免重叠
            
            // 预加载所有页面：消除首次切换的编译卡顿
            // 使用 key 保持状态，但只显示当前选中的页面
            // 这样避免了 z-index 和触摸事件冲突
            
            // 保持各页面的子页面状态 (Hoisted)
            
            // BackHandler 处理
            BackHandler(enabled = chatSubPage != null && selectedTab == 1) {
                chatSubPage = null
            }
            BackHandler(enabled = settingsSubPage != null && selectedTab == 2) {
                settingsSubPage = null
            }
            
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(300)) + 
                     scaleIn(initialScale = 0.98f, animationSpec = tween(300))).togetherWith(
                     fadeOut(animationSpec = tween(300)))
                },
                label = "TabContent"
            ) { targetTab ->
                when (targetTab) {
                    0 -> HomeScreen(
                        backdrop = backdrop,
                        bottomPadding = PaddingValues(bottom = bottomPadding),
                        onNavigateToChat = { selectedTab = 1 },
                        onQuickPrompt = { prompt ->
                            pendingPrompt = prompt
                            selectedTab = 1
                        },
                        viewModel = viewModel
                    )
                    1 -> {
                        AnimatedContent(
                            targetState = chatSubPage,
                            transitionSpec = {
                                if (targetState != null) {
                                    // 进入子页面
                                    (slideInHorizontally { it } + fadeIn()).togetherWith(
                                        slideOutHorizontally { -it } + fadeOut()
                                    )
                                } else {
                                    // 返回聊天页
                                    (slideInHorizontally { -it } + fadeIn()).togetherWith(
                                        slideOutHorizontally { it } + fadeOut()
                                    )
                                }
                            },
                            label = "ChatSubPage"
                        ) { subPage ->
                            if (subPage == "assistant_selection") {
                                AssistantListScreen(
                                    onBack = { chatSubPage = null },
                                    viewModel = viewModel,
                                    backdrop = backdrop,
                                    bottomPadding = PaddingValues(bottom = bottomPadding),
                                    isSelectionMode = true
                                )
                            } else {
                                ChatScreen(
                                    backdrop = backdrop,
                                    bottomPadding = PaddingValues(bottom = bottomPadding), 
                                    onNavigateToSettings = { selectedTab = 2 },
                                    onNavigateToAssistantSelection = { chatSubPage = "assistant_selection" },
                                    viewModel = viewModel,
                                    listState = chatListState,
                                    drawerState = drawerState,
                                    initialPrompt = pendingPrompt,
                                    onPromptConsumed = { pendingPrompt = null }
                                )
                            }
                        }
                    }
                    2 -> {
                        AnimatedContent(
                            targetState = settingsSubPage,
                            transitionSpec = {
                                // 进入子页面：从右滑入，主页面往左滑出
                                // 返回主页面：从左滑入，子页面往右滑出
                                if (targetState != null) {
                                    // 进入子页面
                                    (slideInHorizontally { it } + fadeIn()).togetherWith(
                                        slideOutHorizontally { -it } + fadeOut()
                                    )
                                } else {
                                    // 返回主设置页
                                    (slideInHorizontally { -it } + fadeIn()).togetherWith(
                                        slideOutHorizontally { it } + fadeOut()
                                    )
                                }
                            },
                            label = "SettingsSubPage"
                        ) { subPage ->
                            when (subPage) {
                                "assistants" -> AssistantListScreen(
                                    onBack = { settingsSubPage = null },
                                    viewModel = viewModel,
                                    backdrop = backdrop,
                                    bottomPadding = PaddingValues(bottom = bottomPadding)
                                )
                                "providers" -> ProviderListScreen(
                                    onBack = { settingsSubPage = null },
                                    viewModel = viewModel,
                                    backdrop = backdrop,
                                    bottomPadding = PaddingValues(bottom = bottomPadding)
                                )
                                "display_settings" -> DisplaySettingsScreen(
                                    onBack = { settingsSubPage = null },
                                    viewModel = viewModel,
                                    backdrop = backdrop,
                                    bottomPadding = PaddingValues(bottom = bottomPadding)
                                )
                                "dynamic_island_settings" -> DynamicIslandSettingsScreen(
                                    onBack = { settingsSubPage = null },
                                    viewModel = viewModel,
                                    backdrop = backdrop,
                                    bottomPadding = PaddingValues(bottom = bottomPadding)
                                )
                                else -> SettingsScreen(
                                    onBack = { selectedTab = 1 },
                                    viewModel = viewModel,
                                    backdrop = backdrop,
                                    isTab = true,
                                    bottomPadding = PaddingValues(bottom = bottomPadding),
                                    onNavigateToAssistants = { settingsSubPage = "assistants" },
                                    onNavigateToProviders = { settingsSubPage = "providers" },
                                    onNavigateToDisplay = { settingsSubPage = "display_settings" },
                                    onNavigateToDynamicIsland = { settingsSubPage = "dynamic_island_settings" }
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // 底部导航栏
        // 使用 Offset 实现键盘跟随动画，仅在 Drawer 打开时使用 AnimatedVisibility 隐藏
        // 解决键盘收起时的弹跳问题
        val navDensity = LocalDensity.current
        val navImeHeight = WindowInsets.ime.getBottom(navDensity)
        val navImeHeightDp = with(navDensity) { navImeHeight.toDp() }

        AnimatedVisibility(
            visible = drawerState.isClosed,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = navImeHeightDp)
        ) {
            Box(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 8.dp, start = 72.dp, end = 72.dp)
                    .widthIn(max = 220.dp) // 进一步缩短导航栏宽度
            ) {
                LiquidBottomTabs(
                    selectedTabIndex = { selectedTab },
                    onTabSelected = { 
                        selectedTab = it
                        if (viewModel.hapticFeedbackEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    },
                    backdrop = backdrop,
                    tabsCount = 3,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Tab 0: Home
                    LiquidBottomTab(
                        onClick = { 
                            if (viewModel.hapticFeedbackEnabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            selectedTab = 0 
                            settingsSubPage = null // Reset Settings navigation
                        }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Home,
                                contentDescription = "Home",
                                tint = if (selectedTab == 0) Color(0xFF007AFF) else Color.Gray,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.height(2.dp))
                            BasicText(
                                text = "首页",
                                style = TextStyle(
                                    fontSize = 10.sp,
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == 0) Color(0xFF007AFF) else Color.Gray
                                )
                            )
                        }
                    }
                    
                    // Tab 1: Chat
                    LiquidBottomTab(
                        onClick = { 
                            if (viewModel.hapticFeedbackEnabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            selectedTab = 1 
                            settingsSubPage = null // Reset Settings navigation
                        }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = "Chat",
                                tint = if (selectedTab == 1) Color(0xFF007AFF) else Color.Gray,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.height(2.dp))
                            BasicText(
                                text = "对话",
                                style = TextStyle(
                                    fontSize = 10.sp,
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == 1) Color(0xFF007AFF) else Color.Gray
                                )
                            )
                        }
                    }
                    
                    // Tab 2: Settings
                    LiquidBottomTab(
                        onClick = { 
                            if (viewModel.hapticFeedbackEnabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            selectedTab = 2 
                        }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Settings",
                                tint = if (selectedTab == 2) Color(0xFF007AFF) else Color.Gray,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.height(2.dp))
                            BasicText(
                                text = "设置",
                                style = TextStyle(
                                    fontSize = 10.sp,
                                    fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == 2) Color(0xFF007AFF) else Color.Gray
                                )
                            )
                        }
                    }
                }
            }
        }
        
        // ========== 全局灵动岛 ==========
        // 同步设置到控制器
        LaunchedEffect(
            viewModel.dynamicIslandEnabled,
            viewModel.loginNotificationMode,
            viewModel.showTokenCount,
            viewModel.showElapsedTime
        ) {
            com.liquidglass.fluxhub.chat.ui.components.DynamicIslandController.apply {
                isEnabled = viewModel.dynamicIslandEnabled
                loginNotificationMode = viewModel.loginNotificationMode
                showTokenCountEnabled = viewModel.showTokenCount
                showElapsedTimeEnabled = viewModel.showElapsedTime
            }
        }
        
        // 全局灵动岛 UI
        com.liquidglass.fluxhub.chat.ui.components.DynamicIsland(
            data = com.liquidglass.fluxhub.chat.ui.components.DynamicIslandController.toData(),
            backdrop = backdrop,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding(),
            onExpand = { com.liquidglass.fluxhub.chat.ui.components.DynamicIslandController.expand() },
            onCollapse = { com.liquidglass.fluxhub.chat.ui.components.DynamicIslandController.collapse() },
            onLongPress = { com.liquidglass.fluxhub.chat.ui.components.DynamicIslandController.showLongPressMenu() },
            onStopGeneration = { viewModel.stopGeneration() },
            onDismiss = { com.liquidglass.fluxhub.chat.ui.components.DynamicIslandController.hide() }
        )
        
        // 登录成功通知（仅在首页显示）
        var hasShownLoginSuccess by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
        // 跟踪是否已经在本次应用启动中显示过（用于 "every" 模式，每次启动只显示一次）
        var hasShownThisSession by remember { mutableStateOf(false) }
        
        LaunchedEffect(selectedTab) {
            // 仅在首页（Tab 0）显示
            if (selectedTab != 0) return@LaunchedEffect
            
            val controller = com.liquidglass.fluxhub.chat.ui.components.DynamicIslandController
            
            // 等待 1 秒让 DataStore 设置加载完成
            kotlinx.coroutines.delay(1000)
            
            // 检查是否启用灵动岛
            if (!controller.isEnabled) return@LaunchedEffect
            
            // 检查通知模式
            val shouldShow = when (controller.loginNotificationMode) {
                "every" -> !hasShownThisSession // 每次进入软件显示一次
                "first" -> !hasShownLoginSuccess // 仅登录成功后显示（跨 session 持久化）
                else -> !hasShownLoginSuccess
            }
            
            if (shouldShow) {
                hasShownLoginSuccess = true
                hasShownThisSession = true
                controller.showSuccess("登录成功")
            }
        }
        
        // 用户协议弹窗
        if (!viewModel.agreementAccepted) {
            AgreementDialog(
                backdrop = backdrop,
                onAccept = { viewModel.acceptAgreement() },
                onDecline = {
                    // 退出应用
                    (context as? android.app.Activity)?.finishAffinity()
                }
            )
        }
    }
}

@Composable
fun rememberIsKeyboardVisible(): Boolean {
    val view = LocalView.current
    var isKeyboardVisible by remember { mutableStateOf(false) }
    
    DisposableEffect(view) {
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            val insets = ViewCompat.getRootWindowInsets(view)
            val isVisible = insets?.isVisible(WindowInsetsCompat.Type.ime()) ?: false
            isKeyboardVisible = isVisible
        }
        
        view.viewTreeObserver.addOnGlobalLayoutListener(listener)
        onDispose {
            view.viewTreeObserver.removeOnGlobalLayoutListener(listener)
        }
    }
    
    return isKeyboardVisible
}

@Composable
private fun AgreementDialog(
    backdrop: com.kyant.backdrop.Backdrop,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    // 10秒倒计时
    var countdown by remember { mutableIntStateOf(10) }
    val canAccept = countdown <= 0
    
    LaunchedEffect(Unit) {
        while (countdown > 0) {
            kotlinx.coroutines.delay(1000)
            countdown--
        }
    }
    
    androidx.compose.ui.window.Dialog(
        onDismissRequest = { /* 不允许点击外部关闭 */ },
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(28.dp))
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { androidx.compose.foundation.shape.RoundedCornerShape(28.dp) },
                    effects = { 
                        vibrancy()
                        blur(20.dp.toPx())
                    },
                    onDrawSurface = { drawRect(Color.Black.copy(alpha = 0.7f)) }
                )
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 标题
                BasicText(
                    text = "用户协议与隐私政策",
                    style = TextStyle(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = Color.Black.copy(alpha = 0.5f),
                            blurRadius = 4f
                        )
                    ),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // 协议内容滚动区域
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .padding(16.dp)
                ) {
                    androidx.compose.foundation.lazy.LazyColumn {
                        item {
                            BasicText(
                                text = """
欢迎使用 FluxHub！

在您开始使用本应用之前，请仔细阅读以下协议内容：

【用户协议】

1. 服务说明
FluxHub 是一款 AI 对话应用，通过连接第三方 AI 服务提供商（如 OpenAI、Anthropic 等）为您提供智能对话服务。本应用仅作为连接工具，不直接提供 AI 模型服务。

2. 用户责任
• 您需要自行提供有效的 API 密钥
• 您对使用本应用产生的所有对话内容负责
• 禁止使用本应用进行任何违法活动
• 禁止生成、传播违法、有害或不当内容

3. 免责声明
• 本应用不对 AI 生成的内容的准确性、完整性做任何保证
• 因使用第三方 API 产生的费用由用户自行承担
• 本应用不对因网络问题、API 服务中断等造成的损失负责

【隐私政策】

1. 数据收集
• 本应用不收集您的个人身份信息
• 您的 API 密钥仅存储在本地设备，不会上传至任何服务器
• 对话记录仅保存在您的设备本地

2. 数据使用
• 所有对话数据直接发送至您配置的 AI 服务提供商
• 本应用不会分析、存储或分享您的对话内容

3. 数据安全
• 建议您定期更换 API 密钥
• 妥善保管您的设备，避免他人访问您的对话记录

4. 第三方服务
本应用使用的第三方 AI 服务受其各自隐私政策约束，请在使用前阅读相关服务商的隐私政策。

如您对本协议有任何疑问，请在使用前联系我们。

继续使用本应用即表示您已阅读并同意上述全部内容。
                                """.trimIndent(),
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.9f),
                                    lineHeight = 22.sp
                                )
                            )
                        }
                    }
                }
                
                Spacer(Modifier.height(20.dp))
                
                // 倒计时提示
                if (!canAccept) {
                    BasicText(
                        text = "请仔细阅读协议内容 (${countdown}秒后可操作)",
                        style = TextStyle(
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 12.dp)
                    )
                }
                
                // 按钮区域
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 拒绝按钮（可直接点击，不需等待）
                    com.liquidglass.fluxhub.components.LiquidButton(
                        onClick = onDecline,
                        backdrop = backdrop,
                        modifier = Modifier.weight(1f).height(50.dp),
                        isInteractive = true,
                        tint = Color.Red.copy(alpha = 0.5f)
                    ) {
                        BasicText(
                            text = "我拒绝",
                            style = TextStyle(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        )
                    }
                    
                    // 同意按钮
                    com.liquidglass.fluxhub.components.LiquidButton(
                        onClick = { if (canAccept) onAccept() },
                        backdrop = backdrop,
                        modifier = Modifier.weight(1.5f).height(50.dp),
                        isInteractive = canAccept,
                        tint = if (canAccept) Color(0xFF34C759) else Color.Gray.copy(alpha = 0.3f)
                    ) {
                        BasicText(
                            text = if (canAccept) "我已阅读并同意" else "请阅读协议...",
                            style = TextStyle(
                                color = if (canAccept) Color.White else Color.White.copy(alpha = 0.4f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
