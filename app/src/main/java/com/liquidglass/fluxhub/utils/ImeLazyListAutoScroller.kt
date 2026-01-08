package com.liquidglass.fluxhub.utils

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalDensity

/**
 * 自动跟随键盘滚动的 Hook
 * 参考 RikkaHub 实现，修复键盘收起时往上滑的问题
 */
@Composable
fun ImeLazyListAutoScroller(
    lazyListState: LazyListState,
) {
    val ime = WindowInsets.ime
    val localDensity = LocalDensity.current
    var imeHeight by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(Unit) {
        snapshotFlow {
            ime.getBottom(localDensity)
        }.collect { keyboardHeight ->
            val delta = keyboardHeight - imeHeight
            if (delta != 0) {
                // 无论键盘弹出还是收起，都进行滚动补偿
                lazyListState.scrollBy(delta.toFloat())
            }
            imeHeight = keyboardHeight
        }
    }
}

