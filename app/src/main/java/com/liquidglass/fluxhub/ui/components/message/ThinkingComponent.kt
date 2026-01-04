package com.liquidglass.fluxhub.ui.components.message

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.kyant.backdrop.drawBackdrop
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Brain
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.ChevronUp
import com.liquidglass.fluxhub.ui.components.richtext.MarkdownBlock

@Composable
fun ThinkingComponent(
    content: String,
    isThinking: Boolean,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(true) }
    
    // 呼吸动画
    val infiniteTransition = rememberInfiniteTransition(label = "thinking")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(
                color = Color.White.copy(alpha = 0.03f),
                shape = RoundedCornerShape(16.dp)
            )
            .drawBackdrop(
                backdrop = com.kyant.backdrop.backdrops.rememberLayerBackdrop(), // 实际上应该传入外部的，这里暂用简化版
                shape = { RoundedCornerShape(16.dp) },
                effects = {
                    com.kyant.backdrop.effects.vibrancy()
                    com.kyant.backdrop.effects.blur(2.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = 0.05f))
                }
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Lucide.Sparkles,
                    contentDescription = null,
                    modifier = Modifier
                        .size(14.dp)
                        .then(if (isThinking) Modifier.alpha(alpha) else Modifier.alpha(0.6f)),
                    tint = Color.White
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isThinking) "思考中..." else "已思考",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.6f),
                    letterSpacing = 0.5.sp
                )
            }
            
            // 展开/收起图标
            Icon(
                imageVector = if (expanded) Lucide.ChevronUp else Lucide.ChevronDown,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = Color.White.copy(alpha = 0.4f)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                Spacer(Modifier.height(10.dp))
                // 使用 MarkdownBlock 渲染思考内容
                MarkdownBlock(
                    content = content,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                )
            }
        }
    }
}
