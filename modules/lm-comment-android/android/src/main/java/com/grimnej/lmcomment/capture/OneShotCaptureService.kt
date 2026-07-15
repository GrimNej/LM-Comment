package com.grimnej.lmcomment.capture

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder

class OneShotCaptureService : Service() {
    private val binder = LocalBinder()

    override fun onBind(intent: Intent?): IBinder = binder

    inner class LocalBinder : Binder() {
        fun service(): OneShotCaptureService = this@OneShotCaptureService
    }
}
