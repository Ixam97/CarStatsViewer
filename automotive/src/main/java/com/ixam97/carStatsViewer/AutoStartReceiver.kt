package com.ixam97.carStatsViewer

import android.app.ActivityManager
import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.car.app.activity.CarAppActivity
import androidx.core.content.ContextCompat.startForegroundService
import com.ixam97.carStatsViewer.dataCollector.DataCollector
import com.ixam97.carStatsViewer.ui.activities.PermissionsActivity
import com.ixam97.carStatsViewer.utils.InAppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AutoStartReceiver: BroadcastReceiver() {

    private fun isServiceRunning(className: String): Boolean {
        Log.d("ActivityManager", "Compare to: $className")
        val activityManager = CarStatsViewer.appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.getRunningServices(10).forEach { runningServiceInfo ->
            Log.d("ActivityManager", runningServiceInfo.service.className)
            if (runningServiceInfo.service.className == className) {
                Log.d("ActivityManager", "MATCH")
                return true
            }
        }
        return false
    }

    override fun onReceive(context: Context, intent: Intent?) {

        Log.d("ASR", "Action: ${intent?.action}")

        if (!CarStatsViewer.appPreferences.autostart) {
            InAppLogger.i("[ASR] Autostart disabled, canceling ASR.")
            return
        }

        if ((intent?.action?: "") == "com.ixam97.carStatsViewer.NOTIFICATION_DELETE") {
            CarStatsViewer.restartNotificationDismissed = true
            return
        }

        if (((intent?.action?: "") == Intent.ACTION_BOOT_COMPLETED
                    || (intent?.action?: "") == Intent.ACTION_MY_PACKAGE_REPLACED)
            && !isServiceRunning(DataCollector::class.java.name)) {
            try {
                InAppLogger.i("[ASR] Attempting to start Service on ${intent?.action}")
                startForegroundService(CarStatsViewer.appContext, Intent(CarStatsViewer.appContext, DataCollector::class.java))

            } catch (e: Exception) {
                InAppLogger.e("[ASR] Failed to directly start foreground service! Probably missing background location permission or not supported by OS version!")
                InAppLogger.e(e.stackTraceToString())
            }
        }

        val reasonMap = mapOf(
            "crash" to context.getString(R.string.restart_notification_reason_crash),
            "reboot" to context.getString(R.string.restart_notification_reason_reboot),
            "update" to context.getString(R.string.restart_notification_reason_update),
            "termination" to "an unexpected termination"
        )
        var reason: String? = CarStatsViewer.restartReason

        InAppLogger.v("[ASR] Conditions: Service started: ${isServiceRunning(DataCollector::class.java.name)}, dismissed: ${CarStatsViewer.restartNotificationDismissed}")

        if (CarStatsViewer.restartNotificationDismissed) return

        CarStatsViewer.setupRestartAlarm(CarStatsViewer.appContext, "termination", 9_500, extendedLogging = true)

        // if (CarStatsViewer.foregroundServiceStarted) return
        if (isServiceRunning(DataCollector::class.java.name)) return
        if (CarStatsViewer.restartNotificationShown) return

        InAppLogger.d("[ASR] Auto Star Receiver triggered")

        intent?.let {
            InAppLogger.d("[ASR] ${intent.toString()} ${intent.extras?.keySet().let { key ->
                val stringBuilder = StringBuilder()
                key?.forEach { 
                    stringBuilder.append("$it ")
                }
                stringBuilder.toString()
            }}")
            /*
            if (intent.hasExtra("dismiss")) {
                if (intent.getBooleanExtra("dismiss", false)) {
                    CarStatsViewer.restartNotificationDismissed = true
                    CarStatsViewer.setupRestartAlarm(CarStatsViewer.appContext, "termination", 10_000, cancel = true, extendedLogging = true)
                    CarStatsViewer.notificationManager.cancel(CarStatsViewer.RESTART_NOTIFICATION_ID)
                    InAppLogger.d("[ARS] Dismiss intent")
                    return
                }
            }
            */
            if (reason == null || reason == "termination") {
                reason = if (intent.hasExtra("reason")) {
                    intent.getStringExtra("reason")
                    //reasonMap[]
                } else {
                    when (intent.action) {
                        Intent.ACTION_BOOT_COMPLETED -> "reboot"
                        Intent.ACTION_MY_PACKAGE_REPLACED -> "update"
                        else -> "termination"
                    }
                }
                InAppLogger.i("[ASR] Restart reason: $reason")
                CarStatsViewer.restartReason = reason
            }
        }

        // InAppLogger.i("AutoStartReceiver fired. Reason: $reason")

        val notificationText =
            if (reason != null && reason != "termination") {
                context.getString(R.string.restart_notification_title,
                    context.getString(R.string.app_name_short),
                    reasonMap[reason])
            } else context.getString(R.string.restart_notification_reason_unknown, context.getString(R.string.app_name_short))

        val actionServicePendingIntent = PendingIntent.getForegroundService(
            context.applicationContext,
            1,
            Intent(context.applicationContext, DataCollector::class.java).apply {
                putExtra("reason", reason)
            },
            PendingIntent.FLAG_IMMUTABLE
        )
        val actionActivityPendingIntent = PendingIntent.getActivity(
            context.applicationContext,
            2,
            Intent(context.applicationContext, PermissionsActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val actionCarAppActivityPendingIntent = PendingIntent.getActivity(
            context.applicationContext,
            2,
            Intent(context.applicationContext, CarAppActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val deleteIntent = PendingIntent.getBroadcast(
            context.applicationContext,
            0,
            Intent(context.applicationContext, AutoStartReceiver::class.java).apply {
                action = "com.ixam97.carStatsViewer.NOTIFICATION_DELETE"
            },
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val startupNotificationBuilder = Notification.Builder(
            context.applicationContext,
            CarStatsViewer.RESTART_CHANNEL_ID
        )
            .setContentTitle(notificationText)
            .setContentText(context.getString(R.string.restart_notification_message))
            .setSmallIcon(R.mipmap.ic_launcher_notification)
            .setDeleteIntent(deleteIntent)
            .setOngoing(false)

        startupNotificationBuilder.apply {
            addAction(Notification.Action.Builder(
                    null,
                    context.getString(R.string.restart_notification_service),
                    actionServicePendingIntent
            ).build())
            addAction(
                Notification.Action.Builder(
                    null,
                    context.getString(R.string.restart_notification_app),
                    if (BuildConfig.FLAVOR_aaos != "carapp") actionActivityPendingIntent else actionCarAppActivityPendingIntent
                ).build()
            )
            /*
            addAction(Notification.Action.Builder(
                    null,
                context.getString(R.string.restart_notification_dismiss),
                PendingIntent.getBroadcast(
                    context.applicationContext,
                    3,
                    Intent(context.applicationContext, AutoStartReceiver::class.java).apply {
                        putExtra("dismiss", true)
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            ).build())
             */
        }

        // Notification needs to be of CATEGORY_CALL to be displayed as a heads up notification in AAOS.
        startupNotificationBuilder.setCategory(Notification.CATEGORY_CALL)

        CarStatsViewer.notificationManager.notify(CarStatsViewer.RESTART_NOTIFICATION_ID, startupNotificationBuilder.build())
        CarStatsViewer.restartNotificationShown = true
        CoroutineScope(Dispatchers.Default).launch {
        //     while (!CarStatsViewer.foregroundServiceStarted && !CarStatsViewer.restartNotificationDismissed) {
        //         CarStatsViewer.notificationManager.notify(CarStatsViewer.RESTART_NOTIFICATION_ID, startupNotificationBuilder.build())
        //         delay(5_000)
        //     }
            // The heads up notification disappears after 8 seconds and is not visible in the
            // notification center. Update notification without CATEGORY_CALL to keep it visible.
            delay(8_000)
            startupNotificationBuilder.setCategory(Notification.CATEGORY_STATUS)
            if (!CarStatsViewer.foregroundServiceStarted && !CarStatsViewer.restartNotificationDismissed)
                CarStatsViewer.notificationManager.notify(CarStatsViewer.RESTART_NOTIFICATION_ID, startupNotificationBuilder.build())
        }
    }
}