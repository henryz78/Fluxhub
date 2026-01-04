package com.liquidglass.fluxhub.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.capsule.ContinuousRoundedRectangle
import com.mikepenz.markdown.m3.Markdown

@Composable
fun ChatBubble(
    content: String,
    isUser: Boolean,
    isStreaming: Boolean = false,
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    val bubbleShape = ContinuousRoundedRectangle(16.dp)
    val backgroundColor = if (isUser) {
        Color(0xFF007AFF).copy(alpha = 0.3f)
    } else {
        Color.White.copy(alpha = 0.15f)
    }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { bubbleShape },
                    effects = {
                        vibrancy()
                        blur(12f.dp.toPx())
                    },
                    onDrawSurface = {
                        drawRect(backgroundColor)
                    }
                )
                .padding(12.dp)
        ) {
            if (content.isNotBlank()) {
                Markdown(
                    content = content,
                    modifier = Modifier
                )
            } else if (isStreaming) {
                Text(
                    text = "...",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
