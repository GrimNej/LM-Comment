package com.grimnej.lmcomment.diagnostics

import com.grimnej.lmcomment.capture.CaptureError
import com.grimnej.lmcomment.relay.RelayFailureCode

internal data class SafeDiagnosticsSnapshot(
    val appVersion: String,
    val contractVersion: Int,
    val androidApi: Int,
    val deviceModel: String,
    val overlayPermission: String,
    val notificationPermission: String,
    val bubbleStatus: String,
    val relayHostname: String?,
    val relayHealth: RelayHealthStatus,
    val lastStableErrorCode: String?,
    val captureResourceCounts: Map<String, Int>?,
) {
    /** This allowlisted bridge payload cannot grow content-bearing fields accidentally. */
    fun toBridgeMap(): Map<String, Any?> = mapOf(
        "platform" to "android",
        "appVersion" to appVersion,
        "contractVersion" to contractVersion,
        "androidApi" to androidApi,
        "deviceModel" to deviceModel,
        "overlayPermission" to overlayPermission,
        "notificationPermission" to notificationPermission,
        "bubbleStatus" to bubbleStatus,
        "relayHostname" to relayHostname,
        "relayHealth" to relayHealth.wireValue,
        "lastStableErrorCode" to lastStableErrorCode,
        "captureResourceCounts" to captureResourceCounts,
    )
}

internal object SafeDiagnosticsPolicy {
    private val stableErrorCodes =
        CaptureError.entries.mapTo(mutableSetOf()) { it.name } +
            RelayFailureCode.entries.map { it.name }

    fun stableErrorCode(rawValue: String?): String? =
        rawValue?.takeIf(stableErrorCodes::contains)

    fun deviceLabel(manufacturer: String?, model: String?): String {
        val value = listOfNotNull(manufacturer, model)
            .joinToString(" ")
            .filterNot(Char::isISOControl)
            .trim()
            .take(MAX_DEVICE_LABEL_CHARACTERS)
        return value.ifEmpty { "Unknown Android device" }
    }

    private const val MAX_DEVICE_LABEL_CHARACTERS = 120
}
