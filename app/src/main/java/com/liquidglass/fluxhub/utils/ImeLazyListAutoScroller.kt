package com.liquidglass.fluxhub.utils

import androidx.compose.foundation.gestures.animateScrollBy
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
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * 自动跟随键盘滚动的 Hook
 * 参考 RikkaHub 实现，优化减少卡顿
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
        }
        .distinctUntilChanged() // 避免相同高度重复触发
        .collect { keyboardHeight ->
            val delta = keyboardHeight - imeHeight
            if (delta != 0) {
                // 使用 animateScrollBy 使滚动更平滑
                lazyListState.animateScrollBy(delta.toFloat())
            }
            imeHeight = keyboardHeight
        }
    }
}
