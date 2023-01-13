package com.ixam97.carStatsViewer.services


import com.ixam97.carStatsViewer.plot.*
import com.ixam97.carStatsViewer.objects.*
import com.ixam97.carStatsViewer.*
import android.app.*
import android.car.Car
import android.car.VehicleGear
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.os.*
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import com.ixam97.carStatsViewer.activities.emulatorMode
import com.ixam97.carStatsViewer.activities.emulatorPowerSign
import kotlin.collections.HashMap
import kotlin.math.absoluteValue

lateinit var mainActivityPendingIntent: PendingIntent

class DataCollector : Service() {
    companion object {
        private const val CHANNEL_ID = "TestChannel"
        private const val STATS_NOTIFICATION_ID = 1
        private const val FOREGROUND_NOTIFICATION_ID = 2
        private const val TIMER_HANDLER_DELAY_MILLIS = 1000L
    }

    private var consumptionPlotTracking = false
    private var lastPlotDistance = 0F
    private var lastPlotEnergy = 0F
    private var lastPlotTime = 0L
    private var lastPlotGear = VehicleGear.GEAR_PARK
    private var lastPlotMarker : PlotMarker? = null

    private var notificationCounter = 0

    // private lateinit var sharedPref: SharedPreferences
    private lateinit var appPreferences: AppPreferences

    private val mBinder: LocalBinder = LocalBinder()

    private var notificationsEnabled = true

    private lateinit var car: Car
    private lateinit var carPropertyManager: CarPropertyManager

    private lateinit var notificationTitleString: String

    private lateinit var timerHandler: Handler

    private val updateStatsNotificationTask = object : Runnable {
        override fun run() {
            updateStatsNotification()
            InAppLogger.logNotificationUpdate()
            if (DataHolder.currentGear != VehicleGear.GEAR_PARK) DataHolder.travelTimeMillis += TIMER_HANDLER_DELAY_MILLIS
            timerHandler.postDelayed(this, TIMER_HANDLER_DELAY_MILLIS)
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

        foregroundServiceNotification = Notification.Builder(this, CHANNEL_ID)
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

        startForeground(FOREGROUND_NOTIFICATION_ID, foregroundServiceNotification.build())

        InAppLogger.log(String.format(
            "DataCollector.onCreate in Thread: %s",
            Thread.currentThread().name))

        // sharedPref = this.getSharedPreferences(
        //     getString(R.string.preferences_file_key),
        //     Context.MODE_PRIVATE)

        appPreferences = AppPreferences(applicationContext)

        notificationsEnabled = appPreferences.notifications

        car = Car.createCar(this)
        carPropertyManager = car.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager;
        DataHolder.maxBatteryCapacity = carPropertyManager
            .getProperty<Float>(VehiclePropertyIds.INFO_EV_BATTERY_CAPACITY, 0)
            .value.toInt()

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

        createNotificationChannel()

        if (notificationsEnabled) {
            with(NotificationManagerCompat.from(this)) {
                notify(STATS_NOTIFICATION_ID, statsNotification.build())
            }
        }

        DataHolder.consumptionPlotLine.baseLineAt.add(0f)
        DataHolder.speedPlotLine.baseLineAt.add(0f)

        registerCarPropertyCallbacks()

        timerHandler = Handler(Looper.getMainLooper())
        timerHandler.post(updateStatsNotificationTask)
    }

    override fun onDestroy() {
        super.onDestroy()
        InAppLogger.log("DataCollector.onDestroy")
        car.disconnect()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        InAppLogger.log("DataCollector.onStartCommand")
        return START_STICKY
    }

    private fun registerCarPropertyCallbacks() {

        InAppLogger.log("DataCollector.registerCarPropertyCallbacks")

        val powerRegistered = carPropertyManager.registerCallback(
            carPropertyPowerListener,
            VehiclePropertyIds.EV_BATTERY_INSTANTANEOUS_CHARGE_RATE,
            CarPropertyManager.SENSOR_RATE_FASTEST
        )
        val speedRegistered = carPropertyManager.registerCallback(
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

            powerUpdater(value)
            Log.d("carPropertyPowerListener",
                String.format(
                    "Received value %.0f on thread %s",
                    value.value as Float,
                    Thread.currentThread().name))
        }
        override fun onErrorEvent(propId: Int, zone: Int) {
            Log.w("carPropertyPowerListener",
                "Received error car property event, propId=$propId")
        }
    }

    private var carPropertySpeedListener = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) {
            //InAppLogger.deepLog("DataCollector.carPropertySpeedListener", appPreferences.deepLog)
            InAppLogger.logVHALCallback()

            speedUpdater(value)
            setActiveTrip()

            if (emulatorMode) {
                // Also get power in emulator
                var powerValue = carPropertyManager.getProperty<Float>(VehiclePropertyIds.EV_BATTERY_INSTANTANEOUS_CHARGE_RATE,0)
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
                VehiclePropertyIds.EV_BATTERY_LEVEL -> DataHolder.currentBatteryCapacity = (value.value as Float).toInt()
                VehiclePropertyIds.GEAR_SELECTION -> {
                    if (!emulatorMode) DataHolder.currentGear = value.value as Int
                }
                VehiclePropertyIds.EV_CHARGE_PORT_CONNECTED -> DataHolder.chargePortConnected = value.value as Boolean
            }
        }
        override fun onErrorEvent(propId: Int, zone: Int) {
            Log.w("carPropertyGenericListener","Received error car property event, propId=$propId")
        }
    }

    private fun powerUpdater(value: CarPropertyValue<*>, timestamp: Long) {
        DataHolder.currentPowermW = when (emulatorMode) {
            true -> (value.value as Float) * emulatorPowerSign
            else -> - (value.value as Float)
        }

        val timeDifference = timeDifference(value, 10_000, timestamp)
        // Log.d("powerUpdater", "Time Difference: $timeDifference")
        if (timeDifference != null && !DataHolder.chargePortConnected) {
            DataHolder.usedEnergy += (DataHolder.lastPowermW / 1000) * (timeDifference.toFloat() / (1000 * 60 * 60))
            DataHolder.averageConsumption = when {
                DataHolder.traveledDistance <= 0 -> 0F
                else -> DataHolder.usedEnergy / (DataHolder.traveledDistance / 1_000)
            }
        }

        if (timerTriggered(value, 2_000f, timestamp) && DataHolder.chargePortConnected && DataHolder.currentPowermW < 0) {
            DataHolder.chargePlotLine.addDataPoint(- (DataHolder.currentPowermW / 1_000_000), timestamp,0f)
            DataHolder.stateOfChargePlotLine.addDataPoint(100f / DataHolder.maxBatteryCapacity * DataHolder.currentBatteryCapacity, timestamp, 0f)
        }
        // Update Power min/max values
        if (DataHolder.currentPowermW > DataHolder.maxPowerPositive){
            DataHolder.maxPowerPositive = DataHolder.currentPowermW
        }
        if (DataHolder.currentPowermW < DataHolder.maxPowerNegative){
            DataHolder.maxPowerNegative = DataHolder.currentPowermW
        }
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
                    DataHolder.traveledDistance >= lastPlotDistance + 100 -> true
                    DataHolder.currentGear == VehicleGear.GEAR_PARK -> (lastPlotMarker?:PlotMarker.BEGIN_SESSION) == PlotMarker.BEGIN_SESSION || DataHolder.traveledDistance != lastPlotDistance
                    else -> false
                }
                else -> false
            }

            if (consumptionPlotTrigger) {
                consumptionPlotTracking = DataHolder.currentGear != VehicleGear.GEAR_PARK

                val distanceDifference = DataHolder.traveledDistance - lastPlotDistance
                val timeDifference = value.timestamp - lastPlotTime
                val powerDifference = DataHolder.usedEnergy - lastPlotEnergy

                val newConsumptionPlotValue = powerDifference / (distanceDifference / 1000)
                val newSpeedPlotValue = distanceDifference / (timeDifference.toFloat() / 1_000_000_000 / 3.6f)

                val plotMarker = when(lastPlotGear) {
                    VehicleGear.GEAR_PARK -> when (DataHolder.currentGear) {
                        VehicleGear.GEAR_PARK -> PlotMarker.SINGLE_SESSION
                        else -> PlotMarker.BEGIN_SESSION
                    }
                    else -> when (DataHolder.currentGear) {
                        VehicleGear.GEAR_PARK -> PlotMarker.END_SESSION
                        else -> null
                    }
                }

                lastPlotMarker = plotMarker
                lastPlotGear = DataHolder.currentGear

                resetPlotVar(value.timestamp)

                DataHolder.consumptionPlotLine.addDataPoint(newConsumptionPlotValue, value.timestamp, DataHolder.traveledDistance, timeDifference, distanceDifference, plotMarker)
                DataHolder.speedPlotLine.addDataPoint(newSpeedPlotValue, value.timestamp, DataHolder.traveledDistance, timeDifference, distanceDifference, plotMarker)

                InAppLogger.log("newSpeedPlotValue: $newSpeedPlotValue, currentSpeed: ${DataHolder.currentSpeed * 3.6f}")
            }
        }

        // Update Speed max value
        if (DataHolder.currentSpeed > DataHolder.maxSpeed){
            DataHolder.maxSpeed = DataHolder.currentSpeed
        }
    }

    private fun setActiveTrip(){
        if(DataHolder.currentGear != VehicleGear.GEAR_PARK){
            DataHolder.activeTrip = true
        }
    }

    private fun resetPlotVar(currentPlotTimestampMilliseconds: Long) {
        lastPlotDistance = DataHolder.traveledDistance
        lastPlotTime = currentPlotTimestampMilliseconds
        lastPlotEnergy = DataHolder.usedEnergy
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

}