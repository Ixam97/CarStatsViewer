package com.ixam97.carStatsViewer.dataCollector

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.widget.Toast
import com.google.android.gms.location.LocationServices
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.ui.activities.MainActivity
import com.ixam97.carStatsViewer.carPropertiesClient.CarProperties
import com.ixam97.carStatsViewer.carPropertiesClient.CarPropertiesClient
import com.ixam97.carStatsViewer.dataProcessor.DataProcessor
import com.ixam97.carStatsViewer.emulatorMode
import com.ixam97.carStatsViewer.locationClient.DefaultLocationClient
import com.ixam97.carStatsViewer.locationClient.LocationClient
import com.ixam97.carStatsViewer.utils.InAppLogger
import com.ixam97.carStatsViewer.utils.WatchdogState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class DataCollector: Service() {

    companion object {
        const val LIVE_DATA_TASK_INTERVAL = 5_000
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var foregroundServiceNotification: Notification.Builder
    private lateinit var locationClient: LocationClient
    private var locationClientJob: Job? = null

    private lateinit var carPropertiesClient: CarPropertiesClient
    private lateinit var dataProcessor: DataProcessor

    private var lastLocation: Location? = null
    private var watchdogLocation: Location? = null

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

        // Thread.setDefaultUncaughtExceptionHandler { t, e ->
        //     InAppLogger.e("[NEO] Car Stats Viewer has crashed!\n ${e.stackTraceToString()}")
        //     exitProcess(0)
        // }

        dataProcessor = CarStatsViewer.dataProcessor

        CoroutineScope(Dispatchers.IO).launch {
            dataProcessor.checkTrips()
        }

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
            batteryCapacity = carPropertiesClient.getFloatProperty(CarProperties.INFO_EV_BATTERY_CAPACITY),
            vehicleMake = carPropertiesClient.getStringProperty(CarProperties.INFO_MAKE),
            modelName = carPropertiesClient.getStringProperty(CarProperties.INFO_MODEL)
        )

        dataProcessor.staticVehicleData.let {
            InAppLogger.i("[NEO] Make: ${it.vehicleMake}, model: ${it.modelName}, battery capacity: ${(it.batteryCapacity?:0f)/1000} kWh")
        }

        if (dataProcessor.staticVehicleData.modelName == "Speedy Model") {
            Toast.makeText(this, "Emulator Mode", Toast.LENGTH_LONG).show()
            emulatorMode = true
        }

        locationClient = DefaultLocationClient(
            CarStatsViewer.appContext,
            LocationServices.getFusedLocationProviderClient(this)
        )

        // startLocationClient(5_000)

        CarStatsViewer.liveDataApis[0]
            .requestFlow(
                serviceScope,
                realTimeData = { CarStatsViewer.dataProcessor.realTimeData },
                LIVE_DATA_TASK_INTERVAL
            ).catch { e -> InAppLogger.e("[NEO] requestFlow: ${e.message}") }
            .launchIn(serviceScope)

        CarStatsViewer.liveDataApis[1]
            .requestFlow(
                serviceScope,
                realTimeData = { CarStatsViewer.dataProcessor.realTimeData },
                LIVE_DATA_TASK_INTERVAL
            ).catch { e -> InAppLogger.e("[NEO] requestFlow: ${e.message}") }
            .launchIn(serviceScope)

        carPropertiesClient.getCarPropertiesUpdates()

        CarProperties.usedProperties.forEach {
            carPropertiesClient.updateProperty(it)
        }

        if (CarStatsViewer.appPreferences.autostart)
            CarStatsViewer.setupRestartAlarm(CarStatsViewer.appContext, "termination", 10_000, extendedLogging = true)

        serviceScope.launch {
            CarStatsViewer.watchdog.watchdogTriggerFlow.collect {
                InAppLogger.d("[Watchdog] Watchdog triggered")

                /** Check if location client has crashed or needs to be stopped or started */
                var locationState = WatchdogState.DISABLED
                if (CarStatsViewer.appPreferences.useLocation) {
                    locationState = if (watchdogLocation == lastLocation || locationClientJob == null || lastLocation == null) {
                        if (watchdogLocation == lastLocation) InAppLogger.w("[Watchdog] Location error: Location unchanged.")
                        if (lastLocation == null) InAppLogger.w("[Watchdog] Location error: Location is null.")
                        if (locationClientJob == null) InAppLogger.w("[Watchdog] Location error: Location client not running.")

                        startLocationClient(5_000)
                        WatchdogState.ERROR
                    } else {
                        WatchdogState.NOMINAL
                    }
                    watchdogLocation = lastLocation
                } else if (locationClientJob != null) {
                    stopLocationClient()
                }

                CarStatsViewer.watchdog.updateWatchdogState(CarStatsViewer.watchdog.getCurrentWatchdogState().copy(locationState = locationState))
            }
        }

        serviceScope.launch {
            // Watchdog
            while (true) {
                CarStatsViewer.watchdog.triggerWatchdog()
                delay(10_000)
            }

        }

    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        carPropertiesClient.disconnect()
    }

    private fun stopLocationClient() {
        InAppLogger.i("[NEO] Location client is being canceled")
        locationClientJob?.cancel()
        locationClientJob = null
        dataProcessor.processLocation(null, null, null)
        lastLocation = null
    }

    private fun startLocationClient(interval: Long) {
        InAppLogger.i("[NEO] Location client is being started")

        locationClientJob?.cancel()
        locationClientJob = locationClient.getLocationUpdates(interval)
            .catch { e ->
                InAppLogger.e("[LOC] ${e.message}")
            }
            .onEach { location ->
                if (location != null) {
                    // InAppLogger.v("[NEO] %.2f, %.2f, %.0fm, time: %d".format(location.latitude, location.longitude, location.altitude, location.time))
                    dataProcessor.processLocation(location.latitude, location.longitude, location.altitude)
                } else {
                    dataProcessor.processLocation(null, null, null)
                }
                lastLocation = location
            }
            .launchIn(serviceScope)
    }

}