package com.liquidglass.fluxhub.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.*
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.capsule.ContinuousRoundedRectangle
import com.liquidglass.fluxhub.components.LiquidButton

/**
 * 登录/注册界面 - 液态玻璃风格
 */
@Composable
fun AuthScreen(
    backdrop: Backdrop,
    authState: AuthState,
    onLogin: (username: String, password: String) -> Unit,
    onRegister: (username: String, email: String, password: String, inviteCode: String) -> Unit
) {
    var isLoginMode by remember { mutableStateOf(true) }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var inviteCode by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val keyboardController = LocalSoftwareKeyboardController.current
    
    // 根据 authState 显示错误
    LaunchedEffect(authState) {
        errorMessage = when (authState) {
            is AuthState.Error -> authState.message
            is AuthState.Blocked -> authState.message
            else -> null
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            // Logo
            Text(
                "🚀",
                fontSize = 64.sp
            )
            
            Spacer(Modifier.height(16.dp))
            
            // Title
            Text(
                "FluxHub",
                style = TextStyle(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 8f)
                )
            )
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                if (isLoginMode) "登录账号" else "创建账号",
                style = TextStyle(
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            )
            
            Spacer(Modifier.height(32.dp))
            
            // 表单卡片
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { ContinuousRoundedRectangle(24.dp) },
                        effects = { vibrancy(); blur(20.dp.toPx()) },
                        onDrawSurface = { drawRect(Color.White.copy(alpha = 0.12f)) }
                    )
                    .padding(24.dp)
            ) {
                Column {
                    // 用户名输入
                    AuthTextField(
                        value = username,
                        onValueChange = { username = it; errorMessage = null },
                        placeholder = "用户名",
                        icon = Lucide.User,
                        backdrop = backdrop,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = if (isLoginMode) ImeAction.Next else ImeAction.Next
                        )
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // 邮箱输入（仅注册）
                    AnimatedVisibility(!isLoginMode) {
                        Column {
                            AuthTextField(
                                value = email,
                                onValueChange = { email = it; errorMessage = null },
                                placeholder = "邮箱",
                                icon = Lucide.Mail,
                                backdrop = backdrop,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                )
                            )
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                    
                    // 密码输入
                    AuthTextField(
                        value = password,
                        onValueChange = { password = it; errorMessage = null },
                        placeholder = "密码",
                        icon = Lucide.Lock,
                        backdrop = backdrop,
                        isPassword = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = if (isLoginMode) ImeAction.Done else ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (isLoginMode) {
                                    keyboardController?.hide()
                                    onLogin(username, password)
                                }
                            }
                        )
                    )
                    
                    // 确认密码（仅注册）
                    AnimatedVisibility(!isLoginMode) {
                        Column {
                            Spacer(Modifier.height(16.dp))
                            AuthTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it; errorMessage = null },
                                placeholder = "确认密码",
                                icon = Lucide.Lock,
                                backdrop = backdrop,
                                isPassword = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Next
                                )
                            )
                            Spacer(Modifier.height(16.dp))
                            // 邀请码
                            AuthTextField(
                                value = inviteCode,
                                onValueChange = { inviteCode = it.uppercase(); errorMessage = null },
                                placeholder = "邀请码",
                                icon = Lucide.Ticket,
                                backdrop = backdrop,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        keyboardController?.hide()
                                        if (password == confirmPassword) {
                                            onRegister(username, email, password, inviteCode)
                                        } else {
                                            errorMessage = "两次密码不一致"
                                        }
                                    }
                                )
                            )
                        }
                    }
                    
                    // 错误提示
                    AnimatedVisibility(errorMessage != null) {
                        Column {
                            Spacer(Modifier.height(16.dp))
                            Text(
                                errorMessage ?: "",
                                color = Color(0xFFFF6B6B),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(24.dp))
                    
                    // 提交按钮
                    LiquidButton(
                        onClick = {
                            keyboardController?.hide()
                            if (isLoginMode) {
                                if (username.isBlank() || password.isBlank()) {
                                    errorMessage = "请填写所有字段"
                                    return@LiquidButton
                                }
                                onLogin(username, password)
                            } else {
                                if (username.isBlank() || email.isBlank() || password.isBlank()) {
                                    errorMessage = "请填写所有字段"
                                    return@LiquidButton
                                }
                                if (password != confirmPassword) {
                                    errorMessage = "两次密码不一致"
                                    return@LiquidButton
                                }
                                if (password.length < 6) {
                                    errorMessage = "密码至少6位"
                                    return@LiquidButton
                                }
                                onRegister(username, email, password, inviteCode)
                            }
                        },
                        backdrop = backdrop,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        isInteractive = true,
                        tint = Color(0xFF007AFF)
                    ) {
                        if (authState is AuthState.Checking) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text(
                                if (isLoginMode) "登录" else "注册",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            // 切换登录/注册
            TextButton(
                onClick = {
                    isLoginMode = !isLoginMode
                    errorMessage = null
                }
            ) {
                Text(
                    if (isLoginMode) "没有账号？点击注册" else "已有账号？点击登录",
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    backdrop: Backdrop,
    isPassword: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { ContinuousRoundedRectangle(12.dp) },
                effects = { vibrancy() },
                onDrawSurface = { drawRect(Color.White.copy(alpha = 0.08f)) }
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(Modifier.width(12.dp))
            
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 16.sp
                ),
                cursorBrush = SolidColor(Color.White),
                visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                singleLine = true,
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                placeholder,
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 16.sp
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}

/**
 * 账户过期续期界面
 */
@Composable
fun ExpiredScreen(
    backdrop: Backdrop,
    message: String,
    onRenew: (inviteCode: String) -> Unit,
    onLogout: () -> Unit
) {
    var inviteCode by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    
    val keyboardController = LocalSoftwareKeyboardController.current
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            // 图标
            Text("⏰", fontSize = 64.sp)
            
            Spacer(Modifier.height(16.dp))
            
            // 标题
            Text(
                "账户已过期",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(Modifier.height(8.dp))
            
            // 消息
            Text(
                message,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            
            Spacer(Modifier.height(32.dp))
            
            // 激活码输入框
            AuthTextField(
                value = inviteCode,
                onValueChange = { inviteCode = it.uppercase(); errorMessage = null },
                placeholder = "激活码",
                icon = Lucide.Ticket,
                backdrop = backdrop,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                        if (inviteCode.isNotBlank()) {
                            isLoading = true
                            onRenew(inviteCode)
                        } else {
                            errorMessage = "请输入激活码"
                        }
                    }
                )
            )
            
            // 错误提示
            AnimatedVisibility(errorMessage != null) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        errorMessage ?: "",
                        color = Color(0xFFFF6B6B),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            // 续期按钮
            LiquidButton(
                onClick = {
                    keyboardController?.hide()
                    if (inviteCode.isNotBlank()) {
                        isLoading = true
                        onRenew(inviteCode)
                    } else {
                        errorMessage = "请输入激活码"
                    }
                },
                backdrop = backdrop,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                isInteractive = true,
                tint = Color(0xFF34C759)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        "激活账户",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            // 退出登录
            TextButton(onClick = onLogout) {
                Text(
                    "切换账号",
                    color = Color(0xFFFF3B30),
                    fontSize = 14.sp
                )
            }
        }
    }
}
