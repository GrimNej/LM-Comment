package com.grimnej.lmcomment.bubble

import android.app.Service
import android.content.Intent
import android.os.IBinder

class BubbleOverlayService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
    }

    override fun onDestroy() {
        isRunning = false
        super.onDestroy()
    }

    companion object {
        @Volatile
        var isRunning: Boolean = false
            private set
    }
}
