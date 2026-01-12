package com.liquidglass.fluxhub

import android.app.Application
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.ComposeView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 自定义 Application 类
 * 用于在应用启动时预热 Compose 运行时，减少首次页面切换的卡顿
 */
class FluxhubApplication : Application() {
    
    companion object {
        private const val TAG = "FluxhubApplication"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // 在后台线程预热 Compose 运行时
        // 这会触发 Compose 编译器的初始化，减少首次渲染时的卡顿
        CoroutineScope(Dispatchers.Default).launch {
            try {
                // 预热 Compose 内部状态
                // 这里不需要实际渲染任何内容，只需要触发 Compose 的类加载
                Class.forName("androidx.compose.runtime.ComposerKt")
                Class.forName("androidx.compose.ui.platform.AndroidComposeView")
                Class.forName("androidx.compose.foundation.layout.BoxKt")
                Class.forName("androidx.compose.material3.TextKt")
                Class.forName("androidx.compose.animation.AnimatedContentKt")
                Log.d(TAG, "Compose runtime prewarmed successfully")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to prewarm Compose runtime", e)
            }
        }
    }
}
