package com.liquidglass.fluxhub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liquidglass.fluxhub.chat.ChatViewModel
import com.liquidglass.fluxhub.chat.MainScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // 请求最高刷新率 (120Hz 如果设备支持)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            // 设置首选显示模式为最高刷新率
            val highRefreshMode = display?.supportedModes?.maxByOrNull { it.refreshRate }
            highRefreshMode?.let {
                window.attributes = window.attributes.apply {
                    preferredDisplayModeId = it.modeId
                }
            }
        }

        setContent {
            val chatViewModel: ChatViewModel = viewModel()
            // 观察主题设置
            val themeMode = chatViewModel.themeMode
            val isSystemDark = isSystemInDarkTheme()
            
            val isDarkTheme = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemDark
            }
            
            val colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()
            
            MaterialTheme(colorScheme = colorScheme) {
                MainScreen(viewModel = chatViewModel)
            }
        }
    }
}
