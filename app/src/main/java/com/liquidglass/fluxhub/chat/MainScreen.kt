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

@Composable
fun MainScreen(
    viewModel: ChatViewModel = viewModel()
) {
    val context = LocalContext.current
    // 默认打开首页 (Tab 0)
    var selectedTab by remember { mutableIntStateOf(0) }
    
    val backgroundBitmap = remember {
        BitmapFactory.decodeResource(context.resources, R.drawable.wallpaper_light)
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
            
            // 恢复使用 AnimatedContent 动画
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
                    1 -> ChatScreen(
                        backdrop = backdrop,
                        bottomPadding = PaddingValues(bottom = bottomPadding), 
                        onNavigateToSettings = { selectedTab = 2 },
                        viewModel = viewModel,
                        listState = chatListState,
                        drawerState = drawerState,
                        initialPrompt = pendingPrompt,
                        onPromptConsumed = { pendingPrompt = null }
                    )
                    2 -> {
                        // 设置子页面状态
                        var settingsSubPage by remember { mutableStateOf<String?>(null) }
                        
                        // 处理系统返回键
                        BackHandler(enabled = settingsSubPage != null) {
                            settingsSubPage = null
                        }
                        
                        // 带动画的子页面切换
                        AnimatedContent(
                            targetState = settingsSubPage,
                            transitionSpec = {
                                (slideInHorizontally { it } + fadeIn()).togetherWith(
                                    slideOutHorizontally { -it } + fadeOut()
                                )
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
                                "api_config" -> ApiConfigScreen(
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
                                else -> SettingsScreen(
                                    onBack = { selectedTab = 1 },
                                    viewModel = viewModel,
                                    backdrop = backdrop,
                                    isTab = true,
                                    bottomPadding = PaddingValues(bottom = bottomPadding),
                                    onNavigateToAssistants = { settingsSubPage = "assistants" },
                                    onNavigateToApiConfig = { settingsSubPage = "api_config" },
                                    onNavigateToProviders = { settingsSubPage = "providers" }
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
                    onTabSelected = { selectedTab = it },
                    backdrop = backdrop,
                    tabsCount = 3,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Tab 0: Home
                    LiquidBottomTab(
                        onClick = { selectedTab = 0 }
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
                        onClick = { selectedTab = 1 }
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
                        onClick = { selectedTab = 2 }
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
