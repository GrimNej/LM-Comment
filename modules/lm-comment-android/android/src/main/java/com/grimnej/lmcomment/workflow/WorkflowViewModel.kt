package com.grimnej.lmcomment.workflow

import android.graphics.Bitmap
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.grimnej.lmcomment.capture.CaptureResourceCounters

class WorkflowViewModel : ViewModel() {
    private val mutableState = mutableStateOf<WorkflowState>(WorkflowState.CaptureCloak)
    val state: State<WorkflowState> = mutableState

    fun acceptFrame(bitmap: Bitmap) {
        releaseBitmap()
        CaptureResourceCounters.activeWorkflowBitmap.incrementAndGet()
        mutableState.value = WorkflowState.FrameReady(bitmap)
    }

    fun clearSensitiveState() {
        releaseBitmap()
        mutableState.value = WorkflowState.CaptureCloak
    }

    private fun releaseBitmap() {
        val bitmap = (mutableState.value as? WorkflowState.FrameReady)?.bitmap ?: return
        if (!bitmap.isRecycled) bitmap.recycle()
        CaptureResourceCounters.activeWorkflowBitmap.decrementAndGet()
    }

    override fun onCleared() {
        releaseBitmap()
        super.onCleared()
    }
}
