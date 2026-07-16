package com.grimnej.lmcomment.config

enum class Tone(val wireValue: String) {
    NATURAL("natural"),
    PROFESSIONAL("professional"),
    FRIENDLY("friendly"),
    WITTY("witty"),
    CONCISE("concise");

    companion object {
        fun fromWireValue(value: String): Tone =
            entries.firstOrNull { it.wireValue == value }
                ?: throw IllegalArgumentException("Unsupported tone.")
    }
}

data class DemoConfiguration(
    val relayBaseUrl: String,
    val demoToken: String,
    val defaultTone: Tone,
    val optionCount: Int,
    val demoMode: Boolean,
    /** Derived by validation; never persisted or accepted from the JavaScript bridge. */
    val allowDevelopmentHttp: Boolean = false,
)

data class DemoConfigurationStatus(
    val relayHostname: String?,
    val isDemoTokenConfigured: Boolean,
    val defaultTone: Tone,
    val optionCount: Int,
    val demoMode: Boolean,
)
