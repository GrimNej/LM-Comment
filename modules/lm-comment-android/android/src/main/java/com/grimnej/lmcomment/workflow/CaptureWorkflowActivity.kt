package com.grimnej.lmcomment.workflow

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.ResultReceiver
import android.provider.Settings
import android.view.Choreographer
import android.view.HapticFeedbackConstants
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.grimnej.lmcomment.bubble.BubbleOverlayService
import com.grimnej.lmcomment.capture.CaptureError
import com.grimnej.lmcomment.capture.OneShotCaptureService
import com.grimnej.lmcomment.config.DemoConfigurationStore
import com.grimnej.lmcomment.diagnostics.StableErrorStore

class CaptureWorkflowActivity : ComponentActivity(), OneShotCaptureService.Listener {
    private val viewModel by viewModels<WorkflowViewModel>()
    private var workflowSessionId: String? = null
    private var captureBinder: OneShotCaptureService.LocalBinder? = null
    private var serviceBound = false
    private var cleanupComplete = false
    private var consentLaunched = false
    private var directManualEntry = false
    private var newCaptureTransitionInProgress = false

    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK || result.data == null) {
            finishWorkflow(CaptureError.PROJECTION_CANCELLED)
            return@registerForActivityResult
        }
        waitOneFrame {
            val sessionId = workflowSessionId ?: return@waitOneFrame finishWorkflow(CaptureError.CAPTURE_FAILED)
            val serviceIntent = OneShotCaptureService.captureIntent(
                this,
                sessionId,
                result.resultCode,
                requireNotNull(result.data),
            )
            ContextCompat.startForegroundService(this, serviceIntent)
        }
    }

    private val captureConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? OneShotCaptureService.LocalBinder
                ?: return finishWorkflow(CaptureError.CAPTURE_SERVICE_DISCONNECTED)
            captureBinder = binder
            serviceBound = true
            val sessionId = workflowSessionId
                ?: return finishWorkflow(CaptureError.CAPTURE_FAILED)
            binder.registerListener(sessionId, this@CaptureWorkflowActivity)
            if (!consentLaunched) requestBubbleHide(sessionId)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            captureBinder = null
            serviceBound = false
            if (viewModel.state.value is WorkflowState.CaptureCloak && !isFinishing) {
                finishWorkflow(CaptureError.CAPTURE_SERVICE_DISCONNECTED)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Configuration is process-local private data supplied by the Expo shell.
        // Hand it directly to the workflow without ever logging the demo token.
        viewModel.setDemoConfiguration(DemoConfigurationStore(this).read())
        workflowSessionId = intent.getStringExtra(BubbleOverlayService.EXTRA_WORKFLOW_SESSION_ID)
        directManualEntry = intent.getBooleanExtra(EXTRA_MANUAL_ENTRY, false)
        consentLaunched = savedInstanceState?.getBoolean(STATE_CONSENT_LAUNCHED) == true
        val restoredSensitiveWorkflow = viewModel.state.value.isSensitive
        val unrecoverableCaptureRestoration =
            savedInstanceState != null &&
                !directManualEntry &&
                !restoredSensitiveWorkflow
        when {
            restoredSensitiveWorkflow -> enterSensitiveWorkflow()
            directManualEntry -> {
                // Secure and opaque before manual source text reaches Compose.
                enterSensitiveWorkflow()
                viewModel.enterManualText(
                    sourceText = intent.getStringExtra(EXTRA_INITIAL_TEXT).orEmpty(),
                    directEntry = true,
                )
            }
            else -> configureCaptureCloak()
        }
        setContent {
            val state = viewModel.state.value
            val generationErrorCode = (state as? WorkflowState.GenerationError)?.code?.name
            LaunchedEffect(generationErrorCode) {
                generationErrorCode?.let { StableErrorStore(this@CaptureWorkflowActivity).record(it) }
            }
            WorkflowScreen(
                state = state,
                actions = WorkflowActions(
                    onSelectionChange = viewModel::updateSelection,
                    onResetSelection = viewModel::resetSelection,
                    onUseFullFrame = viewModel::useFullFrame,
                    onExtractText = { viewModel.extractText() },
                    onExtractFullFrame = { viewModel.extractText(useFullFrame = true) },
                    onTypeText = { viewModel.enterManualText() },
                    onReviewedTextChange = viewModel::updateReviewedText,
                    onBackToCrop = { viewModel.backToCrop() },
                    onRetryOcr = { viewModel.extractText() },
                    onToneChange = viewModel::updateTone,
                    onInstructionChange = viewModel::updateInstruction,
                    onOptionCountChange = viewModel::updateOptionCount,
                    onGenerate = viewModel::generate,
                    onCancelGeneration = viewModel::cancelGeneration,
                    onSelectResult = viewModel::selectResult,
                    onEditResult = viewModel::beginEdit,
                    onEditDraftChange = viewModel::updateEditText,
                    onSaveEdit = viewModel::saveEdit,
                    onCancelEdit = viewModel::cancelEdit,
                    onCopyResult = ::copyResult,
                    onRegenerate = viewModel::regenerate,
                    onBackToReview = viewModel::backToReview,
                    onNewCapture = ::startNewCapture,
                    onClose = { finishWorkflow() },
                ),
            )
        }
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (!viewModel.handleBack()) finishWorkflow()
                }
            },
        )
        if (unrecoverableCaptureRestoration) {
            // MediaProjection consent/result data cannot be reconstructed after
            // process or ViewModel loss. Close instead of leaving an idle cloak.
            finishWorkflow(CaptureError.CAPTURE_FAILED)
        } else if (restoredSensitiveWorkflow) {
            // The ViewModel owns the frame across a configuration change. The
            // secure/opaque window was restored before Compose could render it.
        } else if (directManualEntry) {
            // Direct manual entry never starts or binds the capture service.
        } else if (workflowSessionId == null) {
            finishWorkflow(CaptureError.CAPTURE_FAILED)
        } else {
            bindCaptureService()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_CONSENT_LAUNCHED, consentLaunched)
        super.onSaveInstanceState(outState)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val incoming = intent.getStringExtra(BubbleOverlayService.EXTRA_WORKFLOW_SESSION_ID)
        val incomingManual = intent.getBooleanExtra(EXTRA_MANUAL_ENTRY, false)
        if (incoming != workflowSessionId || incomingManual != directManualEntry) {
            finishWorkflow(CaptureError.CAPTURE_FAILED)
        }
    }

    private fun requestBubbleHide(sessionId: String) {
        val receiver = object : ResultReceiver(Handler(mainLooper)) {
            override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                if (resultCode == BubbleOverlayService.RESULT_BUBBLE_HIDDEN && !isFinishing) {
                    waitOneFrame(::launchProjectionConsent)
                }
            }
        }
        startService(
            Intent(this, BubbleOverlayService::class.java)
                .setAction(BubbleOverlayService.ACTION_HIDE)
                .putExtra(BubbleOverlayService.EXTRA_WORKFLOW_SESSION_ID, sessionId)
                .putExtra(BubbleOverlayService.EXTRA_ACK_RECEIVER, receiver),
        )
    }

    private fun launchProjectionConsent() {
        if (consentLaunched || isFinishing) return
        consentLaunched = true
        val manager = getSystemService(MediaProjectionManager::class.java)
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            manager.createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay())
        } else {
            manager.createScreenCaptureIntent()
        }
        projectionLauncher.launch(intent)
    }

    override fun onFrameReady(sessionId: String) {
        if (sessionId != workflowSessionId || isFinishing) return
        val bitmap = captureBinder?.takeBitmap(sessionId)
            ?: return finishWorkflow(CaptureError.CAPTURE_FAILED)
        newCaptureTransitionInProgress = false
        enterSensitiveWorkflow()
        viewModel.acceptFrame(bitmap)
        releaseCaptureBinding(cancel = false)
    }

    override fun onCaptureError(sessionId: String, error: CaptureError) {
        if (sessionId == workflowSessionId && !isFinishing) finishWorkflow(error)
    }

    private fun configureCaptureCloak() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        window.setDimAmount(0f)
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    private fun enterSensitiveWorkflow() {
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        window.setBackgroundDrawable(ColorDrawable(Color.rgb(9, 11, 16)))
        window.statusBarColor = Color.rgb(9, 11, 16)
        window.navigationBarColor = Color.rgb(9, 11, 16)
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    private fun waitOneFrame(block: () -> Unit) {
        Choreographer.getInstance().postFrameCallback {
            if (!isFinishing && !isDestroyed) block()
        }
    }

    /**
     * Starts a fresh one-shot capture while retaining the bubble service's
     * existing workflow session. Sensitive Compose content is removed while
     * the window is still opaque and secure; only the following frame becomes
     * the transparent, non-secure capture cloak.
     */
    private fun startNewCapture() {
        if (cleanupComplete || newCaptureTransitionInProgress || isFinishing) return
        if (workflowSessionId == null) {
            // Standalone manual mode has no bubble-owned workflow session. Keep
            // capture user-initiated: prepare the bubble, close this secure
            // activity, and let the user choose content before tapping it.
            if (Settings.canDrawOverlays(this)) {
                ContextCompat.startForegroundService(
                    this,
                    Intent(this, BubbleOverlayService::class.java)
                        .setAction(BubbleOverlayService.ACTION_START),
                )
                Toast.makeText(
                    this,
                    "Bubble ready. Open the content, then tap the bubble.",
                    Toast.LENGTH_LONG,
                ).show()
            } else {
                Toast.makeText(
                    this,
                    "Enable Display over other apps to start a new capture.",
                    Toast.LENGTH_LONG,
                ).show()
            }
            finishWorkflow()
            return
        }

        newCaptureTransitionInProgress = true
        directManualEntry = false
        intent.putExtra(EXTRA_MANUAL_ENTRY, false)
        releaseCaptureBinding(cancel = true)
        enterSensitiveWorkflow()
        viewModel.prepareForNewCapture()
        // The first callback runs before the frame that draws CaptureCloak.
        // Waiting for the following callback guarantees one complete opaque,
        // secure frame has disposed the prior results before FLAG_SECURE clears.
        waitOneFrame {
            waitOneFrame {
                consentLaunched = false
                configureCaptureCloak()
                bindCaptureService()
            }
        }
    }

    private fun bindCaptureService() {
        if (cleanupComplete || serviceBound || isFinishing) return
        val bound = runCatching {
            bindService(
                Intent(this, OneShotCaptureService::class.java),
                captureConnection,
                Context.BIND_AUTO_CREATE,
            )
        }.getOrDefault(false)
        if (!bound) finishWorkflow(CaptureError.CAPTURE_SERVICE_DISCONNECTED)
    }

    /** Copies only the exact option text resolved by the ViewModel on a tap. */
    private fun copyResult(optionId: String) {
        val text = viewModel.resultTextForCopy(optionId) ?: return
        val copied = runCatching {
            getSystemService(ClipboardManager::class.java).setPrimaryClip(
                ClipData.newPlainText("LM-Comment option", text),
            )
        }.isSuccess
        if (!copied) return

        val feedback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.VIRTUAL_KEY
        }
        window.decorView.performHapticFeedback(feedback)
        viewModel.markCopied(optionId)
    }

    private fun finishWorkflow(error: CaptureError? = null) {
        if (cleanupComplete) return
        cleanupComplete = true
        error?.let {
            StableErrorStore(this).record(it.name)
            Toast.makeText(this, errorMessage(it), Toast.LENGTH_SHORT).show()
        }
        releaseCaptureBinding(cancel = true)
        viewModel.prepareToClose()
        restoreBubble()
        finish()
        overridePendingTransition(0, 0)
    }

    private fun releaseCaptureBinding(cancel: Boolean) {
        val sessionId = workflowSessionId
        val binder = captureBinder
        if (cancel && sessionId != null) binder?.cancel(sessionId)
        binder?.unregisterListener(this)
        captureBinder = null
        if (serviceBound) {
            runCatching { unbindService(captureConnection) }
            serviceBound = false
        }
    }

    private fun restoreBubble() {
        val sessionId = workflowSessionId ?: return
        startService(
            Intent(this, BubbleOverlayService::class.java)
                .setAction(BubbleOverlayService.ACTION_RESTORE)
                .putExtra(BubbleOverlayService.EXTRA_WORKFLOW_SESSION_ID, sessionId),
        )
    }

    private fun errorMessage(error: CaptureError): String = when (error) {
        CaptureError.PROJECTION_CANCELLED -> "Screen capture cancelled"
        CaptureError.CAPTURE_TIMEOUT -> "The frame took too long. Try again."
        CaptureError.CAPTURE_GEOMETRY_CHANGED -> "The display changed during capture. Try again."
        CaptureError.CAPTURE_BLANK_OR_PROTECTED -> "No usable frame was captured."
        CaptureError.CAPTURE_SERVICE_DISCONNECTED -> "Capture service disconnected."
        CaptureError.PROJECTION_STOPPED,
        CaptureError.CAPTURE_FAILED,
        -> "Could not capture a frame. Try again."
    }

    override fun onDestroy() {
        if (isFinishing && !cleanupComplete) finishWorkflow()
        if (!isFinishing) releaseCaptureBinding(cancel = false)
        super.onDestroy()
    }

    companion object {
        const val EXTRA_MANUAL_ENTRY = "com.grimnej.lmcomment.extra.MANUAL_ENTRY"
        const val EXTRA_INITIAL_TEXT = "com.grimnej.lmcomment.extra.INITIAL_TEXT"
        const val STATE_CONSENT_LAUNCHED = "consent_launched"
    }
}
