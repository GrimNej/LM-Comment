package com.grimnej.lmcomment.capture

import java.util.concurrent.atomic.AtomicInteger

object CaptureResourceCounters {
    val activeCaptureService = AtomicInteger(0)
    val activeProjection = AtomicInteger(0)
    val activeVirtualDisplay = AtomicInteger(0)
    val activeImageReader = AtomicInteger(0)
    val activeImage = AtomicInteger(0)
    val activeWorkflowBitmap = AtomicInteger(0)

    fun snapshot(): Map<String, Int> = mapOf(
        "activeCaptureService" to activeCaptureService.get(),
        "activeProjection" to activeProjection.get(),
        "activeVirtualDisplay" to activeVirtualDisplay.get(),
        "activeImageReader" to activeImageReader.get(),
        "activeImage" to activeImage.get(),
        "activeWorkflowBitmap" to activeWorkflowBitmap.get(),
    )
}
