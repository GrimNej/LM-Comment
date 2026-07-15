package com.grimnej.lmcomment.ocr

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BundledOcrInstrumentedTest {
    @Test
    fun recognizesSyntheticLatinFixtureWithoutNetworkModelDelivery() = runBlocking {
        val fixture = Bitmap.createBitmap(1400, 420, Bitmap.Config.ARGB_8888)
        val recognizer = MlKitOcrEngine()
        try {
            val canvas = Canvas(fixture)
            canvas.drawColor(Color.WHITE)
            canvas.drawText(
                "LM COMMENT 2042",
                70f,
                245f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.BLACK
                    textSize = 132f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                },
            )

            val result = recognizer.recognize(fixture)

            assertTrue(
                "Bundled OCR did not recognize the in-memory fixture",
                result.text.replace("-", " ").contains("LM COMMENT 2042", ignoreCase = true),
            )
        } finally {
            recognizer.close()
            fixture.recycle()
        }
    }
}
