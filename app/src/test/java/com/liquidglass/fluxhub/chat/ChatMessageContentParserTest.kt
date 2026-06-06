package com.liquidglass.fluxhub.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatMessageContentParserTest {

    @Test
    fun `keeps plain text unchanged when no leading image is present`() {
        val parsed = ChatMessageContentParser.parse("Hello from Fluxhub")

        assertNull(parsed.imageUrl)
        assertEquals("Hello from Fluxhub", parsed.text)
    }

    @Test
    fun `extracts leading image url and trims following text start`() {
        val parsed = ChatMessageContentParser.parse(
            "![image](content://picked-image)\n\nDescribe this image"
        )

        assertEquals("content://picked-image", parsed.imageUrl)
        assertEquals("Describe this image", parsed.text)
    }

    @Test
    fun `returns empty text when message only contains a leading image`() {
        val parsed = ChatMessageContentParser.parse("![image](file:///tmp/photo.png)")

        assertEquals("file:///tmp/photo.png", parsed.imageUrl)
        assertEquals("", parsed.text)
    }

    @Test
    fun `does not extract image markdown outside the message start`() {
        val content = "Look at this ![image](content://not-leading)"

        val parsed = ChatMessageContentParser.parse(content)

        assertNull(parsed.imageUrl)
        assertEquals(content, parsed.text)
    }
}
