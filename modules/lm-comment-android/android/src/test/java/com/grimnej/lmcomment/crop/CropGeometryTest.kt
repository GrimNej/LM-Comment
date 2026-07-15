package com.grimnej.lmcomment.crop

import java.util.Random
import kotlin.math.max
import kotlin.math.min
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CropGeometryTest {
    @Test
    fun fullFrameMapsToExclusiveBitmapBoundsForEveryFrameShape() {
        val frameSizes = listOf(
            1 to 1,
            1 to 4_096,
            4_096 to 1,
            1_080 to 1_920,
            1_920 to 1_080,
        )

        frameSizes.forEach { (width, height) ->
            assertEquals(
                "$width x $height",
                PixelCropRect(0, 0, width, height),
                CropGeometry.toPixels(NormalizedCropRect.FullFrame, width, height),
            )
        }
    }

    @Test
    fun pixelMappingPropertyAlwaysProducesAnInBoundsNonEmptyRectangle() {
        val random = Random(0x1C0FFEE)
        val frameSizes = listOf(
            1 to 1,
            1 to 4_096,
            4_096 to 1,
            2 to 3,
            17 to 29,
            1_080 to 1_920,
            1_920 to 1_080,
        )

        repeat(1_000) { sample ->
            val x1 = random.nextInt(1_000_000)
            val x2 = random.nextInt(1_000_000)
            val y1 = random.nextInt(1_000_000)
            val y2 = random.nextInt(1_000_000)
            val selection = NormalizedCropRect(
                left = min(x1, x2) / 1_000_001f,
                top = min(y1, y2) / 1_000_001f,
                right = (max(x1, x2) + 1) / 1_000_001f,
                bottom = (max(y1, y2) + 1) / 1_000_001f,
            )

            frameSizes.forEach { (width, height) ->
                val pixels = CropGeometry.toPixels(selection, width, height)
                assertPixelRectInBounds("sample $sample at $width x $height", pixels, width, height)
            }
        }
    }

    @Test
    fun subpixelSelectionStillMapsToAtLeastOnePixel() {
        val tiny = NormalizedCropRect(
            left = 0.5010000f,
            top = 0.7490000f,
            right = 0.5010001f,
            bottom = 0.7490001f,
        )

        listOf(1 to 1, 3 to 5, 1_080 to 1_920, 1_920 to 1_080).forEach { (width, height) ->
            val pixels = CropGeometry.toPixels(tiny, width, height)
            assertPixelRectInBounds("$width x $height", pixels, width, height)
            assertTrue("crop width must contain a pixel", pixels.width >= 1)
            assertTrue("crop height must contain a pixel", pixels.height >= 1)
        }
    }

    @Test
    fun portraitAndLandscapeFramesUseTheirOwnBitmapDimensions() {
        val selection = NormalizedCropRect(0.10f, 0.20f, 0.90f, 0.80f)

        assertEquals(
            PixelCropRect(100, 400, 900, 1_600),
            CropGeometry.toPixels(selection, frameWidth = 1_000, frameHeight = 2_000),
        )
        assertEquals(
            PixelCropRect(200, 200, 1_800, 800),
            CropGeometry.toPixels(selection, frameWidth = 2_000, frameHeight = 1_000),
        )
    }

    @Test
    fun repeatedArbitraryHandleDragsNeverCrossAndPreserveMinimumPixels() {
        val random = Random(0xC0FFEE)
        val frameWidth = 120
        val frameHeight = 240
        val minimumPixels = 24
        var selection = NormalizedCropRect.Suggested

        repeat(2_000) { sample ->
            val handle = CropHandle.entries[random.nextInt(CropHandle.entries.size)]
            val point = CropPoint(
                x = random.nextFloat() * 4f - 1.5f,
                y = random.nextFloat() * 4f - 1.5f,
            )
            selection = CropGeometry.dragHandle(
                selection = selection,
                handle = handle,
                imagePoint = point,
                frameWidth = frameWidth,
                frameHeight = frameHeight,
                minimumPixels = minimumPixels,
            )
            val pixels = CropGeometry.toPixels(selection, frameWidth, frameHeight)

            assertTrue("sample $sample crossed horizontally", selection.left < selection.right)
            assertTrue("sample $sample crossed vertically", selection.top < selection.bottom)
            assertTrue("sample $sample escaped horizontally", selection.left >= 0f && selection.right <= 1f)
            assertTrue("sample $sample escaped vertically", selection.top >= 0f && selection.bottom <= 1f)
            assertTrue("sample $sample width ${pixels.width}", pixels.width >= minimumPixels)
            assertTrue("sample $sample height ${pixels.height}", pixels.height >= minimumPixels)
        }
    }

    @Test
    fun everyDraggedEdgeMovesMonotonicallyWithItsPointerAxis() {
        val original = NormalizedCropRect(0.20f, 0.25f, 0.80f, 0.75f)
        val positions = listOf(-1f, 0f, 0.10f, 0.30f, 0.50f, 0.70f, 0.90f, 1f, 2f)

        CropHandle.entries.forEach { handle ->
            val horizontalEdges = positions.map { x ->
                val moved = CropGeometry.dragHandle(
                    original,
                    handle,
                    CropPoint(x, 0.50f),
                    frameWidth = 100,
                    frameHeight = 200,
                    minimumPixels = 10,
                )
                if (handle == CropHandle.TopLeft || handle == CropHandle.BottomLeft) moved.left else moved.right
            }
            val verticalEdges = positions.map { y ->
                val moved = CropGeometry.dragHandle(
                    original,
                    handle,
                    CropPoint(0.50f, y),
                    frameWidth = 100,
                    frameHeight = 200,
                    minimumPixels = 10,
                )
                if (handle == CropHandle.TopLeft || handle == CropHandle.TopRight) moved.top else moved.bottom
            }

            assertMonotonic("$handle horizontal", horizontalEdges)
            assertMonotonic("$handle vertical", verticalEdges)
        }
    }

    @Test
    fun fitTransformLetterboxesPortraitAndLandscapeAtTheExpectedEdges() {
        val portrait = PreviewTransform.fit(100, 200, 300f, 300f)
        assertPreviewRect(PreviewRect(75f, 0f, 225f, 300f), portrait.contentBounds)

        val landscape = PreviewTransform.fit(200, 100, 300f, 300f)
        assertPreviewRect(PreviewRect(0f, 75f, 300f, 225f), landscape.contentBounds)
    }

    @Test
    fun letterboxPointsClampToNearestImageEdge() {
        val sideBars = PreviewTransform.fit(100, 200, 500f, 200f)
        assertPoint(CropPoint(0f, 0.50f), sideBars.previewToImage(CropPoint(0f, 100f)))
        assertPoint(CropPoint(1f, 0.50f), sideBars.previewToImage(CropPoint(500f, 100f)))

        val topBars = PreviewTransform.fit(200, 100, 200f, 500f)
        assertPoint(CropPoint(0.50f, 0f), topBars.previewToImage(CropPoint(100f, 0f)))
        assertPoint(CropPoint(0.50f, 1f), topBars.previewToImage(CropPoint(100f, 500f)))

        assertPoint(CropPoint(0f, 0f), topBars.previewToImage(CropPoint(-100f, -100f)))
        assertPoint(CropPoint(1f, 1f), topBars.previewToImage(CropPoint(1_000f, 1_000f)))
    }

    @Test
    fun previewRoundTripPropertyHoldsForPortraitLandscapeAndEveryQuarterTurn() {
        val random = Random(0xA11CE)
        val frameSizes = listOf(1_080 to 1_920, 1_920 to 1_080)

        frameSizes.forEach { (width, height) ->
            for (turns in 0..3) {
                val transform = PreviewTransform.fit(width, height, 733f, 1_117f, turns)
                repeat(250) { sample ->
                    val point = CropPoint(random.nextFloat(), random.nextFloat())
                    val result = transform.previewToImage(transform.imageToPreview(point))
                    assertPoint(point, result, "$width x $height, turn $turns, sample $sample")
                }
            }
        }
    }

    @Test
    fun allQuarterTurnsMapNormalizedPointsInTheExpectedDirection() {
        val point = CropPoint(0.25f, 0.75f)
        val expectedOrientedPoints = listOf(
            CropPoint(0.25f, 0.75f),
            CropPoint(0.25f, 0.25f),
            CropPoint(0.75f, 0.25f),
            CropPoint(0.75f, 0.75f),
        )

        for (turns in 0..3) {
            val transform = PreviewTransform.fit(200, 100, 400f, 400f, turns)
            val preview = transform.imageToPreview(point)
            val oriented = CropPoint(
                x = (preview.x - transform.contentBounds.left) / transform.contentBounds.width,
                y = (preview.y - transform.contentBounds.top) / transform.contentBounds.height,
            )

            assertPoint(expectedOrientedPoints[turns], oriented, "turn $turns")
            assertPoint(point, transform.previewToImage(preview), "inverse turn $turns")
        }
    }

    @Test
    fun quarterTurnsAreNormalizedForNegativeAndOverflowValues() {
        val negative = PreviewTransform.fit(200, 100, 400f, 400f, -1)
        val positive = PreviewTransform.fit(200, 100, 400f, 400f, 3)
        val overflow = PreviewTransform.fit(200, 100, 400f, 400f, 7)
        val point = CropPoint(0.12f, 0.89f)

        assertEquals(3, negative.quarterTurnsClockwise)
        assertEquals(3, overflow.quarterTurnsClockwise)
        assertPoint(positive.imageToPreview(point), negative.imageToPreview(point))
        assertPoint(positive.imageToPreview(point), overflow.imageToPreview(point))
    }

    @Test
    fun rotatedSelectionBoundsContainEveryRotatedCorner() {
        val selection = NormalizedCropRect(0.10f, 0.20f, 0.40f, 0.80f)
        val transform = PreviewTransform.fit(200, 100, 400f, 400f, quarterTurnsClockwise = 1)
        val bounds = transform.selectionBounds(selection)

        assertPreviewRect(PreviewRect(140f, 40f, 260f, 160f), bounds)
        CropHandle.entries.forEach { handle ->
            val corner = transform.imageToPreview(selection.corner(handle))
            assertTrue(corner.x in bounds.left..bounds.right)
            assertTrue(corner.y in bounds.top..bounds.bottom)
        }
    }

    @Test
    fun dragInLetterboxedPreviewClampsAtTheImageBoundaryWithoutCrossing() {
        val transform = PreviewTransform.fit(100, 200, 500f, 200f)
        val original = NormalizedCropRect(0.20f, 0.20f, 0.80f, 0.80f)
        val moved = CropGeometry.dragHandleInPreview(
            selection = original,
            handle = CropHandle.TopLeft,
            previewPoint = CropPoint(-1_000f, -1_000f),
            transform = transform,
            minimumPixels = 10,
        )

        assertEquals(0f, moved.left, FLOAT_TOLERANCE)
        assertEquals(0f, moved.top, FLOAT_TOLERANCE)
        assertTrue(moved.left < moved.right)
        assertTrue(moved.top < moved.bottom)
    }

    private fun assertPixelRectInBounds(
        message: String,
        rect: PixelCropRect,
        frameWidth: Int,
        frameHeight: Int,
    ) {
        assertTrue("$message: left ${rect.left}", rect.left in 0 until frameWidth)
        assertTrue("$message: top ${rect.top}", rect.top in 0 until frameHeight)
        assertTrue("$message: right ${rect.right}", rect.right in 1..frameWidth)
        assertTrue("$message: bottom ${rect.bottom}", rect.bottom in 1..frameHeight)
        assertTrue("$message: horizontal edges crossed", rect.left < rect.right)
        assertTrue("$message: vertical edges crossed", rect.top < rect.bottom)
    }

    private fun assertMonotonic(message: String, values: List<Float>) {
        values.zipWithNext().forEachIndexed { index, (before, after) ->
            assertTrue(
                "$message regressed at $index: $before then $after",
                after + FLOAT_TOLERANCE >= before,
            )
        }
    }

    private fun assertPreviewRect(expected: PreviewRect, actual: PreviewRect) {
        assertEquals(expected.left, actual.left, FLOAT_TOLERANCE)
        assertEquals(expected.top, actual.top, FLOAT_TOLERANCE)
        assertEquals(expected.right, actual.right, FLOAT_TOLERANCE)
        assertEquals(expected.bottom, actual.bottom, FLOAT_TOLERANCE)
    }

    private fun assertPoint(expected: CropPoint, actual: CropPoint, message: String = "") {
        assertEquals("$message x", expected.x, actual.x, FLOAT_TOLERANCE)
        assertEquals("$message y", expected.y, actual.y, FLOAT_TOLERANCE)
    }

    private companion object {
        const val FLOAT_TOLERANCE = 0.0001f
    }
}
