package com.grimnej.lmcomment.workflow

import android.graphics.Bitmap
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grimnej.lmcomment.capture.CaptureResourceCounters
import com.grimnej.lmcomment.config.DemoConfiguration
import com.grimnej.lmcomment.config.Tone
import com.grimnej.lmcomment.crop.CropBitmapFactory
import com.grimnej.lmcomment.crop.NormalizedCropRect
import com.grimnej.lmcomment.ocr.MlKitOcrEngine
import com.grimnej.lmcomment.ocr.OcrEngine
import com.grimnej.lmcomment.relay.GenerationOption
import com.grimnej.lmcomment.relay.GenerationRequest
import com.grimnej.lmcomment.relay.GenerationResponse
import com.grimnej.lmcomment.relay.RelayException
import com.grimnej.lmcomment.relay.RelayFailureCode
import java.util.IdentityHashMap
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class WorkflowViewModel internal constructor(
    private val ocrEngine: OcrEngine = MlKitOcrEngine(),
    private val cropBitmapFactory: (Bitmap, NormalizedCropRect) -> Bitmap = CropBitmapFactory::create,
    private val generationGateway: GenerationGateway = RelayGenerationGateway,
) : ViewModel() {
    private val mutableState = mutableStateOf<WorkflowState>(WorkflowState.CaptureCloak)
    val state: State<WorkflowState> = mutableState

    // Bitmap ownership is field-based. UI state may reference the full frame for
    // rendering, but cleanup never depends on casting the current state.
    private var workflowBitmap: Bitmap? = null
    private val recognitionCrops = IdentityHashMap<Bitmap, Unit>()
    private var recognitionJob: Job? = null
    private var recognitionGeneration = 0L
    private var generationJob: Job? = null
    private var generationToken = 0L
    private var demoConfiguration: DemoConfiguration? = null
    private var lastSelection = NormalizedCropRect.Suggested

    fun setDemoConfiguration(configuration: DemoConfiguration?) {
        if (demoConfiguration == configuration) return
        demoConfiguration = configuration
        when (val current = mutableState.value) {
            is WorkflowState.ReviewingText -> {
                mutableState.value = current.copy(
                    tone = configuration?.defaultTone ?: current.tone,
                    optionCount = configuration?.optionCount ?: current.optionCount,
                    demoConfigured = configuration != null,
                )
            }

            is WorkflowState.Generating -> {
                cancelGenerationWork()
                mutableState.value = current.draft.toReviewingText(configuration != null)
            }

            else -> Unit
        }
    }

    fun acceptFrame(bitmap: Bitmap) {
        cancelGenerationWork()
        cancelRecognition()
        mutableState.value = WorkflowState.CaptureCloak
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
        cancelGenerationWork()
        lastSelection = NormalizedCropRect.FullFrame
        mutableState.value = WorkflowState.Cropping(frame, lastSelection)
    }

    fun extractText(useFullFrame: Boolean = false) {
        val frame = workflowBitmap ?: return
        val selection = if (useFullFrame) NormalizedCropRect.FullFrame else lastSelection
        lastSelection = selection
        cancelGenerationWork()
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
                val defaults = defaultGenerationSettings()
                mutableState.value = WorkflowState.ReviewingText(
                    text = result.text,
                    blocks = result.blocks,
                    selection = selection,
                    manualEntry = false,
                    emptyRecognition = !result.hasReadableText,
                    canReturnToCrop = true,
                    tone = defaults.tone,
                    instruction = "",
                    optionCount = defaults.optionCount,
                    demoConfigured = defaults.configured,
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
        cancelGenerationWork()
        cancelRecognition()
        val canReturnToCrop = !directEntry && workflowBitmap != null
        val defaults = defaultGenerationSettings()
        mutableState.value = WorkflowState.ReviewingText(
            text = sourceText,
            blocks = emptyList(),
            selection = lastSelection,
            manualEntry = true,
            emptyRecognition = false,
            canReturnToCrop = canReturnToCrop,
            tone = defaults.tone,
            instruction = "",
            optionCount = defaults.optionCount,
            demoConfigured = defaults.configured,
        )
    }

    fun updateReviewedText(text: String) {
        val current = mutableState.value as? WorkflowState.ReviewingText ?: return
        mutableState.value = current.copy(
            text = text,
            emptyRecognition = false,
        )
    }

    fun updateTone(tone: Tone) {
        val current = mutableState.value as? WorkflowState.ReviewingText ?: return
        mutableState.value = current.copy(tone = tone)
    }

    fun updateInstruction(instruction: String) {
        val current = mutableState.value as? WorkflowState.ReviewingText ?: return
        mutableState.value = current.copy(instruction = instruction.take(MAX_INSTRUCTION_CHARACTERS))
    }

    fun updateOptionCount(optionCount: Int) {
        val current = mutableState.value as? WorkflowState.ReviewingText ?: return
        mutableState.value = current.copy(optionCount = optionCount.coerceIn(MIN_OPTIONS, MAX_OPTIONS))
    }

    fun generate() {
        val review = mutableState.value as? WorkflowState.ReviewingText ?: return
        startGeneration(review.toGenerationDraft())
    }

    fun cancelGeneration() {
        val current = mutableState.value as? WorkflowState.Generating ?: return
        cancelGenerationWork()
        mutableState.value = current.draft.toReviewingText(demoConfiguration != null)
    }

    fun selectResult(optionId: String) {
        val current = mutableState.value as? WorkflowState.ShowingResults ?: return
        if (current.options.none { it.id == optionId }) return
        mutableState.value = current.copy(selectedOptionId = optionId)
    }

    fun beginEdit(optionId: String) {
        val current = mutableState.value as? WorkflowState.ShowingResults ?: return
        val option = current.options.firstOrNull { it.id == optionId } ?: return
        mutableState.value = WorkflowState.EditingResult(
            results = current.copy(selectedOptionId = optionId),
            optionId = optionId,
            draftText = option.text,
        )
    }

    fun updateEditText(text: String) {
        val current = mutableState.value as? WorkflowState.EditingResult ?: return
        mutableState.value = current.copy(draftText = text.take(MAX_OPTION_CHARACTERS))
    }

    fun saveEdit() {
        val current = mutableState.value as? WorkflowState.EditingResult ?: return
        if (current.draftText.isBlank()) return
        val previous = current.results.options.firstOrNull { it.id == current.optionId } ?: return
        val editedOptions = current.results.options.map { option ->
            if (option.id == current.optionId) option.copy(text = current.draftText) else option
        }
        mutableState.value = current.results.copy(
            options = editedOptions,
            selectedOptionId = current.optionId,
            copiedOptionId = current.results.copiedOptionId.takeUnless {
                it == current.optionId && previous.text != current.draftText
            },
        )
    }

    fun cancelEdit() {
        val current = mutableState.value as? WorkflowState.EditingResult ?: return
        mutableState.value = current.results.copy(
            copiedOptionId = current.results.copiedOptionId.takeUnless { it == current.optionId },
        )
    }

    fun resultTextForCopy(optionId: String): String? = when (val current = mutableState.value) {
        is WorkflowState.ShowingResults -> current.options
            .firstOrNull { it.id == optionId }
            ?.text

        is WorkflowState.EditingResult -> if (current.optionId == optionId) {
            current.draftText
        } else {
            current.results.options.firstOrNull { it.id == optionId }?.text
        }

        else -> null
    }

    fun markCopied(optionId: String) {
        when (val current = mutableState.value) {
            is WorkflowState.ShowingResults -> {
                if (current.options.none { it.id == optionId }) return
                mutableState.value = current.copy(
                    selectedOptionId = optionId,
                    copiedOptionId = optionId,
                )
            }

            is WorkflowState.EditingResult -> {
                if (current.results.options.none { it.id == optionId }) return
                mutableState.value = current.copy(
                    results = current.results.copy(
                        selectedOptionId = optionId,
                        copiedOptionId = optionId,
                    ),
                )
            }

            else -> Unit
        }
    }

    fun regenerate() {
        val draft = when (val current = mutableState.value) {
            is WorkflowState.ShowingResults -> current.draft
            is WorkflowState.EditingResult -> current.results.draft
            is WorkflowState.GenerationError -> current.draft
            else -> return
        }
        startGeneration(draft)
    }

    fun backToReview(): Boolean {
        val draft = when (val current = mutableState.value) {
            is WorkflowState.Generating -> current.draft
            is WorkflowState.ShowingResults -> current.draft
            is WorkflowState.EditingResult -> current.results.draft
            is WorkflowState.GenerationError -> current.draft
            else -> return false
        }
        cancelGenerationWork()
        mutableState.value = draft.toReviewingText(demoConfiguration != null)
        return true
    }

    fun backToCrop(): Boolean {
        cancelGenerationWork()
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

        is WorkflowState.Generating -> {
            cancelGeneration()
            true
        }

        is WorkflowState.ShowingResults,
        is WorkflowState.GenerationError,
        -> backToReview()

        is WorkflowState.EditingResult -> {
            cancelEdit()
            true
        }

        is WorkflowState.CaptureCloak,
        is WorkflowState.Cropping,
        is WorkflowState.Closing,
        -> false
    }

    /**
     * Clears the prior workflow before the Activity starts another capture.
     * The configuration remains session-ready, but no bitmap or user text does.
     */
    fun prepareForNewCapture() {
        mutableState.value = WorkflowState.CaptureCloak
        cancelGenerationWork()
        cancelRecognition()
        releaseWorkflowBitmap()
        lastSelection = NormalizedCropRect.Suggested
    }

    /**
     * Hides sensitive content before Activity teardown. The retained frame is
     * recycled from onCleared, after Compose has disposed the old bitmap node.
     */
    fun prepareToClose() {
        mutableState.value = WorkflowState.Closing
        cancelGenerationWork()
        cancelRecognition()
    }

    private fun startGeneration(originalDraft: GenerationDraft) {
        if (mutableState.value is WorkflowState.Generating) return
        cancelGenerationWork()

        val draft = originalDraft.copy(
            instruction = originalDraft.instruction.take(MAX_INSTRUCTION_CHARACTERS),
            optionCount = originalDraft.optionCount.coerceIn(MIN_OPTIONS, MAX_OPTIONS),
            demoConfigured = demoConfiguration != null,
        )
        val validationFailure = when {
            draft.sourceText.isBlank() || draft.sourceText.length > MAX_SOURCE_CHARACTERS ->
                RelayFailureCode.BAD_REQUEST

            demoConfiguration == null -> RelayFailureCode.INVALID_CONFIGURATION
            else -> null
        }
        if (validationFailure != null) {
            mutableState.value = WorkflowState.GenerationError(
                draft = draft,
                code = validationFailure,
                message = validationFailure.safeMessage,
            )
            return
        }

        val configuration = checkNotNull(demoConfiguration)
        val token = ++generationToken
        mutableState.value = WorkflowState.Generating(draft)
        val job = viewModelScope.launch(start = CoroutineStart.LAZY) {
            try {
                val response = generationGateway.generate(
                    configuration = configuration,
                    request = GenerationRequest(
                        sourceText = draft.sourceText,
                        tone = draft.tone,
                        instruction = draft.instruction,
                        optionCount = draft.optionCount,
                    ),
                )
                validateResponse(response, draft.optionCount)
                if (token != generationToken) return@launch
                mutableState.value = WorkflowState.ShowingResults(
                    draft = draft,
                    requestId = response.requestId,
                    options = response.options,
                    selectedOptionId = response.options.firstOrNull()?.id,
                    copiedOptionId = null,
                )
            } catch (cancelled: CancellationException) {
                if (currentCoroutineContext().isActive && token == generationToken) {
                    showGenerationError(draft, RelayFailureCode.PROVIDER_UNAVAILABLE)
                } else {
                    throw cancelled
                }
            } catch (error: RelayException) {
                if (token == generationToken) showGenerationError(draft, error.code)
            } catch (_: Throwable) {
                if (token == generationToken) showGenerationError(draft, RelayFailureCode.INTERNAL)
            }
        }
        generationJob = job
        job.invokeOnCompletion {
            if (generationJob === job) generationJob = null
        }
        job.start()
    }

    private fun validateResponse(response: GenerationResponse, requestedCount: Int) {
        val normalized = response.options.map { option ->
            option.text.trim().lowercase(Locale.ROOT).replace(WHITESPACE, " ")
        }
        val valid = response.requestId.isNotBlank() &&
            response.options.size == requestedCount &&
            response.options.map(GenerationOption::id).distinct().size == response.options.size &&
            response.options.all { it.id.isNotBlank() && it.text.isNotBlank() && it.text.length <= MAX_OPTION_CHARACTERS } &&
            normalized.distinct().size == normalized.size
        if (!valid) throw RelayException(RelayFailureCode.INVALID_RESPONSE)
    }

    private fun showGenerationError(draft: GenerationDraft, code: RelayFailureCode) {
        mutableState.value = WorkflowState.GenerationError(
            draft = draft,
            code = code,
            message = code.safeMessage,
        )
    }

    private fun cancelGenerationWork() {
        generationToken++
        generationJob?.cancel()
        generationJob = null
    }

    private fun cancelRecognition() {
        recognitionGeneration++
        recognitionJob?.cancel()
        recognitionJob = null
    }

    private fun defaultGenerationSettings(): DefaultGenerationSettings {
        val configuration = demoConfiguration
        return DefaultGenerationSettings(
            tone = configuration?.defaultTone ?: Tone.NATURAL,
            optionCount = configuration?.optionCount ?: DEFAULT_OPTION_COUNT,
            configured = configuration != null,
        )
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

    private data class DefaultGenerationSettings(
        val tone: Tone,
        val optionCount: Int,
        val configured: Boolean,
    )

    companion object {
        private const val MIN_OPTIONS = 1
        private const val MAX_OPTIONS = 3
        private const val DEFAULT_OPTION_COUNT = 3
        private const val MAX_SOURCE_CHARACTERS = 8_000
        private const val MAX_INSTRUCTION_CHARACTERS = 500
        private const val MAX_OPTION_CHARACTERS = 700
        private val WHITESPACE = Regex("[\\s\\p{Z}]+")
    }
}

private fun WorkflowState.ReviewingText.toGenerationDraft(): GenerationDraft = GenerationDraft(
    sourceText = text,
    blocks = blocks,
    selection = selection,
    manualEntry = manualEntry,
    canReturnToCrop = canReturnToCrop,
    tone = tone,
    instruction = instruction,
    optionCount = optionCount,
    demoConfigured = demoConfigured,
)

private fun GenerationDraft.toReviewingText(configured: Boolean): WorkflowState.ReviewingText =
    WorkflowState.ReviewingText(
        text = sourceText,
        blocks = blocks,
        selection = selection,
        manualEntry = manualEntry,
        emptyRecognition = false,
        canReturnToCrop = canReturnToCrop,
        tone = tone,
        instruction = instruction,
        optionCount = optionCount,
        demoConfigured = configured,
    )
