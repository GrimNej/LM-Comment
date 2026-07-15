package com.grimnej.lmcomment.workflow

import android.graphics.Bitmap
import com.grimnej.lmcomment.capture.CaptureResourceCounters
import com.grimnej.lmcomment.crop.NormalizedCropRect
import com.grimnej.lmcomment.ocr.OcrEngine
import com.grimnej.lmcomment.ocr.OcrResult
import java.util.concurrent.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    private fun viewModel(
        engine: OcrEngine,
        crops: ArrayDeque<Bitmap> = ArrayDeque(),
    ): WorkflowViewModel = WorkflowViewModel(
        ocrEngine = engine,
        cropBitmapFactory = { _, _: NormalizedCropRect -> crops.removeFirst() },
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
