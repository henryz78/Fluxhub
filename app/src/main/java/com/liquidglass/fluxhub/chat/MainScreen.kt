package com.liquidglass.fluxhub.chat

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
            when (selectedTab) {
                0 -> ChatScreen(
                    backdrop = backdrop,
                    bottomPadding = PaddingValues(bottom = 80.dp), // 留出导航栏空间
                    onNavigateToSettings = { selectedTab = 1 },
                    viewModel = viewModel
                )
                1 -> SettingsScreen(
                    onBack = { selectedTab = 0 },
                    viewModel = viewModel,
                    isTab = true,
                    bottomPadding = PaddingValues(bottom = 80.dp)
                )
            }
        }
        
        // 底部导航栏
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
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
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = "Chat",
                        tint = if (selectedTab == 0) Color(0xFF007AFF) else Color.Gray
                    )
                }
                
                // Tab 1: Settings
                LiquidBottomTab(
                    onClick = { selectedTab = 1 }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = if (selectedTab == 1) Color(0xFF007AFF) else Color.Gray
                    )
                }
            }
        }
    }
}
