package com.grimnej.lmcomment.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicBoolean

/** On-device OCR backed by the Latin model bundled in the application APK. */
class MlKitOcrEngine internal constructor(
    private val recognizer: TextRecognizer,
) : OcrEngine {
    constructor() : this(
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS),
    )

    private val closed = AtomicBoolean(false)

    override suspend fun recognize(bitmap: Bitmap, rotationDegrees: Int): OcrResult {
        require(!bitmap.isRecycled) { "OCR requires a non-recycled bitmap." }
        ensureOpen()
        requireSupportedRotation(rotationDegrees)

        val task = try {
            recognizer.process(InputImage.fromBitmap(bitmap, rotationDegrees))
        } catch (error: Throwable) {
            throw error.asPublicOcrError()
        }

        return try {
            task.awaitBorrowedResult(
                cancellationMessage = "On-device text recognition was cancelled.",
            ) { recognized ->
                OcrResult(
                    text = OcrTextNormalizer.normalize(recognized.text),
                    blocks = recognized.textBlocks
                        .map { block -> OcrTextNormalizer.normalize(block.text) }
                        .filter { block -> block.isNotBlank() },
                )
            }
        } catch (error: Throwable) {
            throw error.asPublicOcrError()
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            recognizer.close()
        }
    }

    private fun ensureOpen() {
        if (closed.get()) throw OcrEngineClosedException()
    }

    private fun requireSupportedRotation(rotationDegrees: Int) {
        require(
            rotationDegrees == 0 || rotationDegrees == 90 ||
                rotationDegrees == 180 || rotationDegrees == 270,
        ) {
            "rotationDegrees must be 0, 90, 180, or 270."
        }
    }

    private fun Throwable.asPublicOcrError(): Throwable = when (this) {
        is CancellationException,
        is IllegalArgumentException,
        is OcrEngineClosedException,
        is OcrRecognitionException,
        -> this

        else -> OcrRecognitionException(this)
    }

}
