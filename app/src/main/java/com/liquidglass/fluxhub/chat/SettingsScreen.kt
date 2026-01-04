package com.liquidglass.fluxhub.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.liquidglass.fluxhub.components.LiquidButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: ChatViewModel,
    isTab: Boolean = false,
    bottomPadding: PaddingValues = PaddingValues(0.dp)
) {
    // 使用本地状态进行编辑
    var apiKeyInput by remember { mutableStateOf(viewModel.apiKey) }
    var baseUrlInput by remember { mutableStateOf(viewModel.baseUrl) }
    var modelInput by remember { mutableStateOf(viewModel.model) }
    
    // 同步 ViewModel 的值
    LaunchedEffect(viewModel.apiKey) { apiKeyInput = viewModel.apiKey }
    LaunchedEffect(viewModel.baseUrl) { baseUrlInput = viewModel.baseUrl }
    LaunchedEffect(viewModel.model) { modelInput = viewModel.model }
    
    Scaffold(
        topBar = {
            if (!isTab) {
                TopAppBar(
                    title = { Text("设置") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF1A1A1A),
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            }
        },
        containerColor = Color(0xFF121212)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(bottomPadding) // 避开底部导航
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isTab) {
                // Tab 模式下显示标题，增加顶部边距
                Text(
                    text = "设置",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                )
            }

            Text(
                text = "API 配置",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            
            OutlinedTextField(
                value = baseUrlInput,
                onValueChange = { baseUrlInput = it },
                label = { Text("Base URL") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedTextColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedBorderColor = Color(0xFF007AFF),
                    unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                    focusedLabelColor = Color(0xFF007AFF)
                )
            )
            
            OutlinedTextField(
                value = apiKeyInput,
                onValueChange = { apiKeyInput = it },
                label = { Text("API Key") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedTextColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedBorderColor = Color(0xFF007AFF),
                    unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                    focusedLabelColor = Color(0xFF007AFF)
                )
            )
            
            OutlinedTextField(
                value = modelInput,
                onValueChange = { modelInput = it },
                label = { Text("Model") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedTextColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedBorderColor = Color(0xFF007AFF),
                    unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                    focusedLabelColor = Color(0xFF007AFF)
                )
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Text(
                text = "支持 OpenAI 兼容 API（如 OpenAI、DeepSeek、Groq 等）",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f)
            )
            
            // 保存按钮
            Button(
                onClick = {
                    viewModel.saveApiKey(apiKeyInput)
                    viewModel.saveBaseUrl(baseUrlInput)
                    viewModel.saveModel(modelInput)
                    if (!isTab) onBack()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF007AFF),
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("保存设置")
            }
        }
    }
}
