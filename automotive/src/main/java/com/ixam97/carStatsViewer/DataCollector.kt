package com.ixam97.carStatsViewer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.car.Car
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlin.math.absoluteValue

object DataHolder {
    var currentPowermW = 0F
    var currentSpeed = 0F
    var traveledDistance = 0F
    var usedEnergy = 0F
    var averageConsumption = 0F
    var currentBatteryCapacity = 0
    var chargePortConnected = false
    var maxBatteryCapacity = 0
}

lateinit var mainActivityPendingIntent: PendingIntent

class DataCollector : Service() {
    companion object {
        private const val CHANNEL_ID = "TestChannel"
        private const val notificationId = 1
    }

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
            timerHandler.postDelayed(this, 1000)
        }
    }

    private var statsNotification = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Title")
        .setContentText("Test Notification from Car Stats Viewer")
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setOngoing(true)

    inner class LocalBinder : Binder() {
        val service: DataCollector
            get() = this@DataCollector
    }

    override fun onBind(intent: Intent): IBinder? {
        return mBinder
    }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onDestroy() {
        super.onDestroy()
        car.disconnect()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        // Register vehicle properties callbacks

        notificationsEnabled = AppPreferences.notifications

        car = Car.createCar(this)
        carPropertyManager = car.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager;
        DataHolder.maxBatteryCapacity = carPropertyManager.getProperty<Float>(VehiclePropertyIds.INFO_EV_BATTERY_CAPACITY, 0).value.toInt()

        notificationTitleString = resources.getString(R.string.notification_title)
        statsNotification.setContentTitle(notificationTitleString).setContentIntent(mainActivityPendingIntent)


        createNotificationChannel()

        if (notificationsEnabled) {
            with(NotificationManagerCompat.from(this)) {
                notify(notificationId, statsNotification.build())
            }
        }

        registerCarPropertyCallbacks()

        timerHandler = Handler(Looper.getMainLooper())
        timerHandler.post(updateStatsNotificationTask)

        return START_STICKY
    }

    private fun registerCarPropertyCallbacks() {
        Log.d("registerCarPropertyCallbacks: ", "registering Callbacks")
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
            carPropertyBatteryListener,
            VehiclePropertyIds.EV_BATTERY_LEVEL,
            CarPropertyManager.SENSOR_RATE_ONCHANGE
        )
        carPropertyManager.registerCallback(
            carPropertyPortListener,
            VehiclePropertyIds.EV_CHARGE_PORT_CONNECTED,
            CarPropertyManager.SENSOR_RATE_ONCHANGE
        )
    }

    private var carPropertyPowerListener = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) {
            if (lastPowerTime != 0F) lastPowerTime = currentPowerTime
            lastPowermW = DataHolder.currentPowermW
            DataHolder.currentPowermW = -(value.value as Float)
            currentPowerTime = SystemClock.elapsedRealtime().toFloat()
            if (lastPowerTime == 0F) lastPowerTime = currentPowerTime
            else {
                var timeDifference = currentPowerTime - lastPowerTime
                if (!DataHolder.chargePortConnected && timeDifference < 1000) DataHolder.usedEnergy += (lastPowermW / 1000) * (timeDifference / (1000 * 60 * 60))
            }
        }
        override fun onErrorEvent(propId: Int, zone: Int) {
            Log.w("carPropertyPowerListener", "Received error car property event, propId=$propId")
        }
    }

    private var carPropertySpeedListener = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) {
            if (lastSpeedTime != 0F) lastSpeedTime = currentSpeedTime
            lastSpeed = DataHolder.currentSpeed
            DataHolder.currentSpeed = (value.value as Float).absoluteValue
            currentSpeedTime = SystemClock.elapsedRealtime().toFloat()
            if (lastSpeedTime == 0F) lastSpeedTime = currentSpeedTime
            else {
                var timeDifference = currentSpeedTime - lastSpeedTime
                if (timeDifference < 1000) DataHolder.traveledDistance += lastSpeed * (timeDifference / 1000)
            }
        }
        override fun onErrorEvent(propId: Int, zone: Int) {
            Log.w("carPropertySpeedListener", "Received error car property event, propId=$propId")
        }
    }

    private var carPropertyBatteryListener = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) {
            lastBatteryCapacity = DataHolder.currentBatteryCapacity
            DataHolder.currentBatteryCapacity = (value.value as Float).toInt()
        }
        override fun onErrorEvent(propId: Int, zone: Int) {
            Log.w("carPropertyBatteryListener", "Received error car property event, propId=$propId")
        }
    }

    private var carPropertyPortListener = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) {
            DataHolder.chargePortConnected = value.value as Boolean
        }
        override fun onErrorEvent(propId: Int, zone: Int) {
            Log.w("carPropertyPortListener", "Received error car property event, propId=$propId")
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
        if (notificationsEnabled && AppPreferences.notifications) {
            with(NotificationManagerCompat.from(this)) {
                val averageConsumption = DataHolder.usedEnergy / (DataHolder.traveledDistance/1000)

                var message = String.format(
                    "P:%.1f kW, D: %.3f km, Ø: %d Wh/km",
                    DataHolder.currentPowermW / 1000000,
                    DataHolder.traveledDistance / 1000,
                    averageConsumption.toInt()
                )
                if (!AppPreferences.consumptionUnit) {
                    message = String.format(
                        "P:%.1f kW, D: %.3f km, Ø: %.1f kWh/100km",
                        DataHolder.currentPowermW / 1000000,
                        DataHolder.traveledDistance / 1000,
                        averageConsumption / 10
                    )
                }
                statsNotification.setContentText(message)
                notify(notificationId, statsNotification.build())
            }
        } else if (notificationsEnabled && !AppPreferences.notifications) {
            notificationsEnabled = false
            with(NotificationManagerCompat.from(this)) {
                cancel(notificationId)
            }
        } else if (!notificationsEnabled && AppPreferences.notifications) {
            notificationsEnabled = true
        }
    }

}