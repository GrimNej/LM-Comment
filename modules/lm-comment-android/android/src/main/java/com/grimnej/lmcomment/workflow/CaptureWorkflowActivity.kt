package com.grimnej.lmcomment.workflow

import android.app.Activity
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
import android.view.Choreographer
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.grimnej.lmcomment.bubble.BubbleOverlayService
import com.grimnej.lmcomment.capture.CaptureError
import com.grimnej.lmcomment.capture.OneShotCaptureService

class CaptureWorkflowActivity : ComponentActivity(), OneShotCaptureService.Listener {
    private val viewModel by viewModels<WorkflowViewModel>()
    private var workflowSessionId: String? = null
    private var captureBinder: OneShotCaptureService.LocalBinder? = null
    private var serviceBound = false
    private var cleanupComplete = false
    private var consentLaunched = false

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
        workflowSessionId = intent.getStringExtra(BubbleOverlayService.EXTRA_WORKFLOW_SESSION_ID)
        consentLaunched = savedInstanceState?.getBoolean(STATE_CONSENT_LAUNCHED) == true
        val restoredFrame = viewModel.state.value is WorkflowState.FrameReady
        if (restoredFrame) enterSensitiveWorkflow() else configureCaptureCloak()
        setContent {
            WorkflowScreen(state = viewModel.state.value, onClose = ::finishWorkflow)
        }
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = finishWorkflow()
            },
        )
        if (restoredFrame) {
            // The ViewModel owns the frame across a configuration change. The
            // secure/opaque window was restored before Compose could render it.
        } else if (workflowSessionId == null) {
            finishWorkflow(CaptureError.CAPTURE_FAILED)
        } else {
            bindService(
                Intent(this, OneShotCaptureService::class.java),
                captureConnection,
                Context.BIND_AUTO_CREATE,
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_CONSENT_LAUNCHED, consentLaunched)
        super.onSaveInstanceState(outState)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val incoming = intent.getStringExtra(BubbleOverlayService.EXTRA_WORKFLOW_SESSION_ID)
        if (incoming != workflowSessionId) finishWorkflow(CaptureError.CAPTURE_FAILED)
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
    }

    private fun waitOneFrame(block: () -> Unit) {
        Choreographer.getInstance().postFrameCallback {
            if (!isFinishing && !isDestroyed) block()
        }
    }

    private fun finishWorkflow(error: CaptureError? = null) {
        if (cleanupComplete) return
        cleanupComplete = true
        error?.let {
            getSharedPreferences("lmcomment_diagnostics", MODE_PRIVATE)
                .edit().putString("last_error", it.name).apply()
            Toast.makeText(this, errorMessage(it), Toast.LENGTH_SHORT).show()
        }
        releaseCaptureBinding(cancel = true)
        viewModel.clearSensitiveState()
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

    private companion object {
        const val STATE_CONSENT_LAUNCHED = "consent_launched"
    }
}
