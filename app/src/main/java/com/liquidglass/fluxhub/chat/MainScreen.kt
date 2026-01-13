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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset

@Composable
fun MainScreen(
    viewModel: ChatViewModel = viewModel()
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    
    val backgroundBitmap = remember(viewModel.wallpaperUri) {
        val uri = viewModel.wallpaperUri
        when {
            uri == null -> {
                // 默认壁纸
                BitmapFactory.decodeResource(context.resources, R.drawable.wallpaper_liquid)
            }
            uri.startsWith("preset:") -> {
                // 预设壁纸
                val presetName = uri.removePrefix("preset:")
                val resourceId = when (presetName) {
                    "wallpaper_liquid" -> R.drawable.wallpaper_liquid
                    "wallpaper_light" -> R.drawable.wallpaper_light
                    else -> R.drawable.wallpaper_liquid
                }
                BitmapFactory.decodeResource(context.resources, resourceId)
            }
            else -> {
                // 自定义壁纸
                try {
                    val parsedUri = android.net.Uri.parse(uri)
                    val stream = context.contentResolver.openInputStream(parsedUri)
                    BitmapFactory.decodeStream(stream)
                } catch (e: Exception) {
                    BitmapFactory.decodeResource(context.resources, R.drawable.wallpaper_liquid)
                }
            }
        }
    }
    
    val backdrop = rememberLayerBackdrop()
    
    // 认证状态检查 - 在应用入口级别
    val authState = viewModel.authState
    
    // 全局页面切换动画
    AnimatedContent(
        targetState = authState,
        transitionSpec = {
            (scaleIn(initialScale = 0.8f, animationSpec = tween(500)) + fadeIn(animationSpec = tween(500)))
                .togetherWith(scaleOut(targetScale = 1.2f, animationSpec = tween(500)) + fadeOut(animationSpec = tween(500)))
        },
        label = "AuthTransition",
        modifier = Modifier.fillMaxSize().background(Color.Black) // 默认黑底，防止转场白屏
    ) { targetState ->
        when (targetState) {
            is AuthState.Checking -> {
                // 显示加载中
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        androidx.compose.material3.CircularProgressIndicator(color = Color.White)
                        Spacer(Modifier.height(16.dp))
                        androidx.compose.material3.Text("正在加载液态玻璃...", color = Color.White)
                    }
                }
            }
            is AuthState.NotLoggedIn, is AuthState.Error -> {
                // 显示登录界面
                Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    // 背景图片
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
                    val msg = if (targetState is AuthState.Error) targetState.message else ""
                    // AuthScreen 不需要传递 authState，只需要是否检查中
                    AuthScreen(
                        backdrop = backdrop,
                        authState = targetState,
                        isCheckingAuth = viewModel.isCheckingAuth,
                        onLogin = { username, password ->
                            viewModel.login(username, password)
                        },
                        onRegister = { username, email, password, inviteCode ->
                            viewModel.register(username, email, password, inviteCode)
                        }
                    )
                }
            }
            is AuthState.Blocked -> {
                // 显示被封禁界面
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        androidx.compose.material3.Text("⛔", fontSize = 64.sp)
                        Spacer(Modifier.height(16.dp))
                        androidx.compose.material3.Text(
                            targetState.message,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                        Spacer(Modifier.height(24.dp))
                        androidx.compose.material3.TextButton(
                            onClick = { viewModel.logout() }
                        ) {
                            androidx.compose.material3.Text("切换账号", color = Color(0xFFFF3B30))
                        }
                    }
                }
            }
            is AuthState.Expired -> {
                // 显示过期续期界面
                Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    // 背景图片
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
                    ExpiredScreen(
                        backdrop = backdrop,
                        message = targetState.message,
                        isCheckingAuth = viewModel.isCheckingAuth,
                        onRenew = { inviteCode -> viewModel.renewAccount(inviteCode, targetState.username) },
                        onLogout = { viewModel.logout() }
                    )
                }
            }
            is AuthState.Authenticated -> {
                // 已认证，显示主应用内容
                AuthenticatedContent(
                    viewModel = viewModel,
                    backdrop = backdrop,
                    backgroundBitmap = backgroundBitmap
                )
            }
        }
    }
}

@Composable
private fun AuthenticatedContent(
    viewModel: ChatViewModel,
    backdrop: com.kyant.backdrop.backdrops.LayerBackdrop,
    backgroundBitmap: android.graphics.Bitmap?
) {
    val context = LocalContext.current
    // 默认打开首页 (Tab 0) - 每次进入已认证状态时重置
    var selectedTab by remember { mutableIntStateOf(0) }
    
    // 监听键盘可见性
    val isKeyboardVisible = rememberIsKeyboardVisible()
    
    // 为 ChatScreen 提升 drawerState
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    // val scope = rememberCoroutineScope() // Unused currently
    
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
            
            // 使用 AnimatedContent 切换页面，只渲染当前选中的页面
            // 避免不可见页面持续占用 GPU 资源
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
                                    (slideInHorizontally { it } + fadeIn()).togetherWith(
                                        slideOutHorizontally { -it } + fadeOut()
                                    )
                                } else {
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
                                if (targetState != null) {
                                    (slideInHorizontally { it } + fadeIn()).togetherWith(
                                        slideOutHorizontally { -it } + fadeOut()
                                    )
                                } else {
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
                                else -> SettingsScreen(
                                    onBack = { selectedTab = 1 },
                                    viewModel = viewModel,
                                    backdrop = backdrop,
                                    isTab = true,
                                    bottomPadding = PaddingValues(bottom = bottomPadding),
                                    onNavigateToAssistants = { settingsSubPage = "assistants" },
                                    onNavigateToProviders = { settingsSubPage = "providers" },
                                    onNavigateToDisplay = { settingsSubPage = "display_settings" }
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
                                modifier = Modifier
                                    .size(24.dp)
                            )
                            Spacer(Modifier.height(2.dp))
                            BasicText(
                                text = "首页",
                                style = TextStyle(
                                    fontSize = 10.sp,
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == 0) Color(0xFF007AFF) else Color.Gray,
                                    shadow = Shadow(color = Color.Black.copy(alpha = 0.2f), blurRadius = 4f, offset = Offset(1f, 1f))
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
                                modifier = Modifier
                                    .size(24.dp)
                            )
                            Spacer(Modifier.height(2.dp))
                            BasicText(
                                text = "对话",
                                style = TextStyle(
                                    fontSize = 10.sp,
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == 1) Color(0xFF007AFF) else Color.Gray,
                                    shadow = Shadow(color = Color.Black.copy(alpha = 0.2f), blurRadius = 4f, offset = Offset(1f, 1f))
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
                                modifier = Modifier
                                    .size(24.dp)
                            )
                            Spacer(Modifier.height(2.dp))
                            BasicText(
                                text = "设置",
                                style = TextStyle(
                                    fontSize = 10.sp,
                                    fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == 2) Color(0xFF007AFF) else Color.Gray,
                                    shadow = Shadow(color = Color.Black.copy(alpha = 0.2f), blurRadius = 4f, offset = Offset(1f, 1f))
                                )
                            )
                        }
                    }
                }
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
