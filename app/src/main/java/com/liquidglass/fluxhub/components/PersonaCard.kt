package com.liquidglass.fluxhub.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.effects.saturation
import com.kyant.backdrop.highlight.Highlight
import com.kyant.capsule.ContinuousRoundedRectangle
import com.liquidglass.fluxhub.data.Persona

@Composable
fun PersonaCard(
    persona: Persona,
    backdrop: Backdrop,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(ContinuousRoundedRectangle(24.dp))
            .clickable(onClick = onClick)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { ContinuousRoundedRectangle(24.dp) },
                effects = {
                    vibrancy()
                    blur(8.dp.toPx()) // 更强的模糊
                    saturation(1.2f)
                },
                highlight = { Highlight.Plain },
                onDrawSurface = {
                    // 使用角色颜色进行微弱着色
                    drawRect(persona.color.copy(alpha = 0.15f))
                    drawRect(Color.White.copy(alpha = 0.1f))
                }
            )
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Icon Container
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { ContinuousRoundedRectangle(12.dp) },
                        effects = {
                             blur(4.dp.toPx())
                        },
                        onDrawSurface = {
                            drawRect(persona.color.copy(alpha = 0.8f))
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = persona.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(Modifier.height(16.dp))
            
            Column {
                Text(
                    text = persona.name,
                    style = MaterialTheme.typography.titleLarge.copy(
                        shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 4f)
                    ),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(Modifier.height(4.dp))
                
                Text(
                    text = persona.description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 2f)
                    ),
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
