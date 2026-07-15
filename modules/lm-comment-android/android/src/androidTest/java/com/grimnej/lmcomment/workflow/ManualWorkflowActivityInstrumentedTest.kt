package com.grimnej.lmcomment.workflow

import android.content.Context
import android.content.Intent
import android.view.WindowManager
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ManualWorkflowActivityInstrumentedTest {
    @Test
    fun directManualEntryIsSecureAndBackCloses() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(context, CaptureWorkflowActivity::class.java)
            .putExtra(CaptureWorkflowActivity.EXTRA_MANUAL_ENTRY, true)
            .putExtra(CaptureWorkflowActivity.EXTRA_INITIAL_TEXT, "Synthetic fixture")

        ActivityScenario.launch<CaptureWorkflowActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                assertFalse(activity.isFinishing)
                assertTrue(
                    activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0,
                )
                activity.onBackPressedDispatcher.onBackPressed()
                assertTrue(activity.isFinishing)
            }
        }
    }
}
