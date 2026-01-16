package com.liquidglass.fluxhub.ui.components.message

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.composables.icons.lucide.*
import com.liquidglass.fluxhub.ui.components.richtext.MarkdownBlock
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * 思考过程展示组件
 * 参考 RikkaHub 的 ChatMessageReasoning 实现
 */
@Composable
fun ThinkingComponent(
    content: String,
    isThinking: Boolean,
    backdrop: Backdrop,
    shouldCollapse: Boolean = false,
    startTime: Long = remember { System.currentTimeMillis() },
    modifier: Modifier = Modifier
) {
    // 折叠状态：如果 shouldCollapse 且不在思考中，初始化为折叠
    var expanded by remember(shouldCollapse, isThinking) { 
        mutableStateOf(!shouldCollapse || isThinking) 
    }
    
    // 思考计时器
    var duration by remember { mutableStateOf(0L) }
    
    // 实时更新计时器
    LaunchedEffect(isThinking) {
        if (isThinking) {
            while (isActive) {
                duration = System.currentTimeMillis() - startTime
                delay(100)
            }
        }
    }
    
    // 自动折叠逻辑
    LaunchedEffect(shouldCollapse, isThinking) {
        if (shouldCollapse && !isThinking) {
            delay(800)
            expanded = false
        }
    }
    
    // Shimmer 动画 (仅在思考时运行)
    val shimmerAlpha = if (isThinking) {
        val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
        infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "shimmer_alpha"
        ).value
    } else {
        0.6f
    }
    
    // 格式化时长
    val durationText = remember(duration) {
        if (duration > 0) {
            val seconds = duration / 1000.0
            "(${String.format("%.1f", seconds)}s)"
        } else ""
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(
                color = Color(0xFFE3F2FD), // Light Blue
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 1.dp,
                color = Color(0xFF2196F3).copy(alpha = 0.3f), // Blue border
                shape = RoundedCornerShape(16.dp)
            )
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Lucide.Sparkles,
                    contentDescription = null,
                    modifier = Modifier
                        .size(14.dp)
                        .alpha(shimmerAlpha),
                    tint = Color(0xFF1976D2) // Dark Blue
                )
                
                Text(
                    text = if (isThinking) "思考中..." else "已思考",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black.copy(alpha = shimmerAlpha),
                    letterSpacing = 0.5.sp
                )
                
                if (durationText.isNotEmpty()) {
                    Text(
                        text = durationText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black.copy(alpha = if (isThinking) shimmerAlpha * 0.7f else 0.5f)
                    )
                }
            }
            
            Icon(
                imageVector = if (expanded) Lucide.ChevronUp else Lucide.ChevronDown,
                contentDescription = if (expanded) "收起" else "展开",
                modifier = Modifier.size(14.dp),
                tint = Color.Black.copy(alpha = 0.4f)
            )
        }

        // 使用简单的 if 替代 AnimatedVisibility 以减少动画开销
        if (expanded) {
            val scrollState = rememberScrollState()
            
            // 内部自动滚动 (仅当 Thinking 在进行时)
            LaunchedEffect(content.length, isThinking) {
                if (isThinking) {
                    scrollState.animateScrollTo(scrollState.maxValue)
                }
            }
            
            // 阻止嵌套滚动传播到父容器 (LazyColumn)
            val nestedScrollConnection = remember {
                object : NestedScrollConnection {
                    override fun onPostScroll(
                        consumed: Offset,
                        available: Offset,
                        source: NestedScrollSource
                    ): Offset {
                        // 消费所有剩余的滚动量，防止传播给父级
                        return available
                    }
                }
            }
            
            Column {
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        // 恢复最大高度限制，形成嵌套滚动
                        .heightIn(max = 240.dp)
                        .nestedScroll(nestedScrollConnection)
                        .verticalScroll(scrollState)
                        .graphicsLayer() // 开启硬件加速图层，优化滚动性能
                ) {
                    MarkdownBlock(
                        content = content,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF333333).copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    )
                }
            }
        }
    }
}
