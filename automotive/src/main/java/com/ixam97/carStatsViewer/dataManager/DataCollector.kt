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
import com.ixam97.carStatsViewer.mailSender.MailSender
import com.ixam97.carStatsViewer.plot.enums.*
import kotlinx.coroutines.*
import java.io.File
import java.io.FileWriter
import java.lang.Runnable
import java.util.*
import java.util.concurrent.TimeUnit
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
        private const val AUTO_RESET_TIME_HOURS = 5L
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

    init {
        startupTimestamp = System.nanoTime()
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                getString(R.string.save_trip_data_broadcast) -> {
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

    private lateinit var statsNotification: Notification.Builder
    private lateinit var foregroundServiceNotification: Notification.Builder

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        var tripRestoreComplete = false
        CoroutineScope(Dispatchers.IO).launch {
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

        /** Get vehicle name to enable dev mode in emulator */
        val carName = carPropertyManager.getProperty<String>(VehiclePropertyIds.INFO_MODEL, 0).value.toString()
        if (carName == "Speedy Model") {
            Toast.makeText(this, "Emulator Mode", Toast.LENGTH_LONG).show()
            emulatorMode = true
            // CurrentTripDataManager.update(VehicleGear.GEAR_PARK, System.nanoTime(), CurrentTripDataManager.CurrentGear.propertyId)
        }

        notificationTitleString = resources.getString(R.string.notification_title)
        statsNotification.setContentTitle(notificationTitleString).setContentIntent(mainActivityPendingIntent)

        if (notificationsEnabled) {
            with(NotificationManagerCompat.from(this)) {
                notify(STATS_NOTIFICATION_ID, statsNotification.build())
            }
        }

        registerCarPropertyCallbacks()

        notificationTimerHandler = Handler(Looper.getMainLooper())
        notificationTimerHandler.post(updateStatsNotificationTask)
        saveTripDataTimerHandler = Handler(Looper.getMainLooper())
        saveTripDataTimerHandler.postDelayed(saveTripDataTask, AUTO_SAVE_INTERVAL_MILLIS)

        registerReceiver(broadcastReceiver, IntentFilter(getString(R.string.save_trip_data_broadcast)))
        registerReceiver(carPropertyEmulatorReceiver, IntentFilter(getString(R.string.VHAL_emulator_broadcast)))

        DataManagers.values().filter { it.doTrack }.forEach {
            for (propertyId in DataManager.propertyIds) {
                refreshProperty(propertyId, it.dataManager)
            }
            it.dataManager.consumptionPlotLine.baseLineAt.add(0f)
            driveStateUpdater(it.dataManager)
            speedUpdater(it.dataManager)
            powerUpdater(it.dataManager)
        }
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

    /** Update DataManagers on new VHAL event */
    private val carPropertyListener = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(carPropertyValue: CarPropertyValue<*>) {
            DataManagers.values().forEach {
                if (it.dataManager.update(
                        carPropertyValue,
                        DO_LOG,
                        valueMustChange = false,
                        allowInvalidTimestamps = false
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
            dataManager.CurrentIgnitionState.propertyId -> driveStateUpdater(dataManager)
            dataManager.ChargePortConnected.propertyId  -> driveStateUpdater(dataManager)
        }
    }

    private fun powerUpdater(dataManager: DataManager) {
        when (dataManager.driveState) {
            DrivingState.DRIVE -> {
                if (!dataManager.CurrentPower.isInitialValue) {
                    val usedEnergyDelta = (dataManager.currentPower / 1_000) * ((dataManager.CurrentPower.timeDelta / 3.6E12).toFloat())
                    dataManager.usedEnergy += usedEnergyDelta
                    dataManager.consumptionPlotEnergyDelta += usedEnergyDelta
                }
            }
            DrivingState.CHARGE -> {
                if (!dataManager.CurrentPower.isInitialValue && dataManager.CurrentPower.timeDelta < CHARGE_PLOT_MARKER_THRESHOLD_NANOS) {
                    val chargedEnergyDelta = (dataManager.currentPower / 1_000) * ((dataManager.CurrentPower.timeDelta / 3.6E12).toFloat())
                    dataManager.chargedEnergy -= chargedEnergyDelta
                    if (dataManager.chargePlotTimeDelta < CHARGE_PLOT_UPDATE_INTERVAL_MILLIS * 1_000_000) {
                        dataManager.chargePlotTimeDelta += dataManager.CurrentPower.timeDelta
                    } else {
                        refreshProperty(dataManager.BatteryLevel.propertyId, dataManager)
                        addChargeDataPoint(dataManager = dataManager)
                        dataManager.chargePlotTimeDelta = 0L
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

    private fun speedUpdater(dataManager: DataManager) {
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
            if (dataManager.currentSpeed.absoluteValue >= 1 && (dataManager.CurrentSpeed.lastValue as Float).absoluteValue < 1) {
                // Drive started -> Distraction optimization
                Log.i("Drive", "started")
            } else if (dataManager.currentSpeed.absoluteValue < 1 && (dataManager.CurrentSpeed.lastValue as Float).absoluteValue > 1) {
                // Drive ended
                Log.i("Drive", "ended")
            }

            if (dataManager.consumptionPlotDistanceDelta < CONSUMPTION_PLOT_UPDATE_DISTANCE) {
                dataManager.consumptionPlotDistanceDelta += traveledDistanceDelta
            } else {
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
            writeTripDataToFile(dataManager.tripData!!, dataManager.printableName)
            sendBroadcast(Intent(getString(R.string.ui_update_plot_broadcast)))
        }
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
        else dataManager.ChargeTime.start()
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
            dataManager.chargePlotLine.getDataPoints(PlotDimension.TIME),
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
            dataManager.CurrentPower.timestamp,
            dataManager.traveledDistance,
            dataManager.stateOfCharge.toFloat(),
            plotLineMarkerType = plotLineMarkerType,
            autoMarkerTimeDeltaThreshold = CHARGE_PLOT_MARKER_THRESHOLD_NANOS
        )
    }

    private fun addConsumptionDataPoint(item: Float, plotLineMarkerType: PlotLineMarkerType? = null, dataManager: DataManager) {
        dataManager.consumptionPlotLine.addDataPoint(
            item,
            dataManager.CurrentSpeed.timestamp,
            dataManager.traveledDistance,
            dataManager.stateOfCharge.toFloat(),
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
        dataManager.update(
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
                writeTripDataToFile(
                    DataManagers.CURRENT_MONTH.dataManager.tripData!!,
                    "MonthData_${DataManagers.CURRENT_MONTH.dataManager.tripStartDate}_%.1f".format(DataManagers.CURRENT_MONTH.dataManager.tripStartDate.month + 1)
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
}