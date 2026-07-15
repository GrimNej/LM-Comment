package com.grimnej.lmcomment.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureGeometryTest {
    @Test
    fun leavesFramesWithinBudgetUnchanged() {
        val result = CaptureGeometry.bounded(1000, 1900, 420)

        assertEquals(CaptureDimensions(1000, 1900, 420), result)
    }

    @Test
    fun neverExceedsHardPixelBudget() {
        val sizes = listOf(
            1440 to 3200,
            3200 to 1440,
            3840 to 2160,
            10_000 to 1,
            1 to 10_000,
        )

        sizes.forEach { (width, height) ->
            val result = CaptureGeometry.bounded(width, height, 560)
            assertTrue("$width x $height became ${result.pixels}", result.pixels <= CaptureGeometry.MAX_PIXELS)
            assertTrue(result.width > 0)
            assertTrue(result.height > 0)
        }
    }

    @Test
    fun keepsPortraitAndLandscapeSymmetric() {
        val portrait = CaptureGeometry.bounded(1440, 3200, 560)
        val landscape = CaptureGeometry.bounded(3200, 1440, 560)

        assertEquals(portrait.width, landscape.height)
        assertEquals(portrait.height, landscape.width)
    }
}
