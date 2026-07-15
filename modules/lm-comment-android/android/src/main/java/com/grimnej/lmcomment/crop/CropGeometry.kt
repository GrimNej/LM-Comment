package com.grimnej.lmcomment.crop

import kotlin.math.ceil
import kotlin.math.floor

/**
 * A crop rectangle expressed against the captured bitmap, independent of the
 * current preview size and device orientation.
 */
data class NormalizedCropRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    init {
        require(listOf(left, top, right, bottom).all { it.isFinite() }) {
            "Crop edges must be finite."
        }
        require(left in 0f..1f && top in 0f..1f && right in 0f..1f && bottom in 0f..1f) {
            "Crop edges must be normalized."
        }
        require(left < right && top < bottom) { "Crop edges must not cross." }
    }

    val width: Float get() = right - left
    val height: Float get() = bottom - top

    companion object {
        val FullFrame = NormalizedCropRect(0f, 0f, 1f, 1f)
        val Suggested = NormalizedCropRect(0.06f, 0.10f, 0.94f, 0.90f)
    }
}

/** Right and bottom are exclusive, matching Android bitmap crop APIs. */
data class PixelCropRect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    init {
        require(left >= 0 && top >= 0) { "Crop origin must be in bounds." }
        require(left < right && top < bottom) { "Crop must contain at least one pixel." }
    }

    val width: Int get() = right - left
    val height: Int get() = bottom - top
}

enum class CropHandle {
    TopLeft,
    TopRight,
    BottomLeft,
    BottomRight,
}

data class CropPoint(val x: Float, val y: Float)

object CropGeometry {
    fun toPixels(
        selection: NormalizedCropRect,
        frameWidth: Int,
        frameHeight: Int,
    ): PixelCropRect {
        require(frameWidth > 0 && frameHeight > 0) { "Frame dimensions must be positive." }

        val left = floor(selection.left * frameWidth).toInt().coerceIn(0, frameWidth - 1)
        val top = floor(selection.top * frameHeight).toInt().coerceIn(0, frameHeight - 1)
        val right = ceil(selection.right * frameWidth).toInt().coerceIn(left + 1, frameWidth)
        val bottom = ceil(selection.bottom * frameHeight).toInt().coerceIn(top + 1, frameHeight)
        return PixelCropRect(left, top, right, bottom)
    }

    fun dragHandle(
        selection: NormalizedCropRect,
        handle: CropHandle,
        imagePoint: CropPoint,
        frameWidth: Int,
        frameHeight: Int,
        minimumPixels: Int = 1,
    ): NormalizedCropRect {
        require(frameWidth > 0 && frameHeight > 0) { "Frame dimensions must be positive." }
        require(minimumPixels >= 1) { "Minimum crop size must be at least one pixel." }

        val minimumWidth = (minimumPixels.toFloat() / frameWidth).coerceAtMost(1f)
        val minimumHeight = (minimumPixels.toFloat() / frameHeight).coerceAtMost(1f)
        val x = imagePoint.x.coerceIn(0f, 1f)
        val y = imagePoint.y.coerceIn(0f, 1f)

        return when (handle) {
            CropHandle.TopLeft -> selection.copy(
                left = x.coerceAtMost((selection.right - minimumWidth).coerceAtLeast(0f)),
                top = y.coerceAtMost((selection.bottom - minimumHeight).coerceAtLeast(0f)),
            )
            CropHandle.TopRight -> selection.copy(
                right = x.coerceAtLeast((selection.left + minimumWidth).coerceAtMost(1f)),
                top = y.coerceAtMost((selection.bottom - minimumHeight).coerceAtLeast(0f)),
            )
            CropHandle.BottomLeft -> selection.copy(
                left = x.coerceAtMost((selection.right - minimumWidth).coerceAtLeast(0f)),
                bottom = y.coerceAtLeast((selection.top + minimumHeight).coerceAtMost(1f)),
            )
            CropHandle.BottomRight -> selection.copy(
                right = x.coerceAtLeast((selection.left + minimumWidth).coerceAtMost(1f)),
                bottom = y.coerceAtLeast((selection.top + minimumHeight).coerceAtMost(1f)),
            )
        }
    }

    fun dragHandleInPreview(
        selection: NormalizedCropRect,
        handle: CropHandle,
        previewPoint: CropPoint,
        transform: PreviewTransform,
        minimumPixels: Int = 1,
    ): NormalizedCropRect = dragHandle(
        selection = selection,
        handle = handle,
        imagePoint = transform.previewToImage(previewPoint),
        frameWidth = transform.frameWidth,
        frameHeight = transform.frameHeight,
        minimumPixels = minimumPixels,
    )

    fun closestHandle(
        selection: NormalizedCropRect,
        previewPoint: CropPoint,
        transform: PreviewTransform,
        hitRadius: Float,
    ): CropHandle? {
        require(hitRadius >= 0f) { "Hit radius must not be negative." }
        val hitRadiusSquared = hitRadius * hitRadius
        return CropHandle.entries
            .map { handle ->
                val point = transform.imageToPreview(selection.corner(handle))
                val dx = point.x - previewPoint.x
                val dy = point.y - previewPoint.y
                handle to (dx * dx + dy * dy)
            }
            .filter { (_, distanceSquared) -> distanceSquared <= hitRadiusSquared }
            .minByOrNull { (_, distanceSquared) -> distanceSquared }
            ?.first
    }
}

fun NormalizedCropRect.corner(handle: CropHandle): CropPoint = when (handle) {
    CropHandle.TopLeft -> CropPoint(left, top)
    CropHandle.TopRight -> CropPoint(right, top)
    CropHandle.BottomLeft -> CropPoint(left, bottom)
    CropHandle.BottomRight -> CropPoint(right, bottom)
}
