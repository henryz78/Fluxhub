package com.liquidglass.fluxhub.ui.components.message

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
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
    
    // Shimmer 动画
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )
    
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
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(16.dp) },
                effects = {
                    vibrancy()
                    blur(2.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = 0.08f))
                }
            )
            .padding(12.dp)
            .animateContentSize()
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
                        .alpha(if (isThinking) shimmerAlpha else 0.6f),
                    tint = Color(0xFFFFD700)
                )
                
                Text(
                    text = if (isThinking) "思考中..." else "已思考",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = if (isThinking) shimmerAlpha else 0.6f),
                    letterSpacing = 0.5.sp
                )
                
                if (durationText.isNotEmpty()) {
                    Text(
                        text = durationText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = if (isThinking) shimmerAlpha * 0.7f else 0.5f)
                    )
                }
            }
            
            Icon(
                imageVector = if (expanded) Lucide.ChevronUp else Lucide.ChevronDown,
                contentDescription = if (expanded) "收起" else "展开",
                modifier = Modifier.size(14.dp),
                tint = Color.White.copy(alpha = 0.4f)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(animationSpec = tween(300)) + fadeIn(),
            exit = shrinkVertically(animationSpec = tween(300)) + fadeOut()
        ) {
            Column {
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    MarkdownBlock(
                        content = content,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.5f),
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
