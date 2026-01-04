package com.liquidglass.fluxhub.chat

import android.graphics.BitmapFactory
import android.graphics.Rect
import android.view.ViewTreeObserver
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
    var selectedTab by remember { mutableIntStateOf(0) }
    
    val backgroundBitmap = remember {
        BitmapFactory.decodeResource(context.resources, R.drawable.wallpaper_light)
    }
    
    val backdrop = rememberLayerBackdrop()
    
    // 监听键盘可见性
    val isKeyboardVisible = rememberIsKeyboardVisible()
    
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
            // 根据键盘状态动态调整底部 padding
            val bottomPadding = if (isKeyboardVisible) 0.dp else 100.dp
            
            when (selectedTab) {
                0 -> ChatScreen(
                    backdrop = backdrop,
                    bottomPadding = PaddingValues(bottom = bottomPadding), 
                    onNavigateToSettings = { selectedTab = 1 },
                    viewModel = viewModel
                )
                1 -> SettingsScreen(
                    onBack = { selectedTab = 0 },
                    viewModel = viewModel,
                    backdrop = backdrop, // 传递 backdrop
                    isTab = true,
                    bottomPadding = PaddingValues(bottom = bottomPadding)
                )
            }
        }
        
        // 底部导航栏
        // 键盘弹起时隐藏
        AnimatedVisibility(
            visible = !isKeyboardVisible,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp, start = 64.dp, end = 64.dp) // 增加 horizontal padding (48->64)
                    .widthIn(max = 280.dp) // 限制最大宽度 (400->280)
            ) {
                LiquidBottomTabs(
                    selectedTabIndex = { selectedTab },
                    onTabSelected = { selectedTab = it },
                    backdrop = backdrop,
                    tabsCount = 2,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Tab 0: Chat
                    LiquidBottomTab(
                        onClick = { selectedTab = 0 }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = "Chat",
                                tint = if (selectedTab == 0) Color(0xFF007AFF) else Color.Gray,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Chat",
                                style = TextStyle(
                                    fontSize = 10.sp,
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == 0) Color(0xFF007AFF) else Color.Gray
                                )
                            )
                        }
                    }
                    
                    // Tab 1: Settings
                    LiquidBottomTab(
                        onClick = { selectedTab = 1 }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Settings",
                                tint = if (selectedTab == 1) Color(0xFF007AFF) else Color.Gray,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Settings",
                                style = TextStyle(
                                    fontSize = 10.sp,
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == 1) Color(0xFF007AFF) else Color.Gray
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
