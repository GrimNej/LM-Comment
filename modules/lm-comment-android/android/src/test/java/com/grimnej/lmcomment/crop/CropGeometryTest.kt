package com.grimnej.lmcomment.crop

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CropGeometryTest {
    @Test
    fun fullFrameMapsToExclusiveBitmapBounds() {
        assertEquals(
            PixelCropRect(0, 0, 1080, 1920),
            CropGeometry.toPixels(NormalizedCropRect.FullFrame, 1080, 1920),
        )
    }

    @Test
    fun crossedDragIsClampedToOnePixelMinimum() {
        val original = NormalizedCropRect(0.2f, 0.2f, 0.8f, 0.8f)
        val moved = CropGeometry.dragHandle(
            original,
            CropHandle.TopLeft,
            CropPoint(1f, 1f),
            frameWidth = 100,
            frameHeight = 200,
        )
        val pixels = CropGeometry.toPixels(moved, 100, 200)

        assertTrue(pixels.width >= 1)
        assertTrue(pixels.height >= 1)
        assertTrue(moved.left < moved.right && moved.top < moved.bottom)
    }

    @Test
    fun fitTransformClampsLetterboxAndRoundTripsCorners() {
        val transform = PreviewTransform.fit(100, 200, 300f, 300f)
        assertEquals(75f, transform.contentBounds.left, 0.001f)
        assertEquals(225f, transform.contentBounds.right, 0.001f)
        assertEquals(CropPoint(0f, 0.5f), transform.previewToImage(CropPoint(0f, 150f)))
        assertEquals(
            CropPoint(1f, 1f),
            transform.previewToImage(transform.imageToPreview(CropPoint(1f, 1f))),
        )
    }

    @Test
    fun quarterTurnRoundTripsNormalizedPoint() {
        val transform = PreviewTransform.fit(1080, 1920, 1920f, 1080f, quarterTurnsClockwise = 1)
        val point = CropPoint(0.23f, 0.77f)
        val result = transform.previewToImage(transform.imageToPreview(point))

        assertEquals(point.x, result.x, 0.0001f)
        assertEquals(point.y, result.y, 0.0001f)
    }
}
