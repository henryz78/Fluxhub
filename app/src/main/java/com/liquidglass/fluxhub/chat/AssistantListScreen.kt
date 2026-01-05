package com.liquidglass.fluxhub.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.User
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.Trash2
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.capsule.ContinuousRoundedRectangle
import com.liquidglass.fluxhub.components.LiquidButton
import com.liquidglass.fluxhub.data.AssistantEntity
import java.util.UUID

/**
 * 助手列表页面 - Liquid Glass 风格
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantListScreen(
    onBack: () -> Unit,
    viewModel: ChatViewModel,
    backdrop: Backdrop,
    bottomPadding: PaddingValues = PaddingValues(0.dp)
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingAssistant by remember { mutableStateOf<AssistantEntity?>(null) }
    
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
                "助手管理",
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
        
        // 助手列表
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(viewModel.assistants, key = { it.id }) { assistant ->
                AssistantCard(
                    assistant = assistant,
                    isSelected = viewModel.currentAssistant?.id == assistant.id,
                    backdrop = backdrop,
                    onSelect = { viewModel.switchAssistant(assistant) },
                    onEdit = { editingAssistant = assistant },
                    onDelete = { viewModel.deleteAssistant(assistant.id) }
                )
            }
            
            // 空状态
            if (viewModel.assistants.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Lucide.User,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "暂无助手",
                                color = Color.White.copy(alpha = 0.6f)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "点击右上角 + 创建第一个助手",
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
    if (showCreateDialog || editingAssistant != null) {
        AssistantEditDialog(
            assistant = editingAssistant,
            onDismiss = {
                showCreateDialog = false
                editingAssistant = null
            },
            onSave = { name, systemPrompt, temperature, topP, avatar ->
                if (editingAssistant != null) {
                    viewModel.updateAssistant(
                        editingAssistant!!.copy(
                            name = name,
                            systemPrompt = systemPrompt,
                            temperature = temperature,
                            topP = topP,
                            avatar = avatar
                        )
                    )
                } else {
                    viewModel.createAssistant(
                        name = name,
                        systemPrompt = systemPrompt,
                        temperature = temperature,
                        topP = topP,
                        avatar = avatar
                    )
                }
                showCreateDialog = false
                editingAssistant = null
            }
        )
    }
}

@Composable
private fun AssistantCard(
    assistant: AssistantEntity,
    isSelected: Boolean,
    backdrop: Backdrop,
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
                effects = { vibrancy() },
                onDrawSurface = {
                    drawRect(
                        if (isSelected) Color(0xFF007AFF).copy(alpha = 0.3f)
                        else Color.White.copy(alpha = 0.3f)
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
                // Avatar
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
                        text = assistant.avatar ?: "🤖",
                        fontSize = 24.sp
                    )
                }
                
                Spacer(Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = assistant.name,
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                            shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 4f)
                        )
                    )
                    if (assistant.systemPrompt.isNotBlank()) {
                        Text(
                            text = assistant.systemPrompt.take(50) + if (assistant.systemPrompt.length > 50) "..." else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f),
                            maxLines = 1
                        )
                    }
                }
            }
            
            // Actions
            Row {
                if (isSelected) {
                    Icon(
                        Lucide.Check,
                        contentDescription = "已选中",
                        tint = Color(0xFF34C759),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                }
                
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Lucide.Pencil, "编辑", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Lucide.Trash2, "删除", tint = Color(0xFFFF3B30).copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssistantEditDialog(
    assistant: AssistantEntity?,
    onDismiss: () -> Unit,
    onSave: (name: String, systemPrompt: String, temperature: Float, topP: Float, avatar: String?) -> Unit
) {
    var name by remember { mutableStateOf(assistant?.name ?: "") }
    var systemPrompt by remember { mutableStateOf(assistant?.systemPrompt ?: "") }
    var temperature by remember { mutableStateOf(assistant?.temperature ?: 0.7f) }
    var topP by remember { mutableStateOf(assistant?.topP ?: 1.0f) }
    var avatar by remember { mutableStateOf(assistant?.avatar ?: "🤖") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(if (assistant == null) "创建助手" else "编辑助手") 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.1f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                        focusedBorderColor = Color(0xFF007AFF),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                
                OutlinedTextField(
                    value = avatar,
                    onValueChange = { avatar = it },
                    label = { Text("头像 (Emoji)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.1f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                        focusedBorderColor = Color(0xFF007AFF),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                
                OutlinedTextField(
                    value = systemPrompt,
                    onValueChange = { systemPrompt = it },
                    label = { Text("系统提示词") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.1f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                        focusedBorderColor = Color(0xFF007AFF),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                
                // Temperature
                Text("Temperature: ${String.format("%.1f", temperature)}")
                Slider(
                    value = temperature,
                    onValueChange = { temperature = it },
                    valueRange = 0f..2f,
                    steps = 19,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF007AFF),
                        activeTrackColor = Color(0xFF007AFF),
                        inactiveTrackColor = Color(0xFF007AFF).copy(alpha = 0.3f)
                    )
                )
                
                // Top P
                Text("Top P: ${String.format("%.2f", topP)}")
                Slider(
                    value = topP,
                    onValueChange = { topP = it },
                    valueRange = 0f..1f,
                    steps = 9,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF007AFF),
                        activeTrackColor = Color(0xFF007AFF),
                        inactiveTrackColor = Color(0xFF007AFF).copy(alpha = 0.3f)
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    if (name.isNotBlank()) {
                        onSave(name, systemPrompt, temperature, topP, avatar.takeIf { it.isNotBlank() })
                    }
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
