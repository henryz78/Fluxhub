package com.liquidglass.fluxhub.chat

import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle
import com.liquidglass.fluxhub.R
import com.liquidglass.fluxhub.components.LiquidButton

private const val TAG = "ChatScreen"

@Composable
fun ChatScreen(
    onNavigateToSettings: () -> Unit = {},
    viewModel: ChatViewModel = viewModel()
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    
    // 自动滚动到底部
    LaunchedEffect(viewModel.messages.size) {
        if (viewModel.messages.isNotEmpty()) {
            listState.animateScrollToItem(viewModel.messages.size - 1)
        }
    }
    
    // 始终使用 Liquid Glass 风格
    LiquidGlassChatContent(
        viewModel = viewModel,
        inputText = inputText,
        onInputTextChange = { inputText = it },
        listState = listState,
        onNavigateToSettings = onNavigateToSettings
    )
}

@Composable
private fun LiquidGlassChatContent(
    viewModel: ChatViewModel,
    inputText: String,
    onInputTextChange: (String) -> Unit,
    listState: LazyListState,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    
    Log.d(TAG, "Loading Liquid Glass components")
    
    val backgroundBitmap = remember {
        BitmapFactory.decodeResource(context.resources, R.drawable.wallpaper_light)
    }
    
    val backdrop = rememberLayerBackdrop()
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // 背景图片
        if (backgroundBitmap != null) {
            Image(
                bitmap = backgroundBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .layerBackdrop(backdrop)
                    .fillMaxSize()
            )
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding() // 键盘弹出时自动调整
        ) {
            // Top Bar - 椭圆形胶囊状
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { ContinuousCapsule }, // 椭圆形
                            effects = {
                                vibrancy()
                                blur(4f.dp.toPx())
                                lens(16f.dp.toPx(), 32f.dp.toPx())
                            },
                            highlight = { Highlight.Plain },
                            onDrawSurface = {
                                drawRect(Color.White.copy(alpha = 0.15f))
                            }
                        )
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicText(
                        text = "Fluxhub",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    
                    // 设置按钮 - 使用正常图标
                    LiquidButton(
                        onClick = onNavigateToSettings,
                        backdrop = backdrop,
                        modifier = Modifier.height(36.dp),
                        tint = Color(0xFF0088FF)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "设置",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            
            // Messages - 使用 weight 让它占据剩余空间
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp, horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(viewModel.messages) { message ->
                    LiquidGlassChatBubble(message = message, backdrop = backdrop)
                }
                
                if (viewModel.isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Loading indicator with glass effect
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .drawBackdrop(
                                        backdrop = backdrop,
                                        shape = { ContinuousCapsule },
                                        effects = {
                                            vibrancy()
                                            blur(4f.dp.toPx())
                                            lens(8f.dp.toPx(), 16f.dp.toPx())
                                        },
                                        onDrawSurface = {
                                            drawRect(Color.White.copy(alpha = 0.2f))
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }
                }
            }
            
            // Error message with animation
            AnimatedVisibility(
                visible = viewModel.showError,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                viewModel.error?.let { errorMsg ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .drawBackdrop(
                                backdrop = backdrop,
                                shape = { ContinuousCapsule },
                                effects = {
                                    vibrancy()
                                    blur(4f.dp.toPx())
                                },
                                onDrawSurface = {
                                    drawRect(Color(0xFFFF3B30).copy(alpha = 0.3f))
                                }
                            )
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        BasicText(
                            text = errorMsg,
                            style = TextStyle(Color.White, 14.sp)
                        )
                    }
                }
            }
            
            // Input with glass effect
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                LiquidGlassChatInputBar(
                    text = inputText,
                    onTextChange = onInputTextChange,
                    onSend = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendMessage(inputText)
                            onInputTextChange("")
                        }
                    },
                    enabled = !viewModel.isLoading,
                    backdrop = backdrop
                )
            }
        }
    }
}

@Composable
private fun LiquidGlassChatBubble(
    message: ChatMessage,
    backdrop: Backdrop
) {
    val isUser = message.role == "user"
    val bubbleShape = ContinuousRoundedRectangle(20.dp)
    val tintColor = if (isUser) Color(0xFF007AFF) else Color(0xFF34C759)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { bubbleShape },
                    effects = {
                        vibrancy()
                        blur(4f.dp.toPx())
                        lens(12f.dp.toPx(), 24f.dp.toPx())
                    },
                    highlight = { Highlight.Plain },
                    onDrawSurface = {
                        drawRect(tintColor.copy(alpha = 0.25f))
                    }
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            BasicText(
                text = message.content,
                style = TextStyle(
                    color = Color.White,
                    fontSize = 16.sp,
                    lineHeight = 22.sp
                )
            )
        }
    }
}

@Composable
private fun LiquidGlassChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean,
    backdrop: Backdrop
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Input field with glass effect
        Box(
            modifier = Modifier
                .weight(1f)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { ContinuousRoundedRectangle(28.dp) },
                    effects = {
                        vibrancy()
                        blur(4f.dp.toPx())
                        lens(12f.dp.toPx(), 24f.dp.toPx())
                    },
                    highlight = { Highlight.Plain },
                    onDrawSurface = {
                        drawRect(Color.White.copy(alpha = 0.15f))
                    }
                )
                .heightIn(min = 56.dp, max = 150.dp)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                enabled = enabled,
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 16.sp
                ),
                cursorBrush = SolidColor(Color(0xFF007AFF)),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                decorationBox = { innerTextField ->
                    Box {
                        if (text.isEmpty()) {
                            BasicText(
                                text = "输入消息...",
                                style = TextStyle(
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 16.sp
                                )
                            )
                        }
                        innerTextField()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // Send button using LiquidButton with icon
        LiquidButton(
            onClick = onSend,
            backdrop = backdrop,
            modifier = Modifier.size(56.dp),
            isInteractive = enabled && text.isNotBlank(),
            tint = if (text.isNotBlank()) Color(0xFF007AFF) else Color.Gray.copy(alpha = 0.5f)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "发送",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
