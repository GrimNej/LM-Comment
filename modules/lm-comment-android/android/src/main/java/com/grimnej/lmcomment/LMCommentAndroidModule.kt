package com.grimnej.lmcomment

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.grimnej.lmcomment.bubble.BubbleAnchorStore
import com.grimnej.lmcomment.bubble.BubbleOverlayService
import com.grimnej.lmcomment.config.DemoConfigurationStore
import com.grimnej.lmcomment.config.DemoConfigurationValidator
import com.grimnej.lmcomment.workflow.CaptureWorkflowActivity
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import expo.modules.kotlin.records.Field
import expo.modules.kotlin.records.Record
import expo.modules.kotlin.records.Required

internal data class DemoConfigurationInput(
    @Field @Required val relayBaseUrl: String,
    @Field @Required val demoToken: String,
    @Field @Required val defaultTone: String,
    @Field @Required val optionCount: Int,
    @Field @Required val demoMode: Boolean,
) : Record

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

        AsyncFunction("requestNotificationPermission") {
            val context = requireNotNull(appContext.reactContext)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                return@AsyncFunction "not-required"
            }
            if (notificationPermission(context) == "granted") {
                return@AsyncFunction "granted"
            }
            val activity = requireNotNull(appContext.currentActivity) {
                "Notification permission requires a visible activity."
            }
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_REQUEST,
            )
            "denied"
        }

        AsyncFunction("startBubble") {
            val context = requireNotNull(appContext.reactContext)
            check(Settings.canDrawOverlays(context)) {
                "Display-over-other-apps permission is required before starting the bubble."
            }
            if (!BubbleOverlayService.isRunning) {
                val intent = Intent(context, BubbleOverlayService::class.java)
                    .setAction(BubbleOverlayService.ACTION_START)
                ContextCompat.startForegroundService(context, intent)
            }
        }

        AsyncFunction("stopBubble") {
            val context = requireNotNull(appContext.reactContext)
            if (BubbleOverlayService.isRunning) {
                context.startService(
                    Intent(context, BubbleOverlayService::class.java)
                        .setAction(BubbleOverlayService.ACTION_STOP),
                )
            } else {
                context.stopService(Intent(context, BubbleOverlayService::class.java))
            }
        }

        AsyncFunction("configureDemo") { input: DemoConfigurationInput ->
            val context = requireNotNull(appContext.reactContext)
            val configuration = DemoConfigurationValidator.validate(
                relayBaseUrl = input.relayBaseUrl,
                demoToken = input.demoToken,
                defaultTone = input.defaultTone,
                optionCount = input.optionCount,
                demoMode = input.demoMode,
                isDebuggable = DemoConfigurationStore.isApplicationDebuggable(context),
            )
            DemoConfigurationStore(context).save(configuration)
            Unit
        }

        AsyncFunction("getDemoConfigurationStatus") {
            val context = requireNotNull(appContext.reactContext)
            val status = DemoConfigurationStore(context).status()
            mapOf(
                "relayHostname" to status.relayHostname,
                "isDemoTokenConfigured" to status.isDemoTokenConfigured,
                "defaultTone" to status.defaultTone.wireValue,
                "optionCount" to status.optionCount,
                "demoMode" to status.demoMode,
            )
        }

        AsyncFunction("resetDemoConfiguration") {
            val context = requireNotNull(appContext.reactContext)
            DemoConfigurationStore(context).clear()
        }

        AsyncFunction("resetBubblePosition") {
            val context = requireNotNull(appContext.reactContext)
            BubbleAnchorStore(context).reset()
            if (BubbleOverlayService.isRunning) {
                context.startService(
                    Intent(context, BubbleOverlayService::class.java)
                        .setAction(BubbleOverlayService.ACTION_RESET_POSITION),
                )
            }
        }

        AsyncFunction("openManualTextWorkflow") { sourceText: String? ->
            val context = requireNotNull(appContext.reactContext)
            if (BubbleOverlayService.isRunning) {
                context.startService(
                    Intent(context, BubbleOverlayService::class.java)
                        .setAction(BubbleOverlayService.ACTION_LAUNCH_MANUAL)
                        .putExtra(CaptureWorkflowActivity.EXTRA_INITIAL_TEXT, sourceText.orEmpty()),
                )
            } else {
                context.startActivity(
                    Intent(context, CaptureWorkflowActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra(CaptureWorkflowActivity.EXTRA_MANUAL_ENTRY, true)
                        .putExtra(CaptureWorkflowActivity.EXTRA_INITIAL_TEXT, sourceText.orEmpty()),
                )
            }
            Unit
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

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST = 4201
    }
}
