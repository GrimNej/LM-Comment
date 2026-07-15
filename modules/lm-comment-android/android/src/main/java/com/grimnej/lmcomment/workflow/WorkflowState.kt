package com.grimnej.lmcomment.workflow

import android.graphics.Bitmap
import com.grimnej.lmcomment.config.Tone
import com.grimnej.lmcomment.crop.NormalizedCropRect
import com.grimnej.lmcomment.relay.GenerationOption
import com.grimnej.lmcomment.relay.RelayFailureCode

/**
 * The approved text and generation controls that survive loading, errors,
 * result editing, and regeneration. It never contains a bitmap or credential.
 */
data class GenerationDraft(
    val sourceText: String,
    val blocks: List<String>,
    val selection: NormalizedCropRect,
    val manualEntry: Boolean,
    val canReturnToCrop: Boolean,
    val tone: Tone,
    val instruction: String,
    val optionCount: Int,
    val demoConfigured: Boolean,
)

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
        val tone: Tone,
        val instruction: String,
        val optionCount: Int,
        val demoConfigured: Boolean,
    ) : WorkflowState

    data class OcrError(
        val selection: NormalizedCropRect,
        val message: String,
    ) : WorkflowState

    data class Generating(
        val draft: GenerationDraft,
    ) : WorkflowState

    data class ShowingResults(
        val draft: GenerationDraft,
        val requestId: String,
        val options: List<GenerationOption>,
        val selectedOptionId: String?,
        val copiedOptionId: String?,
    ) : WorkflowState

    data class EditingResult(
        val results: ShowingResults,
        val optionId: String,
        val draftText: String,
    ) : WorkflowState

    data class GenerationError(
        val draft: GenerationDraft,
        val code: RelayFailureCode,
        val message: String,
    ) : WorkflowState

    /** Opaque terminal state used while the activity and its Compose tree close. */
    data object Closing : WorkflowState
}

val WorkflowState.isSensitive: Boolean
    get() = this !is WorkflowState.CaptureCloak
