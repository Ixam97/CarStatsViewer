package com.ixam97.carStatsViewer

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.ixam97.carStatsViewer.dataManager.DataCollector
import kotlin.system.exitProcess


class AutoStartReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Toast.makeText(context, context.resources.getString(R.string.toast_started_in_background), Toast.LENGTH_SHORT).show()
        // val activityIntent = Intent(context, DataCollector::class.java)
        // context.startForegroundService(activityIntent)
    }
}

class CarStatsViewer : Application() {

    companion object {
        const val CHANNEL_ID = "TestChannel"
        var pendingIntent: PendingIntent? = null
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        startForegroundService(Intent(applicationContext, DataCollector::class.java))

        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            InAppLogger.log("Car Stats Viewer has crashed!\n ${e.stackTraceToString()}")
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

            pendingIntent = PendingIntent.getForegroundService(
                applicationContext,
                0,
                Intent(applicationContext, DataCollector::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT
            )

            alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + 100, pendingIntent)
            exitProcess(2)
        }
    }

    fun createNotificationChannel() {
        val name = "TestChannel"
        val descriptionText = "TestChannel"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}