package com.grimnej.lmcomment.bubble

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowInsets
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import kotlin.math.abs
import kotlin.math.roundToInt

class BubbleWindow(
    private val context: Context,
    private val onTap: () -> Unit,
) {
    private val windowManager = context.getSystemService(WindowManager::class.java)
    private val anchorStore = BubbleAnchorStore(context)
    private val bubbleSize = context.dp(60)
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val bubbleView = ContextLensBubbleView(context)
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

    init {
        bubbleView.contentDescription = "Open LM-Comment capture workflow"
        bubbleView.elevation = context.dp(10).toFloat()
        bubbleView.setOnClickListener { onTap() }
        bubbleView.setOnTouchListener(DragTouchListener())
    }

    fun show(): Boolean {
        if (attached) return true
        if (!Settings.canDrawOverlays(context)) return false
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
        return hidden
    }

    fun onConfigurationChanged(newConfig: Configuration) {
        if (!attached) return
        val anchor = BubbleAnchor.fromPosition(
            layoutParams.x,
            layoutParams.y,
            bounds,
            bubbleSize,
        )
        bounds = safeBounds()
        val position = anchor.position(bounds, bubbleSize)
        layoutParams.x = position.x
        layoutParams.y = position.y
        update()
    }

    fun resetPosition() {
        snapAnimator?.cancel()
        snapAnimator = null
        anchorStore.reset()
        if (!attached) return
        val position = BubbleAnchor.DEFAULT.position(safeBounds(), bubbleSize)
        layoutParams.x = position.x
        layoutParams.y = position.y
        update()
    }

    private fun moveTo(x: Int, y: Int) {
        layoutParams.x = x.coerceIn(bounds.left, (bounds.right - bubbleSize).coerceAtLeast(bounds.left))
        layoutParams.y = y.coerceIn(bounds.top, (bounds.bottom - bubbleSize).coerceAtLeast(bounds.top))
        update()
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
                    WindowInsets.Type.displayCutout() or
                    WindowInsets.Type.systemGestures(),
            )
            val margin = context.dp(8)
            Rect(
                metrics.bounds.left + insets.left + margin,
                metrics.bounds.top + insets.top + margin,
                metrics.bounds.right - insets.right - margin,
                metrics.bounds.bottom - insets.bottom - margin,
            )
        } else {
            val visibleDisplay = Rect()
            windowManager.defaultDisplay.getRectSize(visibleDisplay)
            val margin = context.dp(8)
            Rect(
                visibleDisplay.left + margin,
                visibleDisplay.top + margin,
                visibleDisplay.right - margin,
                visibleDisplay.bottom - margin,
            )
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
                    view.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100L).start()
                    return true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downRawX
                    val dy = event.rawY - downRawY
                    if (!dragging && (abs(dx) > touchSlop || abs(dy) > touchSlop)) dragging = true
                    if (dragging) moveTo(startX + dx.roundToInt(), startY + dy.roundToInt())
                    return true
                }

                MotionEvent.ACTION_UP -> {
                    view.animate().scaleX(1f).scaleY(1f).setDuration(120L).start()
                    if (dragging) {
                        snapToEdge()
                    } else {
                        view.performClick()
                    }
                    return true
                }

                MotionEvent.ACTION_CANCEL -> {
                    view.animate().scaleX(1f).scaleY(1f).setDuration(120L).start()
                    if (dragging) snapToEdge()
                    return true
                }
            }
            return false
        }
    }
}

private class ContextLensBubbleView(context: Context) : View(context) {
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

    init {
        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.TRANSPARENT)
        }
        isClickable = true
        isFocusable = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val center = width / 2f
        canvas.drawCircle(center, height / 2f, width * 0.46f, fill)
        canvas.drawCircle(center, height / 2f, width * 0.46f, border)
        BubbleGlyphGeometry.cornerMarks.forEach { mark ->
            canvas.drawSegment(mark.horizontal)
            canvas.drawSegment(mark.vertical)
        }
        canvas.drawCircle(center, height * 0.49f, width * 0.075f, signalPaint)
    }

    private fun Canvas.drawSegment(segment: NormalizedLineSegment) {
        drawLine(
            width * segment.startX,
            height * segment.startY,
            width * segment.endX,
            height * segment.endY,
            cornerPaint,
        )
    }
}

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
