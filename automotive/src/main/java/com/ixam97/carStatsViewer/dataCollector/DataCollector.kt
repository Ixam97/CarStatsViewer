package com.ixam97.carStatsViewer.dataCollector

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.car.VehicleUnit
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.widget.Toast
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.ui.activities.MainActivity
import com.ixam97.carStatsViewer.carPropertiesClient.CarProperties
import com.ixam97.carStatsViewer.carPropertiesClient.CarPropertiesClient
import com.ixam97.carStatsViewer.dataProcessor.DataProcessor
import com.ixam97.carStatsViewer.emulatorMode
import com.ixam97.carStatsViewer.locationClient.DefaultLocationClient
import com.ixam97.carStatsViewer.locationClient.LocationClient
import com.ixam97.carStatsViewer.utils.DistanceUnitEnum
import com.ixam97.carStatsViewer.utils.InAppLogger
import com.ixam97.carStatsViewer.utils.StringFormatters
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
        intent?.let {
            if (it.hasExtra("reason")) {
                Toast.makeText(applicationContext, getString(R.string.restart_toast_background), Toast.LENGTH_LONG).show()
            }
        }

        startForeground(CarStatsViewer.FOREGROUND_NOTIFICATION_ID + 10, foregroundServiceNotification.build())
        InAppLogger.i("[NEO] Foreground service started in onStartCommand()")
        super.onStartCommand(intent, flags, startId)
        return START_NOT_STICKY
    }

    override fun onCreate() {
        super.onCreate()

        foregroundServiceNotification = Notification.Builder(applicationContext, CarStatsViewer.FOREGROUND_CHANNEL_ID)
            // .setContentTitle(getString(R.string.app_name))
            .setContentTitle(getString(R.string.foreground_service_info))
            .setSmallIcon(R.mipmap.ic_launcher_notification)
            .setOngoing(true)

        foregroundServiceNotification.setContentIntent(
            PendingIntent.getActivity(
                applicationContext,
                0,
                Intent(applicationContext, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )
        )

        startForeground(CarStatsViewer.FOREGROUND_NOTIFICATION_ID + 10, foregroundServiceNotification.build())
        InAppLogger.i("[NEO] Foreground service started in onCreate()")

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

        dataProcessor.staticVehicleData = dataProcessor.staticVehicleData.copy(
            batteryCapacity = carPropertiesClient.getFloatProperty(CarProperties.INFO_EV_BATTERY_CAPACITY),
            vehicleMake = carPropertiesClient.getStringProperty(CarProperties.INFO_MAKE),
            modelName = carPropertiesClient.getStringProperty(CarProperties.INFO_MODEL),
            distanceUnit = when (carPropertiesClient.getIntProperty(CarProperties.DISTANCE_DISPLAY_UNITS)) {
                VehicleUnit.MILE -> DistanceUnitEnum.MILES
                else -> DistanceUnitEnum.KM
            }
        )

        dataProcessor.staticVehicleData.let {
            InAppLogger.i("[NEO] Make: ${it.vehicleMake}, model: ${it.modelName}, battery capacity: ${(it.batteryCapacity?:0f)/1000} kWh")
        }

        if (dataProcessor.staticVehicleData.modelName == "Speedy Model") {
            Toast.makeText(this, "Emulator Mode", Toast.LENGTH_LONG).show()
            emulatorMode = true
        }

        CarStatsViewer.appPreferences.distanceUnit = if (!emulatorMode) dataProcessor.staticVehicleData.distanceUnit else DistanceUnitEnum.KM

        InAppLogger.i("[NEO] Google API availability: ${GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS}")

        locationClient = DefaultLocationClient(
            //CarStatsViewer.appContext,
            //LocationServices.getFusedLocationProviderClient(this)
        )

        // startLocationClient(5_000)

        if (CarStatsViewer.appPreferences.useLocation) {
            startLocationClient(5_000)
        }

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
                delay(10_000)
                CarStatsViewer.watchdog.triggerWatchdog()
            }
        }

        serviceScope.launch {
            // Notification updater
            var simpleNotification = false
            while (true) {
                delay(2_500)
                if (!CarStatsViewer.appPreferences.notifications) {
                    foregroundServiceNotification
                        // .setSmallIcon(R.mipmap.ic_launcher_notification)
                        .setContentTitle(getString(R.string.foreground_service_info))
                        .setContentText("")
                } else {
                    foregroundServiceNotification
                        .setContentTitle(getString(R.string.notification_title) + " " + resources.getStringArray(R.array.trip_type_names)[CarStatsViewer.appPreferences.mainViewTrip + 1])
                        // .setSmallIcon(R.drawable.ic_notification_diagram)
                        .setContentText(String.format(
                            "Dist.: %s, Cons.: %s, Speed: %s",
                            StringFormatters.getTraveledDistanceString(CarStatsViewer.dataProcessor.selectedSessionDataFlow.value?.driven_distance?.toFloat()?:0f),
                            StringFormatters.getAvgConsumptionString(CarStatsViewer.dataProcessor.selectedSessionDataFlow.value?.used_energy?.toFloat()?:0f, CarStatsViewer.dataProcessor.selectedSessionDataFlow.value?.driven_distance?.toFloat()?:0f),
                            StringFormatters.getAvgSpeedString(CarStatsViewer.dataProcessor.selectedSessionDataFlow.value?.driven_distance?.toFloat()?:0f, CarStatsViewer.dataProcessor.selectedSessionDataFlow.value?.drive_time?:0)
                        ))
                    simpleNotification = false
                }
                if (!simpleNotification) {
                    simpleNotification = !CarStatsViewer.appPreferences.notifications
                    CarStatsViewer.notificationManager.notify(
                        CarStatsViewer.FOREGROUND_NOTIFICATION_ID + 10,
                        foregroundServiceNotification.build()
                    )
                    InAppLogger.v("Updating notification")
                }
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
        locationClient.stopLocationUpdates()
        dataProcessor.processLocation(null, null, null)
        lastLocation = null
        locationClientJob?.cancel()
        locationClientJob = null
    }

    private fun startLocationClient(interval: Long) {
        InAppLogger.i("[NEO] Location client is being started")

        locationClient.stopLocationUpdates()
        locationClientJob?.cancel()
        locationClientJob = locationClient.getLocationUpdates(interval, this@DataCollector)
            .catch { e ->
                InAppLogger.e("[LOC] ${e.message}")
            }
            .onEach { location ->
                if (location != null) {
                    dataProcessor.processLocation(location.latitude, location.longitude, location.altitude)
                    CarStatsViewer.watchdog.updateWatchdogState(CarStatsViewer.watchdog.getCurrentWatchdogState().copy(locationState = WatchdogState.NOMINAL))
                } else {
                    dataProcessor.processLocation(null, null, null)
                    CarStatsViewer.watchdog.updateWatchdogState(CarStatsViewer.watchdog.getCurrentWatchdogState().copy(locationState = WatchdogState.ERROR))
                }
                lastLocation = location
            }
            .launchIn(serviceScope)
    }

}