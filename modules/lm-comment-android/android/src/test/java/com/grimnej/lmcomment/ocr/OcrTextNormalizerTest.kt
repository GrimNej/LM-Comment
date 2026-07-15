package com.grimnej.lmcomment.ocr

import org.junit.Assert.assertEquals
import org.junit.Test

class OcrTextNormalizerTest {
    @Test
    fun changesOnlyLineEndingShapeAndOuterNewlines() {
        assertEquals(
            "  Maya 😀  \nkeeps  spacing",
            OcrTextNormalizer.normalize("\r\n  Maya 😀  \r\nkeeps  spacing\r\n"),
        )
    }
}
