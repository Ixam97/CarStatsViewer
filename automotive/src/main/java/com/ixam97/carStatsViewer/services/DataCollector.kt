package com.ixam97.carStatsViewer.services


import com.ixam97.carStatsViewer.objects.*
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
import com.ixam97.carStatsViewer.activities.emulatorPowerSign
import com.ixam97.carStatsViewer.plot.enums.*
import kotlinx.coroutines.*
import java.io.File
import java.io.FileWriter
import java.lang.Runnable
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap
import kotlin.math.absoluteValue

lateinit var mainActivityPendingIntent: PendingIntent

class DataCollector : Service() {
    companion object {
        private const val CHANNEL_ID = "TestChannel"
        private const val STATS_NOTIFICATION_ID = 1
        private const val FOREGROUND_NOTIFICATION_ID = 2
        private const val NOTIFICATION_TIMER_HANDLER_DELAY_MILLIS = 1_000L
        private const val SAVE_TRIP_DATA_TIMER_HANDLER_DELAY_MILLIS = 30_000L
        private const val CHARGE_CURVE_UPDATE_INTERVAL_MILLIS = 10_000L
    }

    private var startupTimestamp: Long = 0L
    private var lastPowerValueTimestamp: Long = 0L
    private var lastSpeedValueTimestamp: Long = 0L

    private var consumptionPlotTracking = false
    private var lastNotificationTimeMillis = 0L

    private var chargeStartTimeNanos = 0L

    private var notificationCounter = 0

    private lateinit var appPreferences: AppPreferences

    private val mBinder: LocalBinder = LocalBinder()

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
                    val tripDataToSave = DataHolder.getTripData()
                    writeTripDataToFile(tripDataToSave, getString(R.string.file_name_current_trip_data))
                }
                else -> {}
            }
        }
    }

    private val saveTripDataTask = object : Runnable {
        override fun run() {
            // sendBroadcast(Intent(getString(R.string.save_trip_data_broadcast)))
            saveTripDataTimerHandler.postDelayed(this, SAVE_TRIP_DATA_TIMER_HANDLER_DELAY_MILLIS)
        }
    }

    private val updateStatsNotificationTask = object : Runnable {
        override fun run() {
            updateStatsNotification()
            InAppLogger.logNotificationUpdate()

            val currentNotificationTimeMillis = System.currentTimeMillis()
            if (DataHolder.currentGear != VehicleGear.GEAR_PARK && lastNotificationTimeMillis > 0) DataHolder.travelTimeMillis += currentNotificationTimeMillis - lastNotificationTimeMillis
            if (DataHolder.chargePortConnected && lastNotificationTimeMillis > 0) DataHolder.chargeTimeMillis += currentNotificationTimeMillis - lastNotificationTimeMillis
            lastNotificationTimeMillis = currentNotificationTimeMillis

            // val ignitionState = carPropertyManager.getIntProperty(VehiclePropertyIds.IGNITION_STATE, 0)
            // val ignitionString = when (ignitionState) {
            //     VehicleIgnitionState.LOCK -> "LOCK"
            //     VehicleIgnitionState.OFF -> "OFF"
            //     VehicleIgnitionState.ACC -> "ACC"
            //     VehicleIgnitionState.ON -> "ON"
            //     VehicleIgnitionState.START -> "START"
            //     else -> "UNDEFINED"
            // }
            // InAppLogger.log(String.format("IAmAlive - Ignition state: %s, current power: %f W", ignitionString, DataHolder.currentPowermW / 1_000))

            notificationTimerHandler.postDelayed(this, NOTIFICATION_TIMER_HANDLER_DELAY_MILLIS)
        }
    }

    private lateinit var statsNotification: Notification.Builder
    private lateinit var foregroundServiceNotification: Notification.Builder

    inner class LocalBinder : Binder() {
        fun getService(): DataCollector = this@DataCollector
    }

    override fun onBind(intent: Intent): IBinder? {
        return mBinder
    }

    override fun onCreate() {
        super.onCreate()

        var tripRestoreComplete = false
        CoroutineScope(Dispatchers.IO).launch {
            val mPrevTripData = readTripDataFromFile(getString(R.string.file_name_current_trip_data))
            runBlocking {
                if (mPrevTripData != null) {
                    DataHolder.applyTripData(mPrevTripData)
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

        InAppLogger.log(String.format(
            "DataCollector.onCreate in Thread: %s",
            Thread.currentThread().name))

        // sharedPref = this.getSharedPreferences(
        //     getString(R.string.preferences_file_key),
        //     Context.MODE_PRIVATE)

        appPreferences = AppPreferences(applicationContext)

        notificationsEnabled = appPreferences.notifications

        car = Car.createCar(this)
        carPropertyManager = car.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager

        DataHolder.maxBatteryCapacity = carPropertyManager.getFloatProperty(VehiclePropertyIds.INFO_EV_BATTERY_CAPACITY, 0)
        DataHolder.currentBatteryCapacity = carPropertyManager.getFloatProperty(VehiclePropertyIds.EV_BATTERY_LEVEL, 0)
        DataHolder.currentGear = carPropertyManager.getIntProperty(VehiclePropertyIds.GEAR_SELECTION, 0)

        // if (DataHolder.resetTimestamp == 0L)  DataHolder.resetTimestamp = System.nanoTime()
        // if (DataHolder.currentGear == VehicleGear.GEAR_PARK) DataHolder.parkTimestamp = DataHolder.resetTimestamp

        /** Get vehicle name to enable dev mode in emulator */
        val carName = carPropertyManager.getProperty<String>(VehiclePropertyIds.INFO_MODEL, 0).value.toString()
        if (carName == "Speedy Model") {
            Toast.makeText(this, "Emulator Mode", Toast.LENGTH_LONG).show()
            emulatorMode = true
            DataHolder.currentGear = VehicleGear.GEAR_PARK
        }

        notificationTitleString = resources.getString(R.string.notification_title)
        statsNotification.setContentTitle(notificationTitleString).setContentIntent(mainActivityPendingIntent)

        if (notificationsEnabled) {
            with(NotificationManagerCompat.from(this)) {
                notify(STATS_NOTIFICATION_ID, statsNotification.build())
            }
        }

        DataHolder.consumptionPlotLine.baseLineAt.add(0f)

        registerCarPropertyCallbacks()

        notificationTimerHandler = Handler(Looper.getMainLooper())
        notificationTimerHandler.post(updateStatsNotificationTask)
        saveTripDataTimerHandler = Handler(Looper.getMainLooper())
        saveTripDataTimerHandler.postDelayed(saveTripDataTask, SAVE_TRIP_DATA_TIMER_HANDLER_DELAY_MILLIS)

        registerReceiver(broadcastReceiver, IntentFilter(getString(R.string.save_trip_data_broadcast)))
    }

    override fun onDestroy() {
        super.onDestroy()
        InAppLogger.log("DataCollector.onDestroy")
        sendBroadcast(Intent(getString(R.string.save_trip_data_broadcast)))
        unregisterReceiver(broadcastReceiver)
        car.disconnect()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        InAppLogger.log("DataCollector.onStartCommand")
        startForeground(FOREGROUND_NOTIFICATION_ID, foregroundServiceNotification.build())
        return START_STICKY
    }

    private fun registerCarPropertyCallbacks() {

        InAppLogger.log("DataCollector.registerCarPropertyCallbacks")

        carPropertyManager.registerCallback(
            carPropertyPowerListener,
            VehiclePropertyIds.EV_BATTERY_INSTANTANEOUS_CHARGE_RATE,
            CarPropertyManager.SENSOR_RATE_FASTEST
        )

        carPropertyManager.registerCallback(
            carPropertySpeedListener,
            VehiclePropertyIds.PERF_VEHICLE_SPEED,
            CarPropertyManager.SENSOR_RATE_FASTEST
        )

        carPropertyManager.registerCallback(
            carPropertyGenericListener,
            VehiclePropertyIds.EV_BATTERY_LEVEL,
            CarPropertyManager.SENSOR_RATE_ONCHANGE
        )

        carPropertyManager.registerCallback(
            carPropertyGenericListener,
            VehiclePropertyIds.EV_CHARGE_PORT_CONNECTED,
            CarPropertyManager.SENSOR_RATE_ONCHANGE
        )

        carPropertyManager.registerCallback(
            carPropertyGenericListener,
            VehiclePropertyIds.GEAR_SELECTION,
            CarPropertyManager.SENSOR_RATE_ONCHANGE
        )
    }

    private val timeDifferenceStore: HashMap<Int, Long> = HashMap()

    private fun timeDifference(value: CarPropertyValue<*>, maxDifferenceInMilliseconds: Int, timestamp: Long) : Float? {
        var timeDifference : Long? = null

        if (timeDifferenceStore.containsKey(value.propertyId)) {
            timeDifference = timestamp - timeDifferenceStore[value.propertyId]!!
        }

        timeDifferenceStore[value.propertyId] = timestamp

        return when {
            timeDifference == null || timeDifference > (maxDifferenceInMilliseconds * 1_000_000) -> null
            else -> timeDifference.toFloat() / 1_000_000
        }
    }

    private fun timeDifference(value: CarPropertyValue<*>, maxDifferenceInMilliseconds: Int) : Float? {
        return timeDifference(value, maxDifferenceInMilliseconds, value.timestamp)
    }

    private val timeTriggerStore: HashMap<Int, Long> = HashMap()

    private fun timerTriggered(value: CarPropertyValue<*>, timerInMilliseconds: Float, timestamp: Long): Boolean {
        var timeTriggered = true

        if (timeTriggerStore.containsKey(value.propertyId)) {
            timeTriggered = timeTriggerStore[value.propertyId]?.plus(timerInMilliseconds * 1_000_000)!! <= timestamp
        }

        if (timeTriggered) {
            timeTriggerStore[value.propertyId] = timestamp
        }
        return timeTriggered
    }

    private fun timerTriggered(value: CarPropertyValue<*>, timerInMilliseconds: Float) : Boolean {
        return timerTriggered(value, timerInMilliseconds, value.timestamp)
    }

    private var carPropertyPowerListener = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) {
            //InAppLogger.deepLog("DataCollector.carPropertyPowerListener", appPreferences.deepLog)

            if (!emulatorMode) powerUpdater(value)
        }
        override fun onErrorEvent(propId: Int, zone: Int) {
            Log.w("carPropertyPowerListener",
                "Received error car property event, propId=$propId")
        }
    }

    private var carPropertySpeedListener = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<Any>) {
            if (value.timestamp < startupTimestamp) return
            InAppLogger.logVHALCallback()

            speedUpdater(value)

            if (emulatorMode) {
                // Also get power in emulator
                var powerValue = carPropertyManager.getProperty<Float>(VehiclePropertyIds.EV_BATTERY_INSTANTANEOUS_CHARGE_RATE,0)
                lastSpeedValueTimestamp = value.timestamp
                powerUpdater(powerValue, value.timestamp)

            }
/*
                val consumptionPlotLineJSON = Gson().toJson(DataHolder.consumptionPlotLine)
                val speedPlotLineJSON = Gson().toJson(DataHolder.speedPlotLine)

                sharedPref.edit()
                    .putString(getString(R.string.userdata_consumption_plot_key), consumptionPlotLineJSON)
                    .putString(getString(R.string.userdata_speed_plot_key), speedPlotLineJSON)
                    .apply()
*/
        }
        override fun onErrorEvent(propId: Int, zone: Int) {
            Log.w("carPropertySpeedListener",
                "Received error car property event, propId=$propId")
        }
    }

    private var carPropertyGenericListener = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) {
            // InAppLogger.deepLog("DataCollector.carPropertyGenericListener", appPreferences.deepLog)

            when (value.propertyId) {
                VehiclePropertyIds.EV_BATTERY_LEVEL -> DataHolder.currentBatteryCapacity = (value.value as Float)
                VehiclePropertyIds.GEAR_SELECTION -> gearUpdater(value.value as Int, value.timestamp)
                VehiclePropertyIds.EV_CHARGE_PORT_CONNECTED -> portUpdater(value.value as Boolean)
            }
        }
        override fun onErrorEvent(propId: Int, zone: Int) {
            Log.w("carPropertyGenericListener","Received error car property event, propId=$propId")
        }
    }

    private fun gearUpdater(gear: Int, timestamp: Long) {
        if (DataHolder.currentGear == gear) return
        DataHolder.currentGear = gear

        when (gear) {
            VehicleGear.GEAR_PARK -> DataHolder.plotMarkers.addMarker(PlotMarkerType.PARK, timestamp)
            else -> DataHolder.plotMarkers.endMarker(timestamp)
        }

        sendBroadcast(Intent(getString(R.string.gear_update_broadcast)))
        if (DataHolder.currentGear == VehicleGear.GEAR_PARK) sendBroadcast(Intent(getString(R.string.save_trip_data_broadcast)))
    }

    private fun portUpdater(connected: Boolean) {
        if (connected != DataHolder.chargePortConnected) {
            DataHolder.chargePortConnected = connected

            if (lastPowerValueTimestamp < startupTimestamp) lastPowerValueTimestamp = startupTimestamp

            if (connected) {
                DataHolder.chargePlotLine.reset()
                DataHolder.chargedEnergy = 0F
                DataHolder.chargeTimeMillis = 0L
                chargeStartTimeNanos = System.nanoTime()
                addChargePlotLine(lastPowerValueTimestamp, PlotLineMarkerType.BEGIN_SESSION)
                DataHolder.plotMarkers.addMarker(PlotMarkerType.CHARGE, lastPowerValueTimestamp)
                sendBroadcast(Intent(getString(R.string.ui_update_plot_broadcast)))
            }

            if (!connected) { // && chargeStartTimeNanos > 0 && DataHolder.chargedEnergy > 0) {
                if (DataHolder.chargePlotLine.getDataPoints(PlotDimension.TIME).last().Marker != PlotLineMarkerType.END_SESSION){
                    addChargePlotLine(lastPowerValueTimestamp, PlotLineMarkerType.END_SESSION)
                    DataHolder.plotMarkers.addMarker(PlotMarkerType.PARK, lastPowerValueTimestamp)
                    sendBroadcast(Intent(getString(R.string.ui_update_plot_broadcast)))
                }
                DataHolder.chargeCurves.add(
                    ChargeCurve(
                        DataHolder.chargePlotLine.getDataPoints(PlotDimension.TIME, null),
                        null,
                        DataHolder.chargeTimeMillis,
                        DataHolder.chargedEnergy,
                        0f, 0f
                    )
                )
                sendBroadcast(Intent(getString(R.string.save_trip_data_broadcast)))
            }
        }
        // } else if (!connected && DataHolder.chargeCurves.isNotEmpty()) {
        //     DataHolder.chargePlotLine.reset()
        //     DataHolder.stateOfChargePlotLine.reset()
        //     DataHolder.chargePlotLine.addDataPoints(DataHolder.chargeCurves.last().chargePlotLine)
        //     DataHolder.stateOfChargePlotLine.addDataPoints(DataHolder.chargeCurves.last().stateOfChargePlotLine)
        // }

    }

    private fun powerUpdater(value: CarPropertyValue<*>, timestamp: Long) {
        lastPowerValueTimestamp = timestamp
        DataHolder.currentPowermW = when (emulatorMode) {
            true -> (value.value as Float) * emulatorPowerSign
            else -> - (value.value as Float)
        }

        val timeDifference = timeDifference(value, 10_000, timestamp)
        if (timeDifference != null) {
            val energy = (DataHolder.lastPowermW / 1_000) * (timeDifference.toFloat() / (1_000 * 60 * 60))
            if (DataHolder.currentGear != VehicleGear.GEAR_PARK && !DataHolder.chargePortConnected) {
                DataHolder.usedEnergy += energy
                DataHolder.averageConsumption = when {
                    DataHolder.traveledDistance <= 0 -> 0F
                    else -> DataHolder.usedEnergy / (DataHolder.traveledDistance / 1_000)
                }
            } else if (DataHolder.chargePortConnected) {
                DataHolder.chargedEnergy += -energy
            }

            if (emulatorMode) {
                DataHolder.currentBatteryCapacity = DataHolder.currentBatteryCapacity - ((DataHolder.lastPowermW / 1_000) * (timeDifference.toFloat() / (1_000 * 60 * 60)))
            }
        }

        if (timerTriggered(value, CHARGE_CURVE_UPDATE_INTERVAL_MILLIS.toFloat(), timestamp) && DataHolder.chargePortConnected && DataHolder.currentGear == VehicleGear.GEAR_PARK) {
            addChargePlotLine(timestamp)
            sendBroadcast(Intent(getString(R.string.ui_update_plot_broadcast)))
            DataHolder.lastChargePower = DataHolder.currentPowermW
        }
    }

    private fun addChargePlotLine(timestamp: Long, marker: PlotLineMarkerType? = null) {
        DataHolder.chargePlotLine.addDataPoint(
            -(DataHolder.currentPowermW / 1_000_000f),
            timestamp,
            DataHolder.traveledDistance,
            DataHolder.stateOfCharge(),
            plotLineMarkerType = marker,
            autoMarkerTimeDeltaThreshold = TimeUnit.MILLISECONDS.toNanos(CHARGE_CURVE_UPDATE_INTERVAL_MILLIS) * 2
        )
    }

    private fun powerUpdater(value: CarPropertyValue<*>) {
        powerUpdater(value, value.timestamp)
    }

    private fun speedUpdater(value: CarPropertyValue<*>) {
        // speed in park = 0 (overrule emulator)
        DataHolder.currentSpeed = when (DataHolder.currentGear) {
            VehicleGear.GEAR_PARK -> 0f
            else -> (value.value as Float).absoluteValue
        }

        // after reset
        if (DataHolder.traveledDistance == 0f) {
            consumptionPlotTracking = false
            resetPlotVar(value.timestamp)
        }

        val timeDifference = timeDifference(value, 1_000)
        if (timeDifference != null) {
            DataHolder.traveledDistance += DataHolder.lastSpeed * (timeDifference.toFloat() / 1000)
            DataHolder.averageConsumption = when {
                DataHolder.traveledDistance <= 0 -> 0F
                else -> DataHolder.usedEnergy / (DataHolder.traveledDistance / 1000)
            }

            if (!consumptionPlotTracking) {
                consumptionPlotTracking = DataHolder.currentGear != VehicleGear.GEAR_PARK
                resetPlotVar(value.timestamp)
            }

            val consumptionPlotTrigger = when {
                consumptionPlotTracking -> when {
                    DataHolder.traveledDistance >= DataHolder.lastPlotDistance + 100 -> true
                    DataHolder.currentGear == VehicleGear.GEAR_PARK -> (DataHolder.lastPlotMarker?: PlotLineMarkerType.BEGIN_SESSION) == PlotLineMarkerType.BEGIN_SESSION || DataHolder.traveledDistance != DataHolder.lastPlotDistance
                    else -> false
                }
                else -> false
            }

            if (consumptionPlotTrigger) {
                consumptionPlotTracking = DataHolder.currentGear != VehicleGear.GEAR_PARK

                val distanceDifference = DataHolder.traveledDistance - DataHolder.lastPlotDistance
                val timeDifference = value.timestamp - DataHolder.lastPlotTime
                val powerDifference = DataHolder.usedEnergy - DataHolder.lastPlotEnergy

                val newConsumptionPlotValue = if (distanceDifference > 0) powerDifference / (distanceDifference / 1000) else 0f

                val plotMarker = when(DataHolder.lastPlotGear) {
                    VehicleGear.GEAR_PARK -> when (DataHolder.currentGear) {
                        VehicleGear.GEAR_PARK -> PlotLineMarkerType.SINGLE_SESSION
                        else -> PlotLineMarkerType.BEGIN_SESSION
                    }
                    else -> when (DataHolder.currentGear) {
                        VehicleGear.GEAR_PARK -> PlotLineMarkerType.END_SESSION
                        else -> null
                    }
                }

                DataHolder.lastPlotMarker = plotMarker
                DataHolder.lastPlotGear = DataHolder.currentGear

                resetPlotVar(value.timestamp)

                DataHolder.consumptionPlotLine.addDataPoint(newConsumptionPlotValue, value.timestamp, DataHolder.traveledDistance, DataHolder.stateOfCharge(), timeDifference, distanceDifference, null, plotMarker)

                sendBroadcast(Intent(getString(R.string.ui_update_plot_broadcast)))
            }
        }
        sendBroadcast(Intent(getString(R.string.ui_update_gages_broadcast)))
    }

    private fun resetPlotVar(currentPlotTimestampMilliseconds: Long) {
        DataHolder.lastPlotDistance = DataHolder.traveledDistance
        DataHolder.lastPlotTime = currentPlotTimestampMilliseconds
        DataHolder.lastPlotEnergy = DataHolder.usedEnergy
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
                val averageConsumption = DataHolder.usedEnergy / (DataHolder.traveledDistance/1000)

                var averageConsumptionString = String.format("%d Wh/km", averageConsumption.toInt())
                if (!appPreferences.consumptionUnit) {
                    averageConsumptionString = String.format(
                        "%.1f kWh/100km",
                        averageConsumption / 10)
                }
                if ((DataHolder.traveledDistance <= 0)) averageConsumptionString = "N/A"

                notificationCounter++

                val message = String.format(
                    "P:%.1f kW, D: %.3f km, Ø: %s",
                    DataHolder.currentPowermW / 1_000_000,
                    DataHolder.traveledDistance / 1000,
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
            InAppLogger.log("TRIP DATA: Saved $fileName.json in Thread ${Thread.currentThread().name}")
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