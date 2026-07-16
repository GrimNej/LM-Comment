package com.grimnej.lmcomment.capture

/** Skips bounded raw compositor buffers before the single accepted frame. */
internal class CompositorWarmupGate(
    private val framesToSkip: Int = 1,
) {
    private var remaining = framesToSkip

    init {
        require(framesToSkip >= 0)
    }

    fun reset() {
        remaining = framesToSkip
    }

    fun shouldConvertNextFrame(): Boolean {
        if (remaining == 0) return true
        remaining--
        return false
    }
}
