package com.grimnej.lmcomment

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.grimnej.lmcomment.bubble.BubbleOverlayService
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition

class LMCommentAndroidModule : Module() {
    override fun definition() = ModuleDefinition {
        Name("LMCommentAndroid")

        AsyncFunction("getReadiness") {
            val context = requireNotNull(appContext.reactContext)
            mapOf(
                "contractVersion" to LmCommentContract.VERSION,
                "overlayPermission" to if (Settings.canDrawOverlays(context)) "granted" else "denied",
                "notificationPermission" to notificationPermission(context),
                "bubbleStatus" to if (BubbleOverlayService.isRunning) "running" else "stopped",
            )
        }

        AsyncFunction("openOverlayPermissionSettings") {
            val context = requireNotNull(appContext.reactContext)
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }

        AsyncFunction("getSafeDiagnostics") {
            val context = requireNotNull(appContext.reactContext)
            mapOf(
                "contractVersion" to LmCommentContract.VERSION,
                "androidApi" to Build.VERSION.SDK_INT,
                "deviceModel" to "${Build.MANUFACTURER} ${Build.MODEL}".trim(),
                "overlayPermission" to Settings.canDrawOverlays(context),
                "notificationPermission" to notificationPermission(context),
                "bubbleStatus" to if (BubbleOverlayService.isRunning) "running" else "stopped",
            )
        }
    }

    private fun notificationPermission(context: android.content.Context): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return "not-required"
        return if (
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        ) "granted" else "denied"
    }
}
