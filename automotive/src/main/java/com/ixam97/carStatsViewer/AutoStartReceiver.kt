package com.ixam97.carStatsViewer

import android.app.AlarmManager
import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ixam97.carStatsViewer.ui.activities.PermissionsActivity
import com.ixam97.carStatsViewer.dataCollector.DataCollector
import com.ixam97.carStatsViewer.utils.InAppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AutoStartReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {


        val reasonMap = mapOf(
            "crash" to context.getString(R.string.restart_notification_reason_crash),
            "reboot" to context.getString(R.string.restart_notification_reason_reboot),
            "update" to context.getString(R.string.restart_notification_reason_update),
            "termination" to "an unexpected termination"
        )
        var reason: String? = CarStatsViewer.restartReason

        InAppLogger.v("[ASR] Service started: ${CarStatsViewer.foregroundServiceStarted}, dismissed: ${CarStatsViewer.restartNotificationDismissed}")

        if (!CarStatsViewer.appPreferences.autostart) return

        CarStatsViewer.setupRestartAlarm(CarStatsViewer.appContext, "termination", 10_000)

        if (CarStatsViewer.foregroundServiceStarted) return
        if (CarStatsViewer.restartNotificationDismissed) return

        intent?.let {
            InAppLogger.d("[ASR] ${intent.toString()} ${intent.extras?.keySet().let { key ->
                val stringBuilder = StringBuilder()
                key?.forEach { 
                    stringBuilder.append("$it ")
                }
                stringBuilder.toString()
            }}")
            if (intent.hasExtra("dismiss")) {
                if (intent.getBooleanExtra("dismiss", false)) {
                    CarStatsViewer.restartNotificationDismissed = true
                    CarStatsViewer.notificationManager.cancel(CarStatsViewer.RESTART_NOTIFICATION_ID)
                    InAppLogger.d("[ARS] Dismiss intent")
                    return
                }
            }
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

        val startupNotificationBuilder = Notification.Builder(
            context.applicationContext,
            CarStatsViewer.RESTART_CHANNEL_ID
        )
            .setContentTitle(notificationText)
            .setContentText(context.getString(R.string.restart_notification_message))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(false)

        startupNotificationBuilder.apply {
            addAction(Notification.Action.Builder(
                    null,
                    context.getString(R.string.restart_notification_service),
                    actionServicePendingIntent
            ).build())
            addAction(Notification.Action.Builder(
                    null,
                context.getString(R.string.restart_notification_app),
                    actionActivityPendingIntent
            ).build())
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
        }

        // Notification needs to be of CATEGORY_CALL to be displayed as a heads up notification in AAOS.
        startupNotificationBuilder.setCategory(Notification.CATEGORY_CALL)

        CarStatsViewer.notificationManager.notify(CarStatsViewer.RESTART_NOTIFICATION_ID, startupNotificationBuilder.build())
        CoroutineScope(Dispatchers.Default).launch {
            while (!CarStatsViewer.foregroundServiceStarted && !CarStatsViewer.restartNotificationDismissed) {
                CarStatsViewer.notificationManager.notify(CarStatsViewer.RESTART_NOTIFICATION_ID, startupNotificationBuilder.build())
                delay(5_000)
            }
            // The heads up notification disappears after 8 seconds and is not visible in the
            // notification center. Update notification without CATEGORY_CALL to keep it visible.
            // delay(8_000)
            // startupNotificationBuilder.setCategory(Notification.CATEGORY_STATUS)
            // if (!CarStatsViewer.foregroundServiceStarted && !CarStatsViewer.restartNotificationDismissed)
            //     CarStatsViewer.notificationManager.notify(CarStatsViewer.RESTART_NOTIFICATION_ID, startupNotificationBuilder.build())
        }
    }
}