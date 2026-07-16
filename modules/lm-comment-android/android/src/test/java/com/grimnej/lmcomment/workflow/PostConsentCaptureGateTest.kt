package com.grimnej.lmcomment.workflow

import org.junit.Assert.assertEquals
import org.junit.Test

class PostConsentCaptureGateTest {
    @Test
    fun `result cannot start capture before focus returns and frames commit`() {
        val gate = PostConsentCaptureGate(requiredCommittedFrames = 2)
        gate.onResumed()
        gate.onWindowFocusChanged(true)
        gate.onConsentLaunched()

        assertEquals(CaptureGateAction.WAIT, gate.onWindowFocusChanged(false))
        assertEquals(CaptureGateAction.WAIT, gate.onPaused())
        assertEquals(CaptureGateAction.WAIT, gate.onConsentAccepted())
        assertEquals(CaptureGateAction.WAIT, gate.onResumed())
        assertEquals(CaptureGateAction.REQUEST_COMMITTED_FRAME, gate.onWindowFocusChanged(true))
        assertEquals(CaptureGateAction.REQUEST_COMMITTED_FRAME, gate.onFrameCommitted())
        assertEquals(CaptureGateAction.WAIT_FOR_SYSTEM_UI_QUIESCENCE, gate.onFrameCommitted())
        assertEquals(CaptureGateAction.REQUEST_COMMITTED_FRAME, gate.onSystemUiQuiescent())
        assertEquals(CaptureGateAction.START_CAPTURE, gate.onFrameCommitted())
        assertEquals(CaptureGateAction.WAIT, gate.onFrameCommitted())
    }

    @Test
    fun `stale pre-consent focus is insufficient`() {
        val gate = PostConsentCaptureGate()
        gate.onResumed()
        gate.onWindowFocusChanged(true)
        gate.onConsentLaunched()

        assertEquals(CaptureGateAction.WAIT, gate.onConsentAccepted())
        assertEquals(CaptureGateAction.WAIT, gate.onResumed())
        assertEquals(CaptureGateAction.WAIT, gate.onWindowFocusChanged(true))
    }

    @Test
    fun `focus loss between committed frames restarts the sequence`() {
        val gate = readyGate()
        assertEquals(CaptureGateAction.REQUEST_COMMITTED_FRAME, gate.onFrameCommitted())
        assertEquals(CaptureGateAction.WAIT, gate.onWindowFocusChanged(false))
        assertEquals(CaptureGateAction.WAIT, gate.onFrameCommitted())
        assertEquals(CaptureGateAction.REQUEST_COMMITTED_FRAME, gate.onWindowFocusChanged(true))
        assertEquals(CaptureGateAction.REQUEST_COMMITTED_FRAME, gate.onFrameCommitted())
        assertEquals(CaptureGateAction.WAIT_FOR_SYSTEM_UI_QUIESCENCE, gate.onFrameCommitted())
        assertEquals(CaptureGateAction.REQUEST_COMMITTED_FRAME, gate.onSystemUiQuiescent())
        assertEquals(CaptureGateAction.START_CAPTURE, gate.onFrameCommitted())
    }

    @Test
    fun `cancel prevents stale callbacks and grant reuse`() {
        val gate = readyGate()
        gate.cancel()

        assertEquals(CaptureGateAction.WAIT, gate.onFrameCommitted())
        assertEquals(CaptureGateAction.WAIT, gate.onResumed())
        assertEquals(CaptureGateAction.WAIT, gate.onWindowFocusChanged(true))
        assertEquals(CaptureGateAction.WAIT, gate.onConsentAccepted())
    }

    @Test
    fun `result arriving after resume and focus still starts exactly once`() {
        val gate = PostConsentCaptureGate(requiredCommittedFrames = 1)
        gate.onConsentLaunched()
        gate.onWindowFocusChanged(false)
        gate.onResumed()
        gate.onWindowFocusChanged(true)

        assertEquals(CaptureGateAction.REQUEST_COMMITTED_FRAME, gate.onConsentAccepted())
        assertEquals(CaptureGateAction.WAIT_FOR_SYSTEM_UI_QUIESCENCE, gate.onFrameCommitted())
        assertEquals(CaptureGateAction.REQUEST_COMMITTED_FRAME, gate.onSystemUiQuiescent())
        assertEquals(CaptureGateAction.START_CAPTURE, gate.onFrameCommitted())
        assertEquals(CaptureGateAction.WAIT, gate.onWindowFocusChanged(true))
        assertEquals(CaptureGateAction.WAIT, gate.onConsentAccepted())
    }

    @Test
    fun `consent pause counts as focus departure when window callback is coalesced`() {
        val gate = PostConsentCaptureGate(requiredCommittedFrames = 1)
        gate.onResumed()
        gate.onWindowFocusChanged(true)
        gate.onConsentLaunched()
        gate.onPaused()
        gate.onConsentAccepted()
        gate.onResumed()

        assertEquals(CaptureGateAction.REQUEST_COMMITTED_FRAME, gate.onWindowFocusChanged(true))
        assertEquals(CaptureGateAction.WAIT_FOR_SYSTEM_UI_QUIESCENCE, gate.onFrameCommitted())
        assertEquals(CaptureGateAction.REQUEST_COMMITTED_FRAME, gate.onSystemUiQuiescent())
        assertEquals(CaptureGateAction.START_CAPTURE, gate.onFrameCommitted())
    }

    @Test
    fun `focus loss during quiescence invalidates the full sequence`() {
        val gate = readyGate()
        assertEquals(CaptureGateAction.REQUEST_COMMITTED_FRAME, gate.onFrameCommitted())
        assertEquals(CaptureGateAction.WAIT_FOR_SYSTEM_UI_QUIESCENCE, gate.onFrameCommitted())

        assertEquals(CaptureGateAction.WAIT, gate.onWindowFocusChanged(false))
        assertEquals(CaptureGateAction.WAIT, gate.onSystemUiQuiescent())
        assertEquals(CaptureGateAction.REQUEST_COMMITTED_FRAME, gate.onWindowFocusChanged(true))
        assertEquals(CaptureGateAction.REQUEST_COMMITTED_FRAME, gate.onFrameCommitted())
        assertEquals(CaptureGateAction.WAIT_FOR_SYSTEM_UI_QUIESCENCE, gate.onFrameCommitted())
    }

    @Test
    fun `system animation timing uses three scaled long-animation windows`() {
        assertEquals(1_500L, PostConsentTiming.quiescenceMillis(500, 1f))
        assertEquals(1_500L, PostConsentTiming.quiescenceMillis(500, 0f))
        assertEquals(2_250L, PostConsentTiming.quiescenceMillis(500, 1.5f))
        assertEquals(4_000L, PostConsentTiming.quiescenceMillis(500, 10f))
        assertEquals(5_500L, PostConsentTiming.readinessTimeoutMillis(1_500L))
    }

    private fun readyGate(): PostConsentCaptureGate = PostConsentCaptureGate(requiredCommittedFrames = 2).apply {
        onConsentLaunched()
        onWindowFocusChanged(false)
        onConsentAccepted()
        onResumed()
        assertEquals(CaptureGateAction.REQUEST_COMMITTED_FRAME, onWindowFocusChanged(true))
    }
}
