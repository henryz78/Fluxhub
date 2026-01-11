package com.liquidglass.fluxhub.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.capsule.ContinuousRoundedRectangle

@Composable
fun LiquidConfirmationDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    title: String,
    message: String,
    confirmText: String = "确定",
    dismissText: String = "取消",
    confirmButtonColor: Color = Color(0xFFFF453A), // 默认红色，适合删除
    icon: ImageVector? = null,
    iconColor: Color = Color(0xFFFF453A),
    backdrop: Backdrop
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { ContinuousRoundedRectangle(24.dp) },
                    effects = {
                        vibrancy()
                        blur(6f.dp.toPx())
                        lens(12f.dp.toPx(), 24f.dp.toPx())
                    },
                    highlight = { Highlight.Plain },
                    onDrawSurface = {
                        drawRect(Color.White.copy(alpha = 0.25f))
                    }
                )
                .drawBehind {
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.15f),
                        cornerRadius = CornerRadius(24.dp.toPx())
                    )
                }
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = message,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LiquidButton(
                        onClick = onDismissRequest,
                        backdrop = backdrop,
                        modifier = Modifier.weight(1f).height(44.dp),
                        tint = Color.White.copy(alpha = 0.2f)
                    ) {
                        Text(dismissText, color = Color.White, fontWeight = FontWeight.Medium)
                    }
                    
                    LiquidButton(
                        onClick = onConfirm,
                        backdrop = backdrop,
                        modifier = Modifier.weight(1f).height(44.dp),
                        tint = confirmButtonColor.copy(alpha = 0.6f)
                    ) {
                        Text(confirmText, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
