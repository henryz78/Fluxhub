package com.liquidglass.fluxhub.ui.components.richtext

import android.content.Intent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.core.net.toUri
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Copy
import com.composables.icons.lucide.Lucide
import kotlinx.coroutines.launch
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import org.intellij.markdown.parser.MarkdownParser
import com.liquidglass.fluxhub.ui.components.table.DataTable

private val flavour by lazy {
    GFMFlavourDescriptor(
        makeHttpsAutoLinks = true, useSafeLinks = true
    )
}

private val parser by lazy {
    MarkdownParser(flavour)
}

private val THINKING_REGEX = Regex("<think>([\\s\\S]*?)(?:</think>|$)", RegexOption.DOT_MATCHES_ALL)
private val CODE_BLOCK_REGEX = Regex("```[\\s\\S]*?```|`[^`\\n]*`", RegexOption.DOT_MATCHES_ALL)

// 预处理markdown内容
private fun preProcess(content: String): String {
    // 替换思考块为引用
    return content.replace(THINKING_REGEX) { matchResult ->
        matchResult.groupValues[1].lines().filter { it.isNotBlank() }.joinToString("\n") { ">$it" }
    }
}

object HeaderStyle {
    val H1 = TextStyle(fontStyle = FontStyle.Normal, fontWeight = FontWeight.Bold, fontSize = 24.sp)
    val H2 = TextStyle(fontStyle = FontStyle.Normal, fontWeight = FontWeight.Bold, fontSize = 20.sp)
    val H3 = TextStyle(fontStyle = FontStyle.Normal, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    val H4 = TextStyle(fontStyle = FontStyle.Normal, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    val H5 = TextStyle(fontStyle = FontStyle.Normal, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    val H6 = TextStyle(fontStyle = FontStyle.Normal, fontWeight = FontWeight.Bold, fontSize = 12.sp)
}

@Composable
fun MarkdownBlock(
    content: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    onClickCitation: (String) -> Unit = {}
) {
    var data by remember {
        val preprocessed = preProcess(content)
        val astTree = parser.buildMarkdownTreeFromString(preprocessed)
        mutableStateOf(preprocessed to astTree, policy = neverEqualPolicy())
    }

    // 监听内容变化，在后台线程重新解析AST树
    val updatedContent by rememberUpdatedState(content)
    LaunchedEffect(Unit) {
        snapshotFlow { updatedContent }
            .distinctUntilChanged()
            .mapLatest { text ->
                // 大幅降低延迟，保证流式输出的实时性
                // 短内容立即解析，长内容稍微等待以避免过于频繁
                val delayMs = when {
                    text.length < 200 -> 0L      // 短内容：无延迟
                    text.length < 1000 -> 50L   // 中等内容：50ms
                    else -> 100L                 // 长内容：100ms（之前是 300-600ms）
                }
                if (delayMs > 0) kotlinx.coroutines.delay(delayMs)
                
                val preprocessed = preProcess(text)
                val astTree = parser.buildMarkdownTreeFromString(preprocessed)
                preprocessed to astTree
            }
            .catch { it.printStackTrace() }
            .flowOn(Dispatchers.Default)
            .collect { data = it }
    }

    val (preprocessed, astTree) = data
    ProvideTextStyle(style) {
        Column(
            modifier = modifier
                .padding(start = 4.dp)
                .animateContentSize() // 顺滑增长
        ) {
            astTree.children.fastForEach { child ->
                MarkdownNode(node = child, content = preprocessed, onClickCitation = onClickCitation)
            }
        }
    }
}

@Composable
private fun MarkdownNode(
    node: ASTNode,
    content: String,
    modifier: Modifier = Modifier,
    onClickCitation: (String) -> Unit = {},
    listLevel: Int = 0
) {
    when (node.type) {
        // 文件根节点
        MarkdownElementTypes.MARKDOWN_FILE -> {
            node.children.fastForEach { child ->
                MarkdownNode(node = child, content = content, modifier = modifier, onClickCitation = onClickCitation)
            }
        }

        // 段落
        MarkdownElementTypes.PARAGRAPH -> {
            Paragraph(node = node, content = content, modifier = modifier, onClickCitation = onClickCitation)
        }

        // 标题
        MarkdownElementTypes.ATX_1, MarkdownElementTypes.ATX_2, MarkdownElementTypes.ATX_3,
        MarkdownElementTypes.ATX_4, MarkdownElementTypes.ATX_5, MarkdownElementTypes.ATX_6 -> {
            val style = when (node.type) {
                MarkdownElementTypes.ATX_1 -> HeaderStyle.H1
                MarkdownElementTypes.ATX_2 -> HeaderStyle.H2
                MarkdownElementTypes.ATX_3 -> HeaderStyle.H3
                MarkdownElementTypes.ATX_4 -> HeaderStyle.H4
                MarkdownElementTypes.ATX_5 -> HeaderStyle.H5
                MarkdownElementTypes.ATX_6 -> HeaderStyle.H6
                else -> HeaderStyle.H1
            }
            ProvideTextStyle(value = style) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    node.children.fastForEach { child ->
                        if (child.type == MarkdownTokenTypes.ATX_CONTENT) {
                            Paragraph(
                                node = child,
                                content = content,
                                onClickCitation = onClickCitation,
                                modifier = modifier.padding(vertical = 8.dp),
                                trim = true,
                            )
                        }
                    }
                }
            }
        }

        // 无序列表
        MarkdownElementTypes.UNORDERED_LIST -> {
            UnorderedListNode(
                node = node,
                content = content,
                modifier = modifier.padding(vertical = 4.dp),
                onClickCitation = onClickCitation,
                level = listLevel
            )
        }

        // 有序列表
        MarkdownElementTypes.ORDERED_LIST -> {
            OrderedListNode(
                node = node,
                content = content,
                modifier = modifier.padding(vertical = 4.dp),
                onClickCitation = onClickCitation,
                level = listLevel
            )
        }

        // Checkbox
        GFMTokenTypes.CHECK_BOX -> {
            val isChecked = node.getTextInNode(content).trim() == "[x]"
            val density = LocalDensity.current
            val checkboxSize = with(density) { 16.dp }
            Surface(
                shape = RoundedCornerShape(2.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = modifier,
            ) {
                Box(
                    modifier = Modifier.padding(2.dp).size(checkboxSize),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isChecked) {
                        Icon(
                            imageVector = Lucide.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // 引用块
        MarkdownElementTypes.BLOCK_QUOTE -> {
            ProvideTextStyle(LocalTextStyle.current.copy(fontStyle = FontStyle.Italic)) {
                val borderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                val bgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                Column(
                    modifier = Modifier
                        .drawWithContent {
                            drawContent()
                            drawRect(color = bgColor, size = size)
                            drawRect(color = borderColor, size = Size(10f, size.height))
                        }
                        .padding(8.dp)
                ) {
                    node.children.fastForEach { child ->
                        MarkdownNode(node = child, content = content, onClickCitation = onClickCitation)
                    }
                }
            }
        }

        // 链接
        MarkdownElementTypes.INLINE_LINK -> {
            val linkText = node.findChildOfTypeRecursive(MarkdownElementTypes.LINK_TEXT)
                ?.findChildOfTypeRecursive(GFMTokenTypes.GFM_AUTOLINK, MarkdownTokenTypes.TEXT)?.getTextInNode(content)
                ?: ""
            val linkDest = node.findChildOfTypeRecursive(MarkdownElementTypes.LINK_DESTINATION)?.getTextInNode(content) ?: ""
            val context = LocalContext.current
            Text(
                text = linkText,
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
                modifier = modifier.clickable {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, linkDest.toUri())
                        context.startActivity(intent)
                    } catch (e: Exception) { e.printStackTrace() }
                }
            )
        }

        // 斜体
        MarkdownElementTypes.EMPH -> {
            ProvideTextStyle(TextStyle(fontStyle = FontStyle.Italic)) {
                node.children.fastForEach { child ->
                    MarkdownNode(node = child, content = content, modifier = modifier, onClickCitation = onClickCitation)
                }
            }
        }

        // 加粗
        MarkdownElementTypes.STRONG -> {
            ProvideTextStyle(TextStyle(fontWeight = FontWeight.SemiBold)) {
                node.children.fastForEach { child ->
                    MarkdownNode(node = child, content = content, modifier = modifier, onClickCitation = onClickCitation)
                }
            }
        }

        // 删除线
        GFMElementTypes.STRIKETHROUGH -> {
            Text(text = node.getTextInNode(content), textDecoration = TextDecoration.LineThrough, modifier = modifier)
        }

        // 表格
        GFMElementTypes.TABLE -> {
            TableNode(node = node, content = content, modifier = modifier)
        }

        // 水平线
        MarkdownTokenTypes.HORIZONTAL_RULE -> {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                thickness = 0.5.dp
            )
        }

        // 行内代码
        MarkdownElementTypes.CODE_SPAN -> {
            val code = node.getTextInNode(content).trim('`')
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = code,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }

        // 代码块
        MarkdownElementTypes.CODE_BLOCK -> {
            val code = node.getTextInNode(content)
            CodeBlock(code = code, language = "plaintext")
        }

        // 围栏代码块
        MarkdownElementTypes.CODE_FENCE -> {
            val contentStartIndex = node.children.indexOfFirst { it.type == MarkdownTokenTypes.CODE_FENCE_CONTENT }
            if (contentStartIndex == -1) return
            val eolElement = node.children.subList(0, contentStartIndex).findLast { it.type == MarkdownTokenTypes.EOL } ?: return
            val codeContentStartOffset = eolElement.endOffset
            val codeContentEndOffset = node.children.findLast { it.type == MarkdownTokenTypes.CODE_FENCE_CONTENT }?.endOffset ?: return
            val code = content.substring(codeContentStartOffset, codeContentEndOffset).trimIndent()
            val language = node.findChildOfTypeRecursive(MarkdownTokenTypes.FENCE_LANG)?.getTextInNode(content) ?: "plaintext"
            
            CodeBlock(code = code, language = language)
        }

        // 纯文本
        MarkdownTokenTypes.TEXT -> {
            Text(text = node.getTextInNode(content), modifier = modifier)
        }

        // 其他类型，递归处理
        else -> {
            node.children.fastForEach { child ->
                MarkdownNode(node = child, content = content, modifier = modifier, onClickCitation = onClickCitation)
            }
        }
    }
}

@Composable
private fun UnorderedListNode(
    node: ASTNode,
    content: String,
    modifier: Modifier = Modifier,
    onClickCitation: (String) -> Unit = {},
    level: Int = 0
) {
    val bulletStyle = when (level % 3) {
        0 -> "• "
        1 -> "◦ "
        else -> "▪ "
    }
    Column(modifier = modifier.padding(start = (level * 8).dp)) {
        node.children.fastForEach { child ->
            if (child.type == MarkdownElementTypes.LIST_ITEM) {
                ListItemNode(node = child, content = content, bulletText = bulletStyle, onClickCitation = onClickCitation, level = level)
            }
        }
    }
}

@Composable
private fun OrderedListNode(
    node: ASTNode,
    content: String,
    modifier: Modifier = Modifier,
    onClickCitation: (String) -> Unit = {},
    level: Int = 0
) {
    Column(modifier.padding(start = (level * 8).dp)) {
        var index = 1
        node.children.fastForEach { child ->
            if (child.type == MarkdownElementTypes.LIST_ITEM) {
                val numberText = child.findChildOfTypeRecursive(MarkdownTokenTypes.LIST_NUMBER)?.getTextInNode(content) ?: "$index. "
                ListItemNode(node = child, content = content, bulletText = numberText, onClickCitation = onClickCitation, level = level)
                index++
            }
        }
    }
}

@Composable
private fun ListItemNode(
    node: ASTNode,
    content: String,
    bulletText: String,
    onClickCitation: (String) -> Unit = {},
    level: Int
) {
    Column {
        val (directContent, nestedLists) = separateContentAndLists(node)
        if (directContent.isNotEmpty()) {
            Row {
                Text(text = bulletText, modifier = Modifier.alignByBaseline())
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    directContent.fastForEach { contentChild ->
                        MarkdownNode(node = contentChild, content = content, onClickCitation = onClickCitation, listLevel = level)
                    }
                }
            }
        }
        nestedLists.fastForEach { nestedList ->
            MarkdownNode(node = nestedList, content = content, onClickCitation = onClickCitation, listLevel = level + 1)
        }
    }
}

private fun separateContentAndLists(listItemNode: ASTNode): Pair<List<ASTNode>, List<ASTNode>> {
    val directContent = mutableListOf<ASTNode>()
    val nestedLists = mutableListOf<ASTNode>()
    listItemNode.children.fastForEach { child ->
        when (child.type) {
            MarkdownElementTypes.UNORDERED_LIST, MarkdownElementTypes.ORDERED_LIST -> nestedLists.add(child)
            else -> directContent.add(child)
        }
    }
    return directContent to nestedLists
}

@Composable
private fun Paragraph(
    node: ASTNode,
    content: String,
    trim: Boolean = false,
    onClickCitation: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.then(
            if (node.nextSibling() != null) Modifier.padding(bottom = 4.dp) else Modifier
        )
    ) {
        node.children.fastForEach { child ->
            MarkdownNode(node = child, content = content, onClickCitation = onClickCitation)
        }
    }
}

@Composable
private fun TableNode(
    node: ASTNode,
    content: String,
    modifier: Modifier = Modifier
) {
    val headerNode = node.children.find { it.type == GFMElementTypes.HEADER }
    val rowNodes = node.children.filter { it.type == GFMElementTypes.ROW }
    
    val headers = headerNode?.children?.filter { it.type == GFMTokenTypes.CELL }?.map { cellNode ->
        @Composable {
            Paragraph(node = cellNode, content = content, trim = true)
        }
    } ?: emptyList()
    
    val rows = rowNodes.map { rowNode ->
        rowNode.children.filter { it.type == GFMTokenTypes.CELL }.map { cellNode ->
            @Composable {
                Paragraph(node = cellNode, content = content, trim = true)
            }
        }
    }
    
    if (headers.isEmpty() && rows.isEmpty()) return
    
    DataTable(
        headers = headers,
        rows = rows,
        modifier = modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun CodeBlock(
    code: String,
    language: String,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val clipboardManager = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var showCopied by remember { mutableStateOf(false) }
    
    // 复制成功提示自动消失
    LaunchedEffect(showCopied) {
        if (showCopied) {
            kotlinx.coroutines.delay(2000)
            showCopied = false
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        // 顶部操作栏：语言标签 + 复制按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.2f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 语言标签
            Text(
                text = language.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.8f)
            )
            
            // 复制按钮
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable {
                        scope.launch {
                            clipboardManager.setClipEntry(
                                ClipEntry(android.content.ClipData.newPlainText("code", code))
                            )
                            showCopied = true
                        }
                    }
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (showCopied) Lucide.Check else Lucide.Copy,
                    contentDescription = if (showCopied) "已复制" else "复制代码",
                    modifier = Modifier.size(12.dp),
                    tint = if (showCopied) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.6f)
                )
                Text(
                    text = if (showCopied) "已复制" else "复制",
                    fontSize = 10.sp,
                    color = if (showCopied) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.6f)
                )
            }
        }
        
        // 代码内容
        SelectionContainer {
            Box(modifier = Modifier.padding(12.dp)) {
                HighlightText(
                    code = code,
                    language = language,
                    modifier = Modifier.horizontalScroll(scrollState),
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

// 扩展函数：递归查找子节点
private fun ASTNode.findChildOfTypeRecursive(vararg types: org.intellij.markdown.IElementType): ASTNode? {
    children.forEach { child ->
        if (child.type in types) return child
        child.findChildOfTypeRecursive(*types)?.let { return it }
    }
    return null
}

// 扩展函数：获取下一个兄弟节点
private fun ASTNode.nextSibling(): ASTNode? {
    val parent = this.parent ?: return null
    val index = parent.children.indexOf(this)
    return if (index >= 0 && index < parent.children.size - 1) {
        parent.children[index + 1]
    } else null
}

// 扩展函数：从文本中获取节点内容
private fun ASTNode.getTextInNode(text: String): String {
    return text.substring(startOffset, endOffset)
}

