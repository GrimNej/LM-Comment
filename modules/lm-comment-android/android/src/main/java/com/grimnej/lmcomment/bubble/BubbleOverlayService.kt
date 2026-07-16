package com.grimnej.lmcomment.bubble

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ResultReceiver
import android.provider.Settings
import androidx.core.app.ServiceCompat
import com.grimnej.lmcomment.LmCommentContract
import com.grimnej.lmcomment.workflow.CaptureWorkflowActivity
import java.util.UUID

class BubbleOverlayService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var bubbleWindow: BubbleWindow? = null
    private var activeWorkflowSessionId: String? = null
    private val hardStop = Runnable { stopBubble() }

    override fun onCreate() {
        super.onCreate()
        instance = this
        bubbleWindow = BubbleWindow(this, ::launchWorkflow, ::stopBubble)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action ?: ACTION_START) {
            ACTION_STOP -> stopBubble()
            ACTION_HIDE -> intent?.let(::hideForWorkflow)
            ACTION_RESTORE -> intent?.let(::restoreAfterWorkflow)
            ACTION_LAUNCH_MANUAL -> intent?.let(::launchManualWorkflow)
            ACTION_RESET_POSITION -> bubbleWindow?.resetPosition()
            ACTION_APPEARANCE_CHANGED -> bubbleWindow?.refreshAppearance()
            ACTION_START -> startBubble()
        }
        if (!isRunning) stopSelf(startId)
        return START_NOT_STICKY
    }

    private fun startBubble() {
        if (!Settings.canDrawOverlays(this)) {
            if (isRunning) stopBubble() else stopSelf()
            return
        }
        if (isRunning) {
            if (bubbleWindow?.show() != true) stopBubble()
            return
        }
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(
            this,
            LmCommentContract.BUBBLE_NOTIFICATION_ID,
            BubbleNotification.create(this),
            type,
        )
        if (bubbleWindow?.show() != true) {
            stopBubble()
            return
        }
        isRunning = true
        handler.removeCallbacks(hardStop)
        handler.postDelayed(hardStop, SESSION_HARD_LIMIT_MS)
    }

    private fun launchWorkflow() {
        if (activeWorkflowSessionId != null) return
        val sessionId = UUID.randomUUID().toString()
        activeWorkflowSessionId = sessionId
        val intent = Intent(this, CaptureWorkflowActivity::class.java)
            .putExtra(EXTRA_WORKFLOW_SESSION_ID, sessionId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
        runCatching { startActivity(intent) }.onFailure {
            activeWorkflowSessionId = null
            if (isRunning && bubbleWindow?.show() != true) stopBubble()
        }
    }

    private fun launchManualWorkflow(intent: Intent) {
        if (!isRunning || activeWorkflowSessionId != null) return
        val sessionId = UUID.randomUUID().toString()
        activeWorkflowSessionId = sessionId
        if (bubbleWindow?.hide() != true) {
            activeWorkflowSessionId = null
            stopBubble()
            return
        }
        runCatching {
            startActivity(
                Intent(this, CaptureWorkflowActivity::class.java)
                    .putExtra(EXTRA_WORKFLOW_SESSION_ID, sessionId)
                    .putExtra(CaptureWorkflowActivity.EXTRA_MANUAL_ENTRY, true)
                    .putExtra(
                        CaptureWorkflowActivity.EXTRA_INITIAL_TEXT,
                        intent.getStringExtra(CaptureWorkflowActivity.EXTRA_INITIAL_TEXT).orEmpty(),
                    )
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION),
            )
        }.onFailure {
            activeWorkflowSessionId = null
            if (isRunning && bubbleWindow?.show() != true) stopBubble()
        }
    }

    private fun hideForWorkflow(intent: Intent) {
        val sessionId = intent.getStringExtra(EXTRA_WORKFLOW_SESSION_ID)
        if (sessionId == null || sessionId != activeWorkflowSessionId) return
        val result = if (bubbleWindow?.hide() == true) {
            RESULT_BUBBLE_HIDDEN
        } else {
            RESULT_BUBBLE_HIDE_FAILED
        }
        intent.resultReceiver()?.send(result, null)
    }

    private fun restoreAfterWorkflow(intent: Intent) {
        val sessionId = intent.getStringExtra(EXTRA_WORKFLOW_SESSION_ID)
        if (sessionId == null || sessionId != activeWorkflowSessionId) return
        activeWorkflowSessionId = null
        if (isRunning && bubbleWindow?.show() != true) stopBubble()
    }

    private fun stopBubble() {
        handler.removeCallbacks(hardStop)
        activeWorkflowSessionId = null
        bubbleWindow?.hide()
        isRunning = false
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        bubbleWindow?.onConfigurationChanged(newConfig)
    }

    override fun onDestroy() {
        handler.removeCallbacks(hardStop)
        bubbleWindow?.hide()
        bubbleWindow = null
        isRunning = false
        instance = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @Suppress("DEPRECATION")
    private fun Intent.resultReceiver(): ResultReceiver? = if (Build.VERSION.SDK_INT >= 33) {
        getParcelableExtra(EXTRA_ACK_RECEIVER, ResultReceiver::class.java)
    } else {
        getParcelableExtra(EXTRA_ACK_RECEIVER)
    }

    companion object {
        const val ACTION_START = "com.grimnej.lmcomment.action.START_BUBBLE"
        const val ACTION_STOP = "com.grimnej.lmcomment.action.STOP_BUBBLE"
        const val ACTION_HIDE = "com.grimnej.lmcomment.action.HIDE_BUBBLE"
        const val ACTION_RESTORE = "com.grimnej.lmcomment.action.RESTORE_BUBBLE"
        const val ACTION_LAUNCH_MANUAL = "com.grimnej.lmcomment.action.LAUNCH_MANUAL"
        const val ACTION_RESET_POSITION = "com.grimnej.lmcomment.action.RESET_BUBBLE_POSITION"
        const val ACTION_APPEARANCE_CHANGED = "com.grimnej.lmcomment.action.APPEARANCE_CHANGED"
        const val EXTRA_WORKFLOW_SESSION_ID = "workflow_session_id"
        const val EXTRA_ACK_RECEIVER = "ack_receiver"
        const val RESULT_BUBBLE_HIDDEN = 1
        const val RESULT_BUBBLE_HIDE_FAILED = 2
        private const val SESSION_HARD_LIMIT_MS = 45L * 60L * 1000L

        @Volatile
        var isRunning: Boolean = false
            private set

        @Volatile
        private var instance: BubbleOverlayService? = null
    }
}
