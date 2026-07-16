package com.grimnej.lmcomment.config

import android.content.Context
import android.content.res.Configuration

enum class AppearancePreference(val wireValue: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    fun resolvesToDark(systemUiMode: Int): Boolean = when (this) {
        SYSTEM -> systemUiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        LIGHT -> false
        DARK -> true
    }

    companion object {
        fun fromWireValue(value: String?): AppearancePreference =
            entries.firstOrNull { it.wireValue == value } ?: SYSTEM
    }
}

class AppearancePreferenceStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun read(): AppearancePreference =
        AppearancePreference.fromWireValue(preferences.getString(KEY_MODE, null))

    fun write(preference: AppearancePreference) {
        preferences.edit().putString(KEY_MODE, preference.wireValue).apply()
    }

    companion object {
        private const val PREFERENCES = "lmcomment_appearance"
        private const val KEY_MODE = "mode"
    }
}
