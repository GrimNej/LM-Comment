package com.grimnej.lmcomment.bubble

import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import kotlin.math.roundToInt

enum class BubbleEdge {
    LEFT,
    RIGHT,
}

internal data class BubbleBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
)

internal data class BubblePosition(
    val x: Int,
    val y: Int,
)

internal object BubbleAnchorMath {
    fun position(
        edge: BubbleEdge,
        verticalFraction: Float,
        bounds: BubbleBounds,
        bubbleSize: Int,
    ): BubblePosition {
        val minY = bounds.top
        val maxY = (bounds.bottom - bubbleSize).coerceAtLeast(minY)
        val y = (minY + (maxY - minY) * verticalFraction.coerceIn(0f, 1f)).roundToInt()
        val x = if (edge == BubbleEdge.LEFT) {
            bounds.left
        } else {
            (bounds.right - bubbleSize).coerceAtLeast(bounds.left)
        }
        return BubblePosition(x, y)
    }

    fun fromPosition(
        x: Int,
        y: Int,
        bounds: BubbleBounds,
        bubbleSize: Int,
    ): Pair<BubbleEdge, Float> {
        val center = bounds.left + (bounds.right - bounds.left) / 2
        val edge = if (x + bubbleSize / 2 < center) BubbleEdge.LEFT else BubbleEdge.RIGHT
        val available = (bounds.bottom - bounds.top - bubbleSize).coerceAtLeast(1)
        val fraction = ((y - bounds.top).toFloat() / available).coerceIn(0f, 1f)
        return edge to fraction
    }
}

data class BubbleAnchor(
    val edge: BubbleEdge,
    val verticalFraction: Float,
) {
    fun position(bounds: Rect, bubbleSize: Int): Point {
        val position = BubbleAnchorMath.position(
            edge = edge,
            verticalFraction = verticalFraction,
            bounds = BubbleBounds(bounds.left, bounds.top, bounds.right, bounds.bottom),
            bubbleSize = bubbleSize,
        )
        return Point(position.x, position.y)
    }

    companion object {
        val DEFAULT = BubbleAnchor(BubbleEdge.RIGHT, 0.42f)

        fun fromPosition(x: Int, y: Int, bounds: Rect, bubbleSize: Int): BubbleAnchor {
            val (edge, fraction) = BubbleAnchorMath.fromPosition(
                x = x,
                y = y,
                bounds = BubbleBounds(bounds.left, bounds.top, bounds.right, bounds.bottom),
                bubbleSize = bubbleSize,
            )
            return BubbleAnchor(edge, fraction)
        }
    }
}

class BubbleAnchorStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun read(): BubbleAnchor {
        val edge = runCatching {
            BubbleEdge.valueOf(preferences.getString(KEY_EDGE, null) ?: return BubbleAnchor.DEFAULT)
        }.getOrDefault(BubbleAnchor.DEFAULT.edge)
        return BubbleAnchor(edge, preferences.getFloat(KEY_FRACTION, BubbleAnchor.DEFAULT.verticalFraction))
    }

    fun write(anchor: BubbleAnchor) {
        preferences.edit()
            .putString(KEY_EDGE, anchor.edge.name)
            .putFloat(KEY_FRACTION, anchor.verticalFraction)
            .apply()
    }

    fun reset() {
        preferences.edit().remove(KEY_EDGE).remove(KEY_FRACTION).apply()
    }

    companion object {
        private const val PREFERENCES = "lmcomment_bubble_anchor"
        private const val KEY_EDGE = "edge"
        private const val KEY_FRACTION = "vertical_fraction"
    }
}
