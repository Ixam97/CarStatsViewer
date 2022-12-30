package com.ixam97.carStatsViewer

import android.app.Activity
import android.app.PendingIntent
import android.car.Car
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import kotlinx.android.synthetic.main.activity_main.*

/**
 * A simple activity that demonstrates connecting to car API and processing car property change
 * events.
 *
 * <p>Please see https://developer.android.com/reference/android/car/packages for API documentation.
 */

class MainActivity : Activity() {
    companion object {
        private val permissions = arrayOf(Car.PERMISSION_ENERGY, Car.PERMISSION_SPEED)
    }
    private lateinit var timerHandler: Handler

    private val updateActivityTask = object : Runnable {
        override fun run() {
            updateActivity()
            timerHandler.postDelayed(this, 1000)
        }
    }

    private lateinit var starterIntent: Intent
    private lateinit var context: Context
    private lateinit var dataCollectorIntent: Intent

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        context = applicationContext

        val sharedPref = context.getSharedPreferences(
            getString(R.string.preferences_file_key), Context.MODE_PRIVATE
        )
        AppPreferences.consumptionUnit = sharedPref.getBoolean(getString(R.string.preferences_consumption_unit_key), false)
        AppPreferences.notifications = sharedPref.getBoolean(getString(R.string.preferences_notifications_key), false)

        dataCollectorIntent = Intent(this, DataCollector::class.java)
        starterIntent = intent
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        mainActivityPendingIntent = pendingIntent
        startService(dataCollectorIntent)

        checkPermissions()

        setContentView(R.layout.activity_main)

        main_button_reset.setOnClickListener {
            resetStats()
        }

        main_button_settings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        timerHandler = Handler(Looper.getMainLooper())
        timerHandler.post(updateActivityTask)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(grantResults[0]==PackageManager.PERMISSION_GRANTED)
        {
            finish()
            startActivity(starterIntent)
            resetStats()
        }
    }

    private fun updateActivity() {
        /** Use data from DataHolder to Update MainActivity text */
        chargePortConnectedTextView.text = DataHolder.chargePortConnected.toString()
        if (DataHolder.currentPowermW > 0) currentPowerTextView.setTextColor(Color.RED)
        else currentPowerTextView.setTextColor(Color.GREEN)
        currentPowerTextView.text = String.format("%.1f kW", DataHolder.currentPowermW / 1000000)
        usedEnergyTextView.text = String.format("%d Wh", DataHolder.usedEnergy.toInt())
        currentSpeedTextView.text = String.format("%d km/h", (DataHolder.currentSpeed*3.6).toInt())
        traveledDistanceTextView.text = String.format("%.3f km", DataHolder.traveledDistance / 1000)
        batteryEnergyTextView.text = String.format("%d/%d, %d%%", DataHolder.currentBatteryCapacity, DataHolder.maxBatteryCapacity, ((DataHolder.currentBatteryCapacity.toFloat()/DataHolder.maxBatteryCapacity.toFloat())*100).toInt())
        if (AppPreferences.consumptionUnit) { // Use Wh/km
            if (DataHolder.currentSpeed > 0) currentInstConsTextView.text = String.format("%d Wh/km", ((DataHolder.currentPowermW / 1000) / (DataHolder.currentSpeed * 3.6)).toInt())
            else currentInstConsTextView.text = "N/A"
            if (DataHolder.traveledDistance > 0) averageConsumptionTextView.text = String.format("%d Wh/km", (DataHolder.usedEnergy/(DataHolder.traveledDistance/1000)).toInt())
            else averageConsumptionTextView.text = "N/A"
        } else { // Use kWh/100km
            if (DataHolder.currentSpeed > 0) currentInstConsTextView.text = String.format("%.1f kWh/100km", ((DataHolder.currentPowermW / 1000) / (DataHolder.currentSpeed * 3.6))/10)
            else currentInstConsTextView.text = "N/A"
            if (DataHolder.traveledDistance > 0) averageConsumptionTextView.text = String.format("%.1f kWh/100km", (DataHolder.usedEnergy/(DataHolder.traveledDistance/1000))/10)
            else averageConsumptionTextView.text = "N/A"
        }
    }

    private fun checkPermissions() {
        if(checkSelfPermission(Car.PERMISSION_ENERGY) == PackageManager.PERMISSION_GRANTED) {
            //your code here
        } else {
            requestPermissions(permissions, 0)
        }
        if(checkSelfPermission(Car.PERMISSION_SPEED) == PackageManager.PERMISSION_GRANTED) {
            //your code here
        } else {
            requestPermissions(permissions, 1)
        }
    }

    private fun resetStats() {
        DataHolder.traveledDistance = 0F
        traveledDistanceTextView.text = String.format("%.3f km", DataHolder.traveledDistance / 1000)
        DataHolder.usedEnergy = 0F
        usedEnergyTextView.text = String.format("%d Wh", DataHolder.usedEnergy.toInt())
        DataHolder.averageConsumption = 0F
        averageConsumptionTextView.text = String.format("%d Wh/km", DataHolder.averageConsumption.toInt())
    }
}
