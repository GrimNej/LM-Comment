package com.grimnej.lmcomment.bubble

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BubbleGlyphGeometryTest {
    @Test
    fun `all four corner marks are present exactly once`() {
        assertEquals(
            BubbleGlyphCorner.entries.toSet(),
            BubbleGlyphGeometry.cornerMarks.map { it.corner }.toSet(),
        )
        assertEquals(4, BubbleGlyphGeometry.cornerMarks.size)
    }

    @Test
    fun `every corner mark opens toward the center`() {
        val marks = BubbleGlyphGeometry.cornerMarks.associateBy { it.corner }
        val topLeft = marks.getValue(BubbleGlyphCorner.TOP_LEFT)
        val topRight = marks.getValue(BubbleGlyphCorner.TOP_RIGHT)
        val bottomLeft = marks.getValue(BubbleGlyphCorner.BOTTOM_LEFT)
        val bottomRight = marks.getValue(BubbleGlyphCorner.BOTTOM_RIGHT)

        assertTrue(topLeft.horizontal.endX > topLeft.horizontal.startX)
        assertTrue(topLeft.vertical.endY > topLeft.vertical.startY)
        assertTrue(topRight.horizontal.startX < topRight.horizontal.endX)
        assertTrue(topRight.vertical.endY > topRight.vertical.startY)
        assertTrue(bottomLeft.horizontal.endX > bottomLeft.horizontal.startX)
        assertTrue(bottomLeft.vertical.endY < bottomLeft.vertical.startY)
        assertTrue(bottomRight.horizontal.startX < bottomRight.horizontal.endX)
        assertTrue(bottomRight.vertical.endY < bottomRight.vertical.startY)
    }

    @Test
    fun `bottom right mark faces inward and mirrors bottom left`() {
        val bottomLeft = BubbleGlyphGeometry.cornerMarks.single {
            it.corner == BubbleGlyphCorner.BOTTOM_LEFT
        }
        val bottomRight = BubbleGlyphGeometry.cornerMarks.single {
            it.corner == BubbleGlyphCorner.BOTTOM_RIGHT
        }

        assertTrue("bottom-right vertical arm must point upward", bottomRight.vertical.endY < bottomRight.vertical.startY)
        assertEquals(bottomLeft.vertical.startY, bottomRight.vertical.startY, 0f)
        assertEquals(bottomLeft.vertical.endY, bottomRight.vertical.endY, 0f)
        assertEquals(bottomRight.vertical.startX, bottomRight.vertical.endX, 0f)
        assertEquals(bottomRight.horizontal.endX, bottomRight.vertical.startX, 0f)
        assertEquals(bottomRight.horizontal.endY, bottomRight.vertical.startY, 0f)
    }
}
