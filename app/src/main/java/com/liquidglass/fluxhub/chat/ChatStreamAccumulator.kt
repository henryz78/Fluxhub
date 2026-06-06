package com.liquidglass.fluxhub.chat

data class ChatStreamSnapshot(
    val content: String,
    val thinkingContent: String?,
    val tokenCount: Int
) {
    val hasContent: Boolean = content.isNotEmpty()
    val contentLength: Int = content.length
}

class ChatStreamAccumulator {
    private var content = ""
    private var thinkingContent = ""
    private var tokenCount = 0
    private var finished = false

    val isFinished: Boolean
        get() = finished

    fun applyDelta(delta: ChatStreamDelta): ChatStreamSnapshot? {
        if (finished) return null

        var changed = false

        if (delta.reasoningContent.isNotEmpty()) {
            thinkingContent += delta.reasoningContent
            changed = true
        }

        if (delta.content.isNotEmpty()) {
            content += delta.content
            tokenCount += (delta.content.length / 4).coerceAtLeast(1)
            changed = true
        }

        return if (changed) snapshot() else null
    }

    fun finish(): Boolean {
        if (finished) return false
        finished = true
        return true
    }

    fun isStreamResetSuccess(message: String): Boolean {
        return message.contains("stream was reset", ignoreCase = true) && snapshot().hasContent
    }

    fun snapshot(): ChatStreamSnapshot {
        return ChatStreamSnapshot(
            content = content,
            thinkingContent = thinkingContent.takeIf { it.isNotEmpty() },
            tokenCount = tokenCount
        )
    }
}
