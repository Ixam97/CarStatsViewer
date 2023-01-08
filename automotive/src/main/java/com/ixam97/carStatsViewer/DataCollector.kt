package com.ixam97.carStatsViewer

import android.app.*
import android.car.Car
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.os.*
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue

lateinit var mainActivityPendingIntent: PendingIntent

var firstPlotValueAdded = false


object DataHolder {
    var currentPowermW = 0F
    var currentSpeed = 0F
    var traveledDistance = 0F
    var usedEnergy = 0F
    var averageConsumption = 0F
    var currentBatteryCapacity = 0
    var chargePortConnected = false
    var maxBatteryCapacity = 0

    var consumptionPlotLine = PlotLine(
        -200f,
        600f,
        100f,
        1f,
        "%.0f",
        "%.0f",
        "Wh/km",
        PlotLabelPosition.LEFT,
        PlotHighlightMethod.AVG
    )

    var speedPlotLine = PlotLine(
        0f,
        120f,
        40f,
        1f,
        "%.0f",
        "Ø %.0f",
        "km/h",
        PlotLabelPosition.RIGHT,
        PlotHighlightMethod.AVG_BY_DIMENSION
    )
}

class DataCollector : Service() {
    companion object {
        private const val CHANNEL_ID = "TestChannel"
        private const val statsNotificationId = 1
        private const val foregroundNotificationId = 2
    }

    var lastPlotDistance = 0F
    var lastPlotEnergy = 0F
    var lastPlotTime = 0F

    private var notificationCounter = 0

    private lateinit var sharedPref: SharedPreferences

    private val mBinder: LocalBinder = LocalBinder()

    private var notificationsEnabled = true

    private lateinit var car: Car
    private lateinit var carPropertyManager: CarPropertyManager

    private var currentPowerTime = 0F
    private var lastPowermW = 0F
    private var lastPowerTime = 0F

    private var currentSpeedTime = 0F
    private var lastSpeed = 0F
    private var lastSpeedTime = 0F

    private var lastBatteryCapacity = 0

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
            // .setStyle(Notification.MediaStyle())
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
        val batteryRegistered = carPropertyManager.registerCallback(
            carPropertyBatteryListener,
            VehiclePropertyIds.EV_BATTERY_LEVEL,
            CarPropertyManager.SENSOR_RATE_ONCHANGE
        )
        val plugRegistered = carPropertyManager.registerCallback(
            carPropertyPortListener,
            VehiclePropertyIds.EV_CHARGE_PORT_CONNECTED,
            CarPropertyManager.SENSOR_RATE_ONCHANGE
        )
    }

    private var carPropertyPowerListener = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) {
            InAppLogger.deepLog("DataCollector.carPropertyPowerListener")
            powerUpdater(value.value as Float)
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
            speedUpdater(value.value as Float)
            val currentPlotTime = SystemClock.elapsedRealtime().toFloat()

            if (!firstPlotValueAdded) {
                lastPlotDistance = DataHolder.traveledDistance
                lastPlotEnergy = DataHolder.usedEnergy
                lastPlotTime = currentPlotTime
                firstPlotValueAdded = true
            } else if (DataHolder.traveledDistance >= (lastPlotDistance + 100)) {
                val distanceDifference = lastPlotDistance - DataHolder.traveledDistance
                val powerDifference = lastPlotEnergy - DataHolder.usedEnergy
                val timeDifference = lastPlotTime - currentPlotTime
                lastPlotTime = currentPlotTime
                lastPlotDistance = DataHolder.traveledDistance
                lastPlotEnergy = DataHolder.usedEnergy

                val newConsumptionPlotValue = powerDifference / (distanceDifference / 1000)
                val newSpeedPlotValue = distanceDifference / (timeDifference / 3600)

                val seconds = TimeUnit.MILLISECONDS.convert(value.timestamp, TimeUnit.NANOSECONDS) / 1000f

                DataHolder.consumptionPlotLine.addDataPoint(
                    newConsumptionPlotValue,
                    seconds,
                    DataHolder.traveledDistance)
                DataHolder.speedPlotLine.addDataPoint(
                    newSpeedPlotValue,
                    seconds,
                    DataHolder.traveledDistance)
/*
                val consumptionPlotLineJSON = Gson().toJson(DataHolder.consumptionPlotLine)
                val speedPlotLineJSON = Gson().toJson(DataHolder.speedPlotLine)

                sharedPref.edit()
                    .putString(getString(R.string.userdata_consumption_plot_key), consumptionPlotLineJSON)
                    .putString(getString(R.string.userdata_speed_plot_key), speedPlotLineJSON)
                    .apply()
*/
            }
        }
        override fun onErrorEvent(propId: Int, zone: Int) {
            Log.w("carPropertySpeedListener",
                "Received error car property event, propId=$propId")
        }
    }

    private var carPropertyBatteryListener = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) {
            InAppLogger.deepLog("DataCollector.carPropertyBatteryListener")
            lastBatteryCapacity = DataHolder.currentBatteryCapacity
            DataHolder.currentBatteryCapacity = (value.value as Float).toInt()
        }
        override fun onErrorEvent(propId: Int, zone: Int) {
            Log.w("carPropertyBatteryListener",
                "Received error car property event, propId=$propId")
        }
    }

    private var carPropertyPortListener = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) {
            InAppLogger.log(String.format(
                "DataCollector.carPropertyPortListener on Thread: %s",
                Thread.currentThread().name))
            DataHolder.chargePortConnected = value.value as Boolean
        }
        override fun onErrorEvent(propId: Int, zone: Int) {
            Log.w("carPropertyPortListener",
                "Received error car property event, propId=$propId")
        }
    }

    private fun powerUpdater(value: Float) {
        if (lastPowerTime != 0F) lastPowerTime = currentPowerTime
        lastPowermW = DataHolder.currentPowermW
        DataHolder.currentPowermW = - value
        currentPowerTime = SystemClock.elapsedRealtime().toFloat()
        if (lastPowerTime == 0F) lastPowerTime = currentPowerTime
        else {
            var timeDifference = currentPowerTime - lastPowerTime
            if (!DataHolder.chargePortConnected && timeDifference < 10000) {
                DataHolder.usedEnergy += (lastPowermW / 1000) * (timeDifference / (1000 * 60 * 60))
                if (DataHolder.traveledDistance <= 0) DataHolder.averageConsumption = 0F
                else DataHolder.averageConsumption = DataHolder.usedEnergy / (DataHolder.traveledDistance / 1000)
            }
        }
    }

    private fun speedUpdater(value: Float) {
        if (lastSpeedTime != 0F) lastSpeedTime = currentSpeedTime
        lastSpeed = DataHolder.currentSpeed
        DataHolder.currentSpeed = value.absoluteValue
        currentSpeedTime = SystemClock.elapsedRealtime().toFloat()
        if (lastSpeedTime == 0F) lastSpeedTime = currentSpeedTime
        else {
            var timeDifference = currentSpeedTime - lastSpeedTime
            if (timeDifference < 1000) {
                DataHolder.traveledDistance += lastSpeed * (timeDifference / 1000)
                if (DataHolder.traveledDistance <= 0) DataHolder.averageConsumption = 0F
                else DataHolder.averageConsumption = DataHolder.usedEnergy / (DataHolder.traveledDistance / 1000)
            }
        }
    }

    private fun createNotificationChannel() {
        val name = "TestChannel"
        val descriptionText = "TestChannel"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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
                    DataHolder.currentPowermW / 1000000,
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