package com.grimnej.lmcomment.workflow

import android.graphics.Bitmap
import com.grimnej.lmcomment.crop.NormalizedCropRect

sealed interface WorkflowState {
    /** Transparent, non-secure window while consent/capture is in progress. */
    data object CaptureCloak : WorkflowState

    data class Cropping(
        val bitmap: Bitmap,
        val selection: NormalizedCropRect,
    ) : WorkflowState

    data class RecognizingText(
        val selection: NormalizedCropRect,
    ) : WorkflowState

    data class ReviewingText(
        val text: String,
        val blocks: List<String>,
        val selection: NormalizedCropRect,
        val manualEntry: Boolean,
        val emptyRecognition: Boolean,
        val canReturnToCrop: Boolean,
    ) : WorkflowState

    data class OcrError(
        val selection: NormalizedCropRect,
        val message: String,
    ) : WorkflowState

    /** Opaque terminal state used while the activity and its Compose tree close. */
    data object Closing : WorkflowState
}

val WorkflowState.isSensitive: Boolean
    get() = this !is WorkflowState.CaptureCloak
