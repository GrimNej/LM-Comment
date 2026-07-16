package com.grimnej.lmcomment.capture

import android.app.Activity
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.view.WindowManager
import androidx.core.app.ServiceCompat
import com.grimnej.lmcomment.LmCommentContract
import java.util.concurrent.atomic.AtomicBoolean

class OneShotCaptureService : Service() {
    interface Listener {
        fun onFrameReady(sessionId: String)
        fun onCaptureError(sessionId: String, error: CaptureError)
    }

    inner class LocalBinder : Binder() {
        fun registerListener(sessionId: String, listener: Listener) {
            this@OneShotCaptureService.listenerSessionId = sessionId
            this@OneShotCaptureService.listener = listener
            if (pendingSessionId == sessionId && pendingBitmap != null) {
                mainHandler.post { listener.onFrameReady(sessionId) }
            } else {
                dispatchPendingError()
            }
        }

        fun unregisterListener(listener: Listener) {
            if (this@OneShotCaptureService.listener === listener) {
                this@OneShotCaptureService.listener = null
                listenerSessionId = null
            }
        }

        fun takeBitmap(sessionId: String): Bitmap? = takePendingBitmap(sessionId)

        fun cancel(sessionId: String) {
            if (activeSessionId == sessionId) fail(CaptureError.PROJECTION_STOPPED)
            if (pendingSessionId == sessionId) recyclePendingBitmap()
        }
    }

    private val binder = LocalBinder()
    private val mainHandler = Handler(android.os.Looper.getMainLooper())
    private lateinit var captureThread: HandlerThread
    private lateinit var captureHandler: Handler
    private var listener: Listener? = null
    private var listenerSessionId: String? = null
    private var activeSessionId: String? = null
    private var pendingSessionId: String? = null
    private var pendingBitmap: Bitmap? = null
    private var pendingErrorSessionId: String? = null
    private var pendingError: CaptureError? = null
    private var captureResources: CaptureSessionResources? = null
    private var dimensions: CaptureDimensions? = null
    private var geometryGeneration = 0
    private var resizeAttempts = 0
    private var blankFrames = 0
    private val compositorWarmupGate = CompositorWarmupGate()
    private var terminal = AtomicBoolean(false)
    private val captureTimeout = Runnable { fail(CaptureError.CAPTURE_TIMEOUT) }
    private val resultTimeout = Runnable { recyclePendingBitmap() }

    override fun onCreate() {
        super.onCreate()
        CaptureResourceCounters.activeCaptureService.incrementAndGet()
        captureThread = HandlerThread("lmcomment-one-shot-capture").apply { start() }
        captureHandler = Handler(captureThread.looper)
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != ACTION_CAPTURE) return START_NOT_STICKY
        val foregroundType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
        } else {
            0
        }
        ServiceCompat.startForeground(
            this,
            LmCommentContract.CAPTURE_NOTIFICATION_ID,
            CaptureNotification.create(this),
            foregroundType,
        )

        val sessionId = intent.getStringExtra(EXTRA_SESSION_ID)
        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
        val resultData = intent.resultData()
        if (sessionId == null || resultCode != Activity.RESULT_OK || resultData == null) {
            stopCaptureForeground()
            sessionId?.let { notifyError(it, CaptureError.CAPTURE_FAILED) }
            return START_NOT_STICKY
        }
        captureHandler.post { beginCapture(sessionId, resultCode, resultData) }
        return START_NOT_STICKY
    }

    private fun beginCapture(sessionId: String, resultCode: Int, resultData: Intent) {
        if (activeSessionId != null || pendingBitmap != null) {
            notifyError(sessionId, CaptureError.CAPTURE_FAILED)
            stopCaptureForeground()
            return
        }
        activeSessionId = sessionId
        terminal = AtomicBoolean(false)
        geometryGeneration = 0
        resizeAttempts = 0
        blankFrames = 0
        compositorWarmupGate.reset()
        val owned = CaptureSessionResources()
        captureResources = owned
        try {
            val manager = getSystemService(MediaProjectionManager::class.java)
            val projection = manager.getMediaProjection(resultCode, resultData)
                ?: error("Android did not return a MediaProjection instance.")
            owned.projection = projection
            val initial = initialDimensions()
            dimensions = initial
            val callback = projectionCallback()
            owned.projectionCallback = callback
            projection.registerCallback(callback, captureHandler)
            val reader = ImageReader.newInstance(
                initial.width,
                initial.height,
                PixelFormat.RGBA_8888,
                1,
            )
            owned.replaceReader(reader, reader.surface)
            attachImageListener(reader, geometryGeneration)
            owned.virtualDisplay = projection.createVirtualDisplay(
                "LMCommentOneShot",
                initial.width,
                initial.height,
                initial.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface,
                null,
                captureHandler,
            )
            captureHandler.removeCallbacks(captureTimeout)
            captureHandler.postDelayed(captureTimeout, CAPTURE_TIMEOUT_MS)
        } catch (_: Throwable) {
            fail(CaptureError.CAPTURE_FAILED)
        }
    }

    private fun projectionCallback() = object : MediaProjection.Callback() {
        override fun onStop() {
            if (!terminal.get()) fail(CaptureError.PROJECTION_STOPPED)
        }

        override fun onCapturedContentResize(width: Int, height: Int) {
            if (terminal.get() || width <= 0 || height <= 0) return
            val current = dimensions ?: return
            val target = CaptureGeometry.bounded(width, height, current.densityDpi)
            if (target.width == current.width && target.height == current.height) return
            if (resizeAttempts >= 1) {
                fail(CaptureError.CAPTURE_GEOMETRY_CHANGED)
                return
            }
            resizeAttempts++
            replaceCaptureGeometry(target)
        }
    }

    private fun replaceCaptureGeometry(target: CaptureDimensions) {
        var newReader: ImageReader? = null
        var adopted = false
        try {
            val owned = captureResources ?: return
            val display = owned.virtualDisplay ?: return
            newReader = ImageReader.newInstance(
                target.width,
                target.height,
                PixelFormat.RGBA_8888,
                1,
            )
            geometryGeneration++
            attachImageListener(newReader, geometryGeneration)
            display.resize(target.width, target.height, target.densityDpi)
            display.surface = newReader.surface
            owned.replaceReader(newReader, newReader.surface)
            adopted = true
            dimensions = target
        } catch (_: Throwable) {
            if (!adopted) {
                runCatching { newReader?.setOnImageAvailableListener(null, null) }
                runCatching { newReader?.close() }
            }
            fail(CaptureError.CAPTURE_GEOMETRY_CHANGED)
        }
    }

    private fun attachImageListener(reader: ImageReader, generation: Int) {
        reader.setOnImageAvailableListener({ available ->
            if (generation != geometryGeneration || terminal.get()) {
                runCatching { available.acquireLatestImage()?.close() }
                return@setOnImageAvailableListener
            }
            val image = runCatching { available.acquireLatestImage() }.getOrNull()
                ?: return@setOnImageAvailableListener
            val owned = captureResources
            val expected = dimensions
            if (owned == null || expected == null) {
                image.close()
                return@setOnImageAvailableListener
            }
            owned.image = image
            if (!compositorWarmupGate.shouldConvertNextFrame()) {
                image.close()
                CaptureResourceCounters.activeImage.decrementAndGet()
                owned.image = null
                return@setOnImageAvailableListener
            }
            try {
                val bitmap = FrameConverter.toBitmap(image, expected.width, expected.height)
                image.close()
                CaptureResourceCounters.activeImage.decrementAndGet()
                owned.image = null
                if (FrameConverter.looksBlank(bitmap)) {
                    bitmap.recycle()
                    blankFrames++
                    if (blankFrames >= MAX_BLANK_FRAMES) {
                        fail(CaptureError.CAPTURE_BLANK_OR_PROTECTED)
                    }
                    return@setOnImageAvailableListener
                }
                owned.bitmap = bitmap
                succeed()
            } catch (_: Throwable) {
                runCatching { image.close() }
                if (owned.image != null) CaptureResourceCounters.activeImage.decrementAndGet()
                owned.image = null
                fail(CaptureError.CAPTURE_FAILED)
            }
        }, captureHandler)
    }

    private fun succeed() {
        if (!terminal.compareAndSet(false, true)) return
        captureHandler.removeCallbacks(captureTimeout)
        val sessionId = activeSessionId
        val owned = captureResources
        if (sessionId == null || owned == null) {
            completeFailure(CaptureError.CAPTURE_FAILED)
            return
        }
        val bitmap = runCatching { owned.takeBitmap() }.getOrElse {
            completeFailure(CaptureError.CAPTURE_FAILED)
            return
        }
        pendingSessionId = sessionId
        pendingBitmap = bitmap
        activeSessionId = null
        captureResources = null
        owned.close()
        mainHandler.post {
            if (listenerSessionId == sessionId) listener?.onFrameReady(sessionId)
        }
        mainHandler.removeCallbacks(resultTimeout)
        mainHandler.postDelayed(resultTimeout, RESULT_CONSUMPTION_TIMEOUT_MS)
        leaveCaptureForeground()
    }

    private fun fail(error: CaptureError) {
        if (!terminal.compareAndSet(false, true)) return
        completeFailure(error)
    }

    private fun completeFailure(error: CaptureError) {
        captureHandler.removeCallbacks(captureTimeout)
        val sessionId = activeSessionId
        activeSessionId = null
        captureResources?.close()
        captureResources = null
        if (sessionId != null) notifyError(sessionId, error)
        mainHandler.removeCallbacks(resultTimeout)
        mainHandler.postDelayed(resultTimeout, RESULT_CONSUMPTION_TIMEOUT_MS)
        leaveCaptureForeground()
    }

    @Synchronized
    private fun notifyError(sessionId: String, error: CaptureError) {
        pendingErrorSessionId = sessionId
        pendingError = error
        dispatchPendingError()
    }

    private fun dispatchPendingError() {
        mainHandler.post {
            val sessionId: String
            val error: CaptureError
            val callback: Listener
            synchronized(this@OneShotCaptureService) {
                sessionId = pendingErrorSessionId ?: return@post
                error = pendingError ?: return@post
                if (listenerSessionId != sessionId) return@post
                callback = listener ?: return@post
                pendingErrorSessionId = null
                pendingError = null
            }
            callback.onCaptureError(sessionId, error)
            stopSelf()
        }
    }

    @Synchronized
    private fun takePendingBitmap(sessionId: String): Bitmap? {
        if (pendingSessionId != sessionId) return null
        mainHandler.removeCallbacks(resultTimeout)
        return pendingBitmap.also {
            pendingBitmap = null
            pendingSessionId = null
            stopSelf()
        }
    }

    @Synchronized
    private fun recyclePendingBitmap() {
        pendingBitmap?.let { if (!it.isRecycled) it.recycle() }
        pendingBitmap = null
        pendingSessionId = null
        pendingErrorSessionId = null
        pendingError = null
        stopSelf()
    }

    private fun leaveCaptureForeground() {
        mainHandler.post {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        }
    }

    private fun stopCaptureForeground() {
        mainHandler.post {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun initialDimensions(): CaptureDimensions {
        val density = resources.displayMetrics.densityDpi
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = getSystemService(WindowManager::class.java).maximumWindowMetrics.bounds
            CaptureGeometry.bounded(bounds.width(), bounds.height(), density)
        } else {
            @Suppress("DEPRECATION")
            CaptureGeometry.bounded(
                resources.displayMetrics.widthPixels,
                resources.displayMetrics.heightPixels,
                density,
            )
        }
    }

    override fun onDestroy() {
        captureHandler.removeCallbacksAndMessages(null)
        mainHandler.removeCallbacks(resultTimeout)
        captureResources?.close()
        captureResources = null
        recyclePendingBitmap()
        captureThread.quitSafely()
        CaptureResourceCounters.activeCaptureService.decrementAndGet()
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    private fun Intent.resultData(): Intent? = if (Build.VERSION.SDK_INT >= 33) {
        getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
    } else {
        getParcelableExtra(EXTRA_RESULT_DATA)
    }

    companion object {
        const val ACTION_CAPTURE = "com.grimnej.lmcomment.action.CAPTURE_ONE_FRAME"
        const val EXTRA_SESSION_ID = "capture_session_id"
        const val EXTRA_RESULT_CODE = "media_projection_result_code"
        const val EXTRA_RESULT_DATA = "media_projection_result_data"
        private const val CAPTURE_TIMEOUT_MS = 5_000L
        private const val RESULT_CONSUMPTION_TIMEOUT_MS = 10_000L
        private const val MAX_BLANK_FRAMES = 2

        fun captureIntent(
            context: Context,
            sessionId: String,
            resultCode: Int,
            resultData: Intent,
        ): Intent = Intent(context, OneShotCaptureService::class.java)
            .setAction(ACTION_CAPTURE)
            .putExtra(EXTRA_SESSION_ID, sessionId)
            .putExtra(EXTRA_RESULT_CODE, resultCode)
            .putExtra(EXTRA_RESULT_DATA, resultData)
    }
}
