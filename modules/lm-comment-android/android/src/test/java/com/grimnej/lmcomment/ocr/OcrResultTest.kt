package com.grimnej.lmcomment.ocr

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrResultTest {
    @Test
    fun whitespaceOnlyResultIsNotReadable() {
        assertFalse(OcrResult(text = " \n\t", blocks = emptyList()).hasReadableText)
    }

    @Test
    fun textResultIsReadable() {
        assertTrue(OcrResult(text = "Hello", blocks = listOf("Hello")).hasReadableText)
    }
}
