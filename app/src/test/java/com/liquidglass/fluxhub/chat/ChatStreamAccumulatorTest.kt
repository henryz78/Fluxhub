package com.liquidglass.fluxhub.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatStreamAccumulatorTest {

    @Test
    fun applyDeltaAccumulatesContentThinkingAndTokenEstimate() {
        val accumulator = ChatStreamAccumulator()

        accumulator.applyDelta(ChatStreamDelta(reasoningContent = "think"))
        val snapshot = accumulator.applyDelta(ChatStreamDelta(content = "hello world"))

        requireNotNull(snapshot)
        assertEquals("hello world", snapshot.content)
        assertEquals("think", snapshot.thinkingContent)
        assertEquals(2, snapshot.tokenCount)
    }

    @Test
    fun applyDeltaReturnsNullWhenDeltaHasNoVisibleContent() {
        val accumulator = ChatStreamAccumulator()

        assertNull(accumulator.applyDelta(ChatStreamDelta()))
    }

    @Test
    fun finishIsIdempotentAndStopsFurtherAccumulation() {
        val accumulator = ChatStreamAccumulator()

        assertTrue(accumulator.finish())
        assertFalse(accumulator.finish())
        assertNull(accumulator.applyDelta(ChatStreamDelta(content = "late")))
        assertEquals("", accumulator.snapshot().content)
    }

    @Test
    fun streamResetIsSuccessOnlyAfterContentExists() {
        val accumulator = ChatStreamAccumulator()

        assertFalse(accumulator.isStreamResetSuccess("stream was reset"))

        accumulator.applyDelta(ChatStreamDelta(content = "partial"))

        assertTrue(accumulator.isStreamResetSuccess("stream was reset"))
        assertTrue(accumulator.isStreamResetSuccess("STREAM WAS RESET"))
    }
}
