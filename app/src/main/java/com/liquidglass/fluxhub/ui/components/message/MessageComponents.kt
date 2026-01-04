package com.liquidglass.fluxhub.ui.components.message

import android.content.ClipData
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * 消息头像组件
 */
@Composable
fun MessageAvatar(
    isUser: Boolean,
    modelName: String? = null,
    userName: String = "你",
    userAvatar: String = "",
    timestamp: Long? = null,
    modifier: Modifier = Modifier
) {
    val aiIcon = remember(modelName) {
        val name = modelName?.lowercase() ?: ""
        when {
            name.contains("gpt") || name.contains("openai") -> Lucide.Zap
            name.contains("claude") -> Lucide.Sparkles
            name.contains("gemini") -> Lucide.Star
            name.contains("deepseek") -> Lucide.Compass
            name.contains("qwen") || name.contains("aliyun") -> Lucide.Cloud
            name.contains("llama") || name.contains("meta") -> Lucide.Globe
            else -> Lucide.Bot
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!isUser) {
            // AI 头像
            Surface(
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = aiIcon,
                        contentDescription = "AI",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = modelName ?: "AI",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                )
                timestamp?.let {
                    Text(
                        text = formatTimestamp(it),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            // 用户头像在右侧
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = userName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                )
                timestamp?.let {
                    Text(
                        text = formatTimestamp(it),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (userAvatar.isNotEmpty()) {
                        Text(
                            text = userAvatar,
                            style = MaterialTheme.typography.titleMedium
                        )
                    } else {
                        Icon(
                            imageVector = Lucide.User,
                            contentDescription = "用户",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}

/**
 * 消息操作按钮 - 复制、重新生成、删除等
 */
@Composable
fun MessageActionButtons(
    content: String,
    isUser: Boolean,
    onRegenerate: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var showCopiedHint by remember { mutableStateOf(false) }
    
    LaunchedEffect(showCopiedHint) {
        if (showCopiedHint) {
            kotlinx.coroutines.delay(2000)
            showCopiedHint = false
        }
    }
    
    FlowRow(
        modifier = modifier.padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        itemVerticalAlignment = Alignment.CenterVertically
    ) {
        // 复制按钮
        ActionButton(
            icon = if (showCopiedHint) Lucide.Check else Lucide.Copy,
            contentDescription = if (showCopiedHint) "已复制" else "复制",
            onClick = {
                scope.launch {
                    clipboardManager.setClipEntry(
                        ClipEntry(ClipData.newPlainText("message", content))
                    )
                    showCopiedHint = true
                }
            }
        )
        
        // AI 消息才显示重新生成
        if (!isUser && onRegenerate != null) {
            ActionButton(
                icon = Lucide.RefreshCw,
                contentDescription = "重新生成",
                onClick = onRegenerate
            )
        }
        
        // 编辑按钮
        if (onEdit != null) {
            ActionButton(
                icon = Lucide.Pencil,
                contentDescription = "编辑",
                onClick = onEdit
            )
        }
        
        // 删除按钮
        if (onDelete != null) {
            ActionButton(
                icon = Lucide.Trash2,
                contentDescription = "删除",
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                onClick = onDelete
            )
        }
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
    onClick: () -> Unit
) {
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(6.dp)
            .size(14.dp),
        tint = tint
    )
}

/**
 * 消息操作底部弹窗
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageActionsSheet(
    content: String,
    isUser: Boolean,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onRegenerate: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onEditAndResend: (() -> Unit)? = null  // 新增：编辑并重发
) {
    // 删除确认状态
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 复制
            SheetActionCard(
                icon = Lucide.Copy,
                text = "复制",
                onClick = {
                    onCopy()
                    onDismiss()
                }
            )
            
            // 编辑并重发 (仅用户消息)
            if (isUser && onEditAndResend != null) {
                SheetActionCard(
                    icon = Lucide.Pencil,
                    text = "编辑并重发",
                    onClick = {
                        onEditAndResend()
                        onDismiss()
                    }
                )
            }
            
            // 重新生成 (仅 AI 消息)
            if (!isUser && onRegenerate != null) {
                SheetActionCard(
                    icon = Lucide.RefreshCw,
                    text = "重新生成",
                    onClick = {
                        onRegenerate()
                        onDismiss()
                    }
                )
            }
            
            // 编辑 (仅 AI 消息)
            if (!isUser && onEdit != null) {
                SheetActionCard(
                    icon = Lucide.Pencil,
                    text = "编辑",
                    onClick = {
                        onEdit()
                        onDismiss()
                    }
                )
            }
            
            // 删除（带确认）
            if (onDelete != null) {
                if (showDeleteConfirm) {
                    // 确认删除状态
                    SheetActionCard(
                        icon = Lucide.TriangleAlert,
                        text = "确认删除？点击删除",
                        containerColor = MaterialTheme.colorScheme.error,
                        onClick = {
                            onDelete()
                            onDismiss()
                        }
                    )
                } else {
                    SheetActionCard(
                        icon = Lucide.Trash2,
                        text = "删除",
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        onClick = {
                            showDeleteConfirm = true
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null)
            Text(text = text, style = MaterialTheme.typography.titleMedium)
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
