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
        bubbleView.setOnTouchListener(DragTouchListener())
    }

    fun show() {
        if (attached) return
        bounds = safeBounds()
        val position = anchorStore.read().position(bounds, bubbleSize)
        layoutParams.x = position.x
        layoutParams.y = position.y
        windowManager.addView(bubbleView, layoutParams)
        attached = true
    }

    fun hide() {
        snapAnimator?.cancel()
        if (!attached) return
        runCatching { windowManager.removeViewImmediate(bubbleView) }
        attached = false
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
            doOnEnd { anchorStore.write(anchor) }
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
                WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout(),
            )
            Rect(
                insets.left + context.dp(8),
                insets.top + context.dp(8),
                metrics.bounds.width() - insets.right - context.dp(8),
                metrics.bounds.height() - insets.bottom - context.dp(8),
            )
        } else {
            val metrics = context.resources.displayMetrics
            Rect(context.dp(8), context.dp(32), metrics.widthPixels - context.dp(8), metrics.heightPixels - context.dp(32))
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
                    if (dragging) snapToEdge() else onTap()
                    view.performClick()
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
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(20, 24, 34) }
    private val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(93, 107, 134)
        style = Paint.Style.STROKE
        strokeWidth = context.dp(1).toFloat()
    }
    private val violet = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(143, 131, 255)
        style = Paint.Style.STROKE
        strokeWidth = context.dp(2).toFloat()
        strokeCap = Paint.Cap.ROUND
    }
    private val cyan = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(80, 215, 197) }

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
        val left = width * 0.29f
        val top = height * 0.29f
        val right = width * 0.71f
        val bottom = height * 0.69f
        val arm = width * 0.12f
        canvas.drawLine(left, top + arm, left, top, violet)
        canvas.drawLine(left, top, left + arm, top, violet)
        canvas.drawLine(right - arm, top, right, top, violet)
        canvas.drawLine(right, top, right, top + arm, violet)
        canvas.drawLine(left, bottom - arm, left, bottom, violet)
        canvas.drawLine(left, bottom, left + arm, bottom, violet)
        canvas.drawLine(right - arm, bottom, right, bottom, violet)
        canvas.drawLine(right, bottom, right, bottom + arm * 1.45f, violet)
        canvas.drawCircle(center, height * 0.49f, width * 0.075f, cyan)
    }
}

private fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

private inline fun ValueAnimator.doOnEnd(crossinline block: () -> Unit) {
    addListener(object : android.animation.AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: android.animation.Animator) = block()
    })
}
