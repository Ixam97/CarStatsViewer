package com.ixam97.carStatsViewer.dataManager

import android.app.AlarmManager
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.widget.Toast
import com.google.android.gms.location.LocationServices
import com.ixam97.carStatsViewer.AutoStartReceiver
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.activities.MainActivity
import com.ixam97.carStatsViewer.carPropertiesClient.CarProperties
import com.ixam97.carStatsViewer.carPropertiesClient.CarPropertiesClient
import com.ixam97.carStatsViewer.dataProcessor.DataProcessor
import com.ixam97.carStatsViewer.dataProcessor.TripDataManager
import com.ixam97.carStatsViewer.emulatorMode
import com.ixam97.carStatsViewer.locationTracking.DefaultLocationClient
import com.ixam97.carStatsViewer.locationTracking.LocationClient
import com.ixam97.carStatsViewer.utils.InAppLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.system.exitProcess

class NeoDataCollector: Service() {

    companion object {
        const val LIVE_DATA_TASK_INTERVAL = 5_000L
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var foregroundServiceNotification: Notification.Builder
    private lateinit var locationClient: LocationClient

    private lateinit var carPropertiesClient: CarPropertiesClient
    private lateinit var dataProcessor: DataProcessor
    private lateinit var tripDataManager: TripDataManager

    init {
        InAppLogger.i("[NEO] Neo DataCollector is initializing...")
        CarStatsViewer.foregroundServiceStarted = true
        CarStatsViewer.notificationManager.cancel(CarStatsViewer.RESTART_NOTIFICATION_ID)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        intent?.let {
            if (it.hasExtra("reason")) {
                Toast.makeText(applicationContext, getString(R.string.restart_toast_background), Toast.LENGTH_LONG).show()
            }
        }

        startForeground(CarStatsViewer.FOREGROUND_NOTIFICATION_ID + 10, foregroundServiceNotification.build())
        return START_NOT_STICKY
    }

    override fun onCreate() {
        super.onCreate()

        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            InAppLogger.e("[NEO] Car Stats Viewer has crashed!\n ${e.stackTraceToString()}")
            exitProcess(0)
        }

        dataProcessor  = (applicationContext as CarStatsViewer).dataProcessor
        tripDataManager = (applicationContext as CarStatsViewer).tripDataManager

        tripDataManager.checkTrips()

        carPropertiesClient = CarPropertiesClient(
            context = applicationContext,
            propertiesProcessor = dataProcessor::processProperty,
            carPropertiesData = dataProcessor.carPropertiesData
        )

        foregroundServiceNotification = Notification.Builder(applicationContext, CarStatsViewer.FOREGROUND_CHANNEL_ID)
            // .setContentTitle(getString(R.string.app_name))
            .setContentTitle(getString(R.string.foreground_service_info) + " (Neo)")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)

        foregroundServiceNotification.setContentIntent(
            PendingIntent.getActivity(
                applicationContext,
                0,
                Intent(applicationContext, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )
        )

        dataProcessor.staticVehicleData = dataProcessor.staticVehicleData.copy(
            batteryCapacity = carPropertiesClient.getFloatProperty(CarProperties.INFO_EV_BATTERY_CAPACITY)
        )

        if (carPropertiesClient.getStringProperty(CarProperties.INFO_MODEL) == "Speedy Model") {
            Toast.makeText(this, "Emulator Mode", Toast.LENGTH_LONG).show()
            emulatorMode = true
        }

        locationClient = DefaultLocationClient(
            CarStatsViewer.appContext,
            LocationServices.getFusedLocationProviderClient(this)
        )

        locationClient
            .getLocationUpdates(5_000L)
            .catch { e ->
                InAppLogger.e("[NEO] LocationClient: ${e.message}")
            }
            .onEach { location ->
                if (location != null) {
                    InAppLogger.v("[NEO] %.2f, %.2f, %.0fm, time: %d".format(location.latitude, location.longitude, location.altitude, location.time))
                    dataProcessor.processLocation(location.latitude, location.longitude, location.altitude)
                } else {
                    dataProcessor.processLocation(null, null, null)
                }
            }
            .launchIn(serviceScope)


        CarStatsViewer.liveDataApis[0]
            .requestFlow(
                serviceScope,
                realTimeData = (applicationContext as CarStatsViewer).dataProcessor.realTimeData,
                LIVE_DATA_TASK_INTERVAL
            ).catch { e -> InAppLogger.e("[NEO] requestFlow: ${e.message}") }
            .launchIn(serviceScope)

        CarStatsViewer.liveDataApis[1]
            .requestFlow(
                serviceScope,
                realTimeData = (applicationContext as CarStatsViewer).dataProcessor.realTimeData,
                LIVE_DATA_TASK_INTERVAL
            ).catch { e -> InAppLogger.e("[NEO] requestFlow: ${e.message}") }
            .launchIn(serviceScope)

        carPropertiesClient.getCarPropertiesUpdates()

        CarProperties.usedProperties.forEach {
            carPropertiesClient.updateProperty(it)
        }

        serviceScope.launch {
            val serviceIntent = Intent(applicationContext, AutoStartReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                applicationContext,
                0,
                serviceIntent,
                PendingIntent.FLAG_ONE_SHOT
            )
            while (true) {
                serviceIntent.action = "com.ixam97.carStatsViewer.RestartAction"
                serviceIntent.putExtra("reason", "termination")
                val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + 5000, pendingIntent)
                delay(4000)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        carPropertiesClient.disconnect()
    }

}