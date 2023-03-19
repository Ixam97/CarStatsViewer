package com.ixam97.carStatsViewer.dataManager

import android.app.*
import android.car.Car
import android.car.VehiclePropertyIds
import android.car.VehicleUnit
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.*
import android.os.*
import android.util.Log
import android.widget.Toast
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.ixam97.carStatsViewer.*
import com.ixam97.carStatsViewer.activities.MainActivity
import com.ixam97.carStatsViewer.appPreferences.AppPreferences
import com.ixam97.carStatsViewer.enums.DistanceUnitEnum
import com.ixam97.carStatsViewer.locationTracking.DefaultLocationClient
import com.ixam97.carStatsViewer.locationTracking.LocationClient
import com.ixam97.carStatsViewer.plot.enums.*
import com.ixam97.carStatsViewer.utils.InAppLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.io.File
import java.io.FileWriter
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue
import kotlin.system.exitProcess

class DataCollector : Service() {
    companion object {
        lateinit var mainActivityPendingIntent: PendingIntent
        val CurrentTripDataManager = DataManagers.CURRENT_TRIP.dataManager
        private const val DO_LOG = false
        private const val FOREGROUND_NOTIFICATION_ID = 2
        private const val NOTIFICATION_TIMER_HANDLER_DELAY_MILLIS = 5_000L
        private const val LIVE_DATA_TASK_INTERVAL = 5_000L
        private const val CONSUMPTION_PLOT_UPDATE_DISTANCE = 100
        private const val CHARGE_PLOT_UPDATE_INTERVAL_MILLIS = 2_000L
        private const val CHARGE_PLOT_MARKER_THRESHOLD_NANOS = 10_000_000_000L // 2 times CHARGE_PLOT_UPDATE_INTERVAL_MILLIS in nanos
        private const val AUTO_SAVE_INTERVAL_MILLIS = 30_000L
        private const val AUTO_RESET_TIME_HOURS = 5L
        private const val POWER_GAGE_HYSTERESIS = 200_000F
        private const val CONS_GAGE_HYSTERESIS = 10F

        var gagePowerValue: Float = 0F
        var gageConsValue: Float = 0F
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var locationClient: LocationClient

    private var startupTimestamp: Long = 0L

    private var lastNotificationTimeMillis = 0L

    private lateinit var appPreferences: AppPreferences

    private lateinit var gson: Gson

    private var notificationsEnabled = true

    private lateinit var car: Car
    private lateinit var carPropertyManager: CarPropertyManager

    private lateinit var notificationTimerHandler: Handler
    private lateinit var saveTripDataTimerHandler: Handler

    private lateinit var liveDataTimerHandler: Handler

    private lateinit var foregroundServiceNotification: Notification.Builder

    init {
        InAppLogger.log("DataCollector is initializing...")
        startupTimestamp = System.nanoTime()
        CarStatsViewer.foregroundServiceStarted = true
        CarStatsViewer.notificationManager.cancel(CarStatsViewer.RESTART_NOTIFICATION_ID)
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                getString(R.string.save_trip_data_broadcast) -> {
                    InAppLogger.log("TRIP DATA: Broadcast save")
                    enumValues<DataManagers>().filter{ it.doTrack}.forEach {
                        writeTripDataToFile(it.dataManager.tripData!!, it.dataManager.printableName)
                    }
                }
                else -> {}
            }
        }
    }

    private val saveTripDataTask = object : Runnable {
        override fun run() {
            InAppLogger.log("TRIP DATA: Autosave")
            enumValues<DataManagers>().filter{ it.doTrack}.forEach {
                writeTripDataToFile(it.dataManager.tripData!!, it.dataManager.printableName)
            }
            saveTripDataTimerHandler.postDelayed(this, AUTO_SAVE_INTERVAL_MILLIS)
        }
    }

    private val updateStatsNotificationTask = object : Runnable {
        override fun run() {
            updateStatsNotification()
            // InAppLogger.logNotificationUpdate()
            val currentNotificationTimeMillis = System.currentTimeMillis()
            lastNotificationTimeMillis = currentNotificationTimeMillis
            notificationTimerHandler.postDelayed(this, NOTIFICATION_TIMER_HANDLER_DELAY_MILLIS)
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            InAppLogger.log("Car Stats Viewer has crashed!\n ${e.stackTraceToString()}")
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val serviceIntent = Intent(applicationContext, AutoStartReceiver::class.java)
            serviceIntent.action = "com.ixam97.carStatsViewer.RestartAction"
            serviceIntent.putExtra("reason", "crash")
            val pendingIntent = PendingIntent.getBroadcast(
                applicationContext,
                0,
                serviceIntent,
                PendingIntent.FLAG_ONE_SHOT
            )
            alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + 100, pendingIntent)
            exitProcess(0)
        }

        locationClient = DefaultLocationClient(
            CarStatsViewer.appContext,
            LocationServices.getFusedLocationProviderClient(this)
        )

        locationClient
            .getLocationUpdates(5_000L)
            .catch { e ->
                InAppLogger.log("LocationClient: ${e.message}")
            }
            .onEach { location ->
                enumValues<DataManagers>().forEach {
                    it.dataManager.location = location
                }
                if (location != null) {
                    InAppLogger.log("Location: lat: %.5f, lon: %.5f, alt: %.2fm, time: %d".format(location.latitude, location.longitude, location.altitude, location.time))
                }
            }
            .launchIn(serviceScope)

        var tripRestoreComplete = false
        runBlocking {
            enumValues<DataManagers>().forEach {
                val mPrevTripData = readTripDataFromFile(it.dataManager.printableName)
                if (mPrevTripData != null) {
                    it.dataManager.tripData = mPrevTripData
                    sendBroadcast(Intent(getString(R.string.ui_update_plot_broadcast)))
                } else {
                    InAppLogger.log("No trip file read!")
                    // Toast.makeText(applicationContext ,R.string.toast_file_read_error, Toast.LENGTH_LONG).show()
                }
            }
            tripRestoreComplete = true
        }

        // while (!tripRestoreComplete) {
        //     // Wait for completed restore before doing anything
        // }

        foregroundServiceNotification = Notification.Builder(applicationContext, CarStatsViewer.FOREGROUND_CHANNEL_ID)
            // .setContentTitle(getString(R.string.app_name))
            .setContentTitle(getString(R.string.foreground_service_info))
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

        appPreferences = AppPreferences(applicationContext)

        gson = GsonBuilder()
            .setExclusionStrategies(appPreferences.exclusionStrategy)
            .setPrettyPrinting()
            .create()

        notificationsEnabled = appPreferences.notifications

        car = Car.createCar(this)
        carPropertyManager = car.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager

        /** Get vehicle name to enable dev mode in emulator */
        val carName = carPropertyManager.getProperty<String>(VehiclePropertyIds.INFO_MODEL, 0).value.toString()
        val carManufacturer = carPropertyManager.getProperty<String>(VehiclePropertyIds.INFO_MAKE, 0).value.toString()
        val carModelYear = carPropertyManager.getIntProperty(VehiclePropertyIds.INFO_MODEL_YEAR, 0).toString()
        if (carName == "Speedy Model") {
            Toast.makeText(this, "Emulator Mode", Toast.LENGTH_LONG).show()
            emulatorMode = true
            // CurrentTripDataManager.update(VehicleGear.GEAR_PARK, System.nanoTime(), CurrentTripDataManager.CurrentGear.propertyId)
        }

        val displayUnit = carPropertyManager.getProperty<Int>(VehiclePropertyIds.DISTANCE_DISPLAY_UNITS, 0).value

        appPreferences.distanceUnit = if (!emulatorMode) {
            when (displayUnit) {
                VehicleUnit.MILE -> DistanceUnitEnum.MILES
                else -> DistanceUnitEnum.KM
            }
        } else DistanceUnitEnum.KM

        // InAppLogger.log("Display distance unit: $displayUnit")
        InAppLogger.log("Car name: $carName, $carManufacturer, $carModelYear")
        InAppLogger.log("Max battery Capacity: ${carPropertyManager.getFloatProperty(VehiclePropertyIds.INFO_EV_BATTERY_CAPACITY, 0)}")
        // InAppLogger.log("Fuel level: ${carPropertyManager.getFloatProperty(VehiclePropertyIds.FUEL_LEVEL, 0)}")
        // InAppLogger.log("Fuel Capacity: ${carPropertyManager.getFloatProperty(VehiclePropertyIds.INFO_FUEL_CAPACITY, 0)}")

        registerCarPropertyCallbacks()

        notificationTimerHandler = Handler(Looper.getMainLooper())
        notificationTimerHandler.post(updateStatsNotificationTask)
        saveTripDataTimerHandler = Handler(Looper.getMainLooper())
        saveTripDataTimerHandler.postDelayed(saveTripDataTask, AUTO_SAVE_INTERVAL_MILLIS)

        liveDataTimerHandler = Handler(Looper.getMainLooper())
        for (liveDataAPI in CarStatsViewer.liveDataApis) {
            var task = liveDataAPI.createLiveDataTask(
                DataManagers.CURRENT_TRIP.dataManager,
                liveDataTimerHandler,
                LIVE_DATA_TASK_INTERVAL
            )
            if (task != null) {
                liveDataTimerHandler.post(task)
            }
        }

        registerReceiver(broadcastReceiver, IntentFilter(getString(R.string.save_trip_data_broadcast)))
        registerReceiver(carPropertyEmulatorReceiver, IntentFilter(getString(R.string.VHAL_emulator_broadcast)))

        DataManagers.values().filter { it.doTrack }.forEach {
            for (propertyId in DataManager.propertyIds) {
                refreshProperty(propertyId, it.dataManager)
            }
            it.dataManager.consumptionPlotLine.baseLineAt.add(0f)
            it.dataManager.maxBatteryLevel = carPropertyManager.getFloatProperty(VehiclePropertyIds.INFO_EV_BATTERY_CAPACITY, 0)
            it.dataManager.model = carName

            driveStateUpdater(it.dataManager)
            speedUpdater(it.dataManager)
            powerUpdater(it.dataManager)
        }
        InAppLogger.log("DataCollector service started")
    }

    override fun onDestroy() {
        super.onDestroy()
        // .log("DataCollector.onDestroy")
        serviceScope.cancel()
        sendBroadcast(Intent(getString(R.string.save_trip_data_broadcast)))
        unregisterReceiver(broadcastReceiver)
        car.disconnect()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        intent?.let {
            if (it.hasExtra("reason")) {
                Toast.makeText(applicationContext, getString(R.string.restart_toast_background), Toast.LENGTH_LONG).show()
            }
        }

        startForeground(CarStatsViewer.FOREGROUND_NOTIFICATION_ID, foregroundServiceNotification.build())

        return START_NOT_STICKY
    }

    /** Update DataManagers on new VHAL event */
    private val carPropertyListener = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(carPropertyValue: CarPropertyValue<*>) {
            DataManagers.values().forEach {
                if (it.dataManager.update(
                        carPropertyValue,
                        DO_LOG,
                        valueMustChange = false,
                        allowInvalidTimestamps = DataManager.allowInvalidTimestampsMap[carPropertyValue.propertyId] == true
                    ) == it.dataManager.VALID) {
                    handleCarPropertyListenerEvent(carPropertyValue.propertyId, it.dataManager)
                }
            }
        }
        override fun onErrorEvent(propertyId: Int, zone: Int) {
            Log.w("carPropertyGenericListener","Received error car property event, propId=$propertyId")
        }
    }

    /** Simulate VHAL event by using broadcast for emulator use */
    private val carPropertyEmulatorReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (emulatorMode) {
                if (DO_LOG) Log.i("EMULATOR","Received Emulated VHAL update")
                val propertyId = intent.getIntExtra(EmulatorIntentExtras.PROPERTY_ID, 0)
                val valueType = intent.getStringExtra(EmulatorIntentExtras.TYPE)
                val timestamp = System.nanoTime()
                val value: Any? = when (valueType) {
                    EmulatorIntentExtras.TYPE_FLOAT -> intent.getFloatExtra(EmulatorIntentExtras.VALUE, 0.0f)
                    EmulatorIntentExtras.TYPE_INT -> intent.getIntExtra(EmulatorIntentExtras.VALUE, 0)
                    EmulatorIntentExtras.TYPE_BOOLEAN -> intent.getBooleanExtra(EmulatorIntentExtras.VALUE, false)
                    EmulatorIntentExtras.TYPE_STRING -> intent.getStringExtra(EmulatorIntentExtras.VALUE)
                    else -> null
                }
                if (value != null) {
                    DataManagers.values().filter { it.doTrack }.forEach {
                    if (it.dataManager.update(value, timestamp, propertyId, DO_LOG, valueMustChange = false) == it.dataManager.VALID)
                        handleCarPropertyListenerEvent(propertyId, it.dataManager)
                    }
                }
            }
        }
    }

    /** Handle incoming property changes by property ID */
    private fun handleCarPropertyListenerEvent(propertyId: Int, dataManager: DataManager) {
        when (propertyId) {
            dataManager.CurrentPower.propertyId         -> powerUpdater(dataManager)
            dataManager.CurrentSpeed.propertyId         -> speedUpdater(dataManager)
            dataManager.CurrentIgnitionState.propertyId -> ignitionUpdater(dataManager)
            dataManager.ChargePortConnected.propertyId  -> driveStateUpdater(dataManager)
        }
    }

    private fun powerUpdater(dataManager: DataManager) {
        if (dataManager == DataManagers.CURRENT_TRIP.dataManager && !dataManager.CurrentPower.isInitialValue) {
            var gageValueChanged = false
            if ((dataManager.currentPower - gagePowerValue).absoluteValue > POWER_GAGE_HYSTERESIS) {
                gagePowerValue = dataManager.currentPower
                gageValueChanged = true
            }
            if (((dataManager.currentPower / 1000)/(dataManager.currentSpeed * 3.6) - gageConsValue).absoluteValue > CONS_GAGE_HYSTERESIS) {
                gageConsValue = ((dataManager.currentPower / 1000)/(dataManager.currentSpeed * 3.6f)).let {
                    if (it.isFinite()) it
                    else 0F
                }
                gageValueChanged = true
            }
            if (gageValueChanged) sendBroadcast(Intent(getString(R.string.ui_update_gages_broadcast)))
        }
        when (dataManager.driveState) {
            DrivingState.DRIVE -> {
                if (!dataManager.CurrentPower.isInitialValue) {
                    val usedEnergyDelta = (dataManager.currentPower / 1_000) * ((dataManager.CurrentPower.timeDelta / 3.6E12).toFloat())
                    dataManager.usedEnergy += usedEnergyDelta
                    dataManager.consumptionPlotEnergyDelta += usedEnergyDelta
                }
            }
            DrivingState.CHARGE -> {
                refreshProperty(dataManager.BatteryLevel.propertyId, dataManager)
                if (!dataManager.CurrentPower.isInitialValue && !dataManager.BatteryLevel.isInitialValue && dataManager.CurrentPower.timeDelta < CHARGE_PLOT_MARKER_THRESHOLD_NANOS && dataManager.BatteryLevel.timeDelta < CHARGE_PLOT_MARKER_THRESHOLD_NANOS) {
                    val chargedEnergyDelta = (dataManager.currentPower / 1_000) * ((dataManager.CurrentPower.timeDelta / 3.6E12).toFloat())
                    dataManager.chargedEnergy -= chargedEnergyDelta
                    dataManager.chargePlotTimeDelta += dataManager.CurrentPower.timeDelta

                    if (dataManager.chargePlotTimeDelta >= CHARGE_PLOT_UPDATE_INTERVAL_MILLIS * 1_000_000) {
                        addChargeDataPoint(dataManager = dataManager)
                        dataManager.chargePlotTimeDelta = 0L
                        sendBroadcast(Intent(getString(R.string.ui_update_plot_broadcast)))
                    }

                } else {
                    if (dataManager == DataManagers.values().first().dataManager) {
                        var printableName1 =
                            (if (dataManager.CurrentPower.timeDelta > CHARGE_PLOT_MARKER_THRESHOLD_NANOS) dataManager.CurrentPower.printableName else "")
                        val printableName2 = (if (dataManager.BatteryLevel.timeDelta > CHARGE_PLOT_MARKER_THRESHOLD_NANOS) dataManager.BatteryLevel.printableName else "")
                        if (printableName2.isNotEmpty()) printableName1 += " and "
                        printableName1 += printableName2

                        if (printableName1.isNotEmpty()) InAppLogger.log("DATA COLLECTOR: Discarded charge plot data Point due to large time delta of $printableName1!")
                        else InAppLogger.log("DATA COLLECTOR: Discarded charge plot data Point due to initial values!")
                    }
                }
            }
            else -> {
                // Supplemental energy usage?
            }
        }
    }

    private fun speedUpdater(dataManager: DataManager) {
        if (dataManager == DataManagers.CURRENT_TRIP.dataManager && !dataManager.CurrentPower.isInitialValue) {
            var gageValueChanged = false
            if ((dataManager.currentPower - gagePowerValue).absoluteValue > POWER_GAGE_HYSTERESIS) {
                gagePowerValue = dataManager.currentPower
                gageValueChanged = true
            }
            if (((dataManager.currentPower / 1000)/(dataManager.currentSpeed * 3.6) - gageConsValue).absoluteValue > CONS_GAGE_HYSTERESIS) {
                gageConsValue = ((dataManager.currentPower / 1000)/(dataManager.currentSpeed * 3.6f)).let {
                    if (it.isFinite()) it
                    else 0F
                }
                gageValueChanged = true
            }
            if (gageValueChanged) sendBroadcast(Intent(getString(R.string.ui_update_gages_broadcast)))
        }
        if (emulatorMode) {
            val emulatePowerIntent = Intent(getString(R.string.VHAL_emulator_broadcast)).apply {
                putExtra(EmulatorIntentExtras.PROPERTY_ID, dataManager.CurrentPower.propertyId)
                putExtra(EmulatorIntentExtras.TYPE, EmulatorIntentExtras.TYPE_FLOAT)
                putExtra(EmulatorIntentExtras.VALUE, carPropertyManager.getFloatProperty(dataManager.CurrentPower.propertyId, 0))
            }
            sendBroadcast(emulatePowerIntent)
        }
        if (!dataManager.CurrentSpeed.isInitialValue && dataManager.driveState == DrivingState.DRIVE) {
            val traveledDistanceDelta = (dataManager.currentSpeed.absoluteValue * dataManager.CurrentSpeed.timeDelta.toFloat()) / 1_000_000_000F
            dataManager.traveledDistance += traveledDistanceDelta
            if (dataManager == DataManagers.CURRENT_TRIP.dataManager) {
                if (dataManager.currentSpeed.absoluteValue >= 1 && !appPreferences.doDistractionOptimization) {
                    // Drive started -> Distraction optimization
                    appPreferences.doDistractionOptimization = true
                    sendBroadcast(Intent(getString(R.string.distraction_optimization_broadcast)))
                    InAppLogger.log("Drive started")
                } else if (dataManager.currentSpeed.absoluteValue < 1 && appPreferences.doDistractionOptimization) {
                    // Drive ended
                    appPreferences.doDistractionOptimization = false
                    sendBroadcast(Intent(getString(R.string.distraction_optimization_broadcast)))
                    InAppLogger.log("Drive ended")
                }
            }

            dataManager.consumptionPlotDistanceDelta += traveledDistanceDelta

            if (dataManager.consumptionPlotDistanceDelta >= CONSUMPTION_PLOT_UPDATE_DISTANCE) {
                if (dataManager.driveState == DrivingState.DRIVE) {
                    addConsumptionDataPoint(
                        if(dataManager.consumptionPlotDistanceDelta > 0) dataManager.consumptionPlotEnergyDelta / (dataManager.consumptionPlotDistanceDelta / 1000) else 0F,
                        dataManager =  dataManager
                    )
                }
                dataManager.consumptionPlotDistanceDelta = 0F
                dataManager.consumptionPlotEnergyDelta = 0F
                sendBroadcast(Intent(getString(R.string.ui_update_plot_broadcast)))
            }
        }
    }

    private fun driveStateUpdater(dataManager: DataManager) {
        // Get real current properties to avoid old values after hibernation.
        refreshProperty(dataManager.CurrentIgnitionState.propertyId, dataManager)
        refreshProperty(dataManager.ChargePortConnected.propertyId, dataManager)
        refreshProperty(dataManager.BatteryLevel.propertyId, dataManager)

        val previousDrivingState = dataManager.DriveState.lastDriveState
        if (dataManager.DriveState.hasChanged()) {
            if (dataManager == DataManagers.values().first().dataManager) InAppLogger.log("DRIVE STATE: ${DrivingState.nameMap[previousDrivingState]} -> ${DrivingState.nameMap[dataManager.driveState]} (${dataManager.CurrentIgnitionState.value}")
            when (dataManager.driveState) {
                DrivingState.DRIVE  -> driveState(previousDrivingState, dataManager)
                DrivingState.CHARGE -> chargeState(previousDrivingState, dataManager)
                DrivingState.PARKED -> parkState(previousDrivingState, dataManager)
            }
            if (dataManager == DataManagers.CURRENT_TRIP.dataManager)
                InAppLogger.log("TRIP DATA: Drive state save")
            writeTripDataToFile(dataManager.tripData!!, dataManager.printableName)
            sendBroadcast(Intent(getString(R.string.ui_update_plot_broadcast)))
        }
    }

    private fun ignitionUpdater(dataManager: DataManager) {
        if (dataManager == DataManagers.values().first().dataManager) {
            InAppLogger.log("Ignition switched to: ${ignitionList[dataManager.currentIgnitionState]}")
        }
        driveStateUpdater(dataManager)
    }

    private fun driveState(previousDrivingState: Int, dataManager: DataManager) {
        resetAutoTrips(previousDrivingState, DrivingState.DRIVE, dataManager)
        resumeTrip(dataManager)
        if (previousDrivingState == DrivingState.CHARGE) stopChargingSession(dataManager)
        if (previousDrivingState != DrivingState.UNKNOWN) dataManager.plotMarkers.endMarker(System.currentTimeMillis(), dataManager.traveledDistance)
        if (previousDrivingState == DrivingState.UNKNOWN) {
            if (dataManager.plotMarkers.markers.isNotEmpty()) {
                val currentMarker = dataManager.plotMarkers.markers.maxWith(Comparator.comparingLong { it.StartTime })
                if (currentMarker.EndTime == null && listOf(PlotMarkerType.CHARGE, PlotMarkerType.PARK).contains(currentMarker.MarkerType)) {
                    dataManager.plotMarkers.endMarker(
                        System.currentTimeMillis(),
                        currentMarker.StartDistance
                    )
                }
            }
        }
    }

    private fun parkState(previousDrivingState: Int, dataManager: DataManager) {
        resetAutoTrips(previousDrivingState, DrivingState.PARKED, dataManager)
        if (previousDrivingState == DrivingState.DRIVE){
            pauseTrip(dataManager)
            dataManager.plotMarkers.addMarker(PlotMarkerType.PARK, System.currentTimeMillis(), dataManager.traveledDistance)
        }
        if (previousDrivingState == DrivingState.CHARGE) stopChargingSession(dataManager)
    }

    private fun chargeState(previousDrivingState: Int, dataManager: DataManager) {
        if (previousDrivingState == DrivingState.DRIVE) pauseTrip(dataManager)
        if (previousDrivingState != DrivingState.UNKNOWN){
            startChargingSession(dataManager)
            dataManager.plotMarkers.addMarker(PlotMarkerType.CHARGE, System.currentTimeMillis(), dataManager.traveledDistance)
        }
        else {
            dataManager.plotMarkers.apply {
                if (markers.isNotEmpty()) {
                    if (markers.last().MarkerType != PlotMarkerType.CHARGE) {
                        startChargingSession(dataManager)
                        dataManager.plotMarkers.addMarker(PlotMarkerType.CHARGE, System.currentTimeMillis(), dataManager.traveledDistance)
                    }
                    else dataManager.ChargeTime.start()
                }
            }
        }
    }

    private fun startChargingSession(dataManager: DataManager) {
        dataManager.chargePlotLine.reset()
        dataManager.chargeStartDate = Date()
        dataManager.chargedEnergy = 0F
        dataManager.ChargeTime.reset()
        dataManager.ChargeTime.start()

        addChargeDataPoint(PlotLineMarkerType.BEGIN_SESSION, dataManager = dataManager)
    }

    private fun stopChargingSession(dataManager: DataManager) {
        if (dataManager == DataManagers.SINCE_CHARGE.dataManager) return
        refreshProperty(dataManager.CurrentPower.propertyId, dataManager)
        refreshProperty(dataManager.BatteryLevel.propertyId, dataManager)

        dataManager.ChargeTime.stop()

        addChargeDataPoint(PlotLineMarkerType.END_SESSION, dataManager = dataManager)

        val chargeCurve = ChargeCurve(
            dataManager.chargePlotLine.getDataPoints(PlotDimensionX.TIME),
            dataManager.chargeTime,
            dataManager.chargedEnergy,
            dataManager.ambientTemperature,
            dataManager.chargeStartDate
        )
        dataManager.chargeCurves.add(chargeCurve)

        if (dataManager != enumValues<DataManagers>().last().dataManager) return
        InAppLogger.log("Added Charge Curve to SINCE_CHARGE")
        DataManagers.SINCE_CHARGE.dataManager.chargeCurves.add(chargeCurve)
    }

    private fun pauseTrip(dataManager: DataManager) {
        dataManager.TravelTime.stop()
        val newPlotItem = if (dataManager.consumptionPlotDistanceDelta > 0) dataManager.consumptionPlotEnergyDelta / (dataManager.consumptionPlotDistanceDelta / 1000) else 0F
        addConsumptionDataPoint(newPlotItem, PlotLineMarkerType.END_SESSION, dataManager)
    }

    private fun resumeTrip(dataManager: DataManager) {
        dataManager.TravelTime.start()
        addConsumptionDataPoint(0F, PlotLineMarkerType.BEGIN_SESSION, dataManager)
    }

    private fun addChargeDataPoint(plotLineMarkerType: PlotLineMarkerType? = null, dataManager: DataManager) {
        dataManager.chargePlotLine.addDataPoint(
            -dataManager.currentPower / 1_000_000,
            System.currentTimeMillis(),
            dataManager.CurrentPower.timestamp,
            dataManager.traveledDistance,
            dataManager.stateOfCharge.toFloat(),
            dataManager.location?.altitude?.toFloat(),
            plotLineMarkerType = plotLineMarkerType,
            autoMarkerTimeDeltaThreshold = CHARGE_PLOT_MARKER_THRESHOLD_NANOS
        )

        if (dataManager.chargePlotLine.getDataPoints(PlotDimensionX.TIME).last().Marker == PlotLineMarkerType.BEGIN_SESSION) {
            val timeSpan = dataManager.chargePlotLine.getDataPoints(PlotDimensionX.TIME).last().EpochTime - dataManager.chargePlotLine.getDataPoints(PlotDimensionX.TIME).first().EpochTime
            dataManager.ChargeTime.reset()
            dataManager.ChargeTime.restore(timeSpan)
            dataManager.ChargeTime.start()
        }
    }

    private fun addConsumptionDataPoint(item: Float, plotLineMarkerType: PlotLineMarkerType? = null, dataManager: DataManager) {
        dataManager.consumptionPlotLine.addDataPoint(
            item,
            System.currentTimeMillis(),
            dataManager.CurrentSpeed.timestamp,
            dataManager.traveledDistance,
            dataManager.stateOfCharge.toFloat(),
            dataManager.location?.altitude?.toFloat(),
            plotLineMarkerType = plotLineMarkerType
        )
    }

    private fun registerCarPropertyCallbacks() {
        // InAppLogger.log("DataCollector.registerCarPropertyCallbacks")
        for (propertyId in DataManager.propertyIds) {
            carPropertyManager.registerCallback(
                carPropertyListener,
                propertyId,
                DataManager.sensorRateMap[propertyId]?: 0.0F
            )
        }
    }

    private fun refreshProperty(propertyId: Int, dataManager: DataManager) {
        val property = carPropertyManager.getProperty<Any>(propertyId, 0)
        getPropertyStatus(propertyId)
        dataManager.update(
            property.value,
            property.timestamp,
            propertyId,
            doLog = false,
            allowInvalidTimestamps = DataManager.allowInvalidTimestampsMap[propertyId] == true)
    }

    private fun getPropertyStatus(propertyId: Int): Int {
        val status = carPropertyManager.getProperty<Any>(propertyId, 0).status
        if (status != CarPropertyValue.STATUS_AVAILABLE) InAppLogger.log("PropertyStatus: $status")
        return status
    }

    private fun updateStatsNotification() {
        if (notificationsEnabled && appPreferences.notifications) {
            with(CarStatsViewer.notificationManager) {
                val dataManager = DataManagers.values()[appPreferences.mainViewTrip].dataManager
                val averageConsumption = dataManager.usedEnergy / (dataManager.traveledDistance/1000)

                var averageConsumptionString = String.format("%d Wh/km", averageConsumption.toInt())
                if (!appPreferences.consumptionUnit) {
                    averageConsumptionString = String.format(
                        Locale.ENGLISH,
                        "%.1f kWh/100km",
                        averageConsumption / 10)
                }
                if ((dataManager.traveledDistance <= 0)) averageConsumptionString = "N/A"

                val trackerName = getString(resources.getIdentifier(dataManager.printableName, "string", packageName))
                val message = String.format(
                    Locale.ENGLISH,
                    "Power: %.1f kW\n$trackerName:  Dist.: %.1f km,  Ø-Cons.: %s",
                    dataManager.currentPower / 1_000_000,
                    dataManager.traveledDistance / 1000,
                    averageConsumptionString
                )

                foregroundServiceNotification.setContentText(message)
                notify(FOREGROUND_NOTIFICATION_ID, foregroundServiceNotification.build())
            }
        } else if (notificationsEnabled && !appPreferences.notifications) {
            notificationsEnabled = false
            foregroundServiceNotification.setContentText("")
            CarStatsViewer.notificationManager.notify(FOREGROUND_NOTIFICATION_ID, foregroundServiceNotification.build())
        } else if (!notificationsEnabled && appPreferences.notifications) {
            notificationsEnabled = true
        }
    }

    private fun writeTripDataToFile(tripData: TripData, fileName: String) {
        val dir = File(applicationContext.filesDir, "TripData")
        if (!dir.exists()) {
            dir.mkdir()
        }

        try {
            val gpxFile = File(dir, "$fileName.json")
            val writer = FileWriter(gpxFile)
            writer.append(gson.toJson(tripData))
            writer.flush()
            writer.close()
            // InAppLogger.log("TRIP DATA: Saved $fileName.json")
        } catch (e: java.lang.Exception) {
            InAppLogger.log("TRIP DATA: Writing $fileName.json failed!")
            InAppLogger.log(e.stackTraceToString())
        }
    }

    private fun readTripDataFromFile(fileName: String): TripData? {

        InAppLogger.log("TRIP DATA: Reading $fileName.json")
        val startTime = System.currentTimeMillis()
        val dir = File(applicationContext.filesDir, "TripData")
        if (!dir.exists()) {
            InAppLogger.log("TRIP DATA: Directory TripData does not exist!")
            return null
        }

        val gpxFile = File(dir, "$fileName.json")
        if (!gpxFile.exists() && gpxFile.length() > 0) {
            InAppLogger.log("TRIP_DATA File $fileName.json does not exist!")
            return null
        }

        return try {
            InAppLogger.log("TRIP DATA: File size: %.1f kB".format(gpxFile.length() / 1024f))

            // val fileReader = FileReader(gpxFile)
            val tripData: TripData = Gson().fromJson(gpxFile.readText(), TripData::class.java)
            // fileReader.close()

            InAppLogger.log("TRIP DATA: Time to read: ${System.currentTimeMillis() - startTime} ms")

            tripData

        } catch (e: java.lang.Exception) {
            InAppLogger.log("Error reading File: $e")
            null
        }
    }

    private fun resetAutoTrips(previousDrivingState: Int, newDrivingState: Int, dataManager: DataManager) {
        // Handle resets on different dataManagers
        if (DataManagers.CURRENT_MONTH.dataManager == dataManager &&
            DataManagers.CURRENT_MONTH.doTrack &&
            newDrivingState == DrivingState.DRIVE) {
            // Reset if in different Month than start and save old month
            if (Date().month != DataManagers.CURRENT_MONTH.dataManager.tripStartDate.month) {
                InAppLogger.log("TRIP DATA: Saving past Month")
                writeTripDataToFile(
                    DataManagers.CURRENT_MONTH.dataManager.tripData!!,
                    "MonthData_${DataManagers.CURRENT_MONTH.dataManager.tripStartDate}_${DataManagers.CURRENT_MONTH.dataManager.tripStartDate.month + 1}"
                )
                DataManagers.CURRENT_MONTH.dataManager.reset()
                InAppLogger.log("Resetting ${DataManagers.CURRENT_MONTH.dataManager.printableName}")
            }
        }
        if (DataManagers.AUTO_DRIVE.dataManager == dataManager &&
            DataManagers.AUTO_DRIVE.doTrack &&
            newDrivingState == DrivingState.DRIVE) {
            // Reset if parked for x hours
            if (DataManagers.AUTO_DRIVE.dataManager.plotMarkers.markers.isNotEmpty()) {
                if (
                    DataManagers.AUTO_DRIVE.dataManager.plotMarkers.markers.last().EndTime == null &&
                    DataManagers.AUTO_DRIVE.dataManager.plotMarkers.markers.last().StartTime < (Date().time - TimeUnit.HOURS.toMillis(AUTO_RESET_TIME_HOURS))
                ){
                    DataManagers.AUTO_DRIVE.dataManager.reset()
                    InAppLogger.log("Resetting ${DataManagers.AUTO_DRIVE.dataManager.printableName}")
                }
            }
        }
        if (DataManagers.SINCE_CHARGE.dataManager == dataManager &&
            DataManagers.SINCE_CHARGE.doTrack &&
            previousDrivingState == DrivingState.CHARGE) {
            DataManagers.SINCE_CHARGE.dataManager.reset()
            InAppLogger.log("Resetting ${DataManagers.SINCE_CHARGE.dataManager.printableName}")
        }
    }

    private val ignitionList = listOf<String>(
        "UNKNOWN", "Lock", "Off", "Accessories", "On", "Start"
    )
}
