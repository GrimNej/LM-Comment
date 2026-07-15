package com.grimnej.lmcomment.ocr

/** Preserves words, spacing, emoji, and names; only line-ending shape is normalized. */
object OcrTextNormalizer {
    fun normalize(text: String): String = text
        .replace("\r\n", "\n")
        .replace('\r', '\n')
        .trim('\n')
}
