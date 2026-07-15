package com.grimnej.lmcomment.ocr

import android.graphics.Bitmap
import java.io.Closeable

data class OcrResult(
    val text: String,
    val blocks: List<String>,
) {
    val hasReadableText: Boolean get() = text.isNotBlank()
}

interface OcrEngine : Closeable {
    suspend fun recognize(bitmap: Bitmap, rotationDegrees: Int = 0): OcrResult
}

class OcrRecognitionException(cause: Throwable) :
    Exception("On-device text recognition failed.", cause)
