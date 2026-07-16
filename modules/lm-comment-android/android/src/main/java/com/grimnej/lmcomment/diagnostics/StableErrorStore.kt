package com.grimnej.lmcomment.diagnostics

import android.content.Context

/** Stores only an allowlisted enum name; never exception details or user content. */
internal class StableErrorStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun read(): String? = preferences.getString(KEY_LAST_ERROR, null)

    fun record(code: String) {
        val safeCode = SafeDiagnosticsPolicy.stableErrorCode(code) ?: return
        preferences.edit().putString(KEY_LAST_ERROR, safeCode).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "lmcomment_diagnostics"
        const val KEY_LAST_ERROR = "last_error"
    }
}
