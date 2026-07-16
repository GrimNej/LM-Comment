package com.grimnej.lmcomment.bubble

internal enum class BubbleGlyphCorner {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
}

internal data class NormalizedLineSegment(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
)

internal data class BubbleCornerMark(
    val corner: BubbleGlyphCorner,
    val horizontal: NormalizedLineSegment,
    val vertical: NormalizedLineSegment,
)

/**
 * Normalized artwork geometry for the four inward-facing Context Lens marks.
 * Keeping the geometry independent from Android Canvas makes its orientation
 * deterministic and directly unit-testable.
 */
internal object BubbleGlyphGeometry {
    private const val LEFT = 0.29f
    private const val TOP = 0.29f
    private const val RIGHT = 0.71f
    private const val BOTTOM = 0.69f
    private const val ARM = 0.12f

    val cornerMarks = listOf(
        BubbleCornerMark(
            corner = BubbleGlyphCorner.TOP_LEFT,
            horizontal = NormalizedLineSegment(LEFT, TOP, LEFT + ARM, TOP),
            vertical = NormalizedLineSegment(LEFT, TOP, LEFT, TOP + ARM),
        ),
        BubbleCornerMark(
            corner = BubbleGlyphCorner.TOP_RIGHT,
            horizontal = NormalizedLineSegment(RIGHT - ARM, TOP, RIGHT, TOP),
            vertical = NormalizedLineSegment(RIGHT, TOP, RIGHT, TOP + ARM),
        ),
        BubbleCornerMark(
            corner = BubbleGlyphCorner.BOTTOM_LEFT,
            horizontal = NormalizedLineSegment(LEFT, BOTTOM, LEFT + ARM, BOTTOM),
            vertical = NormalizedLineSegment(LEFT, BOTTOM, LEFT, BOTTOM - ARM),
        ),
        BubbleCornerMark(
            corner = BubbleGlyphCorner.BOTTOM_RIGHT,
            horizontal = NormalizedLineSegment(RIGHT - ARM, BOTTOM, RIGHT, BOTTOM),
            vertical = NormalizedLineSegment(RIGHT, BOTTOM, RIGHT, BOTTOM - ARM),
        ),
    )
}
