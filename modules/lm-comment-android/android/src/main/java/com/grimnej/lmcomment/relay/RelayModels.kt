package com.grimnej.lmcomment.relay

import com.grimnej.lmcomment.config.Tone
import java.io.IOException

data class GenerationRequest(
    val sourceText: String,
    val tone: Tone,
    val instruction: String,
    val optionCount: Int,
)

data class GenerationOption(
    val id: String,
    val text: String,
)

data class GenerationResponse(
    val requestId: String,
    val options: List<GenerationOption>,
)

/**
 * Frozen relay codes plus client-only failures needed to present safe, actionable UI.
 * Messages are deliberately local constants: response bodies and exception details are
 * never exposed to the workflow or logs.
 */
enum class RelayFailureCode(
    val safeMessage: String,
    internal val expectedHttpStatus: Int? = null,
) {
    BAD_REQUEST(
        "The request is invalid. Review the text and generation settings.",
        400,
    ),
    UNAUTHORIZED("Demo access is not configured.", 401),
    GENERATION_DISABLED("Generation is temporarily disabled.", 503),
    RATE_LIMITED(
        "The demo is receiving too many requests. Try again shortly.",
        429,
    ),
    DAILY_LIMIT_REACHED(
        "The demo generation limit has been reached for today.",
        429,
    ),
    PROVIDER_TIMEOUT(
        "Generation took too long. Check the connection and try again.",
        504,
    ),
    PROVIDER_RATE_LIMIT(
        "The generation service is busy. Try again shortly.",
        429,
    ),
    PROVIDER_UNAVAILABLE(
        "The generation service is unavailable. Try again shortly.",
        503,
    ),
    INVALID_PROVIDER_RESPONSE(
        "The generation service returned an unusable response. Try again.",
        502,
    ),
    INTERNAL("The relay could not complete the request.", 500),

    INVALID_CONFIGURATION("Demo access is not configured."),
    NETWORK_UNAVAILABLE("Unable to reach generation. Check your connection and try again."),
    NETWORK_TIMEOUT("Generation took too long. Check the connection and try again."),
    INVALID_RESPONSE("The relay returned an unusable response. Try again."),
    ;

    internal val isRemoteCode: Boolean
        get() = expectedHttpStatus != null

    companion object {
        internal fun fromRemoteWireValue(value: String): RelayFailureCode? =
            entries.firstOrNull { it.isRemoteCode && it.name == value }
    }
}

class RelayException(
    val code: RelayFailureCode,
    val requestId: String? = null,
) : IOException(code.safeMessage)

data class RelayTimeouts(
    val connectMillis: Int = DEFAULT_CONNECT_TIMEOUT_MILLIS,
    val readMillis: Int = DEFAULT_READ_TIMEOUT_MILLIS,
    val overallMillis: Long = DEFAULT_OVERALL_TIMEOUT_MILLIS,
) {
    init {
        require(connectMillis in 1..MAX_TIMEOUT_MILLIS) { "Invalid connect timeout." }
        require(readMillis in 1..MAX_TIMEOUT_MILLIS) { "Invalid read timeout." }
        require(overallMillis in 1L..MAX_TIMEOUT_MILLIS.toLong()) { "Invalid overall timeout." }
    }

    companion object {
        const val DEFAULT_CONNECT_TIMEOUT_MILLIS = 5_000
        const val DEFAULT_READ_TIMEOUT_MILLIS = 20_000
        const val DEFAULT_OVERALL_TIMEOUT_MILLIS = 22_000L
        private const val MAX_TIMEOUT_MILLIS = 120_000
    }
}

internal data class RelayErrorEnvelope(
    val code: RelayFailureCode,
    val requestId: String,
)
