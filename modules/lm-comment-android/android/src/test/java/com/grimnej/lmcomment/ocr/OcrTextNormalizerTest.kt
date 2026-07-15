package com.grimnej.lmcomment.ocr

import org.junit.Assert.assertEquals
import org.junit.Test

class OcrTextNormalizerTest {
    @Test
    fun changesOnlyLineEndingShapeAndOuterNewlines() {
        assertEquals(
            "  Maya \uD83D\uDE00  \nkeeps  spacing",
            OcrTextNormalizer.normalize("\r\n  Maya \uD83D\uDE00  \r\nkeeps  spacing\r\n"),
        )
    }

    @Test
    fun normalizesLoneCarriageReturnsWithoutChangingOtherWhitespace() {
        assertEquals(
            "first\n\n\tsecond  ",
            OcrTextNormalizer.normalize("first\r\r\tsecond  "),
        )
    }

    @Test
    fun normalizationIsIdempotent() {
        val normalized = OcrTextNormalizer.normalize("Ada\r\nLovelace\r")
        assertEquals(normalized, OcrTextNormalizer.normalize(normalized))
    }
}
