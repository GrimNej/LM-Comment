package com.grimnej.lmcomment.capture

import android.graphics.Bitmap
import android.media.Image

object FrameConverter {
    fun toBitmap(image: Image, expectedWidth: Int, expectedHeight: Int): Bitmap {
        val plane = image.planes.firstOrNull() ?: error("Captured image had no color plane.")
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        require(pixelStride > 0 && rowStride > 0) { "Captured buffer stride was invalid." }
        val rowPadding = rowStride - pixelStride * expectedWidth
        val paddedWidth = expectedWidth + rowPadding.coerceAtLeast(0) / pixelStride
        val padded = Bitmap.createBitmap(paddedWidth, expectedHeight, Bitmap.Config.ARGB_8888)
        plane.buffer.rewind()
        padded.copyPixelsFromBuffer(plane.buffer)
        if (paddedWidth == expectedWidth) return padded
        return Bitmap.createBitmap(padded, 0, 0, expectedWidth, expectedHeight).also {
            if (it !== padded && !padded.isRecycled) padded.recycle()
        }
    }

    fun looksBlank(bitmap: Bitmap): Boolean {
        if (bitmap.width == 0 || bitmap.height == 0) return true
        val columns = 9
        val rows = 13
        var minLuminance = 255
        var maxLuminance = 0
        var visibleSamples = 0
        for (row in 0 until rows) {
            val y = ((bitmap.height - 1) * row / (rows - 1)).coerceIn(0, bitmap.height - 1)
            for (column in 0 until columns) {
                val x = ((bitmap.width - 1) * column / (columns - 1)).coerceIn(0, bitmap.width - 1)
                val pixel = bitmap.getPixel(x, y)
                val alpha = pixel ushr 24 and 0xff
                val red = pixel ushr 16 and 0xff
                val green = pixel ushr 8 and 0xff
                val blue = pixel and 0xff
                val luminance = (red * 299 + green * 587 + blue * 114) / 1000
                minLuminance = minOf(minLuminance, luminance)
                maxLuminance = maxOf(maxLuminance, luminance)
                if (alpha > 8) visibleSamples++
            }
        }
        return visibleSamples == 0 || (maxLuminance <= 3 && minLuminance <= 3)
    }
}
