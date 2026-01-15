package com.liquidglass.fluxhub.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicText
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.capsule.ContinuousRoundedRectangle
import com.liquidglass.fluxhub.data.Persona

/**
 * 灵动角色卡片组件
 * 采用水平布局：图标在左侧，文字在右侧
 */
@Composable
fun PersonaCard(
    persona: Persona,
    backdrop: Backdrop,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { ContinuousRoundedRectangle(20.dp) },
                effects = {
                    vibrancy()
                    blur(8.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(persona.color.copy(alpha = 0.5f))
                }
            )
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            // 左侧图标容器
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { ContinuousRoundedRectangle(14.dp) },
                        effects = { blur(0f) },
                        onDrawSurface = { drawRect(Color.White.copy(alpha = 0.2f)) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = persona.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(Modifier.width(14.dp))
            
            // 右侧文字信息
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                BasicText(
                    text = persona.name,
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 4f)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                BasicText(
                    text = persona.description,
                    style = TextStyle(
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        shadow = Shadow(color = Color.Black.copy(alpha = 0.3f), blurRadius = 2f)
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
