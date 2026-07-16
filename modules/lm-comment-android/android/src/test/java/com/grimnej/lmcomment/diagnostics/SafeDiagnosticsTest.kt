package com.grimnej.lmcomment.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class SafeDiagnosticsTest {
    @Test
    fun `bridge payload contains only safe allowlisted keys`() {
        val payload = SafeDiagnosticsSnapshot(
            appVersion = "0.1.0",
            contractVersion = 1,
            androidApi = 36,
            deviceModel = "Google Pixel",
            overlayPermission = "granted",
            notificationPermission = "granted",
            bubbleStatus = "running",
            relayHostname = "relay.example",
            relayHealth = RelayHealthStatus.HEALTHY,
            lastStableErrorCode = "CAPTURE_TIMEOUT",
            captureResourceCounts = mapOf("activeImage" to 0),
        ).toBridgeMap()

        assertEquals(
            setOf(
                "platform",
                "appVersion",
                "contractVersion",
                "androidApi",
                "deviceModel",
                "overlayPermission",
                "notificationPermission",
                "bubbleStatus",
                "relayHostname",
                "relayHealth",
                "lastStableErrorCode",
                "captureResourceCounts",
            ),
            payload.keys,
        )
        assertFalse(payload.keys.any { it.contains("token", ignoreCase = true) })
        assertFalse(payload.keys.any { it.contains("text", ignoreCase = true) })
        assertFalse(payload.keys.any { it.contains("request", ignoreCase = true) })
        assertFalse(payload.keys.any { it.contains("screenshot", ignoreCase = true) })
    }

    @Test
    fun `last error rejects arbitrary preference content`() {
        assertEquals("NETWORK_TIMEOUT", SafeDiagnosticsPolicy.stableErrorCode("NETWORK_TIMEOUT"))
        assertEquals("CAPTURE_FAILED", SafeDiagnosticsPolicy.stableErrorCode("CAPTURE_FAILED"))
        assertNull(SafeDiagnosticsPolicy.stableErrorCode("user supplied detail"))
        assertNull(SafeDiagnosticsPolicy.stableErrorCode(null))
    }

    @Test
    fun `device label is bounded and strips controls`() {
        val label = SafeDiagnosticsPolicy.deviceLabel("Maker\n", "X".repeat(200))
        assertFalse(label.any(Char::isISOControl))
        assertEquals(120, label.length)
    }
}
