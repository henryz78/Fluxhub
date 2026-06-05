package com.liquidglass.fluxhub.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatInputPreparerTest {

    @Test
    fun prepareRejectsBlankTextWithoutImage() {
        assertNull(ChatInputPreparer.prepare("   ", null))
    }

    @Test
    fun prepareKeepsPlainTextMessage() {
        val input = ChatInputPreparer.prepare("hello", null)

        requireNotNull(input)
        assertEquals("hello", input.finalContent)
        assertEquals("hello", input.plainText)
        assertEquals("hello", input.title)
        assertFalse(input.hasImage)
    }

    @Test
    fun preparePrefixesImageMarkdownWhenImageExists() {
        val input = ChatInputPreparer.prepare("what is this?", "content://image/1")

        requireNotNull(input)
        assertEquals("![image](content://image/1)\nwhat is this?", input.finalContent)
        assertEquals("what is this?", input.plainText)
        assertEquals("what is this?", input.title)
        assertTrue(input.hasImage)
    }

    @Test
    fun titleForLimitsToFiftyCharacters() {
        val title = ChatInputPreparer.titleFor("a".repeat(60))

        assertEquals(50, title.length)
        assertEquals("a".repeat(50), title)
    }
}
