package com.liquidglass.fluxhub.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 统一的毛玻璃字体样式系统
 * 
 * 使用双层阴影技术确保文字在任何壁纸背景下都清晰可见：
 * - 深色阴影（黑色）：在浅色壁纸上提供对比度
 * - 通过增强的模糊半径确保阴影均匀分布
 * 
 * 使用方法：
 * Text(text = "标题", style = GlassTypography.title)
 * BasicText(text = "正文", style = GlassTypography.body)
 */
object GlassTypography {
    
    // ============== 标题样式 ==============
    
    /**
     * 超大标题 - 用于主页问候等
     * 36sp, Bold, 强阴影
     */
    val displayLarge = TextStyle(
        fontSize = 36.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.6f),
            blurRadius = 12f,
            offset = Offset(0f, 2f)
        )
    )
    
    /**
     * 大标题 - 用于页面标题
     * 32sp, Bold
     */
    val title = TextStyle(
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.6f),
            blurRadius = 10f,
            offset = Offset(0f, 2f)
        )
    )
    
    /**
     * 中等标题 - 用于卡片标题、对话标题等
     * 18sp, Medium
     */
    val titleMedium = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium,
        color = Color.White,
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.5f),
            blurRadius = 6f,
            offset = Offset(0f, 1f)
        )
    )
    
    /**
     * 小标题 - 用于列表项标题
     * 16sp, Medium
     */
    val titleSmall = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        color = Color.White,
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.5f),
            blurRadius = 5f,
            offset = Offset(0f, 1f)
        )
    )
    
    // ============== 正文样式 ==============
    
    /**
     * 正文 - 主要内容文字
     * 14sp, Normal
     */
    val body = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        color = Color.White,
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.5f),
            blurRadius = 4f
        )
    )
    
    /**
     * 正文（强调）- 重要的正文内容
     * 14sp, Medium
     */
    val bodyMedium = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = Color.White,
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.5f),
            blurRadius = 4f
        )
    )
    
    /**
     * 正文（大）- 对话内容等
     * 16sp, Normal
     */
    val bodyLarge = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        color = Color.White,
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.5f),
            blurRadius = 4f
        )
    )
    
    // ============== 辅助样式 ==============
    
    /**
     * 标签 - 用于分组标题、标签等
     * 13sp, SemiBold, 带字间距
     */
    val label = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color.White.copy(alpha = 0.9f),
        letterSpacing = 1.sp,
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.5f),
            blurRadius = 4f
        )
    )
    
    /**
     * 副标题/描述 - 用于次要信息
     * 12sp, Normal, 稍透明
     */
    val caption = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        color = Color.White.copy(alpha = 0.8f),
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.4f),
            blurRadius = 3f
        )
    )
    
    /**
     * 小字 - 用于提示、时间戳等
     * 11sp, Normal
     */
    val small = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.Normal,
        color = Color.White.copy(alpha = 0.7f),
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.4f),
            blurRadius = 3f
        )
    )
    
    /**
     * 超小字 - 用于版本号等
     * 10sp, Normal
     */
    val tiny = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.Normal,
        color = Color.White.copy(alpha = 0.7f),
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.3f),
            blurRadius = 2f
        )
    )
    
    // ============== 按钮样式 ==============
    
    /**
     * 按钮文字
     * 15sp, Bold
     */
    val button = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.4f),
            blurRadius = 3f
        )
    )
    
    /**
     * 小按钮文字
     * 13sp, Medium
     */
    val buttonSmall = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = Color.White,
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.4f),
            blurRadius = 3f
        )
    )
    
    // ============== 导航栏样式 ==============
    
    /**
     * 底部导航栏文字
     * 10sp, 带阴影确保可见
     */
    val navLabel = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.Normal,
        color = Color.White,
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.4f),
            blurRadius = 4f,
            offset = Offset(1f, 1f)
        )
    )
    
    /**
     * 底部导航栏文字（选中状态）
     */
    val navLabelSelected = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF007AFF),
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.4f),
            blurRadius = 4f,
            offset = Offset(1f, 1f)
        )
    )
}

/**
 * 工具函数：根据选中状态返回对应的导航栏样式
 */
fun GlassTypography.navLabelStyle(isSelected: Boolean): TextStyle {
    return if (isSelected) navLabelSelected else navLabel.copy(color = Color.Gray)
}
