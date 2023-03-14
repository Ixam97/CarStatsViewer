package com.ixam97.carStatsViewer

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.StrictMode
import com.ixam97.carStatsViewer.liveData.abrpLiveData.AbrpLiveData
import android.os.StrictMode.VmPolicy
import android.util.TypedValue
import androidx.core.graphics.toColor
import com.ixam97.carStatsViewer.appPreferences.AppPreferences
import com.ixam97.carStatsViewer.dataManager.DataCollector
import com.ixam97.carStatsViewer.liveData.LiveDataApi
import com.ixam97.carStatsViewer.liveData.http.HttpLiveData
import com.ixam97.carStatsViewer.utils.InAppLogger
import kotlin.properties.Delegates
import kotlin.system.exitProcess


class CarStatsViewer : Application() {

    companion object {
        const val CHANNEL_ID = "TestChannel"
        lateinit var appContext: Context
        // lateinit var abrpLiveData: LiveDataApiInterface

        lateinit var liveDataApis: ArrayList<LiveDataApi>
        lateinit var appPreferences: AppPreferences
        var primaryColor by Delegates.notNull<Int>()
    }

    override fun onCreate() {
        super.onCreate()

        val typedValue = TypedValue()
        applicationContext.theme.resolveAttribute(android.R.attr.colorControlActivated, typedValue, true)
        primaryColor = typedValue.data

        // StrictMode.setVmPolicy(
        //     VmPolicy.Builder(StrictMode.getVmPolicy())
        //         .detectLeakedClosableObjects()
        //         .build()
        // )

        appContext = applicationContext
        appPreferences = AppPreferences(applicationContext)

        val abrpApiKey = if (resources.getIdentifier("abrp_api_key", "string", applicationContext.packageName) != 0) {
            getString(resources.getIdentifier("abrp_api_key", "string", applicationContext.packageName))
        } else ""

        /*
        Add live data APIs here
         */
        liveDataApis = arrayListOf(
            AbrpLiveData(abrpApiKey),
            HttpLiveData()
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