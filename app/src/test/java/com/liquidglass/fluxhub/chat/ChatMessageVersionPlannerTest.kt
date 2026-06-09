package com.liquidglass.fluxhub.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatMessageVersionPlannerTest {

    @Test
    fun `returns null when message is not found`() {
        val plan = ChatMessageVersionPlanner.planSwitch(
            messages = listOf(message(id = "existing", versionIndex = 0, totalVersions = 2)),
            messageId = "missing",
            direction = 1
        )

        assertNull(plan)
    }

    @Test
    fun `returns null when clamped version stays unchanged`() {
        val plan = ChatMessageVersionPlanner.planSwitch(
            messages = listOf(message(id = "m1", versionIndex = 0, totalVersions = 3)),
            messageId = "m1",
            direction = -1
        )

        assertNull(plan)
    }

    @Test
    fun `returns null when total versions is not positive`() {
        val plan = ChatMessageVersionPlanner.planSwitch(
            messages = listOf(message(id = "m1", versionIndex = 0, totalVersions = 0)),
            messageId = "m1",
            direction = 1
        )

        assertNull(plan)
    }

    @Test
    fun `moves version within available range`() {
        val plan = ChatMessageVersionPlanner.planSwitch(
            messages = listOf(message(id = "m1", versionIndex = 1, totalVersions = 3)),
            messageId = "m1",
            direction = 1
        )

        assertEquals(0, plan?.messageIndex)
        assertEquals(2, plan?.updatedMessage?.versionIndex)
    }

    @Test
    fun `clamps version to last available index`() {
        val plan = ChatMessageVersionPlanner.planSwitch(
            messages = listOf(message(id = "m1", versionIndex = 0, totalVersions = 2)),
            messageId = "m1",
            direction = 5
        )

        assertEquals(0, plan?.messageIndex)
        assertEquals(1, plan?.updatedMessage?.versionIndex)
    }

    @Test
    fun `reports original list index`() {
        val plan = ChatMessageVersionPlanner.planSwitch(
            messages = listOf(
                message(id = "before", versionIndex = 0, totalVersions = 1),
                message(id = "target", versionIndex = 0, totalVersions = 2)
            ),
            messageId = "target",
            direction = 1
        )

        assertEquals(1, plan?.messageIndex)
        assertEquals("target", plan?.updatedMessage?.id)
    }

    private fun message(
        id: String,
        versionIndex: Int,
        totalVersions: Int
    ): UiMessage {
        return UiMessage(
            id = id,
            role = "assistant",
            content = id,
            versionIndex = versionIndex,
            totalVersions = totalVersions
        )
    }
}
