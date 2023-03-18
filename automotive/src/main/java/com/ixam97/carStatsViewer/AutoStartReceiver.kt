package com.ixam97.carStatsViewer

import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.widget.Toast
import androidx.core.app.NotificationCompat.Action
import com.ixam97.carStatsViewer.activities.PermissionsActivity
import com.ixam97.carStatsViewer.dataManager.DataCollector
import com.ixam97.carStatsViewer.utils.InAppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AutoStartReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        var reason: String? = null

        intent?.let {
            if (intent.hasExtra("dismiss"))
            {
                CarStatsViewer.restartNotificationDismissed = true
                CarStatsViewer.notificationManager.cancel(CarStatsViewer.RESTART_NOTIFICATION_ID)
                return
            }
            reason = if (intent.hasExtra("reason")) {
                intent.getStringExtra("reason")
            } else {
                when (intent.action) {
                    Intent.ACTION_BOOT_COMPLETED -> "reboot"
                    Intent.ACTION_MY_PACKAGE_REPLACED -> "update"
                    else -> "unknown event"
                }
            }
        }

        InAppLogger.log("AutStartReceiver fired. Reason: $reason")

        val notificationText =
            if (reason != null) "Car Stats Viewer has restarted after $reason."
            else "Car Stats Viewer started in Background."

        val actionServicePendingIntent = PendingIntent.getForegroundService(
            context.applicationContext,
            0,
            Intent(context.applicationContext, DataCollector::class.java).apply {
                putExtra("reason", reason)
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        val actionDismissPendingIntent = PendingIntent.getBroadcast(
            context.applicationContext,
            0,
            Intent(context.applicationContext, this.javaClass).apply {
                putExtra("dismiss", "")
            },
            PendingIntent.FLAG_ONE_SHOT
        )
        val actionActivityPendingIntent = PendingIntent.getActivity(
            context.applicationContext,
            0,
            Intent(context.applicationContext, PermissionsActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val startupNotificationBuilder = Notification.Builder(
            context.applicationContext,
            CarStatsViewer.RESTART_CHANNEL_ID
        )
            .setContentTitle(notificationText)
            .setContentText("Please restart foreground service for stats tracking.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)

        startupNotificationBuilder.apply {
            addAction(Notification.Action.Builder(
                    null,
                    "Start service",
                    actionServicePendingIntent
            ).build())
            addAction(Notification.Action.Builder(
                    null,
                    "Start app",
                    actionActivityPendingIntent
            ).build())
            addAction(Notification.Action.Builder(
                    null,
                    "Dismiss",
                    actionDismissPendingIntent
            ).build())
        }

        // Notification needs to be of CATEGORY_CALL to be displayed as a heads up notification in AAOS.
        startupNotificationBuilder.setCategory(Notification.CATEGORY_CALL)

        CarStatsViewer.notificationManager.notify(CarStatsViewer.RESTART_NOTIFICATION_ID, startupNotificationBuilder.build())
        CoroutineScope(Dispatchers.Default).launch {
            // The heads up notification disappears after 8 seconds and is not visible in the
            // notification center. Update notification without CATEGORY_CALL to keep it visible.
            delay(8_000)
            startupNotificationBuilder.setCategory(Notification.CATEGORY_STATUS)
            if (!CarStatsViewer.foregroundServiceStarted && !CarStatsViewer.restartNotificationDismissed)
                CarStatsViewer.notificationManager.notify(CarStatsViewer.RESTART_NOTIFICATION_ID, startupNotificationBuilder.build())
        }
    }
}