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
import com.ixam97.carStatsViewer.database.tripData.*
import com.ixam97.carStatsViewer.liveData.LiveDataApi
import com.ixam97.carStatsViewer.liveData.abrpLiveData.AbrpLiveData
import com.ixam97.carStatsViewer.liveData.http.HttpLiveData
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
        lateinit var tripDao: TripDao

    }

    // val dataProcessor = DataProcessor()

    override fun onCreate() {
        super.onCreate()

        tripDatabase = Room.databaseBuilder(
            applicationContext,
            TripDataDatabase::class.java,
            "TripDatabase"
        ).build()
        tripDao = tripDatabase.tripDao()

        CoroutineScope(Dispatchers.IO).launch {

            tripDatabase.clearAllTables()

            val sessionId = tripDao.addDrivingSession(DrivingSession(
                start_epoch_time = System.currentTimeMillis(),
                end_epoch_time = null,
                session_type = 0,
                drive_time = 0,
                used_energy = 0.0,
                driven_distance = 0.0,
                note = "Test"
            ))

            val chargingSessionId = tripDao.addChargingSession(
                chargingSession = ChargingSession(
                    start_epoch_time = System.currentTimeMillis(),
                    end_epoch_time = null,
                    charged_energy = 0.0,
                    charged_soc = 0f,
                    outside_temp = 10.0f,
                    lat = null,
                    lon = null
                ),
                activeSessions = listOf(sessionId)
            )

            for(i in 0 ..3) {
                tripDao.addChargingPoint(
                    chargingPoint = ChargingPoint(
                        charging_point_epoch_time = System.currentTimeMillis(),
                        charging_session_id = chargingSessionId,
                        energy_delta = 0f,
                        power = 0f,
                        state_of_charge = 0.5f,
                        point_marker_type = 0
                    )
                )
                delay(500)
            }

            for(i in 0 ..5) {
                tripDao.addDrivingPoint(
                    drivingPoint = DrivingPoint(
                        driving_point_epoch_time = System.currentTimeMillis(),
                        energy_delta = 0f,
                        distance_delta = 0f,
                        point_marker_type = 0,
                        state_of_charge = .5f,
                        lat = null,
                        lon = null,
                        alt = null
                    ),
                    activeSessions = listOf(sessionId)
                )
                delay(500)
            }

            var drivingSession = tripDao.getCompleteDrivingSessionById(sessionId)
            var tripDataJson = Gson().toJson(drivingSession)
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