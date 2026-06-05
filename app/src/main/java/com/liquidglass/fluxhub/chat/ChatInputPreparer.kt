package com.liquidglass.fluxhub.chat

data class PreparedUserInput(
    val finalContent: String,
    val plainText: String,
    val imageUri: String?,
    val title: String
) {
    val hasImage: Boolean = imageUri != null
}

object ChatInputPreparer {
    fun prepare(content: String, imageUri: String?): PreparedUserInput? {
        if (content.isBlank() && imageUri == null) return null

        return PreparedUserInput(
            finalContent = if (imageUri != null) {
                "![image]($imageUri)\n$content"
            } else {
                content
            },
            plainText = content,
            imageUri = imageUri,
            title = titleFor(content)
        )
    }

    fun titleFor(content: String): String = content.take(50)
}
