package com.liquidglass.fluxhub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liquidglass.fluxhub.chat.ChatViewModel
import com.liquidglass.fluxhub.chat.MainScreen

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

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
            
            // 禁用 overscroll 光晕效果，避免背景白色问题
            CompositionLocalProvider(
                LocalOverscrollConfiguration provides null
            ) {
                MaterialTheme(colorScheme = colorScheme) {
                    MainScreen(viewModel = chatViewModel)
                }
            }
        }
    }
}
