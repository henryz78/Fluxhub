package com.liquidglass.fluxhub.chat.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.backdrops.rememberBackdrop
import com.composables.icons.lucide.*
import com.composables.icons.lucide.Lucide
import com.liquidglass.fluxhub.components.LiquidButton
import kotlin.math.PI
import kotlin.math.sin

enum class DynamicIslandState {
    Hidden,
    Collapsed,
    Expanded,
    LongPressMenu
}

data class DynamicIslandData(
    val title: String = "正在思考...",
    val modelName: String? = null,
    val assistantAvatar: String? = null,
    val state: DynamicIslandState = DynamicIslandState.Hidden,
    val tokenCount: Int = 0,          // 已生成 token 数量
    val elapsedSeconds: Int = 0,      // 已耗时秒数
    val isCompleted: Boolean = false  // 是否已完成（用于显示完成动画）
)

@Composable
fun DynamicIsland(
    data: DynamicIslandData,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    onExpand: () -> Unit = {},
    onCollapse: () -> Unit = {},
    onLongPress: () -> Unit = {},
    onStopGeneration: () -> Unit = {},
    onDismiss: () -> Unit = {} // Added for manual dismiss if needed
) {
    // 是否显示（用于 AnimatedVisibility）
    val isVisible = data.state != DynamicIslandState.Hidden
    
    // 内部状态转换动画（用于 Collapsed/Expanded/LongPressMenu 之间切换）
    val transition = updateTransition(targetState = data.state, label = "DynamicIslandTransition")
    
    // 宽度动画 - 更慢的弹簧
    val width by transition.animateDp(
        transitionSpec = {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        },
        label = "width"
    ) { state ->
        when (state) {
            DynamicIslandState.Hidden -> 180.dp // 保持初始大小，让 AnimatedVisibility 处理缩放
            DynamicIslandState.Collapsed -> 180.dp
            DynamicIslandState.Expanded -> 340.dp
            DynamicIslandState.LongPressMenu -> 280.dp
        }
    }

    // 高度动画 - 更慢的弹簧
    val height by transition.animateDp(
        transitionSpec = {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        },
        label = "height"
    ) { state ->
        when (state) {
            DynamicIslandState.Hidden -> 36.dp
            DynamicIslandState.Collapsed -> 36.dp
            DynamicIslandState.Expanded -> 140.dp
            DynamicIslandState.LongPressMenu -> 160.dp
        }
    }

    // 使用 AnimatedVisibility 处理进入/退出动画
    AnimatedVisibility(
        visible = isVisible,
        modifier = modifier, // 对齐 modifier 放在这里
        enter = scaleIn(
            initialScale = 0.5f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            transformOrigin = TransformOrigin(0.5f, 0f) // 从顶部中心缩放
        ) + fadeIn(animationSpec = tween(400)),
        exit = scaleOut(
            targetScale = 0.5f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessLow
            ),
            transformOrigin = TransformOrigin(0.5f, 0f)
        ) + fadeOut(animationSpec = tween(400))
    ) {
        Box(
            modifier = Modifier
                .padding(top = 8.dp) // 距离顶部的间距
                .size(width, height)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(40.dp) },
                    effects = {
                        vibrancy()
                        blur(20f.dp.toPx())
                    },
                    onDrawSurface = {
                        drawRect(Color.Black.copy(alpha = 0.6f)) // 深色半透明背景
                    }
                )
                .clip(RoundedCornerShape(40.dp))
                .combinedClickable(
                    onClick = {
                        if (data.state == DynamicIslandState.Collapsed) {
                            onExpand()
                        } else {
                            onCollapse()
                        }
                    },
                    onLongClick = {
                        if (data.state == DynamicIslandState.Collapsed) {
                            onLongPress()
                        }
                    }
                )
        ) {
            // 波浪动画背景 (仅在非菜单状态且非隐藏状态显示)
            if (data.state != DynamicIslandState.LongPressMenu) {
                WaveAnimationBackground()
            }

            // 内容区域
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.animation.AnimatedContent(
                    targetState = data.state,
                    label = "content"
                ) { targetState ->
                    when (targetState) {
                        DynamicIslandState.Collapsed -> CollapsedContent(data)
                        DynamicIslandState.Expanded -> ExpandedContent(data)
                        DynamicIslandState.LongPressMenu -> LongPressMenuContent(data, onStopGeneration, onCollapse, backdrop)
                        else -> {}
                    }
                }
            }
        }
    }
}

@Composable
private fun CollapsedContent(data: DynamicIslandData) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        if (data.isCompleted) {
            // 完成状态：显示动画勾
            AnimatedCheckmark(modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "完成",
                style = MaterialTheme.typography.labelLarge.copy(
                    color = Color(0xFF34C759), // 绿色
                    fontWeight = FontWeight.Bold
                )
            )
        } else {
            // 加载状态：显示旋转图标 + token数 + 耗时
            val infiniteTransition = rememberInfiniteTransition(label = "loading")
            val angle by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = LinearEasing)
                ),
                label = "rotation"
            )
            
            Icon(
                imageVector = Lucide.Loader,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier
                    .size(14.dp)
                    .graphicsLayer { rotationZ = angle }
            )
            
            Spacer(modifier = Modifier.width(6.dp))
            
            // Token 计数
            if (data.tokenCount > 0) {
                Text(
                    text = "${data.tokenCount}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color(0xFF007AFF), // 蓝色
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "tokens",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White.copy(alpha = 0.5f)
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            // 耗时
            Text(
                text = "${data.elapsedSeconds}s",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = Color.White.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

/**
 * 动画勾 - 先画绿色圆圈，再画勾
 */
@Composable
private fun AnimatedCheckmark(modifier: Modifier = Modifier) {
    // 圆圈绘制进度 (0 -> 1)
    val circleProgress = remember { Animatable(0f) }
    // 勾绘制进度 (0 -> 1)
    val checkProgress = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        // 先画圆圈 (500ms)
        circleProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(500, easing = FastOutSlowInEasing)
        )
        // 再画勾 (400ms)
        checkProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(400, easing = FastOutSlowInEasing)
        )
    }
    
    val greenColor = Color(0xFF34C759)
    
    Canvas(modifier = modifier) {
        val strokeWidth = 2.dp.toPx()
        val radius = (size.minDimension / 2) - strokeWidth
        val center = center
        
        // 绘制圆圈 - 从顶部顺时针画
        if (circleProgress.value > 0f) {
            drawArc(
                color = greenColor,
                startAngle = -90f,
                sweepAngle = 360f * circleProgress.value,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
        }
        
        // 绘制勾 - 两笔画出
        if (checkProgress.value > 0f) {
            val checkPath = Path().apply {
                // 勾的起点 (左中)
                val startX = center.x - radius * 0.4f
                val startY = center.y + radius * 0.1f
                // 勾的转折点 (中下)
                val midX = center.x - radius * 0.1f
                val midY = center.y + radius * 0.4f
                // 勾的终点 (右上)
                val endX = center.x + radius * 0.5f
                val endY = center.y - radius * 0.35f
                
                moveTo(startX, startY)
                
                // 第一笔 (0 - 0.4 进度)
                val firstStrokeProgress = (checkProgress.value / 0.4f).coerceIn(0f, 1f)
                if (firstStrokeProgress > 0f) {
                    lineTo(
                        startX + (midX - startX) * firstStrokeProgress,
                        startY + (midY - startY) * firstStrokeProgress
                    )
                }
                
                // 第二笔 (0.4 - 1.0 进度)
                if (checkProgress.value > 0.4f) {
                    val secondStrokeProgress = ((checkProgress.value - 0.4f) / 0.6f).coerceIn(0f, 1f)
                    lineTo(
                        midX + (endX - midX) * secondStrokeProgress,
                        midY + (endY - midY) * secondStrokeProgress
                    )
                }
            }
            
            drawPath(
                path = checkPath,
                color = greenColor,
                style = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
        }
    }
}

@Composable
private fun ExpandedContent(data: DynamicIslandData) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxSize()
    ) {
        // 顶部信息：头像和状态
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = data.assistantAvatar ?: "🤖",
                fontSize = 24.sp,
                modifier = Modifier.padding(end = 12.dp)
            )
            Column {
                Text(
                    text = "AI 正在回复",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = data.modelName ?: "Unknown Model",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.7f)
                    )
                )
            }
        }
        
        // 可视化波形条 (模拟音频/思维活动)
        ThinkingWaveform()
    }
}

@Composable
private fun LongPressMenuContent(
    data: DynamicIslandData,
    onStop: () -> Unit,
    onCancel: () -> Unit,
    backdrop: Backdrop // 传入 backdrop 以创建 empty backdrop
) {
    // 创建一个不绘图的 backdrop 用于子组件
    val emptyBackdrop = rememberBackdrop(backdrop) {}

    Column(
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        // 停止生成按钮
        LiquidButton(
            onClick = {
                onStop()
                onCancel()
            },
            backdrop = emptyBackdrop,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            isInteractive = true,
            surfaceColor = Color(0xFFFF3B30).copy(alpha = 0.2f), // 红色背景替代 containerColor
            tint = Color(0xFFFF453A) // 红色前景替代 contentColor
        ) {
           Row(verticalAlignment = Alignment.CenterVertically) {
               Icon(Lucide.Square, contentDescription = null, modifier = Modifier.size(16.dp))
               Spacer(Modifier.width(8.dp))
               Text("停止生成", fontWeight = FontWeight.Bold)
           }
        }
        
        Spacer(Modifier.height(8.dp))
        
        // 取消按钮
        LiquidButton(
            onClick = onCancel,
            backdrop = emptyBackdrop,
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            isInteractive = true,
            surfaceColor = Color.White.copy(alpha = 0.1f), // 替代 containerColor
            tint = Color.White // 替代 contentColor
        ) {
            Text("关闭")
        }
    }
}

@Composable
private fun WaveAnimationBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    
    // 自定义颜色
    val color1 = Color(0xFF007AFF).copy(alpha = 0.15f) // 蓝色
    val color2 = Color(0xFF5856D6).copy(alpha = 0.15f) // 紫色
    
    // 波浪相位动画
    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing)
        ),
        label = "phase1"
    )
    
    val phase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2300, easing = LinearEasing)
        ),
        label = "phase2"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val centerY = height / 2
        
        // 绘制第一层波浪
        val path1 = Path().apply {
            moveTo(0f, centerY)
            for (x in 0..width.toInt() step 10) {
                val xFloat = x.toFloat()
                val y = centerY + 10.dp.toPx() * sin((xFloat / width) * 2 * PI + phase1).toFloat()
                lineTo(xFloat, y)
            }
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        drawPath(path1, color1)
        
        // 绘制第二层波浪
        val path2 = Path().apply {
            moveTo(0f, centerY)
            for (x in 0..width.toInt() step 10) {
                val xFloat = x.toFloat()
                val y = centerY + 8.dp.toPx() * sin((xFloat / width) * 3 * PI + phase2).toFloat()
                lineTo(xFloat, y)
            }
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        drawPath(path2, color2)
    }
}

@Composable
private fun ThinkingWaveform() {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(30.dp)
    ) {
        repeat(5) { index ->
            val heightScale by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(500, delayMillis = index * 100, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_$index"
            )
            
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight(heightScale)
                    .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(2.dp))
            )
        }
    }
}
