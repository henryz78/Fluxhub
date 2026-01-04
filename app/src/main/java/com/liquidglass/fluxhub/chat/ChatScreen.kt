package com.liquidglass.fluxhub.chat

import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liquidglass.fluxhub.R

private const val TAG = "ChatScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onNavigateToSettings: () -> Unit = {},
    viewModel: ChatViewModel = viewModel()
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    
    // 自动滚动到底部
    LaunchedEffect(viewModel.messages.size) {
        if (viewModel.messages.isNotEmpty()) {
            listState.animateScrollToItem(viewModel.messages.size - 1)
        }
    }
    
    // 根据开关选择不同的界面
    if (viewModel.useLiquidGlass) {
        Log.d(TAG, "Rendering Liquid Glass style")
        LiquidGlassChatContent(
            viewModel = viewModel,
            inputText = inputText,
            onInputTextChange = { inputText = it },
            listState = listState,
            onNavigateToSettings = onNavigateToSettings,
            context = context
        )
    } else {
        Log.d(TAG, "Rendering standard style")
        StandardChatContent(
            viewModel = viewModel,
            inputText = inputText,
            onInputTextChange = { inputText = it },
            listState = listState,
            onNavigateToSettings = onNavigateToSettings
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StandardChatContent(
    viewModel: ChatViewModel,
    inputText: String,
    onInputTextChange: (String) -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onNavigateToSettings: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fluxhub") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Filled.Settings, "设置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A1A),
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF121212)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Messages
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(viewModel.messages) { message ->
                    StandardChatBubble(message = message)
                }
                
                if (viewModel.isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF007AFF),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
            
            // Error message
            viewModel.error?.let { errorMsg ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("关闭")
                        }
                    }
                ) {
                    Text(errorMsg)
                }
            }
            
            // Input
            StandardChatInputBar(
                text = inputText,
                onTextChange = onInputTextChange,
                onSend = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendMessage(inputText)
                        onInputTextChange("")
                    }
                },
                enabled = !viewModel.isLoading
            )
        }
    }
}

@Composable
private fun LiquidGlassChatContent(
    viewModel: ChatViewModel,
    inputText: String,
    onInputTextChange: (String) -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onNavigateToSettings: () -> Unit,
    context: android.content.Context
) {
    // 尝试使用 Liquid Glass 效果
    try {
        Log.d(TAG, "Loading Liquid Glass components")
        
        val backgroundBitmap = remember {
            BitmapFactory.decodeResource(context.resources, R.drawable.wallpaper_light)
        }
        
        val backdrop = com.kyant.backdrop.backdrops.rememberLayerBackdrop()
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .com.kyant.backdrop.backdrops.layerBackdrop(backdrop)
        ) {
            // 背景图片
            if (backgroundBitmap != null) {
                Image(
                    bitmap = backgroundBitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
            ) {
                // Top Bar with glass effect
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .com.kyant.backdrop.drawBackdrop(
                            backdrop = backdrop,
                            shape = { com.kyant.capsule.ContinuousRoundedRectangle(0.dp, 0.dp, 16.dp, 16.dp) },
                            effects = {
                                com.kyant.backdrop.effects.vibrancy()
                                com.kyant.backdrop.effects.blur(16f.dp.toPx())
                            },
                            onDrawSurface = {
                                drawRect(Color.Black.copy(alpha = 0.3f))
                            }
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Fluxhub",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge
                        )
                        
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "设置",
                                tint = Color.White
                            )
                        }
                    }
                }
                
                // Messages
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 8.dp)
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
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
                
                // Error message
                viewModel.error?.let { errorMsg ->
                    Snackbar(
                        modifier = Modifier.padding(16.dp),
                        action = {
                            TextButton(onClick = { viewModel.clearError() }) {
                                Text("关闭")
                            }
                        }
                    ) {
                        Text(errorMsg)
                    }
                }
                
                // Input with glass effect
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
    } catch (e: Exception) {
        Log.e(TAG, "Liquid Glass rendering failed, falling back to standard", e)
        // 回退到标准样式
        StandardChatContent(
            viewModel = viewModel,
            inputText = inputText,
            onInputTextChange = onInputTextChange,
            listState = listState,
            onNavigateToSettings = onNavigateToSettings
        )
    }
}

@Composable
private fun StandardChatBubble(message: ChatMessage) {
    val isUser = message.role == "user"
    val backgroundColor = if (isUser) Color(0xFF007AFF) else Color(0xFF2A2A2A)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(backgroundColor)
                .padding(12.dp)
        ) {
            Text(text = message.content, color = Color.White, fontSize = 16.sp)
        }
    }
}

@Composable
private fun LiquidGlassChatBubble(
    message: ChatMessage,
    backdrop: com.kyant.backdrop.Backdrop
) {
    val isUser = message.role == "user"
    val bubbleShape = com.kyant.capsule.ContinuousRoundedRectangle(16.dp)
    val backgroundColor = if (isUser) {
        Color(0xFF007AFF).copy(alpha = 0.3f)
    } else {
        Color.White.copy(alpha = 0.15f)
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .com.kyant.backdrop.drawBackdrop(
                    backdrop = backdrop,
                    shape = { bubbleShape },
                    effects = {
                        com.kyant.backdrop.effects.vibrancy()
                        com.kyant.backdrop.effects.blur(12f.dp.toPx())
                    },
                    onDrawSurface = {
                        drawRect(backgroundColor)
                    }
                )
                .padding(12.dp)
        ) {
            Text(text = message.content, color = Color.White, fontSize = 16.sp)
        }
    }
}

@Composable
private fun StandardChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A1A))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF2A2A2A))
                .heightIn(min = 48.dp, max = 120.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                enabled = enabled,
                textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                cursorBrush = SolidColor(Color(0xFF007AFF)),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                decorationBox = { innerTextField ->
                    Box {
                        if (text.isEmpty()) {
                            Text("输入消息...", color = Color.White.copy(alpha = 0.5f), fontSize = 16.sp)
                        }
                        innerTextField()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        IconButton(
            onClick = onSend,
            enabled = enabled && text.isNotBlank(),
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(if (text.isNotBlank()) Color(0xFF007AFF) else Color(0xFF2A2A2A))
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, "发送", tint = Color.White)
        }
    }
}

@Composable
private fun LiquidGlassChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean,
    backdrop: com.kyant.backdrop.Backdrop
) {
    val inputShape = com.kyant.capsule.ContinuousRoundedRectangle(24.dp)
    val containerColor = Color.White.copy(alpha = 0.15f)
    val accentColor = Color(0xFF007AFF)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .com.kyant.backdrop.drawBackdrop(
                    backdrop = backdrop,
                    shape = { inputShape },
                    effects = {
                        com.kyant.backdrop.effects.vibrancy()
                        com.kyant.backdrop.effects.blur(12f.dp.toPx())
                    },
                    onDrawSurface = {
                        drawRect(containerColor)
                    }
                )
                .heightIn(min = 48.dp, max = 120.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                enabled = enabled,
                textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                cursorBrush = SolidColor(accentColor),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                decorationBox = { innerTextField ->
                    Box {
                        if (text.isEmpty()) {
                            Text("输入消息...", color = Color.White.copy(alpha = 0.5f), fontSize = 16.sp)
                        }
                        innerTextField()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        IconButton(
            onClick = onSend,
            enabled = enabled && text.isNotBlank(),
            modifier = Modifier
                .size(48.dp)
                .com.kyant.backdrop.drawBackdrop(
                    backdrop = backdrop,
                    shape = { com.kyant.capsule.ContinuousRoundedRectangle(24.dp) },
                    effects = {
                        com.kyant.backdrop.effects.vibrancy()
                        com.kyant.backdrop.effects.blur(8f.dp.toPx())
                    },
                    onDrawSurface = {
                        val color = if (text.isNotBlank()) accentColor.copy(alpha = 0.8f) else containerColor
                        drawRect(color)
                    }
                )
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, "发送", tint = Color.White)
        }
    }
}
