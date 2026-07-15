package com.grimnej.lmcomment.config

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DemoConfigurationStoreInstrumentedTest {
    @Test
    fun configurationPersistsPrivatelyAndSafeStatusNeverContainsCredentials() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val firstStore = DemoConfigurationStore(context)
        firstStore.clear()

        try {
            firstStore.save(
                DemoConfiguration(
                    relayBaseUrl = "https://relay.example/",
                    demoToken = "test-demo-token-123456",
                    defaultTone = Tone.FRIENDLY,
                    optionCount = 2,
                    demoMode = true,
                ),
            )

            // A fresh repository instance models the workflow reading after the
            // React Native activity has gone away.
            val restored = DemoConfigurationStore(context).read()
            assertEquals("https://relay.example/", restored?.relayBaseUrl)
            assertEquals("test-demo-token-123456", restored?.demoToken)

            val safeStatus = DemoConfigurationStore(context).status()
            assertEquals("relay.example", safeStatus.relayHostname)
            assertTrue(safeStatus.isDemoTokenConfigured)
            assertEquals(Tone.FRIENDLY, safeStatus.defaultTone)
            assertEquals(2, safeStatus.optionCount)
            assertTrue(safeStatus.demoMode)

            firstStore.clear()
            assertNull(DemoConfigurationStore(context).read())
            assertFalse(DemoConfigurationStore(context).status().isDemoTokenConfigured)
        } finally {
            firstStore.clear()
        }
    }
}
