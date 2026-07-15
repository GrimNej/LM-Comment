package com.grimnej.lmcomment.workflow

import android.graphics.Bitmap
import com.grimnej.lmcomment.capture.CaptureResourceCounters
import com.grimnej.lmcomment.config.DemoConfiguration
import com.grimnej.lmcomment.config.Tone
import com.grimnej.lmcomment.crop.NormalizedCropRect
import com.grimnej.lmcomment.ocr.OcrEngine
import com.grimnej.lmcomment.ocr.OcrResult
import com.grimnej.lmcomment.relay.GenerationOption
import com.grimnej.lmcomment.relay.GenerationRequest
import com.grimnej.lmcomment.relay.GenerationResponse
import com.grimnej.lmcomment.relay.RelayException
import com.grimnej.lmcomment.relay.RelayFailureCode
import java.util.concurrent.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

@OptIn(ExperimentalCoroutinesApi::class)
class WorkflowViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        CaptureResourceCounters.activeWorkflowBitmap.set(0)
    }

    @After
    fun tearDown() {
        CaptureResourceCounters.activeWorkflowBitmap.set(0)
        Dispatchers.resetMain()
    }

    @Test
    fun directManualEntryClosesInsteadOfReturningToMissingCrop() {
        val viewModel = viewModel(engine = FakeOcrEngine { OcrResult("", emptyList()) })

        viewModel.enterManualText(sourceText = "Editable", directEntry = true)

        val state = viewModel.state.value as WorkflowState.ReviewingText
        assertTrue(state.manualEntry)
        assertFalse(state.canReturnToCrop)
        assertFalse(viewModel.handleBack())
        clear(viewModel)
    }

    @Test
    fun emptyRecognitionShowsRecoveryAndReleasesOnlyTemporaryCrop() = runTest(dispatcher) {
        val frame = bitmap()
        val crop = bitmap()
        val viewModel = viewModel(
            engine = FakeOcrEngine { OcrResult("", emptyList()) },
            crops = ArrayDeque(listOf(crop)),
        )

        viewModel.acceptFrame(frame)
        viewModel.extractText()
        advanceUntilIdle()

        val review = viewModel.state.value as WorkflowState.ReviewingText
        assertTrue(review.emptyRecognition)
        assertTrue(review.canReturnToCrop)
        assertEquals(1, CaptureResourceCounters.activeWorkflowBitmap.get())
        verify(crop, times(1)).recycle()

        assertTrue(viewModel.handleBack())
        assertTrue(viewModel.state.value is WorkflowState.Cropping)
        clear(viewModel)
        assertEquals(0, CaptureResourceCounters.activeWorkflowBitmap.get())
        verify(frame, times(1)).recycle()
    }

    @Test
    fun secondExtractionCancelsFirstAndDoesNotLeakEitherCrop() = runTest(dispatcher) {
        val frame = bitmap()
        val firstCrop = bitmap()
        val secondCrop = bitmap()
        val firstResult = CompletableDeferred<OcrResult>()
        var calls = 0
        val viewModel = viewModel(
            engine = FakeOcrEngine {
                calls++
                if (calls == 1) firstResult.await() else OcrResult("Second result", listOf("Second result"))
            },
            crops = ArrayDeque(listOf(firstCrop, secondCrop)),
        )

        viewModel.acceptFrame(frame)
        viewModel.extractText()
        runCurrent()
        assertEquals(2, CaptureResourceCounters.activeWorkflowBitmap.get())

        viewModel.extractText()
        advanceUntilIdle()

        val review = viewModel.state.value as WorkflowState.ReviewingText
        assertEquals("Second result", review.text)
        assertEquals(1, CaptureResourceCounters.activeWorkflowBitmap.get())
        verify(firstCrop, times(1)).recycle()
        verify(secondCrop, times(1)).recycle()
        clear(viewModel)
        assertEquals(0, CaptureResourceCounters.activeWorkflowBitmap.get())
    }

    @Test
    fun providerSideCancellationBecomesRecoverableOcrError() = runTest(dispatcher) {
        val frame = bitmap()
        val crop = bitmap()
        val viewModel = viewModel(
            engine = FakeOcrEngine { throw CancellationException("provider cancelled") },
            crops = ArrayDeque(listOf(crop)),
        )

        viewModel.acceptFrame(frame)
        viewModel.extractText()
        advanceUntilIdle()

        assertTrue(viewModel.state.value is WorkflowState.OcrError)
        assertEquals(1, CaptureResourceCounters.activeWorkflowBitmap.get())
        verify(crop, times(1)).recycle()
        clear(viewModel)
        assertEquals(0, CaptureResourceCounters.activeWorkflowBitmap.get())
    }

    @Test
    fun closeDuringRecognitionCancelsWorkAndReturnsCountersToZero() = runTest(dispatcher) {
        val frame = bitmap()
        val crop = bitmap()
        val pending = CompletableDeferred<OcrResult>()
        val viewModel = viewModel(
            engine = FakeOcrEngine { pending.await() },
            crops = ArrayDeque(listOf(crop)),
        )

        viewModel.acceptFrame(frame)
        viewModel.extractText()
        runCurrent()
        assertEquals(2, CaptureResourceCounters.activeWorkflowBitmap.get())

        viewModel.prepareToClose()
        advanceUntilIdle()

        assertTrue(viewModel.state.value is WorkflowState.Closing)
        assertEquals(1, CaptureResourceCounters.activeWorkflowBitmap.get())
        verify(crop, times(1)).recycle()
        clear(viewModel)
        assertEquals(0, CaptureResourceCounters.activeWorkflowBitmap.get())
        verify(frame, times(1)).recycle()
    }

    @Test
    fun tenRepeatedCropRecognitionCyclesReleaseEveryTemporaryBitmap() = runTest(dispatcher) {
        val frame = bitmap()
        val crops = List(10) { bitmap() }
        var recognitionCount = 0
        val viewModel = viewModel(
            engine = FakeOcrEngine {
                recognitionCount += 1
                OcrResult("Result $recognitionCount", listOf("Result $recognitionCount"))
            },
            crops = ArrayDeque(crops),
        )

        viewModel.acceptFrame(frame)
        repeat(10) { cycle ->
            viewModel.extractText()
            advanceUntilIdle()

            val review = viewModel.state.value as WorkflowState.ReviewingText
            assertEquals("Result ${cycle + 1}", review.text)
            assertEquals(1, CaptureResourceCounters.activeWorkflowBitmap.get())
            assertTrue(viewModel.backToCrop())
        }

        assertEquals(10, recognitionCount)
        crops.forEach { crop -> verify(crop, times(1)).recycle() }
        clear(viewModel)
        assertEquals(0, CaptureResourceCounters.activeWorkflowBitmap.get())
        verify(frame, times(1)).recycle()
    }

    @Test
    fun generationSendsReviewedFieldsAndReturnsExactlyOneToThreeOptions() = runTest(dispatcher) {
        val requests = mutableListOf<GenerationRequest>()
        var requestSequence = 0
        val viewModel = viewModel(
            engine = FakeOcrEngine { OcrResult("", emptyList()) },
            gateway = GenerationGateway { _, request ->
                requests += request
                requestSequence += 1
                response(request.optionCount, requestSequence)
            },
        )
        viewModel.setDemoConfiguration(configuration(optionCount = 1))
        viewModel.enterManualText("Reviewed source exactly", directEntry = true)
        viewModel.updateTone(Tone.PROFESSIONAL)
        viewModel.updateInstruction("Keep the concrete detail")

        (1..3).forEach { count ->
            viewModel.updateOptionCount(count)
            viewModel.generate()
            advanceUntilIdle()

            val results = viewModel.state.value as WorkflowState.ShowingResults
            assertEquals(count, results.options.size)
            assertEquals(count, results.draft.optionCount)
            assertEquals("Reviewed source exactly", requests.last().sourceText)
            assertEquals(Tone.PROFESSIONAL, requests.last().tone)
            assertEquals("Keep the concrete detail", requests.last().instruction)
            assertEquals(count, requests.last().optionCount)
            if (count < 3) assertTrue(viewModel.backToReview())
        }

        assertEquals(listOf(1, 2, 3), requests.map(GenerationRequest::optionCount))
        clear(viewModel)
    }

    @Test
    fun duplicateGenerateIsIgnoredAndCancelRejectsLateResponse() = runTest(dispatcher) {
        val pending = CompletableDeferred<GenerationResponse>()
        var calls = 0
        val viewModel = configuredManualViewModel(
            gateway = GenerationGateway { _, _ ->
                calls += 1
                withContext(NonCancellable) { pending.await() }
            },
        )

        viewModel.generate()
        viewModel.generate()
        runCurrent()
        assertEquals(1, calls)
        assertTrue(viewModel.state.value is WorkflowState.Generating)

        viewModel.cancelGeneration()
        assertTrue(viewModel.state.value is WorkflowState.ReviewingText)
        pending.complete(response(3, 1))
        advanceUntilIdle()

        assertEquals(1, calls)
        assertTrue(viewModel.state.value is WorkflowState.ReviewingText)
        clear(viewModel)
    }

    @Test
    fun relayFailuresExposeOnlySafeMappedErrors() = runTest(dispatcher) {
        listOf(
            RelayFailureCode.NETWORK_TIMEOUT,
            RelayFailureCode.UNAUTHORIZED,
            RelayFailureCode.PROVIDER_UNAVAILABLE,
        ).forEach { code ->
            val viewModel = configuredManualViewModel(
                gateway = GenerationGateway { _, _ -> throw RelayException(code, "opaque-request") },
            )

            viewModel.generate()
            advanceUntilIdle()

            val error = viewModel.state.value as WorkflowState.GenerationError
            assertEquals(code, error.code)
            assertEquals(code.safeMessage, error.message)
            assertFalse(error.message.contains("opaque-request"))
            clear(viewModel)
        }
    }

    @Test
    fun editIsBoundedRejectsBlankAndClearsStaleCopiedMarker() = runTest(dispatcher) {
        val viewModel = configuredManualViewModel(
            gateway = GenerationGateway { _, request -> response(request.optionCount, 1) },
        )
        viewModel.generate()
        advanceUntilIdle()
        viewModel.markCopied("option-1-1")
        viewModel.beginEdit("option-1-1")

        viewModel.updateEditText("   ")
        viewModel.saveEdit()
        assertTrue(viewModel.state.value is WorkflowState.EditingResult)

        viewModel.updateEditText("x".repeat(701))
        val editing = viewModel.state.value as WorkflowState.EditingResult
        assertEquals(700, editing.draftText.length)
        viewModel.saveEdit()

        val results = viewModel.state.value as WorkflowState.ShowingResults
        assertEquals(700, results.options.first { it.id == "option-1-1" }.text.length)
        assertNull(results.copiedOptionId)
        clear(viewModel)
    }

    @Test
    fun exactVisibleEditedTextIsReturnedAndMarkedCopied() = runTest(dispatcher) {
        val viewModel = configuredManualViewModel(
            gateway = GenerationGateway { _, request -> response(request.optionCount, 1) },
        )
        viewModel.generate()
        advanceUntilIdle()
        viewModel.beginEdit("option-1-2")
        val exactText = "  A deliberately edited reply.\nSecond line.  "
        viewModel.updateEditText(exactText)

        assertEquals(exactText, viewModel.resultTextForCopy("option-1-2"))
        viewModel.saveEdit()
        assertEquals(exactText, viewModel.resultTextForCopy("option-1-2"))
        viewModel.markCopied("option-1-2")

        val results = viewModel.state.value as WorkflowState.ShowingResults
        assertEquals("option-1-2", results.selectedOptionId)
        assertEquals("option-1-2", results.copiedOptionId)
        assertEquals(exactText, viewModel.resultTextForCopy("option-1-2"))
        clear(viewModel)
    }

    @Test
    fun regenerateReusesApprovedDraftAndCompletesTenResultCycles() = runTest(dispatcher) {
        val requests = mutableListOf<GenerationRequest>()
        val viewModel = configuredManualViewModel(
            configuration = configuration(optionCount = 2),
            gateway = GenerationGateway { _, request ->
                requests += request
                response(request.optionCount, requests.size)
            },
        )
        viewModel.updateTone(Tone.WITTY)
        viewModel.updateInstruction("Use one understated joke")
        viewModel.generate()

        repeat(10) { cycle ->
            advanceUntilIdle()
            val results = viewModel.state.value as WorkflowState.ShowingResults
            assertEquals("request-${cycle + 1}", results.requestId)
            assertEquals(2, results.options.size)
            if (cycle < 9) viewModel.regenerate()
        }

        assertEquals(10, requests.size)
        requests.forEach { request ->
            assertEquals("Source approved by the user", request.sourceText)
            assertEquals(Tone.WITTY, request.tone)
            assertEquals("Use one understated joke", request.instruction)
            assertEquals(2, request.optionCount)
        }
        assertEquals(0, CaptureResourceCounters.activeWorkflowBitmap.get())
        clear(viewModel)
    }

    @Test
    fun invalidProviderResultBecomesSafeError() = runTest(dispatcher) {
        val viewModel = configuredManualViewModel(
            gateway = GenerationGateway { _, _ ->
                GenerationResponse(
                    requestId = "request-invalid",
                    options = listOf(
                        GenerationOption("one", "Same\u2003text"),
                        GenerationOption("two", "same text"),
                        GenerationOption("three", "Different"),
                    ),
                )
            },
        )

        viewModel.generate()
        advanceUntilIdle()

        val error = viewModel.state.value as WorkflowState.GenerationError
        assertEquals(RelayFailureCode.INVALID_RESPONSE, error.code)
        assertEquals(RelayFailureCode.INVALID_RESPONSE.safeMessage, error.message)
        clear(viewModel)
    }

    @Test
    fun closeCancelsNetworkAndClearsGeneratedTextState() = runTest(dispatcher) {
        val pending = CompletableDeferred<GenerationResponse>()
        val viewModel = configuredManualViewModel(
            gateway = GenerationGateway { _, _ ->
                withContext(NonCancellable) { pending.await() }
            },
        )
        viewModel.generate()
        runCurrent()

        viewModel.prepareToClose()
        assertTrue(viewModel.state.value is WorkflowState.Closing)
        pending.complete(response(3, 1))
        advanceUntilIdle()

        assertTrue(viewModel.state.value is WorkflowState.Closing)
        assertEquals(0, CaptureResourceCounters.activeWorkflowBitmap.get())
        clear(viewModel)
    }

    @Test
    fun newCaptureReleasesFrameAndAllSensitiveWorkflowState() = runTest(dispatcher) {
        val frame = bitmap()
        val viewModel = viewModel(
            engine = FakeOcrEngine { OcrResult("", emptyList()) },
            gateway = GenerationGateway { _, request -> response(request.optionCount, 1) },
        )
        viewModel.setDemoConfiguration(configuration())
        viewModel.acceptFrame(frame)
        viewModel.enterManualText("Sensitive approved source")
        viewModel.generate()
        advanceUntilIdle()

        viewModel.prepareForNewCapture()

        assertTrue(viewModel.state.value is WorkflowState.CaptureCloak)
        assertEquals(0, CaptureResourceCounters.activeWorkflowBitmap.get())
        verify(frame, times(1)).recycle()
        clear(viewModel)
        verify(frame, times(1)).recycle()
    }

    @Test
    fun unchangedConfigurationDoesNotCancelGenerationOrResetReviewControls() = runTest(dispatcher) {
        val config = configuration(optionCount = 2)
        val pending = CompletableDeferred<GenerationResponse>()
        val viewModel = configuredManualViewModel(
            configuration = config,
            gateway = GenerationGateway { _, _ -> pending.await() },
        )
        viewModel.updateTone(Tone.CONCISE)
        viewModel.updateOptionCount(1)
        viewModel.setDemoConfiguration(config)
        val review = viewModel.state.value as WorkflowState.ReviewingText
        assertEquals(Tone.CONCISE, review.tone)
        assertEquals(1, review.optionCount)

        viewModel.generate()
        runCurrent()
        viewModel.setDemoConfiguration(config)
        assertTrue(viewModel.state.value is WorkflowState.Generating)
        pending.complete(response(1, 1))
        advanceUntilIdle()
        assertTrue(viewModel.state.value is WorkflowState.ShowingResults)
        clear(viewModel)
    }

    private fun viewModel(
        engine: OcrEngine,
        crops: ArrayDeque<Bitmap> = ArrayDeque(),
        gateway: GenerationGateway = GenerationGateway { _, _ ->
            throw AssertionError("Unexpected generation request")
        },
    ): WorkflowViewModel = WorkflowViewModel(
        ocrEngine = engine,
        cropBitmapFactory = { _, _: NormalizedCropRect -> crops.removeFirst() },
        generationGateway = gateway,
    )

    private fun configuredManualViewModel(
        configuration: DemoConfiguration = configuration(),
        gateway: GenerationGateway,
    ): WorkflowViewModel = viewModel(
        engine = FakeOcrEngine { OcrResult("", emptyList()) },
        gateway = gateway,
    ).also { viewModel ->
        viewModel.setDemoConfiguration(configuration)
        viewModel.enterManualText("Source approved by the user", directEntry = true)
    }

    private fun configuration(optionCount: Int = 3): DemoConfiguration = DemoConfiguration(
        relayBaseUrl = "https://relay.example",
        demoToken = "demo-token-12345",
        defaultTone = Tone.NATURAL,
        optionCount = optionCount,
        demoMode = true,
    )

    private fun response(optionCount: Int, sequence: Int): GenerationResponse = GenerationResponse(
        requestId = "request-$sequence",
        options = (1..optionCount).map { index ->
            GenerationOption(
                id = "option-$sequence-$index",
                text = "Result $sequence option $index",
            )
        },
    )

    private fun bitmap(): Bitmap = mock(Bitmap::class.java).also { bitmap ->
        `when`(bitmap.isRecycled).thenReturn(false)
    }

    private fun clear(viewModel: WorkflowViewModel) {
        WorkflowViewModel::class.java.getDeclaredMethod("onCleared").apply {
            isAccessible = true
            invoke(viewModel)
        }
    }

    private class FakeOcrEngine(
        private val block: suspend (Bitmap) -> OcrResult,
    ) : OcrEngine {
        override suspend fun recognize(bitmap: Bitmap, rotationDegrees: Int): OcrResult = block(bitmap)
        override fun close() = Unit
    }
}
