package com.ixam97.carStatsViewer.dataCollector

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.car.VehicleUnit
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.car.app.activity.CarAppActivity
import com.ixam97.carStatsViewer.BuildConfig
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.CarStatsViewer.Companion.SCREENSHOT_CHANNEL_ID
import com.ixam97.carStatsViewer.CarStatsViewer.Companion.notificationManager
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.carPropertiesClient.CarProperties
import com.ixam97.carStatsViewer.carPropertiesClient.CarPropertiesClient
import com.ixam97.carStatsViewer.dataProcessor.DataProcessor
import com.ixam97.carStatsViewer.emulatorMode
import com.ixam97.carStatsViewer.locationClient.DefaultLocationClient
import com.ixam97.carStatsViewer.locationClient.LocationClient
import com.ixam97.carStatsViewer.ui.activities.MainActivity
import com.ixam97.carStatsViewer.utils.DistanceUnitEnum
import com.ixam97.carStatsViewer.utils.InAppLogger
import com.ixam97.carStatsViewer.utils.ScreenshotService
import com.ixam97.carStatsViewer.utils.StringFormatters
import com.ixam97.carStatsViewer.utils.WatchdogState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class DataCollector: Service() {

    companion object {
        const val LIVE_DATA_TASK_INTERVAL = 5_000
        private const val PROPERTY_INIT_MAX_ATTEMPTS = 10
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var foregroundServiceNotification: Notification.Builder
    private var locationClient: LocationClient? = null
    private var locationClientJob: Job? = null

    private lateinit var carPropertiesClient: CarPropertiesClient
    private lateinit var dataProcessor: DataProcessor

    private var lastLocation: Location? = null
    private var watchdogLocation: Location? = null

    private var carPropertiesInitialized = false
    private var simpleNotification = false

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
        // super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    /**
     * Setup the necessary Car Properties and clients when starting the service. Make sure the code
     * is not blocking to prevent ANRs.
     */
    override fun onCreate() {
        super.onCreate()

        setupServiceNotification()
        startForeground(CarStatsViewer.FOREGROUND_NOTIFICATION_ID + 10, foregroundServiceNotification.build())
        InAppLogger.i("[NEO] Foreground service started in onCreate()")

        dataProcessor = CarStatsViewer.dataProcessor
        locationClient = DefaultLocationClient()
        carPropertiesClient = CarPropertiesClient(
            context = applicationContext,
            propertiesProcessor = dataProcessor::processProperty,
            carPropertiesData = dataProcessor.carPropertiesData
        )

        /** detect if system is an emulator and show a toast */
        if (BuildConfig.DEBUG) {
            Toast.makeText(applicationContext, "Emulator Mode", Toast.LENGTH_LONG).show()
            emulatorMode = true
        }

        serviceScope.launch {
            withContext(Dispatchers.IO) { dataProcessor.checkTrips() }
            readInitialStaticProperties()
            setupDynamicCarProperties()

            carPropertiesInitialized = true
        }

        /** Setup the live data APIs */
        // TODO: Migrate these to Retrofit and move them to the repository package

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

        if (CarStatsViewer.appPreferences.autostart)
            CarStatsViewer.setupRestartAlarm(CarStatsViewer.appContext, "termination", 9_500, extendedLogging = true)

        if (CarStatsViewer.appPreferences.useLocation) { startLocationClient(5_000) }

        /** Check if location client has crashed or needs to be stopped or started after settings changed. */
        serviceScope.launch {
            CarStatsViewer.watchdog.watchdogTriggerFlow.collect {
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
            while (true) {
                delay(2_500)
                updateServiceNotification()
                if (!dataProcessor.realTimeData.isOptionalInitialized()) {
                    setupOptionalDynamicProperties()
                }
                if (!dataProcessor.staticVehicleData.isOptionalInitialized()) {
                    readStaticProperties()
                }
                if (!ScreenshotService.screenshotServiceState.value.isServiceRunning) {
                    notificationManager.deleteNotificationChannel(SCREENSHOT_CHANNEL_ID)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        carPropertiesClient.disconnect()
    }

    /**
     * Stop the location client if disabled by settings.
     */
    private fun stopLocationClient() {
        locationClient?.let {
            InAppLogger.i("[NEO] Location client is being canceled")

            it.stopLocationUpdates()
            dataProcessor.processLocation(null, null, null)
            lastLocation = null
            locationClientJob?.cancel()
            locationClientJob = null
        }
    }

    /**
     * Start the location client and get location updates in a fixed interval.
     */
    private fun startLocationClient(interval: Long) {
        locationClient?.let {
            InAppLogger.i("[NEO] Location client is being started")

            it.stopLocationUpdates()
            locationClientJob?.cancel()
            locationClientJob = it.getLocationUpdates(interval, this@DataCollector)
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

    /**
     * Vehicle Make in Emulator does not always match the actual emulator Make. This returns the
     * make based on various criteria.
     */
    private fun emulatorCarMake(): String {
        val propertyMake = carPropertiesClient.getStringProperty(CarProperties.INFO_MAKE)?: "Unknown"
        if (propertyMake == "Toy Vehicle") {
            if (File("/product/fonts/PolestarUnica77-Regular.otf").exists())
                return "Polestar"
            else return Build.BRAND
        }
        return propertyMake
    }

    /**
     * Read static Car Properties
     */
    private suspend fun readInitialStaticProperties() {
        var attemptCounter = 0
        while(!dataProcessor.staticVehicleData.isEssentialInitialized()) {
            if (attemptCounter > 0) {
                delay(500)
            }

            readStaticProperties()

            attemptCounter++
            if (attemptCounter > PROPERTY_INIT_MAX_ATTEMPTS) {
                val msg = "Service init failed: Not all required static Car Properties are available!"
                InAppLogger.e("[NEO] $msg")
                // throw Exception(msg)
                CarStatsViewer.watchdog.updateWatchdogState(CarStatsViewer.watchdog.getCurrentWatchdogState().copy(
                    appErrorState = WatchdogState.Companion.AppErrorState("$msg\nCSV will not work as intended.  Please contact the developer for debugging.")
                ))
                break
            }
        }

        InAppLogger.i("[NEO] Static Car Properties read.")
        InAppLogger.i("Brand: ${emulatorCarMake()}")
        dataProcessor.staticVehicleData.apply {
            InAppLogger.i("[NEO] Make: ${vehicleMake}, model: ${modelName}, battery capacity: ${(batteryCapacity?:0f)/1000} kWh, distance unit: ${distanceUnit?.name}")
        }
    }

    fun readStaticProperties() {
        InAppLogger.d("[NEO] Attempting to read static Properties...")
        dataProcessor.staticVehicleData = dataProcessor.staticVehicleData.copy(
            batteryCapacity = carPropertiesClient.getFloatProperty(CarProperties.INFO_EV_BATTERY_CAPACITY),
            vehicleMake =  emulatorCarMake(),
            modelName = carPropertiesClient.getStringProperty(CarProperties.INFO_MODEL),
            distanceUnit = when (carPropertiesClient.getIntProperty(CarProperties.DISTANCE_DISPLAY_UNITS)) {
                VehicleUnit.MILE -> DistanceUnitEnum.MILES
                VehicleUnit.KILOMETER -> DistanceUnitEnum.KM
                else -> null
            }
        )
        /** Show km in the emulator by default. Can be changed to miles in dev settings in the app. */
        setDistanceUnitPreference()
    }

    /**
     * Check availability of all Car Properties and try to register Callbacks.
     */
    private suspend fun setupDynamicCarProperties() {
        var allPropertiesAvailable = false
        var attemptCounter = 0
        InAppLogger.d("[NEO] Attempting to init essential dynamic Properties...")
        while (!allPropertiesAvailable) {
            if (attemptCounter > 0) {
                delay(500)
            }
            allPropertiesAvailable = true
            CarProperties.essentialDynamicProperties.forEach { propertyId ->
                if (!carPropertiesClient.registeredProperties.contains(propertyId)) {
                    if (!carPropertiesClient.getCarPropertyUpdates(propertyId)) {
                        allPropertiesAvailable = false
                        val warnMsg = "[NEO] Essential Property ${CarProperties.getNameById(propertyId)} ($propertyId) is currently not available!"
                        InAppLogger.logWithFirebase(warnMsg, Log.WARN)
                    } else {
                        InAppLogger.i("[NEO] Essential Property ${CarProperties.getNameById(propertyId)} ($propertyId) registered.")
                    }
                }
            }
            attemptCounter++
            if (attemptCounter > PROPERTY_INIT_MAX_ATTEMPTS) {
                val msg = "Service init failed: Not all required dynamic Car Properties are available!"
                InAppLogger.e("[NEO] $msg")
                // throw Exception(msg)
                CarStatsViewer.watchdog.updateWatchdogState(CarStatsViewer.watchdog.getCurrentWatchdogState().copy(
                    appErrorState = WatchdogState.Companion.AppErrorState("$msg\nCSV will not work as intended. Please contact the developer for debugging.")
                ))
                break
            }
        }

        setupOptionalDynamicProperties()

        CarProperties.usedProperties.forEach {
            carPropertiesClient.updateProperty(it)
        }

        if (allPropertiesAvailable)
            InAppLogger.i("[NEO] Dynamic Car Properties have been registered successfully.")
        else
            InAppLogger.w("[NEO] Unable to register all dynamic Car Properties. App functionality limited")
    }

    private fun setupOptionalDynamicProperties() {
        InAppLogger.d("[NEO] Attempting to init optional dynamic Properties...")
        CarProperties.optionalDynamicProperties.forEach { propertyId ->
            if (!carPropertiesClient.registeredProperties.contains(propertyId)) {
                if (!carPropertiesClient.getCarPropertyUpdates(propertyId)) {
                    val warnMsg = "[NEO] Optional Property ${CarProperties.getNameById(propertyId)} ($propertyId) is currently not available!"
                    InAppLogger.logWithFirebase(warnMsg, Log.WARN)
                } else {
                    InAppLogger.i("[NEO] Optional Property ${CarProperties.getNameById(propertyId)} ($propertyId) registered.")
                }
            }
        }
    }

    /**
     * Initial setup of the notification needed for a foreground service.
     */
    private fun setupServiceNotification() {
        foregroundServiceNotification = Notification.Builder(applicationContext, CarStatsViewer.FOREGROUND_CHANNEL_ID)
            .setContentTitle("Car Properties are initializing...")
            .setSmallIcon(R.mipmap.ic_launcher_notification)
            .setOngoing(true)

        foregroundServiceNotification.setContentIntent(
            PendingIntent.getActivity(
                applicationContext,
                0,
                Intent(applicationContext, if (BuildConfig.FLAVOR_aaos != "carapp") MainActivity::class.java else CarAppActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )
        )
    }

    /**
     * Updates the foreground service notification depending on the current settings and state.
     */
    private fun updateServiceNotification() {
        if (!carPropertiesInitialized || !dataProcessor.staticVehicleData.isInitialized()) {
            foregroundServiceNotification.apply {
                setContentTitle("Car Properties are initializing...")
                if (BuildConfig.FLAVOR_version == "dev") setContentText("DEV BUILD: ${BuildConfig.VERSION_NAME}")
                else setContentText("")
            }
        } else if (!CarStatsViewer.appPreferences.notifications) {
            foregroundServiceNotification.apply {
                setContentTitle(getString(R.string.foreground_service_info))
                if (BuildConfig.FLAVOR_version == "dev") setContentText("DEV BUILD: ${BuildConfig.VERSION_NAME}")
                else setContentText("")
            }
        } else {
            foregroundServiceNotification.apply{
                var detailsString = String.format(
                    "Dist.: %s, Cons.: %s, Speed: %s",
                    StringFormatters.getTraveledDistanceString(CarStatsViewer.dataProcessor.selectedSessionDataFlow.value?.driven_distance?.toFloat()?:0f),
                    StringFormatters.getAvgConsumptionString(CarStatsViewer.dataProcessor.selectedSessionDataFlow.value?.used_energy?.toFloat()?:0f, CarStatsViewer.dataProcessor.selectedSessionDataFlow.value?.driven_distance?.toFloat()?:0f),
                    StringFormatters.getAvgSpeedString(CarStatsViewer.dataProcessor.selectedSessionDataFlow.value?.driven_distance?.toFloat()?:0f, CarStatsViewer.dataProcessor.selectedSessionDataFlow.value?.drive_time?:0)
                )
                if (BuildConfig.FLAVOR_version == "dev") detailsString += "\nDEV BUILD: ${BuildConfig.VERSION_NAME}"
                setContentTitle(getString(R.string.notification_title) + " " + resources.getStringArray(R.array.trip_type_names)[CarStatsViewer.appPreferences.mainViewTrip + 1])
                setContentText(detailsString)
            }
            simpleNotification = false
        }
        if (!simpleNotification) {
            simpleNotification = !CarStatsViewer.appPreferences.notifications
            CarStatsViewer.notificationManager.notify(
                CarStatsViewer.FOREGROUND_NOTIFICATION_ID + 10,
                foregroundServiceNotification.build()
            )
        }
    }

    private fun setDistanceUnitPreference() {
        val distanceUnit = dataProcessor.staticVehicleData.distanceUnit?:DistanceUnitEnum.KM
        InAppLogger.d("[NEO] Setting distance unit preference to ${distanceUnit.unit()}")
        CarStatsViewer.appPreferences.distanceUnit =
            if (!emulatorMode) distanceUnit
            else DistanceUnitEnum.KM
    }
}