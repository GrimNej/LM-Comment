package com.grimnej.lmcomment.config

import android.content.Context
import android.content.pm.ApplicationInfo
import java.net.URI

class DemoConfigurationStore(context: Context) {
    private val applicationContext = context.applicationContext
    private val preferences = applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )
    private val isDebuggable = isApplicationDebuggable(applicationContext)

    fun save(configuration: DemoConfiguration) {
        val validated = DemoConfigurationValidator.validate(configuration, isDebuggable)
        check(
            preferences.edit()
                .putString(KEY_RELAY_BASE_URL, validated.relayBaseUrl)
                .putString(KEY_DEMO_TOKEN, validated.demoToken)
                .putString(KEY_DEFAULT_TONE, validated.defaultTone.wireValue)
                .putInt(KEY_OPTION_COUNT, validated.optionCount)
                .putBoolean(KEY_DEMO_MODE, validated.demoMode)
                .commit(),
        ) { "Unable to persist demo configuration." }
    }

    fun read(): DemoConfiguration? {
        if (!REQUIRED_KEYS.all(preferences::contains)) return null

        return try {
            DemoConfigurationValidator.validate(
                relayBaseUrl = preferences.getString(KEY_RELAY_BASE_URL, null) ?: return null,
                demoToken = preferences.getString(KEY_DEMO_TOKEN, null) ?: return null,
                defaultTone = preferences.getString(KEY_DEFAULT_TONE, null) ?: return null,
                optionCount = preferences.getInt(KEY_OPTION_COUNT, DEFAULT_OPTION_COUNT),
                demoMode = preferences.getBoolean(KEY_DEMO_MODE, DEFAULT_DEMO_MODE),
                isDebuggable = isDebuggable,
            )
        } catch (_: IllegalArgumentException) {
            null
        } catch (_: ClassCastException) {
            null
        }
    }

    fun status(): DemoConfigurationStatus {
        val configuration = read()
        return DemoConfigurationStatus(
            relayHostname = configuration?.relayBaseUrl?.let { URI(it).host },
            isDemoTokenConfigured = configuration?.demoToken?.isNotEmpty() == true,
            defaultTone = configuration?.defaultTone ?: DEFAULT_TONE,
            optionCount = configuration?.optionCount ?: DEFAULT_OPTION_COUNT,
            demoMode = configuration?.demoMode ?: DEFAULT_DEMO_MODE,
        )
    }

    fun clear() {
        check(preferences.edit().clear().commit()) {
            "Unable to reset demo configuration."
        }
    }

    companion object {
        val DEFAULT_TONE = Tone.NATURAL
        const val DEFAULT_OPTION_COUNT = 3
        const val DEFAULT_DEMO_MODE = true

        private const val PREFERENCES_NAME = "lmcomment_demo_configuration"
        private const val KEY_RELAY_BASE_URL = "relayBaseUrl"
        private const val KEY_DEMO_TOKEN = "demoToken"
        private const val KEY_DEFAULT_TONE = "defaultTone"
        private const val KEY_OPTION_COUNT = "optionCount"
        private const val KEY_DEMO_MODE = "demoMode"
        private val REQUIRED_KEYS = setOf(
            KEY_RELAY_BASE_URL,
            KEY_DEMO_TOKEN,
            KEY_DEFAULT_TONE,
            KEY_OPTION_COUNT,
            KEY_DEMO_MODE,
        )

        fun isApplicationDebuggable(context: Context): Boolean =
            context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    }
}
