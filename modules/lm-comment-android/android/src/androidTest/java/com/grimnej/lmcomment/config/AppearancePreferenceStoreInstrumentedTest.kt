package com.grimnej.lmcomment.config

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppearancePreferenceStoreInstrumentedTest {
    @Test
    fun preferenceRoundTripsInPrivateAppStorage() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val store = AppearancePreferenceStore(context)
        try {
            store.write(AppearancePreference.LIGHT)
            assertEquals(AppearancePreference.LIGHT, store.read())
            store.write(AppearancePreference.DARK)
            assertEquals(AppearancePreference.DARK, store.read())
        } finally {
            store.write(AppearancePreference.SYSTEM)
        }
    }
}
