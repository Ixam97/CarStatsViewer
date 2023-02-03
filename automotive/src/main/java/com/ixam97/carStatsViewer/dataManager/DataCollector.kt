package com.ixam97.carStatsViewer.dataManager

import com.ixam97.carStatsViewer.*
import android.app.*
import android.car.Car
import android.car.VehicleGear
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.*
import android.graphics.BitmapFactory
import android.os.*
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import com.google.gson.Gson
import com.ixam97.carStatsViewer.activities.emulatorMode
import com.ixam97.carStatsViewer.appPreferences.AppPreferences
import com.ixam97.carStatsViewer.plot.enums.*
import kotlinx.coroutines.*
import java.io.File
import java.io.FileWriter
import java.lang.Runnable
import kotlin.math.absoluteValue

class DataCollector : Service() {
    companion object {
        lateinit var mainActivityPendingIntent: PendingIntent
        val CurrentTripDataManager = DataManagers.CURRENT_TRIP.dataManager
        private const val DO_LOG = true
        private const val CHANNEL_ID = "TestChannel"
        private const val STATS_NOTIFICATION_ID = 1
        private const val FOREGROUND_NOTIFICATION_ID = 2
        private const val NOTIFICATION_TIMER_HANDLER_DELAY_MILLIS = 1_000L
        private const val CONSUMPTION_PLOT_UPDATE_DISTANCE = 100
        private const val CHARGE_PLOT_UPDATE_INTERVAL_MILLIS = 2_000L
        private const val CHARGE_PLOT_MARKER_THRESHOLD_NANOS = 10_000_000_000L // 2 times CHARGE_PLOT_UPDATE_INTERVAL_MILLIS in nanos
        private const val AUTO_SAVE_INTERVAL_MILLIS = 30_000L
    }

    private var startupTimestamp: Long = 0L

    private var lastNotificationTimeMillis = 0L

    private var notificationCounter = 0

    private lateinit var appPreferences: AppPreferences

    private var notificationsEnabled = true

    private lateinit var car: Car
    private lateinit var carPropertyManager: CarPropertyManager

    private lateinit var notificationTitleString: String

    private lateinit var notificationTimerHandler: Handler
    private lateinit var saveTripDataTimerHandler: Handler

    private var consumptionPlotEnergyDelta = 0F
    private var consumptionPlotDistanceDelta = 0F
    private var chargePlotTimeDelta = 0L

    init {
        startupTimestamp = System.nanoTime()
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                getString(R.string.save_trip_data_broadcast) -> {
                    writeTripDataToFile(CurrentTripDataManager.tripData!!, getString(R.string.file_name_current_trip_data))
                }
                else -> {}
            }
        }
    }

    private val saveTripDataTask = object : Runnable {
        override fun run() {
            writeTripDataToFile(CurrentTripDataManager.tripData!!, getString(R.string.file_name_current_trip_data))
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

    private lateinit var statsNotification: Notification.Builder
    private lateinit var foregroundServiceNotification: Notification.Builder

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        var tripRestoreComplete = false
        CoroutineScope(Dispatchers.IO).launch {
            val mPrevTripData = readTripDataFromFile(getString(R.string.file_name_current_trip_data))
            runBlocking {
                if (mPrevTripData != null) {
                    CurrentTripDataManager.tripData = mPrevTripData
                    sendBroadcast(Intent(getString(R.string.ui_update_plot_broadcast)))
                } else {
                    InAppLogger.log("No trip file read!")
                    // Toast.makeText(applicationContext ,R.string.toast_file_read_error, Toast.LENGTH_LONG).show()
                }
                tripRestoreComplete = true
            }
        }

        while (!tripRestoreComplete) {
            // Wait for completed restore before doing anything
        }

        createNotificationChannel()

        foregroundServiceNotification = Notification.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.foreground_service_info))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)

        statsNotification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Title")
            .setContentText("Test Notification from Car Stats Viewer")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.ic_launcher_foreground))
            .setStyle(Notification.MediaStyle())
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .setOngoing(true)

        // InAppLogger.log(String.format( "DataCollector.onCreate in Thread: %s", Thread.currentThread().name))

        appPreferences = AppPreferences(applicationContext)

        notificationsEnabled = appPreferences.notifications

        car = Car.createCar(this)
        carPropertyManager = car.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager

        CurrentTripDataManager.maxBatteryLevel = carPropertyManager.getFloatProperty(VehiclePropertyIds.INFO_EV_BATTERY_CAPACITY, 0)

        /** Get vehicle name to enable dev mode in emulator */
        val carName = carPropertyManager.getProperty<String>(VehiclePropertyIds.INFO_MODEL, 0).value.toString()
        if (carName == "Speedy Model") {
            Toast.makeText(this, "Emulator Mode", Toast.LENGTH_LONG).show()
            emulatorMode = true
            CurrentTripDataManager.update(VehicleGear.GEAR_PARK, System.nanoTime(), CurrentTripDataManager.CurrentGear.propertyId)
        }

        notificationTitleString = resources.getString(R.string.notification_title)
        statsNotification.setContentTitle(notificationTitleString).setContentIntent(mainActivityPendingIntent)

        if (notificationsEnabled) {
            with(NotificationManagerCompat.from(this)) {
                notify(STATS_NOTIFICATION_ID, statsNotification.build())
            }
        }

        CurrentTripDataManager.consumptionPlotLine.baseLineAt.add(0f)

        registerCarPropertyCallbacks()

        notificationTimerHandler = Handler(Looper.getMainLooper())
        notificationTimerHandler.post(updateStatsNotificationTask)
        saveTripDataTimerHandler = Handler(Looper.getMainLooper())
        saveTripDataTimerHandler.postDelayed(saveTripDataTask, AUTO_SAVE_INTERVAL_MILLIS)

        registerReceiver(broadcastReceiver, IntentFilter(getString(R.string.save_trip_data_broadcast)))
        registerReceiver(carPropertyEmulatorReceiver, IntentFilter(getString(R.string.VHAL_emulator_broadcast)))
    }

    override fun onDestroy() {
        super.onDestroy()
        // .log("DataCollector.onDestroy")
        sendBroadcast(Intent(getString(R.string.save_trip_data_broadcast)))
        unregisterReceiver(broadcastReceiver)
        car.disconnect()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        // InAppLogger.log("DataCollector.onStartCommand")
        startForeground(FOREGROUND_NOTIFICATION_ID, foregroundServiceNotification.build())
        return START_STICKY
    }

    private val carPropertyListener = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(carPropertyValue: CarPropertyValue<*>) {
            if (CurrentTripDataManager.update(
                    carPropertyValue,
                    DO_LOG,
                    valueMustChange = false,
                    allowInvalidTimestamps = false
                ) == CurrentTripDataManager.VALID) {
                handleCarPropertyListenerEvent(carPropertyValue.propertyId)
            }
            // handleCarPropertyListenerEvent()
        }
        override fun onErrorEvent(propertyId: Int, zone: Int) {
            Log.w("carPropertyGenericListener","Received error car property event, propId=$propertyId")
        }
    }

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
                    if (CurrentTripDataManager.update(value, timestamp, propertyId, DO_LOG, valueMustChange = false) == CurrentTripDataManager.VALID)
                        handleCarPropertyListenerEvent(propertyId)
                }
            }
        }
    }

    /** Handle incoming property changes by property ID */
    private fun handleCarPropertyListenerEvent(propertyId: Int) {
        // Always update the drive state to not miss any changes
        // driveStateUpdater()
        when (propertyId) {
            CurrentTripDataManager.CurrentPower.propertyId         -> powerUpdater()
            CurrentTripDataManager.CurrentSpeed.propertyId         -> {
                speedUpdater()
                driveStateUpdater()
            }
            // CurrentTripDataManager.CurrentIgnitionState.propertyId -> driveStateUpdater()
            // CurrentTripDataManager.ChargePortConnected.propertyId  -> driveStateUpdater()
        }
    }

    private fun handleCarPropertyListenerEvent() {
        for (propertyId in CurrentTripDataManager.getVehiclePropertyIds()) {
            val newValue = carPropertyManager.getProperty<Any>(propertyId, 0)
            CurrentTripDataManager.update(newValue.value, System.nanoTime(), propertyId, doLog = true)
        }
        powerUpdater()
        speedUpdater()
        driveStateUpdater()
    }

    private fun powerUpdater() {
        when (CurrentTripDataManager.driveState) {
            DrivingState.DRIVE -> {
                if (!CurrentTripDataManager.CurrentPower.isInitialValue) {
                    val usedEnergyDelta = (CurrentTripDataManager.currentPower / 1_000) * ((CurrentTripDataManager.CurrentPower.timeDelta / 3.6E12).toFloat())
                    CurrentTripDataManager.usedEnergy += usedEnergyDelta
                    consumptionPlotEnergyDelta += usedEnergyDelta
                }
            }
            DrivingState.CHARGE -> {
                if (!CurrentTripDataManager.CurrentPower.isInitialValue && CurrentTripDataManager.CurrentPower.timeDelta < CHARGE_PLOT_MARKER_THRESHOLD_NANOS) {
                    val chargedEnergyDelta = (CurrentTripDataManager.currentPower / 1_000) * ((CurrentTripDataManager.CurrentPower.timeDelta / 3.6E12).toFloat())
                    CurrentTripDataManager.chargedEnergy -= chargedEnergyDelta
                    if (chargePlotTimeDelta < CHARGE_PLOT_UPDATE_INTERVAL_MILLIS * 1_000_000) {
                        chargePlotTimeDelta += CurrentTripDataManager.CurrentPower.timeDelta
                    } else {
                        CurrentTripDataManager.chargePlotLine.addDataPoint(
                            -CurrentTripDataManager.currentPower / 1_000_000,
                            CurrentTripDataManager.CurrentPower.timestamp,
                            CurrentTripDataManager.traveledDistance,
                            CurrentTripDataManager.stateOfCharge.toFloat(),
                            autoMarkerTimeDeltaThreshold = CHARGE_PLOT_MARKER_THRESHOLD_NANOS
                        )
                        chargePlotTimeDelta = 0L
                        sendBroadcast(Intent(getString(R.string.ui_update_plot_broadcast)))
                    }

                } else {
                    InAppLogger.log("DATA COLLECTOR: Discarded charge plot data Point due to large time delta!")
                }
            }
            else -> {
                // Supplemental energy usage?
            }
        }
    }

    private fun speedUpdater() {
        if (emulatorMode) {
            val emulatePowerIntent = Intent(getString(R.string.VHAL_emulator_broadcast)).apply {
                putExtra(EmulatorIntentExtras.PROPERTY_ID, CurrentTripDataManager.CurrentPower.propertyId)
                putExtra(EmulatorIntentExtras.TYPE, EmulatorIntentExtras.TYPE_FLOAT)
                putExtra(EmulatorIntentExtras.VALUE, carPropertyManager.getFloatProperty(CurrentTripDataManager.CurrentPower.propertyId, 0))
            }
            sendBroadcast(emulatePowerIntent)
        }
        if (!CurrentTripDataManager.CurrentSpeed.isInitialValue && CurrentTripDataManager.driveState == DrivingState.DRIVE) {
            val traveledDistanceDelta = (CurrentTripDataManager.currentSpeed.absoluteValue * CurrentTripDataManager.CurrentSpeed.timeDelta.toFloat()) / 1_000_000_000F
            CurrentTripDataManager.traveledDistance += traveledDistanceDelta
            if (CurrentTripDataManager.currentSpeed.absoluteValue >= 1 && (CurrentTripDataManager.CurrentSpeed.lastValue as Float).absoluteValue < 1) {
                // Drive started -> Distraction optimization
                Log.i("Drive", "started")
            } else if (CurrentTripDataManager.currentSpeed.absoluteValue < 1 && (CurrentTripDataManager.CurrentSpeed.lastValue as Float).absoluteValue > 1) {
                // Drive ended
                Log.i("Drive", "ended")
            }

            if (consumptionPlotDistanceDelta < CONSUMPTION_PLOT_UPDATE_DISTANCE) {
                consumptionPlotDistanceDelta += traveledDistanceDelta
            } else {
                if (CurrentTripDataManager.driveState == DrivingState.DRIVE) {
                    CurrentTripDataManager.consumptionPlotLine.addDataPoint(
                        if(consumptionPlotDistanceDelta > 0) consumptionPlotEnergyDelta / (consumptionPlotDistanceDelta / 1000) else 0F,
                        CurrentTripDataManager.CurrentSpeed.timestamp,
                        CurrentTripDataManager.traveledDistance,
                        CurrentTripDataManager.stateOfCharge.toFloat()
                    )
                }
                consumptionPlotDistanceDelta = 0F
                consumptionPlotEnergyDelta = 0F
                sendBroadcast(Intent(getString(R.string.ui_update_plot_broadcast)))
            }
        }
    }

    private fun driveStateUpdater() {
        // Get real current properties to avoid old values after hibernation.
        refreshProperty(CurrentTripDataManager.CurrentIgnitionState.propertyId)
        refreshProperty(CurrentTripDataManager.ChargePortConnected.propertyId)

        val previousDrivingState = CurrentTripDataManager.DriveState.lastDriveState
        if (CurrentTripDataManager.DriveState.hasChanged()) {
            InAppLogger.log("DRIVE STATE: ${DrivingState.nameMap[previousDrivingState]} -> ${DrivingState.nameMap[CurrentTripDataManager.driveState]}")
            when (CurrentTripDataManager.driveState) {
                DrivingState.DRIVE -> {
                    resumeTrip()
                    if (previousDrivingState == DrivingState.CHARGE) stopChargingSession()
                    if (previousDrivingState != DrivingState.UNKNOWN) CurrentTripDataManager.plotMarkers.endMarker(System.nanoTime())
                }
                DrivingState.CHARGE -> {
                    if (previousDrivingState == DrivingState.DRIVE) pauseTrip()
                    if (previousDrivingState != DrivingState.UNKNOWN){
                        startChargingSession()
                        CurrentTripDataManager.plotMarkers.addMarker(PlotMarkerType.CHARGE, System.nanoTime())
                    }
                    else CurrentTripDataManager.ChargeTime.start()
                }
                DrivingState.PARKED -> {
                    if (previousDrivingState == DrivingState.DRIVE){
                        pauseTrip()
                        CurrentTripDataManager.plotMarkers.addMarker(PlotMarkerType.PARK, System.nanoTime())
                    }
                    if (previousDrivingState == DrivingState.CHARGE) stopChargingSession()

                }
            }
            writeTripDataToFile(CurrentTripDataManager.tripData!!, getString(R.string.file_name_current_trip_data))
            sendBroadcast(Intent(getString(R.string.ui_update_plot_broadcast)))
        }
    }

    private fun startChargingSession() {
        CurrentTripDataManager.chargePlotLine.reset()
        CurrentTripDataManager.chargedEnergy = 0F
        CurrentTripDataManager.ChargeTime.reset()
        CurrentTripDataManager.ChargeTime.start()

        CurrentTripDataManager.chargePlotLine.addDataPoint(
            CurrentTripDataManager.currentPower,
            CurrentTripDataManager.CurrentPower.timestamp,
            CurrentTripDataManager.traveledDistance,
            CurrentTripDataManager.stateOfCharge.toFloat(),
            plotLineMarkerType = PlotLineMarkerType.BEGIN_SESSION,
            autoMarkerTimeDeltaThreshold = CHARGE_PLOT_MARKER_THRESHOLD_NANOS
        )
    }

    private fun stopChargingSession() {
        refreshProperty(CurrentTripDataManager.CurrentPower.propertyId)
        refreshProperty(CurrentTripDataManager.BatteryLevel.propertyId)

        CurrentTripDataManager.ChargeTime.stop()
        CurrentTripDataManager.chargePlotLine.addDataPoint(
            CurrentTripDataManager.currentPower,
            System.nanoTime(),
            CurrentTripDataManager.traveledDistance,
            CurrentTripDataManager.stateOfCharge.toFloat(),
            plotLineMarkerType = PlotLineMarkerType.END_SESSION,
            autoMarkerTimeDeltaThreshold = CHARGE_PLOT_MARKER_THRESHOLD_NANOS
        )

        CurrentTripDataManager.chargeCurves.add(
            ChargeCurve(
                CurrentTripDataManager.chargePlotLine.getDataPoints(PlotDimension.TIME),
                CurrentTripDataManager.chargeTime,
                CurrentTripDataManager.chargedEnergy,
                CurrentTripDataManager.ambientTemperature
            )
        )
    }

    private fun pauseTrip() {
        CurrentTripDataManager.TravelTime.stop()
        val newPlotItem = if (consumptionPlotDistanceDelta > 0) consumptionPlotEnergyDelta / (consumptionPlotDistanceDelta / 1000) else 0F
        CurrentTripDataManager.consumptionPlotLine.addDataPoint(
            newPlotItem,
            CurrentTripDataManager.CurrentSpeed.timestamp,
            CurrentTripDataManager.traveledDistance,
            CurrentTripDataManager.stateOfCharge.toFloat(),
            plotLineMarkerType = PlotLineMarkerType.END_SESSION
        )
    }

    private fun resumeTrip() {
        CurrentTripDataManager.TravelTime.start()
        CurrentTripDataManager.consumptionPlotLine.addDataPoint(
            0F,
            CurrentTripDataManager.CurrentSpeed.timestamp,
            CurrentTripDataManager.traveledDistance,
            CurrentTripDataManager.stateOfCharge.toFloat(),
            plotLineMarkerType = PlotLineMarkerType.BEGIN_SESSION
        )
    }

    private fun registerCarPropertyCallbacks() {
        // InAppLogger.log("DataCollector.registerCarPropertyCallbacks")
        for (propertyId in CurrentTripDataManager.getVehiclePropertyIds()) {
            carPropertyManager.registerCallback(
                carPropertyListener,
                propertyId,
                CurrentTripDataManager.sensorRateMap[propertyId]?: 0.0F
            )
        }
    }

    private fun refreshProperty(propertyId: Int) {
        CurrentTripDataManager.update(
            carPropertyManager.getProperty<Any>(propertyId, 0).value,
            System.nanoTime(),
            propertyId,
            doLog = true)
    }

    private fun createNotificationChannel() {
        val name = "TestChannel"
        val descriptionText = "TestChannel"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun updateStatsNotification() {
        if (notificationsEnabled && appPreferences.notifications) {
            with(NotificationManagerCompat.from(this)) {
                val averageConsumption = CurrentTripDataManager.usedEnergy / (CurrentTripDataManager.traveledDistance/1000)

                var averageConsumptionString = String.format("%d Wh/km", averageConsumption.toInt())
                if (!appPreferences.consumptionUnit) {
                    averageConsumptionString = String.format(
                        "%.1f kWh/100km",
                        averageConsumption / 10)
                }
                if ((CurrentTripDataManager.traveledDistance <= 0)) averageConsumptionString = "N/A"

                notificationCounter++

                val message = String.format(
                    "P:%.1f kW, D: %.3f km, Ø: %s",
                    CurrentTripDataManager.currentPower / 1_000_000,
                    CurrentTripDataManager.traveledDistance / 1000,
                    averageConsumptionString
                )

                statsNotification.setContentText(message)
                foregroundServiceNotification.setContentText(message)
                notify(STATS_NOTIFICATION_ID, statsNotification.build())
                notify(FOREGROUND_NOTIFICATION_ID, foregroundServiceNotification.build())
            }
        } else if (notificationsEnabled && !appPreferences.notifications) {
            notificationsEnabled = false
            with(NotificationManagerCompat.from(this)) {
                cancel(STATS_NOTIFICATION_ID)
            }
            foregroundServiceNotification.setContentText(getString(R.string.foreground_service_info))
            NotificationManagerCompat.from(this).notify(FOREGROUND_NOTIFICATION_ID, foregroundServiceNotification.build())
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
            writer.append(Gson().toJson(tripData))
            writer.flush()
            writer.close()
            // InAppLogger.log("TRIP DATA: Saved $fileName.json in Thread ${Thread.currentThread().name}")
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    private fun readTripDataFromFile(fileName: String): TripData? {

        InAppLogger.log("TRIP DATA: Reading $fileName.json in Thread ${Thread.currentThread().name}")
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
            InAppLogger.log(e.toString())
            null
        }
    }
}