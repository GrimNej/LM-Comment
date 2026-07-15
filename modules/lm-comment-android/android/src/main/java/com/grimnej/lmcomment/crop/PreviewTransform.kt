package com.grimnej.lmcomment.crop

import kotlin.math.min

data class PreviewRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

/**
 * Maps the captured bitmap into a ContentScale.Fit-style preview. Quarter-turn
 * support keeps normalized selections stable if a host chooses to rotate the
 * preview rather than recreating crop state.
 */
class PreviewTransform private constructor(
    val frameWidth: Int,
    val frameHeight: Int,
    val previewWidth: Float,
    val previewHeight: Float,
    val quarterTurnsClockwise: Int,
    val contentBounds: PreviewRect,
) {
    fun imageToPreview(point: CropPoint): CropPoint {
        val oriented = rotateClockwise(point, quarterTurnsClockwise)
        return CropPoint(
            x = contentBounds.left + oriented.x * contentBounds.width,
            y = contentBounds.top + oriented.y * contentBounds.height,
        )
    }

    /** Points in the letterbox are clamped to the nearest image edge. */
    fun previewToImage(point: CropPoint): CropPoint {
        val oriented = CropPoint(
            x = ((point.x - contentBounds.left) / contentBounds.width).coerceIn(0f, 1f),
            y = ((point.y - contentBounds.top) / contentBounds.height).coerceIn(0f, 1f),
        )
        return rotateCounterClockwise(oriented, quarterTurnsClockwise)
    }

    fun selectionBounds(selection: NormalizedCropRect): PreviewRect {
        val points = CropHandle.entries.map { imageToPreview(selection.corner(it)) }
        return PreviewRect(
            left = points.minOf(CropPoint::x),
            top = points.minOf(CropPoint::y),
            right = points.maxOf(CropPoint::x),
            bottom = points.maxOf(CropPoint::y),
        )
    }

    companion object {
        fun fit(
            frameWidth: Int,
            frameHeight: Int,
            previewWidth: Float,
            previewHeight: Float,
            quarterTurnsClockwise: Int = 0,
        ): PreviewTransform {
            require(frameWidth > 0 && frameHeight > 0) { "Frame dimensions must be positive." }
            require(previewWidth > 0f && previewHeight > 0f) { "Preview dimensions must be positive." }
            require(previewWidth.isFinite() && previewHeight.isFinite()) {
                "Preview dimensions must be finite."
            }

            val turns = normalizeQuarterTurns(quarterTurnsClockwise)
            val orientedWidth = if (turns % 2 == 0) frameWidth else frameHeight
            val orientedHeight = if (turns % 2 == 0) frameHeight else frameWidth
            val scale = min(previewWidth / orientedWidth, previewHeight / orientedHeight)
            val contentWidth = orientedWidth * scale
            val contentHeight = orientedHeight * scale
            val left = (previewWidth - contentWidth) / 2f
            val top = (previewHeight - contentHeight) / 2f

            return PreviewTransform(
                frameWidth = frameWidth,
                frameHeight = frameHeight,
                previewWidth = previewWidth,
                previewHeight = previewHeight,
                quarterTurnsClockwise = turns,
                contentBounds = PreviewRect(left, top, left + contentWidth, top + contentHeight),
            )
        }
    }
}

private fun normalizeQuarterTurns(turns: Int): Int = ((turns % 4) + 4) % 4

private fun rotateClockwise(point: CropPoint, turns: Int): CropPoint = when (turns) {
    0 -> point
    1 -> CropPoint(1f - point.y, point.x)
    2 -> CropPoint(1f - point.x, 1f - point.y)
    3 -> CropPoint(point.y, 1f - point.x)
    else -> error("Quarter turns must be normalized.")
}

private fun rotateCounterClockwise(point: CropPoint, turns: Int): CropPoint = when (turns) {
    0 -> point
    1 -> CropPoint(point.y, 1f - point.x)
    2 -> CropPoint(1f - point.x, 1f - point.y)
    3 -> CropPoint(1f - point.y, point.x)
    else -> error("Quarter turns must be normalized.")
}
