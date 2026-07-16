package com.grimnej.lmcomment.capture

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompositorWarmupGateTest {
    @Test
    fun `default gate discards one raw buffer then converts`() {
        val gate = CompositorWarmupGate()

        assertFalse(gate.shouldConvertNextFrame())
        assertTrue(gate.shouldConvertNextFrame())
        assertTrue(gate.shouldConvertNextFrame())
    }

    @Test
    fun `reset restores the bounded warmup`() {
        val gate = CompositorWarmupGate()
        assertFalse(gate.shouldConvertNextFrame())
        assertTrue(gate.shouldConvertNextFrame())

        gate.reset()

        assertFalse(gate.shouldConvertNextFrame())
        assertTrue(gate.shouldConvertNextFrame())
    }
}
