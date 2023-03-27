package com.ixam97.carStatsViewer

import android.app.*
import android.content.Context
import android.util.Log
import android.util.TypedValue
import androidx.room.Room
import com.google.gson.Gson
import com.ixam97.carStatsViewer.appPreferences.AppPreferences
import com.ixam97.carStatsViewer.dataManager.DataManager
import com.ixam97.carStatsViewer.dataManager.TripData
import com.ixam97.carStatsViewer.dataProcessor.DataProcessor
import com.ixam97.carStatsViewer.database.tripData.*
import com.ixam97.carStatsViewer.liveDataApi.LiveDataApi
import com.ixam97.carStatsViewer.liveDataApi.abrpLiveData.AbrpLiveData
import com.ixam97.carStatsViewer.liveDataApi.http.HttpLiveData
import com.ixam97.carStatsViewer.utils.InAppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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



        lateinit var tripDatabase: TripDataDatabase
        lateinit var tripDataSource: LocalTripDataSource

    }

    val dataProcessor = DataProcessor()

    override fun onCreate() {
        super.onCreate()

        tripDatabase = Room.databaseBuilder(
            applicationContext,
            TripDataDatabase::class.java,
            "TripDatabase"
        ).build()

        tripDataSource = LocalTripDataSource(tripDatabase.tripDao())

        CoroutineScope(Dispatchers.IO).launch {

            tripDatabase.clearAllTables()

            if (tripDataSource.getActiveDrivingSessionsIds().isEmpty()) {
                // initial population of Database
                val startTime = System.currentTimeMillis()
                tripDataSource.startDrivingSession(startTime, TripType.MANUAL)
                tripDataSource.startDrivingSession(startTime, TripType.AUTO)
                tripDataSource.startDrivingSession(startTime, TripType.SINCE_CHARGE)
                tripDataSource.startDrivingSession(startTime, TripType.MONTH)
            }

            val drivingSessionIds = tripDataSource.getActiveDrivingSessionsIdsMap()
            InAppLogger.d("Trip Database: ${drivingSessionIds.toString()}")

            val chargingSessionId = tripDataSource.startChargingSession(
                timestamp = System.currentTimeMillis(),
                outsideTemp = 1.0f
            )

            for(i in 0 ..3) {
                tripDataSource.addChargingPoint(ChargingPoint(
                    charging_point_epoch_time = System.currentTimeMillis(),
                    charging_session_id = chargingSessionId,
                    energy_delta = 0f,
                    power = 0f,
                    state_of_charge = 0.5f,
                    point_marker_type = 0
                ))
                delay(500)
            }

            tripDataSource.endChargingSession(System.currentTimeMillis(), chargingSessionId)

            val drivingSession = tripDataSource.getFullDrivingSession(drivingSessionIds[TripType.MANUAL]?: 0)
            val tripDataJson = Gson().toJson(drivingSession)
            Log.d("Trip Database", tripDataJson)
        }



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