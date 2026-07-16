package com.grimnej.lmcomment.bubble

import org.junit.Assert.assertEquals
import org.junit.Test

class BubbleAnchorMathTest {
    private val bounds = BubbleBounds(left = 10, top = 20, right = 410, bottom = 820)

    @Test
    fun `position clamps vertical fraction and honors both edges`() {
        assertEquals(
            BubblePosition(10, 20),
            BubbleAnchorMath.position(BubbleEdge.LEFT, -4f, bounds, bubbleSize = 60),
        )
        assertEquals(
            BubblePosition(350, 760),
            BubbleAnchorMath.position(BubbleEdge.RIGHT, 4f, bounds, bubbleSize = 60),
        )
    }

    @Test
    fun `position remains inside bounds when bubble is larger than the window`() {
        assertEquals(
            BubblePosition(10, 20),
            BubbleAnchorMath.position(BubbleEdge.RIGHT, 1f, bounds, bubbleSize = 1_000),
        )
    }

    @Test
    fun `anchor selection clamps fraction and selects nearest horizontal edge`() {
        assertEquals(
            BubbleEdge.LEFT to 0f,
            BubbleAnchorMath.fromPosition(x = 10, y = -200, bounds = bounds, bubbleSize = 60),
        )
        assertEquals(
            BubbleEdge.RIGHT to 1f,
            BubbleAnchorMath.fromPosition(x = 350, y = 2_000, bounds = bounds, bubbleSize = 60),
        )
    }
}
