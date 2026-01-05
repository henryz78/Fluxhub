package com.liquidglass.fluxhub.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.vibrancy
import com.kyant.capsule.ContinuousRoundedRectangle
import com.liquidglass.fluxhub.components.LiquidButton

/**
 * API 配置页面 - Liquid Glass 风格
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiConfigScreen(
    onBack: () -> Unit,
    viewModel: ChatViewModel,
    backdrop: Backdrop,
    bottomPadding: PaddingValues = PaddingValues(0.dp)
) {
    var apiKeyInput by remember { mutableStateOf(viewModel.apiKey) }
    var baseUrlInput by remember { mutableStateOf(viewModel.baseUrl) }
    
    LaunchedEffect(viewModel.apiKey) { apiKeyInput = viewModel.apiKey }
    LaunchedEffect(viewModel.baseUrl) { baseUrlInput = viewModel.baseUrl }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(bottomPadding)
            .padding(16.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LiquidButton(
                onClick = onBack,
                backdrop = backdrop,
                modifier = Modifier.size(44.dp),
                isInteractive = true,
                padding = PaddingValues(0.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = Color.White)
            }
            
            Spacer(Modifier.width(16.dp))
            
            Text(
                "API 配置",
                style = TextStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 4f)
                )
            )
        }
        
        Spacer(Modifier.height(32.dp))
        
        // Base URL
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { ContinuousRoundedRectangle(16.dp) },
                    effects = { vibrancy() },
                    onDrawSurface = { drawRect(Color.White.copy(alpha = 0.1f)) }
                )
                .padding(16.dp)
        ) {
            Column {
                Text(
                    "Base URL",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = baseUrlInput,
                    onValueChange = { baseUrlInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("https://api.openai.com/v1") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedTextColor = Color.White,
                        focusedTextColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedBorderColor = Color(0xFF007AFF),
                        unfocusedPlaceholderColor = Color.White.copy(alpha = 0.4f)
                    )
                )
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        // API Key
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { ContinuousRoundedRectangle(16.dp) },
                    effects = { vibrancy() },
                    onDrawSurface = { drawRect(Color.White.copy(alpha = 0.1f)) }
                )
                .padding(16.dp)
        ) {
            Column {
                Text(
                    "API Key",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("sk-...") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedTextColor = Color.White,
                        focusedTextColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedBorderColor = Color(0xFF007AFF),
                        unfocusedPlaceholderColor = Color.White.copy(alpha = 0.4f)
                    )
                )
            }
        }
        
        Spacer(Modifier.weight(1f))
        
        // 提示
        Text(
            text = "支持 OpenAI 兼容 API（如 OpenAI、DeepSeek、Groq、硅基流动等）",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // 保存按钮
        LiquidButton(
            onClick = {
                viewModel.saveApiKey(apiKeyInput)
                viewModel.saveBaseUrl(baseUrlInput)
                onBack()
            },
            backdrop = backdrop,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            tint = Color(0xFF007AFF)
        ) {
            Icon(Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "保存设置", 
                color = Color.White,
                style = TextStyle(
                    fontWeight = FontWeight.Medium,
                    shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 4f)
                )
            )
        }
    }
}
