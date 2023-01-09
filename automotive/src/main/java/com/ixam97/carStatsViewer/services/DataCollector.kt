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
import com.ixam97.carStatsViewer.activities.devMode
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue

lateinit var mainActivityPendingIntent: PendingIntent

class DataCollector : Service() {
    companion object {
        private const val CHANNEL_ID = "TestChannel"
        private const val statsNotificationId = 1
        private const val foregroundNotificationId = 2
    }

    private var consumptionPlotTracking = false
    private var lastPlotDistance = 0F
    private var lastPlotEnergy = 0F
    private var lastPlotTime = 0F
    private var lastPlotGear = VehicleGear.GEAR_PARK

    private var notificationCounter = 0

    private lateinit var sharedPref: SharedPreferences

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
            timerHandler.postDelayed(this, 500)
        }
    }

    private lateinit var statsNotification: Notification.Builder

    inner class LocalBinder : Binder() {
        fun getService(): DataCollector = this@DataCollector
    }

    override fun onBind(intent: Intent): IBinder? {
        return mBinder
    }

    override fun onCreate() {
        super.onCreate()

        val foregroundServiceNotification = Notification.Builder(this, CHANNEL_ID)
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

        startForeground(foregroundNotificationId, foregroundServiceNotification.build())

        InAppLogger.log(String.format(
            "DataCollector.onCreate in Thread: %s",
            Thread.currentThread().name))

        sharedPref = this.getSharedPreferences(
            getString(R.string.preferences_file_key),
            Context.MODE_PRIVATE)
/*
        val consumptionPlotLineJSON = sharedPref.getString(getString(R.string.userdata_consumption_plot_key), "")
        val speedPlotLineJSON = sharedPref.getString(getString(R.string.userdata_speed_plot_key), "")

        if (consumptionPlotLineJSON != "") DataHolder.consumptionPlotLine = Gson().fromJson(consumptionPlotLineJSON, PlotLine::class.java)
        if (speedPlotLineJSON != "") DataHolder.consumptionPlotLine = Gson().fromJson(speedPlotLineJSON, PlotLine::class.java)
*/
        notificationsEnabled = AppPreferences.notifications

        // val carPropertyCallbacksTaskHandlerThread = HandlerThread("CarPropertyCallbacksTaskThread")
        // carPropertyCallbacksTaskHandlerThread.start()
        // val carPropertyCallbacksTaskHandler = Handler(carPropertyCallbacksTaskHandlerThread.looper)

        car = Car.createCar(this)
        carPropertyManager = car.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager;
        DataHolder.maxBatteryCapacity = carPropertyManager
            .getProperty<Float>(VehiclePropertyIds.INFO_EV_BATTERY_CAPACITY, 0)
            .value.toInt()


        /** Get vehicle name to enable dev mode in emulator */
        val carName = carPropertyManager.getProperty<String>(VehiclePropertyIds.INFO_MODEL, 0).value.toString()
        if (carName == "Speedy Model") {
            Toast.makeText(this, "Dev Mode", Toast.LENGTH_LONG).show()
            devMode = true
        }

        DataHolder.currentGear = carPropertyManager.getIntProperty(VehiclePropertyIds.GEAR_SELECTION, 0)

        notificationTitleString = resources.getString(R.string.notification_title)
        statsNotification.setContentTitle(notificationTitleString).setContentIntent(mainActivityPendingIntent)


        createNotificationChannel()

        if (notificationsEnabled) {
            with(NotificationManagerCompat.from(this)) {
                notify(statsNotificationId, statsNotification.build())
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

    private fun timestampAsMilliseconds(value: CarPropertyValue<*>) : Float {
        return TimeUnit.MILLISECONDS.convert(value.timestamp, TimeUnit.NANOSECONDS).toFloat()
    }

    private val timeDifferenceStore: HashMap<Int, Float> = HashMap()

    private fun timeDifference(value: CarPropertyValue<*>, maxDiff: Float) : Float? {
        val timeInMilliseconds = timestampAsMilliseconds(value)
        var timeDifference : Float? = null

        if (timeDifferenceStore.containsKey(value.propertyId)) {
            timeDifference = timeInMilliseconds - timeDifferenceStore[value.propertyId]!!
        }

        timeDifferenceStore[value.propertyId] = timeInMilliseconds

        return when {
            timeDifference == null || timeDifference > maxDiff -> null
            else -> timeDifference
        }
    }

    private val timeTriggerStore: HashMap<Int, Float> = HashMap()

    private fun timerTriggered(value: CarPropertyValue<*>, timer: Float) : Boolean {
        val timeInMilliseconds = timestampAsMilliseconds(value)
        var timeTriggered = true

        if (timeTriggerStore.containsKey(value.propertyId)) {
            timeTriggered = timeTriggerStore[value.propertyId]?.plus(timer)!! <= timeInMilliseconds
        }

        if (timeTriggered) {
            timeTriggerStore[value.propertyId] = timeInMilliseconds
        }

        return timeTriggered
    }

    private var carPropertyPowerListener = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) {
            InAppLogger.deepLog("DataCollector.carPropertyPowerListener")

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
            InAppLogger.deepLog("DataCollector.carPropertySpeedListener")
            InAppLogger.logVHALCallback()

            speedUpdater(value)
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
            InAppLogger.deepLog("DataCollector.carPropertyGenericListener")

            when (value.propertyId) {
                VehiclePropertyIds.EV_BATTERY_LEVEL -> DataHolder.currentBatteryCapacity = (value.value as Float).toInt()
                VehiclePropertyIds.GEAR_SELECTION -> {
                    DataHolder.currentGear = value.value as Int
                    InAppLogger.log("Current gear: ${DataHolder.currentGear}")
                }
                VehiclePropertyIds.EV_CHARGE_PORT_CONNECTED -> {
                    DataHolder.chargePortConnected = value.value as Boolean
                    InAppLogger.log("Charge Port Connected: ${DataHolder.chargePortConnected}")
                }
            }
        }
        override fun onErrorEvent(propId: Int, zone: Int) {
            Log.w("carPropertyGenericListener","Received error car property event, propId=$propId")
        }
    }

    private fun powerUpdater(value: CarPropertyValue<*>) {
        DataHolder.currentPowermW = - (value.value as Float)

        val timeDifference = timeDifference(value, 10_000f)
        if (timeDifference != null && !DataHolder.chargePortConnected) {
            DataHolder.usedEnergy += (DataHolder.lastPowermW / 1000) * (timeDifference / (1000 * 60 * 60))
            DataHolder.averageConsumption = when {
                DataHolder.traveledDistance <= 0 -> 0F
                else -> DataHolder.usedEnergy / (DataHolder.traveledDistance / 1_000)
            }
        }

        if (timerTriggered(value, 2_000f) && DataHolder.chargePortConnected && DataHolder.currentPowermW < 0) {
            DataHolder.chargePlotLine.addDataPoint(- (DataHolder.currentPowermW / 1_000_000), value.timestamp,0f)
            DataHolder.stateOfChargePlotLine.addDataPoint(100f / DataHolder.maxBatteryCapacity * DataHolder.currentBatteryCapacity, value.timestamp, 0f)
        }
    }

    private fun speedUpdater(value: CarPropertyValue<*>) {
        val currentPlotTime = timestampAsMilliseconds(value)

        // speed in park = 0 (overrule emulator)
        DataHolder.currentSpeed = when (DataHolder.currentGear) {
            VehicleGear.GEAR_PARK -> 0f
            else -> (value.value as Float).absoluteValue
        }

        // after reset
        if (DataHolder.traveledDistance == 0f) {
            consumptionPlotTracking = false
            resetPlotVar(currentPlotTime)
        }

        val timeDifference = timeDifference(value, 1_000f)
        if (timeDifference != null) {
            DataHolder.traveledDistance += DataHolder.lastSpeed * (timeDifference / 1000)
            DataHolder.averageConsumption = when {
                DataHolder.traveledDistance <= 0 -> 0F
                else -> DataHolder.usedEnergy / (DataHolder.traveledDistance / 1000)
            }
        }

        if (!consumptionPlotTracking && timeDifference != null) {
            consumptionPlotTracking = DataHolder.currentGear != VehicleGear.GEAR_PARK
            resetPlotVar(currentPlotTime)
        }

        if (consumptionPlotTracking && (DataHolder.currentGear == VehicleGear.GEAR_PARK || DataHolder.traveledDistance >= (lastPlotDistance + 100))) {
            val distanceDifference = DataHolder.traveledDistance - lastPlotDistance
            val powerDifference = DataHolder.usedEnergy - lastPlotEnergy
            val timeDifference = currentPlotTime - lastPlotTime

            val newConsumptionPlotValue = powerDifference / (distanceDifference / 1000)
            val newSpeedPlotValue = distanceDifference / (timeDifference / 3600)

            val plotMarker = when(lastPlotGear) {
                VehicleGear.GEAR_PARK -> PlotMarker.BEGIN_SESSION
                else -> when (DataHolder.currentGear) {
                    VehicleGear.GEAR_PARK -> PlotMarker.END_SESSION
                    else -> null
                }
            }

            DataHolder.consumptionPlotLine.addDataPoint(newConsumptionPlotValue, value.timestamp, DataHolder.traveledDistance, plotMarker)
            DataHolder.speedPlotLine.addDataPoint(newSpeedPlotValue, value.timestamp, DataHolder.traveledDistance, plotMarker)

            lastPlotDistance = DataHolder.traveledDistance
            lastPlotEnergy = DataHolder.usedEnergy
            lastPlotGear = DataHolder.currentGear
            lastPlotTime = currentPlotTime

            consumptionPlotTracking = DataHolder.currentGear != VehicleGear.GEAR_PARK
        }
    }

    private fun resetPlotVar(currentPlotTime: Float) {
        lastPlotDistance = DataHolder.traveledDistance
        lastPlotEnergy = DataHolder.usedEnergy
        lastPlotTime = currentPlotTime
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
        InAppLogger.deepLog("DataCollector.updateStatsNotification")
        if (notificationsEnabled && AppPreferences.notifications) {
            with(NotificationManagerCompat.from(this)) {
                val averageConsumption = DataHolder.usedEnergy / (DataHolder.traveledDistance/1000)

                var averageConsumptionString = String.format("%d Wh/km", averageConsumption.toInt())
                if (!AppPreferences.consumptionUnit) {
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
                notify(statsNotificationId, statsNotification.build())
            }
        } else if (notificationsEnabled && !AppPreferences.notifications) {
            notificationsEnabled = false
            with(NotificationManagerCompat.from(this)) {
                cancel(statsNotificationId)
            }
        } else if (!notificationsEnabled && AppPreferences.notifications) {
            notificationsEnabled = true
        }
    }

}