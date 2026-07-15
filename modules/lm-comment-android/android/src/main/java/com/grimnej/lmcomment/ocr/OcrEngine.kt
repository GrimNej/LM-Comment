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
    /**
     * Recognizes text from an in-memory bitmap.
     *
     * The caller retains ownership of [bitmap]. It must remain valid and must not
     * be mutated or recycled until this function returns. That guarantee also
     * holds when the calling coroutine is cancelled: implementations do not
     * return until the underlying recognizer reaches a terminal state. The engine
     * never recycles the bitmap.
     *
     * Screenshot pixels remain in-process and are never persisted or uploaded by
     * this API. Implementations must not log recognized content.
     */
    suspend fun recognize(bitmap: Bitmap, rotationDegrees: Int = 0): OcrResult
}

class OcrRecognitionException(cause: Throwable) :
    Exception("On-device text recognition failed.", cause)

class OcrEngineClosedException :
    IllegalStateException("On-device text recognition is unavailable because the engine is closed.")
