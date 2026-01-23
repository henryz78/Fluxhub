package com.liquidglass.fluxhub.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.SlidersHorizontal
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.capsule.ContinuousRoundedRectangle
import com.liquidglass.fluxhub.components.LiquidButton

/**
 * 聊天输入栏 - 液态玻璃风格
 */
@Composable
fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    isLoading: Boolean,
    isStreaming: Boolean = false,
    backdrop: Backdrop,
    onInteractionChanged: (Boolean) -> Unit = {},
    onPickImage: () -> Unit = {},
    onOpenToolbox: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 左侧按钮组：+ 和工具箱
        LiquidButton(
            onClick = onPickImage,
            backdrop = backdrop,
            modifier = Modifier.size(44.dp),
            isInteractive = true,
            onPressed = onInteractionChanged,
            tint = Color(0xFF34C759).copy(alpha = 0.8f)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add",
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
        
        LiquidButton(
            onClick = onOpenToolbox,
            backdrop = backdrop,
            modifier = Modifier.size(44.dp),
            isInteractive = true,
            onPressed = onInteractionChanged,
            tint = Color(0xFF9B59B6).copy(alpha = 0.7f)
        ) {
            Icon(
                imageVector = Lucide.SlidersHorizontal,
                contentDescription = "工具箱",
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }

        // 输入框（带滚动条）
        val scrollState = rememberScrollState()
        Box(
            modifier = Modifier
                .weight(1f)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { ContinuousRoundedRectangle(24.dp) },
                    effects = {
                        vibrancy()
                        blur(4f.dp.toPx())
                        lens(12f.dp.toPx(), 24f.dp.toPx())
                    },
                    highlight = { Highlight.Plain },
                    onDrawSurface = {
                        // 增加 alpha 值使输入框在深色主题下更清晰
                        drawRect(Color.White.copy(alpha = 0.25f))
                    }
                )
                .heightIn(min = 44.dp, max = 160.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = onTextChange,
                    enabled = true, // 始终启用，允许预先编辑下一条消息
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 4f)
                    ),
                    cursorBrush = SolidColor(Color(0xFF007AFF)),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() }),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .verticalScroll(scrollState),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (text.isEmpty()) {
                                BasicText(
                                    text = "输入消息...",
                                    style = TextStyle(
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 15.sp,
                                        lineHeight = 22.sp,
                                        shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 4f)
                                    )
                                )
                            }
                            innerTextField()
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                
                // 滚动条指示器（多行时显示）
                if (text.count { it == '\n' } > 1 || text.length > 80) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .fillMaxHeight()
                            .padding(vertical = 10.dp, horizontal = 2.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.5f))
                    )
                }
            }
        }
            
        // 发送/停止按钮
        val isGenerating = isLoading || isStreaming
        LiquidButton(
            onClick = if (isGenerating) onStop else onSend,
            backdrop = backdrop,
            modifier = Modifier.size(44.dp),
            isInteractive = isGenerating || text.isNotBlank(),
            onPressed = onInteractionChanged,
            tint = if (isGenerating) Color(0xFFFF3B30) else if (text.isNotBlank()) Color(0xFF007AFF) else Color.Gray.copy(alpha = 0.5f)
        ) {
            Icon(
                imageVector = if (isGenerating) Icons.Default.Stop else Icons.AutoMirrored.Filled.Send,
                contentDescription = if (isGenerating) "停止" else "发送",
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}
