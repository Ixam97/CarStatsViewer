package com.ixam97.carStatsViewer

import android.app.*
import android.content.Context
import android.content.Intent
import com.ixam97.carStatsViewer.abrpLiveData.AbrpLiveData
import com.ixam97.carStatsViewer.dataManager.DataCollector
import com.ixam97.carStatsViewer.utils.InAppLogger
import kotlin.system.exitProcess

class CarStatsViewer : Application() {

    companion object {
        const val CHANNEL_ID = "TestChannel"
        lateinit var appContext: Context
        // lateinit var abrpLiveData: LiveDataApiInterface

        lateinit var liveDataApis: ArrayList<LiveDataApi>

    }

    override fun onCreate() {
        super.onCreate()

        appContext = applicationContext

        val abrpApiKey = if (resources.getIdentifier("abrp_api_key", "string", applicationContext.packageName) != 0) {
            getString(resources.getIdentifier("abrp_api_key", "string", applicationContext.packageName))
        } else ""

        /*
        Add live data APIs here
         */
        liveDataApis = arrayListOf(
            AbrpLiveData(abrpApiKey)
        )
        // abrpLiveData = AbrpLiveData(abrpApiKey)

        createNotificationChannel()

        startForegroundService(Intent(applicationContext, DataCollector::class.java))

        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            InAppLogger.log("Car Stats Viewer has crashed!\n ${e.stackTraceToString()}")
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val serviceIntent = Intent(applicationContext, DataCollector::class.java)
            serviceIntent.putExtra("reason", "crash")
            val pendingIntent = PendingIntent.getForegroundService(
                applicationContext,
                0,
                serviceIntent,
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