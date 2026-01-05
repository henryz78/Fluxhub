package com.liquidglass.fluxhub.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.lucide.*

data class Persona(
    val id: String,
    val name: String,
    val description: String,
    val systemPrompt: String,
    val icon: ImageVector,
    val color: Color
)

object Personas {
    val default = Persona(
        id = "flux_assistant",
        name = "Flux 助手",
        description = "全能型 AI 助手，随时为您服务",
        systemPrompt = "You are FluxHub, a helpful and intelligent AI assistant.",
        icon = Lucide.Sparkles,
        color = Color(0xFF007AFF) // Blue
    )

    val all = listOf(
        default,
        Persona(
            id = "translator",
            name = "翻译专家",
            description = "精通多国语言，提供地道翻译",
            systemPrompt = "你是专业的翻译专家。请直接翻译用户输入的内容，保持信达雅。如果用户没有指定目标语言，默认翻译成中文或英文（取决于输入语种）。不要解释，直接给出翻译结果。",
            icon = Lucide.Languages,
            color = Color(0xFFFF9500) // Orange
        ),
        Persona(
            id = "coder",
            name = "代码大师",
            description = "专注于解决编程难题与架构设计",
            systemPrompt = "你是一位资深全栈工程师和架构师。请提供高效、安全、可读性强的代码。解释代码时要清晰简洁。优先使用 Kotlin, Python, TypeScript 等现代语言。",
            icon = Lucide.Code,
            color = Color(0xFF30D158) // Green
        ),
        Persona(
            id = "writer",
            name = "灵感写手",
            description = "无论是文案、故事还是通过润色",
            systemPrompt = "你是一位富有创意的作家。擅长各种文风的写作，包括广告文案、小说、散文、邮件等。请根据用户的需求调整语气和风格。",
            icon = Lucide.PenTool,
            color = Color(0xFFFF2D55) // Pink
        ),
        Persona(
            id = "psychologist",
            name = "倾听者",
            description = "温暖治愈，为您排忧解难",
            systemPrompt = "你是一位富有同理心的心理咨询师。请耐心地倾听用户的烦恼，提供温暖、非评判性的支持和建议。关注用户的情绪。",
            icon = Lucide.Heart,
            color = Color(0xFFBF5AF2) // Purple
        )
    )
}
