package com.grimnej.lmcomment.workflow

import android.graphics.Bitmap
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grimnej.lmcomment.capture.CaptureResourceCounters
import com.grimnej.lmcomment.crop.CropBitmapFactory
import com.grimnej.lmcomment.crop.NormalizedCropRect
import com.grimnej.lmcomment.ocr.MlKitOcrEngine
import com.grimnej.lmcomment.ocr.OcrEngine
import java.util.IdentityHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class WorkflowViewModel internal constructor(
    private val ocrEngine: OcrEngine = MlKitOcrEngine(),
    private val cropBitmapFactory: (Bitmap, NormalizedCropRect) -> Bitmap = CropBitmapFactory::create,
) : ViewModel() {
    private val mutableState = mutableStateOf<WorkflowState>(WorkflowState.CaptureCloak)
    val state: State<WorkflowState> = mutableState

    // Bitmap ownership is field-based. UI state may reference the full frame for
    // rendering, but cleanup never depends on casting the current state.
    private var workflowBitmap: Bitmap? = null
    private val recognitionCrops = IdentityHashMap<Bitmap, Unit>()
    private var recognitionJob: Job? = null
    private var recognitionGeneration = 0L
    private var lastSelection = NormalizedCropRect.Suggested

    fun acceptFrame(bitmap: Bitmap) {
        cancelRecognition()
        releaseWorkflowBitmap()
        workflowBitmap = bitmap
        CaptureResourceCounters.activeWorkflowBitmap.incrementAndGet()
        lastSelection = NormalizedCropRect.Suggested
        mutableState.value = WorkflowState.Cropping(bitmap, lastSelection)
    }

    fun updateSelection(selection: NormalizedCropRect) {
        val frame = workflowBitmap ?: return
        if (mutableState.value !is WorkflowState.Cropping) return
        lastSelection = selection
        mutableState.value = WorkflowState.Cropping(frame, selection)
    }

    fun resetSelection() = updateSelection(NormalizedCropRect.Suggested)

    fun useFullFrame() {
        val frame = workflowBitmap ?: return
        lastSelection = NormalizedCropRect.FullFrame
        mutableState.value = WorkflowState.Cropping(frame, lastSelection)
    }

    fun extractText(useFullFrame: Boolean = false) {
        val frame = workflowBitmap ?: return
        val selection = if (useFullFrame) NormalizedCropRect.FullFrame else lastSelection
        lastSelection = selection
        cancelRecognition()

        val crop = try {
            cropBitmapFactory(frame, selection)
        } catch (_: Exception) {
            mutableState.value = WorkflowState.OcrError(
                selection = selection,
                message = "Could not prepare that crop. Adjust it and try again.",
            )
            return
        }
        trackRecognitionCrop(crop)

        val generation = ++recognitionGeneration
        mutableState.value = WorkflowState.RecognizingText(selection)
        val job = viewModelScope.launch(start = CoroutineStart.LAZY) {
            try {
                // The OCR engine borrows the crop and, even after cancellation,
                // does not return until ML Kit has stopped reading its pixels.
                val result = ocrEngine.recognize(crop)
                if (generation != recognitionGeneration) return@launch
                mutableState.value = WorkflowState.ReviewingText(
                    text = result.text,
                    blocks = result.blocks,
                    selection = selection,
                    manualEntry = false,
                    emptyRecognition = !result.hasReadableText,
                    canReturnToCrop = true,
                )
            } catch (cancelled: CancellationException) {
                if (currentCoroutineContext().isActive && generation == recognitionGeneration) {
                    // A provider-side Task cancellation is a recoverable OCR
                    // failure. Navigation/close cancellation still exits quietly.
                    mutableState.value = WorkflowState.OcrError(
                        selection = selection,
                        message = "Text recognition stopped. Try again or type the text.",
                    )
                } else {
                    throw cancelled
                }
            } catch (_: Throwable) {
                if (generation == recognitionGeneration) {
                    mutableState.value = WorkflowState.OcrError(
                        selection = selection,
                        message = "Text recognition did not finish. Try again or type the text.",
                    )
                }
            }
        }
        recognitionJob = job
        job.invokeOnCompletion {
            releaseRecognitionCrop(crop)
            if (recognitionJob === job) recognitionJob = null
        }
        job.start()
    }

    fun enterManualText(sourceText: String = "", directEntry: Boolean = false) {
        cancelRecognition()
        val canReturnToCrop = !directEntry && workflowBitmap != null
        mutableState.value = WorkflowState.ReviewingText(
            text = sourceText,
            blocks = emptyList(),
            selection = lastSelection,
            manualEntry = true,
            emptyRecognition = false,
            canReturnToCrop = canReturnToCrop,
        )
    }

    fun updateReviewedText(text: String) {
        val current = mutableState.value as? WorkflowState.ReviewingText ?: return
        mutableState.value = current.copy(
            text = text,
            emptyRecognition = false,
        )
    }

    fun backToCrop(): Boolean {
        cancelRecognition()
        val frame = workflowBitmap ?: return false
        mutableState.value = WorkflowState.Cropping(frame, lastSelection)
        return true
    }

    /** Returns true when the workflow consumed Back without closing the activity. */
    fun handleBack(): Boolean = when (val current = mutableState.value) {
        is WorkflowState.ReviewingText -> current.canReturnToCrop && backToCrop()
        is WorkflowState.RecognizingText,
        is WorkflowState.OcrError,
        -> backToCrop()

        is WorkflowState.CaptureCloak,
        is WorkflowState.Cropping,
        is WorkflowState.Closing,
        -> false
    }

    /**
     * Hides sensitive content before Activity teardown. The retained frame is
     * recycled from onCleared, after Compose has disposed the old bitmap node.
     */
    fun prepareToClose() {
        mutableState.value = WorkflowState.Closing
        cancelRecognition()
    }

    private fun cancelRecognition() {
        recognitionGeneration++
        recognitionJob?.cancel()
        recognitionJob = null
    }

    private fun trackRecognitionCrop(bitmap: Bitmap) {
        synchronized(recognitionCrops) {
            recognitionCrops[bitmap] = Unit
        }
        CaptureResourceCounters.activeWorkflowBitmap.incrementAndGet()
    }

    private fun releaseRecognitionCrop(bitmap: Bitmap) {
        val owned = synchronized(recognitionCrops) {
            recognitionCrops.remove(bitmap) != null
        }
        if (!owned) return
        if (!bitmap.isRecycled) bitmap.recycle()
        CaptureResourceCounters.activeWorkflowBitmap.decrementAndGet()
    }

    private fun releaseWorkflowBitmap() {
        val bitmap = workflowBitmap ?: return
        workflowBitmap = null
        if (!bitmap.isRecycled) bitmap.recycle()
        CaptureResourceCounters.activeWorkflowBitmap.decrementAndGet()
    }

    override fun onCleared() {
        prepareToClose()
        releaseWorkflowBitmap()
        ocrEngine.close()
        super.onCleared()
    }
}
