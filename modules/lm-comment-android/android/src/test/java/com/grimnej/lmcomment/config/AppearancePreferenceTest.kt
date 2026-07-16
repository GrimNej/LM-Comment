package com.grimnej.lmcomment.config

import android.content.res.Configuration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppearancePreferenceTest {
    @Test
    fun `wire values are strict and unknown values use system`() {
        assertEquals(AppearancePreference.SYSTEM, AppearancePreference.fromWireValue("system"))
        assertEquals(AppearancePreference.LIGHT, AppearancePreference.fromWireValue("light"))
        assertEquals(AppearancePreference.DARK, AppearancePreference.fromWireValue("dark"))
        assertEquals(AppearancePreference.SYSTEM, AppearancePreference.fromWireValue("sepia"))
        assertEquals(AppearancePreference.SYSTEM, AppearancePreference.fromWireValue(null))
    }

    @Test
    fun `explicit modes override system night state`() {
        assertFalse(AppearancePreference.LIGHT.resolvesToDark(Configuration.UI_MODE_NIGHT_YES))
        assertTrue(AppearancePreference.DARK.resolvesToDark(Configuration.UI_MODE_NIGHT_NO))
    }

    @Test
    fun `system mode follows configuration`() {
        assertTrue(AppearancePreference.SYSTEM.resolvesToDark(Configuration.UI_MODE_NIGHT_YES))
        assertFalse(AppearancePreference.SYSTEM.resolvesToDark(Configuration.UI_MODE_NIGHT_NO))
    }
}
