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
    var modelInput by remember { mutableStateOf(viewModel.model) }
    
    // 同步 ViewModel 的值 (仅当 ViewModel 变化且本地未修改时... 但很难判断是否修改。
    // 简单的策略：初始化时设置一次，或者信任 ViewModel 为 source of truth。
    // 如果用户正在输入，ViewModel 不会变（除非后台更新）。
    // 但如果 ViewModel 是初始值空字符串，然后异步加载了真实值，我们需要更新本地状态。
    // 问题是：如果用户手快，在异步加载完成前输入了，然后异步加载完成了（覆盖了用户输入）。
    // DataStore 读取通常很快。
    // 为了防止"恢复默认"，我们只在 viewModel 的值不等于默认值时更新，或者...
    // 其实更好的做法是直接用 viewModel 的属性作为 TextField 的 value，但为了"保存"按钮的语义，我们需要 buffer。
    // 修正策略：只在 viewModel 值变化时更新，这点 LaunchedEffect 已经做到了。
    // 可能是 DataStore 读取失败返回了默认值？
    
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
            
            // 保存按钮 - 使用 LiquidButton
            LiquidButton(
                onClick = {
                    viewModel.saveApiKey(apiKeyInput)
                    viewModel.saveBaseUrl(baseUrlInput)
                    viewModel.saveModel(modelInput)
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
