package com.liquidglass.fluxhub.ui.components.richtext

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach

val LocalHighlighter = compositionLocalOf<Highlighter> { error("No Highlighter provided") }

private const val MAX_CODE_LENGTH = 8192
private const val COLLAPSE_THRESHOLD = 12
private const val COLLAPSE_LINES = 10

@Composable
fun ProvideHighlighter(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val highlighter = remember { Highlighter(context) }
    CompositionLocalProvider(LocalHighlighter provides highlighter) {
        content()
    }
}

@Composable
fun HighlightText(
    code: String,
    language: String,
    modifier: Modifier = Modifier,
    colors: HighlightTextColorPalette = if (isSystemInDarkTheme()) AtomOneDarkPalette else AtomOneLightPalette,
    fontSize: TextUnit = 12.sp,
    fontFamily: FontFamily = FontFamily.Monospace,
    fontStyle: FontStyle = FontStyle.Normal,
    fontWeight: FontWeight = FontWeight.Normal,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
) {
    val highlighter = LocalHighlighter.current
    var annotatedString by remember { mutableStateOf(AnnotatedString(code)) }

    val updatedCode by rememberUpdatedState(code)
    val updatedLanguage by rememberUpdatedState(language)

    LaunchedEffect(Unit) {
        var lastHighlightedCode = ""
        snapshotFlow { updatedCode to updatedLanguage }.collect { (code, lang) ->
            if (code == lastHighlightedCode) return@collect
            
            if (code.length <= MAX_CODE_LENGTH) {
                // 流式输出优化：如果代码正在快速增长，增加防抖
                // 且如果增量很小（比如不到 10 个字符），可以多等一会儿
                val delta = code.length - lastHighlightedCode.length
                if (delta in 1..10 && code.endsWith("\n").not()) {
                    kotlinx.coroutines.delay(400)
                } else {
                    kotlinx.coroutines.delay(200)
                }
                
                try {
                    val tokens = highlighter.highlight(code, lang)
                    val newAnnotatedString = buildAnnotatedString {
                        tokens.fastForEach { token ->
                            buildHighlightText(token, colors)
                        }
                    }
                    annotatedString = newAnnotatedString
                    lastHighlightedCode = code
                } catch (e: Exception) {
                    e.printStackTrace()
                    // 出错时至少更新纯文本
                    annotatedString = AnnotatedString(code)
                }
            } else {
                annotatedString = AnnotatedString(code)
            }
        }
    }

    val lines = remember(code) { code.lines() }
    val canCollapse = lines.size > COLLAPSE_THRESHOLD
    var isExpanded by remember { mutableStateOf(false) }
    
    val displayCode = if (canCollapse && !isExpanded) {
        lines.take(COLLAPSE_LINES).joinToString("\n")
    } else {
        code
    }

    // 分级渲染策略：如果代码超长，关闭昂贵的实时高亮计算以保性能
    val isVeryLong = remember(code) { code.length > 2000 }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.35f)) // 增强背景对比度
            .padding(10.dp)
            .animateContentSize()
    ) {
        // 单层渲染：优先使用高亮版本，超长代码使用纯文本
        Text(
            text = if (isVeryLong) displayCode else {
                if (canCollapse && !isExpanded) {
                    annotatedString.subSequence(0, displayCode.length.coerceAtMost(annotatedString.length))
                } else {
                    annotatedString
                }
            },
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = fontSize,
                fontFamily = fontFamily,
                fontStyle = fontStyle,
                fontWeight = fontWeight,
                lineHeight = lineHeight,
                color = Color.White.copy(alpha = 0.9f)
            ),
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines,
            minLines = minLines
        )

        if (canCollapse) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isExpanded) "收起代码" else "展开全文 (${lines.size} 行)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .clickable { isExpanded = !isExpanded }
                        .padding(4.dp)
                )
            }
        }
    }
}

fun AnnotatedString.Builder.buildHighlightText(
    token: HighlightToken,
    colors: HighlightTextColorPalette
) {
    when (token) {
        is HighlightToken.Plain -> {
            append(token.content)
        }

        is HighlightToken.Token.StringContent -> {
            withStyle(getStyleForTokenType(token.type, colors)) {
                append(token.content)
            }
        }

        is HighlightToken.Token.Nested -> {
            token.content.forEach {
                buildHighlightText(it, colors)
            }
        }
    }
}

data class HighlightTextColorPalette(
    val keyword: Color,
    val string: Color,
    val number: Color,
    val comment: Color,
    val function: Color,
    val operator: Color,
    val punctuation: Color,
    val className: Color,
    val property: Color,
    val boolean: Color,
    val variable: Color,
    val tag: Color,
    val attrName: Color,
    val attrValue: Color,
    val fallback: Color
)

val AtomOneDarkPalette = HighlightTextColorPalette(
    keyword = Color(0xFFC678DD),
    string = Color(0xFF98C379),
    number = Color(0xFFD19A66),
    comment = Color(0xFF5C6370),
    function = Color(0xFF61AFEF),
    operator = Color(0xFF56B6C2),
    punctuation = Color(0xFFABB2BF),
    className = Color(0xFFE5C07B),
    property = Color(0xFFE06C75),
    boolean = Color(0xFFD19A66),
    variable = Color(0xFFE06C75),
    tag = Color(0xFFE06C75),
    attrName = Color(0xFFD19A66),
    attrValue = Color(0xFF98C379),
    fallback = Color(0xFFABB2BF)
)

val AtomOneLightPalette = HighlightTextColorPalette(
    keyword = Color(0xFFA626A4),
    string = Color(0xFF50A14F),
    number = Color(0xFF986801),
    comment = Color(0xFFA0A1A7),
    function = Color(0xFF4078F2),
    operator = Color(0xFF0184BC),
    punctuation = Color(0xFF383A42),
    className = Color(0xFFC18401),
    property = Color(0xFFE45649),
    boolean = Color(0xFF986801),
    variable = Color(0xFFE45649),
    tag = Color(0xFFE45649),
    attrName = Color(0xFF986801),
    attrValue = Color(0xFF50A14F),
    fallback = Color(0xFF383A42)
)

private fun getStyleForTokenType(type: String, colors: HighlightTextColorPalette): SpanStyle {
    return when (type) {
        "keyword" -> SpanStyle(color = colors.keyword, fontWeight = FontWeight.Bold)
        "string", "attr-value" -> SpanStyle(color = colors.string)
        "number", "boolean", "constant" -> SpanStyle(color = colors.number)
        "comment" -> SpanStyle(color = colors.comment, fontStyle = FontStyle.Italic)
        "function", "method" -> SpanStyle(color = colors.function)
        "operator" -> SpanStyle(color = colors.operator)
        "punctuation" -> SpanStyle(color = colors.punctuation)
        "class-name", "maybe-class-name" -> SpanStyle(color = colors.className)
        "property" -> SpanStyle(color = colors.property)
        "variable", "parameter" -> SpanStyle(color = colors.variable)
        "tag" -> SpanStyle(color = colors.tag)
        "attr-name" -> SpanStyle(color = colors.attrName)
        else -> SpanStyle(color = colors.fallback)
    }
}
