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
import com.kyant.backdrop.Backdrop
import com.liquidglass.fluxhub.components.LiquidButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: ChatViewModel,
    backdrop: Backdrop,
    isTab: Boolean = false,
    bottomPadding: PaddingValues = PaddingValues(0.dp)
) {
    // 使用本地状态进行编辑
    var apiKeyInput by remember { mutableStateOf(viewModel.apiKey) }
    var baseUrlInput by remember { mutableStateOf(viewModel.baseUrl) }
    
    LaunchedEffect(viewModel.apiKey) { apiKeyInput = viewModel.apiKey }
    LaunchedEffect(viewModel.baseUrl) { baseUrlInput = viewModel.baseUrl }
    
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
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(bottomPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isTab) {
                Text(
                    text = "设置",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                )
            }

            Text(
                text = "API 配置",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            OutlinedTextField(
                value = baseUrlInput,
                onValueChange = { baseUrlInput = it },
                label = { Text("Base URL") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    focusedLabelColor = MaterialTheme.colorScheme.primary
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
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    focusedLabelColor = MaterialTheme.colorScheme.primary
                )
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Text(
                text = "支持 OpenAI 兼容 API（如 OpenAI、DeepSeek、Groq 等）",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f)
            )
            
            LiquidButton(
                onClick = {
                    viewModel.saveApiKey(apiKeyInput)
                    viewModel.saveBaseUrl(baseUrlInput)
                    if (!isTab) onBack()
                },
                backdrop = backdrop,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                tint = Color(0xFF007AFF)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("保存设置", color = Color.White)
            }
        }
    }
}
