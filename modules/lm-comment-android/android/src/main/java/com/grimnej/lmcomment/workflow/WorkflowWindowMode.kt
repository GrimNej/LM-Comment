package com.grimnej.lmcomment.workflow

sealed interface WorkflowWindowMode {
    data object CaptureCloak : WorkflowWindowMode
    data object SensitiveWorkflow : WorkflowWindowMode
}
