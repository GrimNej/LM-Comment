package com.grimnej.lmcomment.bubble

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.grimnej.lmcomment.LmCommentContract
import com.grimnej.lmcomment.nativebridge.R

object BubbleNotification {
    fun create(context: Context): Notification {
        createChannel(context)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val openIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val openPendingIntent = openIntent?.let {
            PendingIntent.getActivity(context, 0, it, flags)
        }
        val stopIntent = Intent(context, BubbleOverlayService::class.java)
            .setAction(BubbleOverlayService.ACTION_STOP)
        val stopPendingIntent = PendingIntent.getService(context, 1, stopIntent, flags)

        return NotificationCompat.Builder(context, LmCommentContract.BUBBLE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_lm_comment_notification)
            .setContentTitle("LM-Comment bubble is active")
            .setContentText("Tap the bubble over content to create a response.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Tap the bubble over content to create a response. Screenshots are captured only after Android asks for permission."),
            )
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .apply { if (openPendingIntent != null) setContentIntent(openPendingIntent) }
            .apply {
                if (openPendingIntent != null) {
                    addAction(0, "Open app", openPendingIntent)
                }
            }
            .addAction(0, "Stop", stopPendingIntent)
            .build()
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            LmCommentContract.BUBBLE_CHANNEL_ID,
            "Floating bubble",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Shown while the user-started LM-Comment bubble is active"
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }
        manager.createNotificationChannel(channel)
    }
}
