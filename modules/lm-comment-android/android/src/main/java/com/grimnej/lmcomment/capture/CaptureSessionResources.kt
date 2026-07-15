package com.grimnej.lmcomment.capture

import android.graphics.Bitmap
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.view.Surface
import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean

class CaptureSessionResources : Closeable {
    var projection: MediaProjection? = null
        set(value) {
            if (field == null && value != null) CaptureResourceCounters.activeProjection.incrementAndGet()
            field = value
        }
    var projectionCallback: MediaProjection.Callback? = null
    var virtualDisplay: VirtualDisplay? = null
        set(value) {
            if (field == null && value != null) CaptureResourceCounters.activeVirtualDisplay.incrementAndGet()
            field = value
        }
    var imageReader: ImageReader? = null
        set(value) {
            if (field == null && value != null) CaptureResourceCounters.activeImageReader.incrementAndGet()
            field = value
        }
    var surface: Surface? = null
    var image: Image? = null
        set(value) {
            if (field == null && value != null) CaptureResourceCounters.activeImage.incrementAndGet()
            field = value
        }
    var bitmap: Bitmap? = null
    private val closed = AtomicBoolean(false)

    @Synchronized
    fun takeBitmap(): Bitmap = checkNotNull(bitmap) {
        "No captured bitmap is available."
    }.also { bitmap = null }

    @Synchronized
    fun replaceReader(reader: ImageReader, readerSurface: Surface) {
        imageReader?.setOnImageAvailableListener(null, null)
        runCatching { surface?.release() }
        surface = null
        if (imageReader != null) CaptureResourceCounters.activeImageReader.decrementAndGet()
        runCatching { imageReader?.close() }
        imageReader = null
        imageReader = reader
        surface = readerSurface
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        imageReader?.setOnImageAvailableListener(null, null)
        image?.let {
            runCatching { it.close() }
            CaptureResourceCounters.activeImage.decrementAndGet()
        }
        image = null
        virtualDisplay?.let {
            runCatching { it.release() }
            CaptureResourceCounters.activeVirtualDisplay.decrementAndGet()
        }
        virtualDisplay = null
        runCatching { surface?.release() }
        surface = null
        imageReader?.let {
            runCatching { it.close() }
            CaptureResourceCounters.activeImageReader.decrementAndGet()
        }
        imageReader = null
        projection?.let { mediaProjection ->
            projectionCallback?.let { callback -> runCatching { mediaProjection.unregisterCallback(callback) } }
            runCatching { mediaProjection.stop() }
            CaptureResourceCounters.activeProjection.decrementAndGet()
        }
        projection = null
        projectionCallback = null
        bitmap?.let { owned -> if (!owned.isRecycled) owned.recycle() }
        bitmap = null
    }
}
