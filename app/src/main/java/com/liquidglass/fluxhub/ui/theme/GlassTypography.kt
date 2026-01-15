package com.liquidglass.fluxhub.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 动态字体样式生成器
 * 
 * 根据用户设置动态生成字体样式：
 * - textColorMode: "white" 或 "black"
 * - textShadowEnabled: 是否启用阴影
 */
object GlassTypography {
    
    /**
     * 根据设置生成所有样式
     */
    fun create(
        colorMode: String = "white",
        shadowEnabled: Boolean = true
    ): GlassTextStyles {
        val baseColor = if (colorMode == "black") Color.Black else Color.White
        val shadowColor = if (colorMode == "black") Color.White else Color.Black
        
        return GlassTextStyles(
            baseColor = baseColor,
            shadowColor = shadowColor,
            shadowEnabled = shadowEnabled
        )
    }
    
    // ============== 兼容旧代码的默认样式 ==============
    // 这些属性保持向后兼容，使用默认的白色+阴影配置
    
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
    
    val body = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        color = Color.White,
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.5f),
            blurRadius = 4f
        )
    )
    
    val bodyMedium = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = Color.White,
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.5f),
            blurRadius = 4f
        )
    )
    
    val bodyLarge = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        color = Color.White,
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.5f),
            blurRadius = 4f
        )
    )
    
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
    
    val caption = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        color = Color.White.copy(alpha = 0.8f),
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.4f),
            blurRadius = 3f
        )
    )
    
    val small = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.Normal,
        color = Color.White.copy(alpha = 0.7f),
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.4f),
            blurRadius = 3f
        )
    )
    
    val tiny = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.Normal,
        color = Color.White.copy(alpha = 0.7f),
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.3f),
            blurRadius = 2f
        )
    )
    
    val button = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.4f),
            blurRadius = 3f
        )
    )
    
    val buttonSmall = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = Color.White,
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.4f),
            blurRadius = 3f
        )
    )
    
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
 * 动态生成的字体样式集合
 */
data class GlassTextStyles(
    val baseColor: Color,
    val shadowColor: Color,
    val shadowEnabled: Boolean
) {
    private fun shadow(alpha: Float, blurRadius: Float, offset: Offset = Offset.Zero): Shadow? {
        return if (shadowEnabled) {
            Shadow(color = shadowColor.copy(alpha = alpha), blurRadius = blurRadius, offset = offset)
        } else null
    }
    
    val displayLarge: TextStyle get() = TextStyle(
        fontSize = 36.sp,
        fontWeight = FontWeight.Bold,
        color = baseColor,
        shadow = shadow(0.6f, 12f, Offset(0f, 2f))
    )
    
    val title: TextStyle get() = TextStyle(
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        color = baseColor,
        shadow = shadow(0.6f, 10f, Offset(0f, 2f))
    )
    
    val titleMedium: TextStyle get() = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium,
        color = baseColor,
        shadow = shadow(0.5f, 6f, Offset(0f, 1f))
    )
    
    val titleSmall: TextStyle get() = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        color = baseColor,
        shadow = shadow(0.5f, 5f, Offset(0f, 1f))
    )
    
    val body: TextStyle get() = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        color = baseColor,
        shadow = shadow(0.5f, 4f)
    )
    
    val bodyMedium: TextStyle get() = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = baseColor,
        shadow = shadow(0.5f, 4f)
    )
    
    val bodyLarge: TextStyle get() = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        color = baseColor,
        shadow = shadow(0.5f, 4f)
    )
    
    val label: TextStyle get() = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = baseColor.copy(alpha = 0.9f),
        letterSpacing = 1.sp,
        shadow = shadow(0.5f, 4f)
    )
    
    val caption: TextStyle get() = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        color = baseColor.copy(alpha = 0.8f),
        shadow = shadow(0.4f, 3f)
    )
    
    val small: TextStyle get() = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.Normal,
        color = baseColor.copy(alpha = 0.7f),
        shadow = shadow(0.4f, 3f)
    )
    
    val button: TextStyle get() = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = baseColor,
        shadow = shadow(0.4f, 3f)
    )
    
    fun navLabelStyle(isSelected: Boolean): TextStyle {
        return if (isSelected) {
            TextStyle(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF007AFF),
                shadow = shadow(0.4f, 4f, Offset(1f, 1f))
            )
        } else {
            TextStyle(
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal,
                color = Color.Gray,
                shadow = shadow(0.4f, 4f, Offset(1f, 1f))
            )
        }
    }
}

/**
 * 工具函数：根据选中状态返回对应的导航栏样式（向后兼容）
 */
fun GlassTypography.navLabelStyle(isSelected: Boolean): TextStyle {
    return if (isSelected) navLabelSelected else navLabel.copy(color = Color.Gray)
}
