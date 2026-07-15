package com.grimnej.lmcomment.workflow

import android.graphics.Bitmap

sealed interface WorkflowState {
    data object CaptureCloak : WorkflowState
    data class FrameReady(val bitmap: Bitmap) : WorkflowState
}
