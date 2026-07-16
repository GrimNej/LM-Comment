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
        assertEquals(CaptureGateAction.START_CAPTURE, gate.onFrameCommitted())
    }

    private fun readyGate(): PostConsentCaptureGate = PostConsentCaptureGate(requiredCommittedFrames = 2).apply {
        onConsentLaunched()
        onWindowFocusChanged(false)
        onConsentAccepted()
        onResumed()
        assertEquals(CaptureGateAction.REQUEST_COMMITTED_FRAME, onWindowFocusChanged(true))
    }
}
