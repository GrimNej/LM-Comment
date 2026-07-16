package com.grimnej.lmcomment.bubble

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowInsets
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import com.grimnej.lmcomment.config.AppearancePreferenceStore
import kotlin.math.abs
import kotlin.math.roundToInt

class BubbleWindow(
    private val context: Context,
    private val onTap: () -> Unit,
    private val onDismiss: () -> Unit,
) {
    private val windowManager = context.getSystemService(WindowManager::class.java)
    private val anchorStore = BubbleAnchorStore(context)
    private val appearanceStore = AppearancePreferenceStore(context)
    private val bubbleSize = context.dp(60)
    private val dismissTargetRadius = context.dp(34)
    private val dismissCaptureRadius = context.dp(52)
    private val dismissBottomGap = context.dp(20)
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val bubbleView = ContextLensBubbleView(context, bubbleSize, dismissTargetRadius)
    private val layoutParams = WindowManager.LayoutParams(
        bubbleSize,
        bubbleSize,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        },
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        android.graphics.PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        windowAnimations = 0
    }

    private var attached = false
    private var bounds = safeBounds()
    private var snapAnimator: ValueAnimator? = null
    private var dragPosition: BubblePosition? = null
    private var dismissTarget: BubblePoint? = null
    private var dismissArmed = false

    init {
        bubbleView.contentDescription = "Open LM-Comment capture workflow"
        bubbleView.setOnClickListener { onTap() }
        bubbleView.setOnTouchListener(DragTouchListener())
    }

    fun show(): Boolean {
        if (attached) return true
        if (!Settings.canDrawOverlays(context)) return false
        refreshAppearance()
        resetCompactLayout(updateWindow = false)
        bounds = safeBounds()
        val position = anchorStore.read().position(bounds, bubbleSize)
        layoutParams.x = position.x
        layoutParams.y = position.y
        attached = runCatching {
            windowManager.addView(bubbleView, layoutParams)
        }.isSuccess
        return attached
    }

    fun hide(): Boolean {
        snapAnimator?.cancel()
        if (!attached && !bubbleView.isAttachedToWindow) return true
        runCatching { windowManager.removeViewImmediate(bubbleView) }
        val hidden = !bubbleView.isAttachedToWindow
        attached = !hidden
        if (hidden) resetCompactLayout(updateWindow = false)
        return hidden
    }

    fun onConfigurationChanged(newConfig: Configuration) {
        if (!attached) return
        val currentPosition = dragPosition ?: BubblePosition(layoutParams.x, layoutParams.y)
        val anchor = BubbleAnchor.fromPosition(
            currentPosition.x,
            currentPosition.y,
            bounds,
            bubbleSize,
        )
        resetCompactLayout(updateWindow = false)
        bounds = safeBounds()
        val position = anchor.position(bounds, bubbleSize)
        layoutParams.x = position.x
        layoutParams.y = position.y
        refreshAppearance()
        update()
    }

    fun resetPosition() {
        snapAnimator?.cancel()
        snapAnimator = null
        anchorStore.reset()
        if (!attached) return
        resetCompactLayout(updateWindow = false)
        bounds = safeBounds()
        val position = BubbleAnchor.DEFAULT.position(safeBounds(), bubbleSize)
        layoutParams.x = position.x
        layoutParams.y = position.y
        update()
    }

    fun refreshAppearance() {
        val darkMode = appearanceStore.read().resolvesToDark(context.resources.configuration.uiMode)
        bubbleView.setDarkMode(darkMode)
    }

    private fun moveTo(x: Int, y: Int) {
        val position = BubbleDragMath.clampedPosition(x, y, bounds.toBubbleBounds(), bubbleSize)
        layoutParams.x = position.x
        layoutParams.y = position.y
        update()
    }

    private fun beginDragMode() {
        val position = BubblePosition(layoutParams.x, layoutParams.y)
        val target = BubbleDragMath.dismissTarget(
            bounds = bounds.toBubbleBounds(),
            targetRadius = dismissTargetRadius,
            bottomGap = dismissBottomGap,
        )
        dragPosition = position
        dismissTarget = target
        dismissArmed = false
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
        layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT
        layoutParams.x = 0
        layoutParams.y = 0
        bubbleView.enterDragMode(position, target)
        update()
    }

    private fun updateDragMode(x: Int, y: Int) {
        val target = dismissTarget ?: return
        val position = BubbleDragMath.clampedPosition(x, y, bounds.toBubbleBounds(), bubbleSize)
        val armed = BubbleDragMath.isInsideDismissTarget(
            position = position,
            bubbleSize = bubbleSize,
            target = target,
            captureRadius = dismissCaptureRadius,
        )
        if (armed && !dismissArmed) {
            bubbleView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
        dragPosition = position
        dismissArmed = armed
        bubbleView.updateDrag(position, armed)
    }

    private fun resetCompactLayout(updateWindow: Boolean) {
        val position = dragPosition
        layoutParams.width = bubbleSize
        layoutParams.height = bubbleSize
        if (position != null) {
            layoutParams.x = position.x
            layoutParams.y = position.y
        }
        dragPosition = null
        dismissTarget = null
        dismissArmed = false
        bubbleView.exitDragMode()
        if (updateWindow) update()
    }

    private fun snapToEdge() {
        val anchor = BubbleAnchor.fromPosition(layoutParams.x, layoutParams.y, bounds, bubbleSize)
        val target = anchor.position(bounds, bubbleSize)
        val startX = layoutParams.x
        val startY = layoutParams.y
        snapAnimator?.cancel()
        snapAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 180L
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                val fraction = animator.animatedFraction
                moveTo(
                    (startX + (target.x - startX) * fraction).roundToInt(),
                    (startY + (target.y - startY) * fraction).roundToInt(),
                )
            }
            doOnCompleted { anchorStore.write(anchor) }
            start()
        }
    }

    private fun update() {
        if (attached) runCatching { windowManager.updateViewLayout(bubbleView, layoutParams) }
    }

    @Suppress("DEPRECATION")
    private fun safeBounds(): Rect {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = windowManager.currentWindowMetrics
            val insets = metrics.windowInsets.getInsetsIgnoringVisibility(
                WindowInsets.Type.systemBars() or
                    WindowInsets.Type.displayCutout(),
            )
            Rect(
                metrics.bounds.left + insets.left,
                metrics.bounds.top + insets.top,
                metrics.bounds.right - insets.right,
                metrics.bounds.bottom - insets.bottom,
            )
        } else {
            val visibleDisplay = Rect()
            windowManager.defaultDisplay.getRectSize(visibleDisplay)
            visibleDisplay
        }
    }

    private inner class DragTouchListener : View.OnTouchListener {
        private var downRawX = 0f
        private var downRawY = 0f
        private var startX = 0
        private var startY = 0
        private var dragging = false

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    snapAnimator?.cancel()
                    downRawX = event.rawX
                    downRawY = event.rawY
                    startX = layoutParams.x
                    startY = layoutParams.y
                    dragging = false
                    bubbleView.setBubblePressed(true)
                    beginDragMode()
                    return true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downRawX
                    val dy = event.rawY - downRawY
                    if (!dragging && (abs(dx) > touchSlop || abs(dy) > touchSlop)) dragging = true
                    if (dragging) {
                        updateDragMode(startX + dx.roundToInt(), startY + dy.roundToInt())
                    }
                    return true
                }

                MotionEvent.ACTION_UP -> {
                    bubbleView.setBubblePressed(false)
                    if (dragging) {
                        if (dismissArmed) {
                            onDismiss()
                        } else {
                            resetCompactLayout(updateWindow = true)
                            snapToEdge()
                        }
                    } else {
                        resetCompactLayout(updateWindow = true)
                        view.performClick()
                    }
                    return true
                }

                MotionEvent.ACTION_CANCEL -> {
                    bubbleView.setBubblePressed(false)
                    resetCompactLayout(updateWindow = true)
                    if (dragging) snapToEdge()
                    return true
                }
            }
            return false
        }
    }
}

private class ContextLensBubbleView(
    context: Context,
    private val bubbleSize: Int,
    private val dismissTargetRadius: Int,
) : View(context) {
    private val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(76, 0, 0, 0)
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(16, 20, 17) }
    private val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(112, 244, 240, 230)
        style = Paint.Style.STROKE
        strokeWidth = context.dp(1).toFloat()
    }
    private val cornerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(244, 240, 230)
        style = Paint.Style.STROKE
        strokeWidth = context.dp(2).toFloat()
        strokeCap = Paint.Cap.ROUND
    }
    private val signalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(185, 232, 74)
    }
    private val dismissFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(16, 20, 17)
    }
    private val dismissBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(180, 244, 240, 230)
        style = Paint.Style.STROKE
        strokeWidth = context.dp(1).toFloat()
    }
    private val dismissHalo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(104, 233, 109, 76)
        style = Paint.Style.STROKE
        strokeWidth = context.dp(3).toFloat()
    }
    private val dismissCross = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(244, 240, 230)
        style = Paint.Style.STROKE
        strokeWidth = context.dp(3).toFloat()
        strokeCap = Paint.Cap.ROUND
    }
    private var dragMode = false
    private var dragPosition = BubblePosition(0, 0)
    private var dismissTarget = BubblePoint(0f, 0f)
    private var dismissArmed = false
    private var pressed = false
    private var darkMode = false

    init {
        setBackgroundColor(Color.TRANSPARENT)
        isClickable = true
        isFocusable = true
    }

    fun enterDragMode(position: BubblePosition, target: BubblePoint) {
        dragMode = true
        dragPosition = position
        dismissTarget = target
        dismissArmed = false
        invalidate()
    }

    fun updateDrag(position: BubblePosition, armed: Boolean) {
        dragPosition = position
        dismissArmed = armed
        invalidate()
    }

    fun exitDragMode() {
        dragMode = false
        dragPosition = BubblePosition(0, 0)
        dismissTarget = BubblePoint(0f, 0f)
        dismissArmed = false
        invalidate()
    }

    fun setBubblePressed(value: Boolean) {
        pressed = value
        invalidate()
    }

    fun setDarkMode(value: Boolean) {
        darkMode = value
        if (value) {
            fill.color = Color.rgb(9, 11, 16)
            border.color = Color.argb(152, 155, 140, 255)
            cornerPaint.color = Color.rgb(155, 140, 255)
            signalPaint.color = Color.rgb(85, 225, 208)
            dismissBorder.color = Color.argb(190, 155, 140, 255)
            dismissCross.color = Color.rgb(247, 248, 252)
        } else {
            fill.color = Color.rgb(16, 20, 17)
            border.color = Color.argb(112, 244, 240, 230)
            cornerPaint.color = Color.rgb(244, 240, 230)
            signalPaint.color = Color.rgb(185, 232, 74)
            dismissBorder.color = Color.argb(180, 244, 240, 230)
            dismissCross.color = Color.rgb(244, 240, 230)
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (dragMode) drawDismissTarget(canvas)
        val left = if (dragMode) dragPosition.x.toFloat() else 0f
        val top = if (dragMode) dragPosition.y.toFloat() else 0f
        val scale = when {
            dismissArmed -> 0.82f
            pressed -> 0.94f
            else -> 1f
        }
        drawBubble(canvas, left, top, scale)
    }

    private fun drawDismissTarget(canvas: Canvas) {
        val scale = if (dismissArmed) 1.12f else 1f
        val radius = dismissTargetRadius * scale
        canvas.drawCircle(
            dismissTarget.x,
            dismissTarget.y + context.dp(3),
            radius,
            shadow,
        )
        dismissFill.color = if (dismissArmed) {
            if (darkMode) Color.rgb(255, 113, 130) else Color.rgb(233, 109, 76)
        } else {
            if (darkMode) Color.rgb(9, 11, 16) else Color.rgb(16, 20, 17)
        }
        if (dismissArmed) {
            canvas.drawCircle(dismissTarget.x, dismissTarget.y, radius + context.dp(7), dismissHalo)
        }
        canvas.drawCircle(dismissTarget.x, dismissTarget.y, radius, dismissFill)
        canvas.drawCircle(dismissTarget.x, dismissTarget.y, radius, dismissBorder)
        val arm = radius * 0.30f
        canvas.drawLine(
            dismissTarget.x - arm,
            dismissTarget.y - arm,
            dismissTarget.x + arm,
            dismissTarget.y + arm,
            dismissCross,
        )
        canvas.drawLine(
            dismissTarget.x + arm,
            dismissTarget.y - arm,
            dismissTarget.x - arm,
            dismissTarget.y + arm,
            dismissCross,
        )
    }

    private fun drawBubble(canvas: Canvas, left: Float, top: Float, scale: Float) {
        val drawSize = bubbleSize * scale
        val centerX = left + bubbleSize / 2f
        val centerY = top + bubbleSize / 2f
        val drawLeft = centerX - drawSize / 2f
        val drawTop = centerY - drawSize / 2f
        canvas.drawCircle(centerX, centerY + context.dp(2), drawSize * 0.46f, shadow)
        canvas.drawCircle(centerX, centerY, drawSize * 0.46f, fill)
        canvas.drawCircle(centerX, centerY, drawSize * 0.46f, border)
        BubbleGlyphGeometry.cornerMarks.forEach { mark ->
            canvas.drawSegment(mark.horizontal, drawLeft, drawTop, drawSize)
            canvas.drawSegment(mark.vertical, drawLeft, drawTop, drawSize)
        }
        canvas.drawCircle(
            centerX,
            drawTop + drawSize * 0.49f,
            drawSize * 0.075f,
            signalPaint,
        )
    }

    private fun Canvas.drawSegment(
        segment: NormalizedLineSegment,
        left: Float,
        top: Float,
        size: Float,
    ) {
        drawLine(
            left + size * segment.startX,
            top + size * segment.startY,
            left + size * segment.endX,
            top + size * segment.endY,
            cornerPaint,
        )
    }
}

private fun Rect.toBubbleBounds(): BubbleBounds = BubbleBounds(left, top, right, bottom)

private fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

private inline fun ValueAnimator.doOnCompleted(crossinline block: () -> Unit) {
    addListener(object : android.animation.AnimatorListenerAdapter() {
        private var cancelled = false

        override fun onAnimationStart(animation: android.animation.Animator) {
            cancelled = false
        }

        override fun onAnimationCancel(animation: android.animation.Animator) {
            cancelled = true
        }

        override fun onAnimationEnd(animation: android.animation.Animator) {
            if (!cancelled) block()
        }
    })
}
