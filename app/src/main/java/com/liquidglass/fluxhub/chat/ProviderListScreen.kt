package com.liquidglass.fluxhub.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Server
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.Trash2
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.effects.blur
import com.kyant.capsule.ContinuousRoundedRectangle
import com.liquidglass.fluxhub.components.LiquidButton
import com.liquidglass.fluxhub.components.LiquidTextField
import com.liquidglass.fluxhub.components.LiquidConfirmationDialog
import com.liquidglass.fluxhub.data.ProviderEntity

/**
 * 服务商列表页面 - Liquid Glass 风格
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderListScreen(
    onBack: () -> Unit,
    viewModel: ChatViewModel,
    backdrop: Backdrop,
    bottomPadding: PaddingValues = PaddingValues(0.dp)
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingProvider by remember { mutableStateOf<ProviderEntity?>(null) }

    val glassOpacity = viewModel.glassOpacity
    val glassBlur = viewModel.glassBlur
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(bottomPadding)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
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
            
            Text(
                "服务商管理",
                style = TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 4f)
                )
            )
            
            LiquidButton(
                onClick = { showCreateDialog = true },
                backdrop = backdrop,
                modifier = Modifier.size(44.dp),
                isInteractive = true,
                padding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Default.Add, "新建", tint = Color.White)
            }
        }
        
        // 服务商列表
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(viewModel.providers, key = { it.id }) { provider ->
                ProviderCard(
                    provider = provider,
                    isActive = viewModel.currentProvider?.id == provider.id,
                    backdrop = backdrop,
                    onSelect = { viewModel.switchProvider(provider) },
                    onEdit = { editingProvider = provider },
                    onDelete = { viewModel.deleteProvider(provider.id) }
                )
            }
            
            // 空状态
            if (viewModel.providers.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Lucide.Server,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "暂无服务商",
                                color = Color.White.copy(alpha = 0.6f)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "点击右上角 + 添加 API Provider",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }
        }
    }
    
    // 创建/编辑对话框
    if (showCreateDialog || editingProvider != null) {
        ProviderEditDialog(
            provider = editingProvider,
            onDismiss = {
                showCreateDialog = false
                editingProvider = null
            },
            onSave = { name, baseUrl, apiKey, icon ->
                if (editingProvider != null) {
                    viewModel.updateProvider(
                        editingProvider!!.copy(
                            name = name,
                            baseUrl = baseUrl,
                            apiKey = apiKey,
                            icon = icon
                        )
                    )
                } else {
                    viewModel.createProvider(
                        name = name,
                        baseUrl = baseUrl,
                        apiKey = apiKey,
                        icon = icon
                    )
                }
                showCreateDialog = false
                editingProvider = null
            },
            backdrop = backdrop
        )
    }
}

@Composable
private fun ProviderCard(
    provider: ProviderEntity,
    isActive: Boolean,
    backdrop: Backdrop,
    glassOpacity: Float = 0.1f,
    glassBlur: Float = 16f,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { ContinuousRoundedRectangle(16.dp) },
                effects = { vibrancy(); blur(glassBlur.dp.toPx()) },
                onDrawSurface = {
                    drawRect(
                        if (isActive) Color(0xFFFF9500).copy(alpha = glassOpacity + 0.1f)
                        else Color.White.copy(alpha = glassOpacity)
                    )
                }
            )
            .clickable { onSelect() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { ContinuousRoundedRectangle(12.dp) },
                            effects = { vibrancy() },
                            onDrawSurface = { drawRect(Color.White.copy(alpha = 0.15f)) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = provider.icon ?: "🌐",
                        fontSize = 24.sp
                    )
                }
                
                Spacer(Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = provider.name,
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                            shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 4f)
                        )
                    )
                    Text(
                        text = provider.baseUrl.replace("https://", "").take(30),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f),
                        maxLines = 1
                    )
                }
            }
            
            // Actions
            Row {
                if (isActive) {
                    Icon(
                        Lucide.Check,
                        contentDescription = "激活中",
                        tint = Color(0xFF34C759),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                }
                
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Lucide.Pencil, "编辑", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                }
                
                var showDeleteDialog by remember { mutableStateOf(false) }
                IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Lucide.Trash2, "删除", tint = Color(0xFFFF3B30).copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                }
                
                if (showDeleteDialog) {
                    LiquidConfirmationDialog(
                        onDismissRequest = { showDeleteDialog = false },
                        onConfirm = {
                            onDelete()
                            showDeleteDialog = false
                        },
                        title = "删除服务商",
                        message = "确定要删除此服务商吗？此操作不可撤销。",
                        confirmText = "删除",
                        icon = Lucide.Trash2,
                        backdrop = backdrop
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderEditDialog(
    provider: ProviderEntity?,
    onDismiss: () -> Unit,
    onSave: (name: String, baseUrl: String, apiKey: String, icon: String?) -> Unit,
    backdrop: Backdrop
) {
    var name by remember { mutableStateOf(provider?.name ?: "") }
    var baseUrl by remember { mutableStateOf(provider?.baseUrl ?: "https://api.openai.com/v1") }
    var apiKey by remember { mutableStateOf(provider?.apiKey ?: "") }
    var icon by remember { mutableStateOf(provider?.icon ?: "🌐") }
    
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { ContinuousRoundedRectangle(28.dp) },
                    effects = {
                        vibrancy()
                        blur(20.dp.toPx())
                    },
                    onDrawSurface = { drawRect(Color.Black.copy(alpha = 0.5f)) }
                )
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (provider == null) "添加服务商" else "编辑服务商",
                    style = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 4f)
                    )
                )

                LiquidTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "名称",
                    placeholder = "如：OpenAI、DeepSeek",
                    backdrop = backdrop
                )
                
                LiquidTextField(
                    value = icon,
                    onValueChange = { icon = it },
                    label = "图标",
                    placeholder = "输入 Emoji",
                    backdrop = backdrop
                )
                
                LiquidTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = "Base URL",
                    placeholder = "https://api.openai.com/v1",
                    backdrop = backdrop
                )
                
                LiquidTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = "API Key",
                    placeholder = "sk-...",
                    visualTransformation = PasswordVisualTransformation(),
                    backdrop = backdrop
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    LiquidButton(
                        onClick = onDismiss,
                        backdrop = backdrop,
                        modifier = Modifier.height(44.dp).padding(horizontal = 8.dp),
                        isInteractive = true,
                        tint = Color(0xFF8E8E93).copy(alpha = 0.5f)
                    ) {
                        Text("取消", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    
                    Spacer(Modifier.width(12.dp))
                    
                    LiquidButton(
                        onClick = {
                            if (name.isNotBlank() && baseUrl.isNotBlank() && apiKey.isNotBlank()) {
                                onSave(name, baseUrl, apiKey, icon.takeIf { it.isNotBlank() })
                            }
                        },
                        backdrop = backdrop,
                        modifier = Modifier.height(44.dp),
                        isInteractive = true,
                        tint = Color(0xFF007AFF)
                    ) {
                        Text("保存", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
