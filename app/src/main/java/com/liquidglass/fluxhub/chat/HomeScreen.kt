package com.liquidglass.fluxhub.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.ContinuousRoundedRectangle
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MessageCircle
import com.composables.icons.lucide.Sparkles
import androidx.compose.material3.Icon
import androidx.compose.foundation.clickable

/**
 * 首页 - 欢迎页面
 */
@Composable
fun HomeScreen(
    backdrop: Backdrop,
    bottomPadding: PaddingValues = PaddingValues(0.dp),
    onNavigateToChat: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottomPadding),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            // Logo / App 标题
            Icon(
                imageVector = Lucide.Sparkles,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color(0xFFFFD700) // 金色
            )
            
            BasicText(
                text = "FluxHub",
                style = TextStyle(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            )
            
            BasicText(
                text = "与 AI 对话，探索无限可能",
                style = TextStyle(
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 开始对话按钮
            Box(
                modifier = Modifier
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { ContinuousRoundedRectangle(24.dp) },
                        effects = {
                            vibrancy()
                            blur(4.dp.toPx())
                        },
                        onDrawSurface = {
                            drawRect(Color(0xFF007AFF).copy(alpha = 0.3f))
                        }
                    )
                    .clickable { onNavigateToChat() }
                    .padding(horizontal = 32.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Lucide.MessageCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.White
                    )
                    BasicText(
                        text = "开始对话",
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    )
                }
            }
        }
    }
}
