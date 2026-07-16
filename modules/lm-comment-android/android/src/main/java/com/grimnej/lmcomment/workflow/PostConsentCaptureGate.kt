package com.grimnej.lmcomment.workflow

internal enum class CaptureGateAction {
    WAIT,
    REQUEST_COMMITTED_FRAME,
    WAIT_FOR_SYSTEM_UI_QUIESCENCE,
    START_CAPTURE,
}

/**
 * Event-driven gate between Android's MediaProjection consent result and the
 * one-shot capture service. A result alone is not enough: SystemUI may still
 * own focus or have an exit surface in the compositor.
 */
internal class PostConsentCaptureGate(
    private val requiredCommittedFrames: Int = 2,
) {
    private var epochActive = false
    private var focusLossObserved = false
    private var grantAccepted = false
    private var resumed = false
    private var focused = false
    private var frameRequestInFlight = false
    private var committedFrames = 0
    private var quiescenceInFlight = false
    private var quiescenceSatisfied = false
    private var started = false

    init {
        require(requiredCommittedFrames > 0)
    }

    fun onConsentLaunched() {
        epochActive = true
        focusLossObserved = false
        grantAccepted = false
        frameRequestInFlight = false
        committedFrames = 0
        quiescenceInFlight = false
        quiescenceSatisfied = false
        started = false
    }

    fun onConsentAccepted(): CaptureGateAction {
        if (!epochActive || started) return CaptureGateAction.WAIT
        grantAccepted = true
        return evaluate()
    }

    fun onResumed(): CaptureGateAction {
        resumed = true
        return evaluate()
    }

    fun onPaused(): CaptureGateAction {
        resumed = false
        focused = false
        if (epochActive) focusLossObserved = true
        invalidateFrameSequence()
        return CaptureGateAction.WAIT
    }

    fun onWindowFocusChanged(hasFocus: Boolean): CaptureGateAction {
        focused = hasFocus
        if (epochActive && !hasFocus) {
            focusLossObserved = true
            invalidateFrameSequence()
        }
        return evaluate()
    }

    fun onFrameCommitted(): CaptureGateAction {
        if (!frameRequestInFlight) return CaptureGateAction.WAIT
        frameRequestInFlight = false
        if (!isReadyForFrames()) {
            committedFrames = 0
            return CaptureGateAction.WAIT
        }
        if (quiescenceSatisfied) {
            started = true
            epochActive = false
            return CaptureGateAction.START_CAPTURE
        }
        committedFrames++
        return evaluate()
    }

    fun onSystemUiQuiescent(): CaptureGateAction {
        if (!quiescenceInFlight || !isReadyForFrames()) {
            invalidateFrameSequence()
            return CaptureGateAction.WAIT
        }
        quiescenceInFlight = false
        quiescenceSatisfied = true
        committedFrames = 0
        return evaluate()
    }

    fun cancel() {
        epochActive = false
        focusLossObserved = false
        grantAccepted = false
        resumed = false
        focused = false
        invalidateFrameSequence()
        started = false
    }

    private fun evaluate(): CaptureGateAction {
        if (!isReadyForFrames() || frameRequestInFlight || quiescenceInFlight) {
            return CaptureGateAction.WAIT
        }
        if (!quiescenceSatisfied && committedFrames >= requiredCommittedFrames) {
            quiescenceInFlight = true
            return CaptureGateAction.WAIT_FOR_SYSTEM_UI_QUIESCENCE
        }
        frameRequestInFlight = true
        return CaptureGateAction.REQUEST_COMMITTED_FRAME
    }

    private fun isReadyForFrames(): Boolean =
        epochActive && focusLossObserved && grantAccepted && resumed && focused && !started

    private fun invalidateFrameSequence() {
        frameRequestInFlight = false
        committedFrames = 0
        quiescenceInFlight = false
        quiescenceSatisfied = false
    }
}

internal object PostConsentTiming {
    private const val SYSTEM_ANIMATION_WINDOWS = 3L
    private const val MAX_QUIESCENCE_MILLIS = 4_000L

    fun quiescenceMillis(longAnimationMillis: Int, animatorDurationScale: Float): Long {
        require(longAnimationMillis > 0)
        val safeScale = animatorDurationScale
            .takeIf { it.isFinite() }
            ?.coerceAtLeast(1f)
            ?: 1f
        return (longAnimationMillis * SYSTEM_ANIMATION_WINDOWS * safeScale)
            .toLong()
            .coerceAtMost(MAX_QUIESCENCE_MILLIS)
    }

    fun readinessTimeoutMillis(quiescenceMillis: Long): Long {
        require(quiescenceMillis > 0)
        return quiescenceMillis + 4_000L
    }
}
