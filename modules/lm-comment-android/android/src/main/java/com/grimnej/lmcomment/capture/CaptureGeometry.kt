package com.grimnej.lmcomment.capture

import kotlin.math.floor
import kotlin.math.sqrt

data class CaptureDimensions(
    val width: Int,
    val height: Int,
    val densityDpi: Int,
) {
    val pixels: Long get() = width.toLong() * height.toLong()
}

object CaptureGeometry {
    const val MAX_PIXELS = 2_000_000L

    fun bounded(width: Int, height: Int, densityDpi: Int): CaptureDimensions {
        require(width > 0 && height > 0) { "Capture dimensions must be positive." }
        val pixels = width.toLong() * height.toLong()
        if (pixels <= MAX_PIXELS) return CaptureDimensions(width, height, densityDpi)
        val scale = sqrt(MAX_PIXELS.toDouble() / pixels.toDouble())
        var boundedWidth = floor(width * scale).toInt().coerceAtLeast(1)
        var boundedHeight = floor(height * scale).toInt().coerceAtLeast(1)

        // Floating-point rounding must never push the backing ImageReader over
        // the hard allocation budget. Keep the aspect ratio as close as
        // possible while correcting the larger edge when necessary.
        while (boundedWidth.toLong() * boundedHeight.toLong() > MAX_PIXELS) {
            if (boundedWidth >= boundedHeight) boundedWidth-- else boundedHeight--
        }
        return CaptureDimensions(boundedWidth, boundedHeight, densityDpi)
    }
}
