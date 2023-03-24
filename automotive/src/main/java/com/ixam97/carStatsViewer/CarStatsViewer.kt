package com.ixam97.carStatsViewer

import android.app.*
import android.content.Context
import android.util.TypedValue
import com.ixam97.carStatsViewer.appPreferences.AppPreferences
import com.ixam97.carStatsViewer.dataManager.DataManager
import com.ixam97.carStatsViewer.dataManager.TripData
import com.ixam97.carStatsViewer.liveData.LiveDataApi
import com.ixam97.carStatsViewer.liveData.abrpLiveData.AbrpLiveData
import com.ixam97.carStatsViewer.liveData.http.HttpLiveData
import com.ixam97.carStatsViewer.utils.InAppLogger
import kotlin.properties.Delegates

var emulatorMode = false
var emulatorPowerSign = -1

class CarStatsViewer : Application() {

    companion object {
        const val RESTART_CHANNEL_ID = "RestartChannel"
        const val RESTART_NOTIFICATION_ID = 1
        const val FOREGROUND_CHANNEL_ID = "ForegroundChannel"
        const val FOREGROUND_NOTIFICATION_ID = 2

        lateinit var appContext: Context
        lateinit var liveDataApis: ArrayList<LiveDataApi>
        lateinit var appPreferences: AppPreferences
        lateinit var notificationManager: NotificationManager
        var primaryColor by Delegates.notNull<Int>()
        var disabledAlpha by Delegates.notNull<Float>()

        var foregroundServiceStarted = false
        var restartNotificationDismissed = false

        var tripData: TripData? = null
        var dataManager: DataManager? = null



        // lateinit var tripDatabase: TripDataDatabase
        // lateinit var tripDao: TripDao

    }

    // val dataProcessor = DataProcessor()

    override fun onCreate() {
        super.onCreate()
/*
        tripDatabase = Room.databaseBuilder(
            applicationContext,
            TripDataDatabase::class.java,
            "TripDatabase"
        ).build()
        tripDao = tripDatabase.tripDao()

 */

        val typedValue = TypedValue()
        applicationContext.theme.resolveAttribute(android.R.attr.colorControlActivated, typedValue, true)
        primaryColor = typedValue.data

        applicationContext.theme.resolveAttribute(android.R.attr.disabledAlpha, typedValue, true)
        disabledAlpha = typedValue.float

        // StrictMode.setVmPolicy(
        //     VmPolicy.Builder(StrictMode.getVmPolicy())
        //         .detectLeakedClosableObjects()
        //         .build()
        // )

        appContext = applicationContext
        appPreferences = AppPreferences(applicationContext)

        InAppLogger.i("${appContext.getString(R.string.app_name)} v${BuildConfig.VERSION_NAME} started")

        val abrpApiKey = if (resources.getIdentifier("abrp_api_key", "string", applicationContext.packageName) != 0) {
            getString(resources.getIdentifier("abrp_api_key", "string", applicationContext.packageName))
        } else ""

        liveDataApis = arrayListOf(
            AbrpLiveData(abrpApiKey),
            HttpLiveData()
        )

        notificationManager = createNotificationManager()

    }

    private fun createNotificationManager(): NotificationManager {
        val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val restartChannel = NotificationChannel(
            RESTART_CHANNEL_ID,
            RESTART_CHANNEL_ID,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = RESTART_CHANNEL_ID
        }

        val foregroundChannel = NotificationChannel(
            FOREGROUND_CHANNEL_ID,
            FOREGROUND_CHANNEL_ID,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = FOREGROUND_CHANNEL_ID
        }

        notificationManager.createNotificationChannels(listOf(
            restartChannel,
            foregroundChannel
        ))
        return notificationManager
    }
}