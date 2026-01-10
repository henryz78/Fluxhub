package com.liquidglass.fluxhub.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.liquidglass.fluxhub.utils.DampedDragAnimation
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.capsule.ContinuousCapsule
import kotlinx.coroutines.flow.collectLatest

@Composable
@Composable
fun LiquidToggle(
    selected: () -> Boolean,
    onSelect: (Boolean) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    val isSelected = selected()
    val density = LocalDensity.current
    val animationScope = rememberCoroutineScope()
    
    // Animation state
    val thumbOffset = remember { androidx.compose.animation.core.Animatable(if (isSelected) 1f else 0f) }
    
    LaunchedEffect(isSelected) {
        thumbOffset.animateTo(
            targetValue = if (isSelected) 1f else 0f,
            animationSpec = androidx.compose.animation.core.spring(
                dampingRatio = 0.7f,
                stiffness = 400f
            )
        )
    }

    // Colors
    val trackColor = if (isSelected) Color(0xFF34C759) else Color(0xFF787880).copy(alpha = 0.3f)
    val thumbColor = Color.White
    
    Box(
        modifier = modifier
            .size(width = 52.dp, height = 32.dp)
            .clip(ContinuousCapsule)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { ContinuousCapsule },
                effects = {
                    vibrancy()
                    blur(8f.dp.toPx()) 
                },
                onDrawSurface = {
                    drawRect(trackColor)
                }
            )
            .androidx.compose.foundation.clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { onSelect(!isSelected) },
        contentAlignment = Alignment.CenterStart
    ) {
        // Thumb
        Box(
            modifier = Modifier
                .padding(start = 2.dp, end = 2.dp) // Padding for track
                .align(Alignment.CenterStart)
                .graphicsLayer {
                    val maxOffset = 20.dp.toPx() // Total travel distance (52 - 32 + padding adj) approx
                    // More precise calculation: Width (52) - ThumbSize (28) - Padding (4) = 20
                    translationX = thumbOffset.value * maxOffset
                }
                .size(28.dp)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { androidx.compose.foundation.shape.CircleShape },
                    effects = {
                        vibrancy()
                    },
                    shadow = {
                         Shadow(
                            radius = 4f.dp,
                            color = Color.Black.copy(alpha = 0.15f),
                            offset = androidx.compose.ui.geometry.Offset(0f, 2f)
                        )
                    },
                    onDrawSurface = {
                        drawRect(thumbColor)
                    }
                )
        )
    }
}
