package com.grimnej.lmcomment.capture

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.grimnej.lmcomment.LmCommentContract
import com.grimnej.lmcomment.nativebridge.R

object CaptureNotification {
    fun create(context: Context): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    LmCommentContract.CAPTURE_CHANNEL_ID,
                    "One-frame capture",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Visible briefly while LM-Comment captures one approved frame"
                    setShowBadge(false)
                    enableVibration(false)
                    setSound(null, null)
                },
            )
        }
        return NotificationCompat.Builder(context, LmCommentContract.CAPTURE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_lm_comment_notification)
            .setContentTitle("LM-Comment is capturing one approved frame")
            .setContentText("The frame remains on this device.")
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
