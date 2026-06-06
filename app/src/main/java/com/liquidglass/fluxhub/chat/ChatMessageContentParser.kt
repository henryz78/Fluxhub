package com.liquidglass.fluxhub.chat

data class ParsedMessageContent(
    val imageUrl: String?,
    val text: String
)

object ChatMessageContentParser {
    private val leadingImageRegex = Regex("^!\\[image\\]\\((.+?)\\)")

    fun parse(content: String): ParsedMessageContent {
        val imageMatch = leadingImageRegex.find(content)
            ?: return ParsedMessageContent(imageUrl = null, text = content)

        return ParsedMessageContent(
            imageUrl = imageMatch.groupValues[1],
            text = content.substring(imageMatch.range.last + 1).trimStart()
        )
    }
}
