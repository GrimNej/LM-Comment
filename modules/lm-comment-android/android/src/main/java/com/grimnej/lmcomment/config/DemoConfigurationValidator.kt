package com.grimnej.lmcomment.config

import java.net.URI
import java.net.URISyntaxException

object DemoConfigurationValidator {
    const val MIN_DEMO_TOKEN_LENGTH = 12
    const val MAX_DEMO_TOKEN_LENGTH = 512

    private val developmentHttpHosts = setOf("localhost", "127.0.0.1", "10.0.2.2")

    fun validate(
        relayBaseUrl: String,
        demoToken: String,
        defaultTone: String,
        optionCount: Int,
        demoMode: Boolean,
        isDebuggable: Boolean,
    ): DemoConfiguration = validate(
        DemoConfiguration(
            relayBaseUrl = relayBaseUrl,
            demoToken = demoToken,
            defaultTone = Tone.fromWireValue(defaultTone),
            optionCount = optionCount,
            demoMode = demoMode,
        ),
        isDebuggable,
    )

    fun validate(
        configuration: DemoConfiguration,
        isDebuggable: Boolean,
    ): DemoConfiguration {
        val normalizedUrl = validateAndNormalizeUrl(configuration.relayBaseUrl, isDebuggable)
        require(configuration.demoToken.none(Char::isISOControl)) {
            "Demo token must not contain control characters."
        }
        val normalizedToken = configuration.demoToken.trim()
        require(normalizedToken.length in MIN_DEMO_TOKEN_LENGTH..MAX_DEMO_TOKEN_LENGTH) {
            "Demo token must be between $MIN_DEMO_TOKEN_LENGTH and $MAX_DEMO_TOKEN_LENGTH characters."
        }
        require(configuration.optionCount in 1..3) {
            "Option count must be between 1 and 3."
        }

        return configuration.copy(
            relayBaseUrl = normalizedUrl,
            demoToken = normalizedToken,
        )
    }

    private fun validateAndNormalizeUrl(value: String, isDebuggable: Boolean): String {
        val candidate = value.trim()
        require(candidate.isNotEmpty()) { "Relay URL is required." }

        val uri = try {
            URI(candidate)
        } catch (_: URISyntaxException) {
            throw IllegalArgumentException("Relay URL is invalid.")
        }

        require(!uri.isOpaque && uri.isAbsolute) { "Relay URL must be absolute." }
        require(!uri.host.isNullOrBlank()) { "Relay URL must include a host." }
        require(uri.rawUserInfo == null) { "Relay URL must not include user information." }
        require(uri.rawQuery == null) { "Relay URL must not include a query." }
        require(uri.rawFragment == null) { "Relay URL must not include a fragment." }
        require(uri.port == -1 || uri.port in 1..65_535) { "Relay URL port is invalid." }

        val scheme = uri.scheme.lowercase()
        val host = requireNotNull(uri.host).lowercase()
        val allowedScheme = scheme == "https" ||
            (isDebuggable && scheme == "http" && host in developmentHttpHosts)
        require(allowedScheme) {
            if (isDebuggable) {
                "Relay URL must use HTTPS; debug HTTP is limited to local development hosts."
            } else {
                "Relay URL must use HTTPS."
            }
        }

        val schemeNormalized = scheme + candidate.substring(candidate.indexOf(':'))
        return schemeNormalized.trimEnd('/') + "/"
    }
}
