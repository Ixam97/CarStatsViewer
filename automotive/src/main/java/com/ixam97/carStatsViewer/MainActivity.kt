package com.ixam97.carStatsViewer

import android.app.Activity
import android.app.AlertDialog
import android.app.PendingIntent
import android.car.Car
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : Activity() {
    companion object {
        private val permissions = arrayOf(Car.PERMISSION_ENERGY, Car.PERMISSION_SPEED)
    }
    private lateinit var timerHandler: Handler

    private val updateActivityTask = object : Runnable {
        override fun run() {
            InAppLogger.deepLog("MainActivity.updateActivityTask")
            updateActivity()
            timerHandler.postDelayed(this, 500)
        }
    }

    private lateinit var starterIntent: Intent
    private lateinit var context: Context
    private lateinit var dataCollectorIntent: Intent

    override fun onResume() {
        super.onResume()

        if (AppPreferences.consumptionUnit) {
            DataHolder.consumptionPlotLine.Unit = "Wh/km"
            DataHolder.consumptionPlotLine.HighlightFormat = "Ø %.0f"
            DataHolder.consumptionPlotLine.LabelFormat = "%.0f"
            DataHolder.consumptionPlotLine.Divider = 1f
        } else {
            DataHolder.consumptionPlotLine.Unit = "kWh/100km"
            DataHolder.consumptionPlotLine.HighlightFormat = "Ø %.1f"
            DataHolder.consumptionPlotLine.LabelFormat = "%.1f"
            DataHolder.consumptionPlotLine.Divider = 10f
        }

        InAppLogger.log("MainActivity.onResume")
        //checkPermissions()
    }

    override fun onPause() {
        super.onPause()
        InAppLogger.log("MainActivity.onPause")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        InAppLogger.log("MainActivity.onCreate")

        context = applicationContext

        val sharedPref = context.getSharedPreferences(
            getString(R.string.preferences_file_key), Context.MODE_PRIVATE
        )
        AppPreferences.consumptionUnit = sharedPref.getBoolean(getString(R.string.preferences_consumption_unit_key), false)
        AppPreferences.notifications = sharedPref.getBoolean(getString(R.string.preferences_notifications_key), false)
        AppPreferences.debug = sharedPref.getBoolean(getString(R.string.preferences_debug_key), false)
        AppPreferences.consumptionPlot = sharedPref.getBoolean(getString(R.string.preferences_consumption_plot_key), false)
        AppPreferences.deepLog = sharedPref.getBoolean(getString(R.string.preferences_deep_log_key), false)
        AppPreferences.plotDistance = sharedPref.getInt(getString(R.string.preferences_plot_distance_key), 1)
        AppPreferences.plotSpeed = sharedPref.getBoolean(getString(R.string.preferences_plot_speed_key), false)

        dataCollectorIntent = Intent(this, DataCollector::class.java)
        starterIntent = intent
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        mainActivityPendingIntent = pendingIntent
        startForegroundService(dataCollectorIntent)

        checkPermissions()

        setContentView(R.layout.activity_main)

        main_checkbox_speed.isChecked = AppPreferences.plotSpeed
        var plotDistanceId = main_radio_10.id
        when (AppPreferences.plotDistance) {
            1 -> plotDistanceId = main_radio_10.id
            2 -> plotDistanceId = main_radio_25.id
            3 -> plotDistanceId = main_radio_50.id
        }
        main_radio_group_distance.check(plotDistanceId)

        if (AppPreferences.consumptionPlot) main_consumption_plot_container.visibility = View.VISIBLE

        main_consumption_plot.reset()
        main_consumption_plot.addPlotLine(DataHolder.consumptionPlotLine)
        main_consumption_plot.addPlotLine(DataHolder.speedPlotLine)
        DataHolder.speedPlotLine.visible = main_checkbox_speed.isChecked
        main_consumption_plot.displayItemCount = 101
        main_consumption_plot.invalidate()

        main_title.setOnClickListener {
            finish()
        }

        main_button_reset.setOnClickListener {

            val builder = AlertDialog.Builder(this@MainActivity)
            builder.setTitle(getString(R.string.main_dialog_reset_title))
                .setMessage(getString(R.string.main_dialog_reset_message))
                .setCancelable(true)
                .setPositiveButton(getString(R.string.dialog_confirm)) { dialog, id ->
                    resetStats()
                }
                .setNegativeButton(getString(R.string.dialog_dismiss)) { dialog, id ->
                    // Dismiss the dialog
                    InAppLogger.log("Dismiss reset but refresh MainActivity")
                    finish()
                    startActivity(intent)
                }
            val alert = builder.create()
            alert.show()
        }

        main_button_settings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        main_radio_group_distance.setOnCheckedChangeListener { group, checkedId ->
            var id = 1
            when (checkedId) {
                main_radio_10.id -> {
                    main_consumption_plot.displayItemCount = 101
                    id = 1
                }
                main_radio_25.id -> {
                    main_consumption_plot.displayItemCount = 251
                    id = 2
                }
                main_radio_50.id -> {
                    main_consumption_plot.displayItemCount = 501
                    id = 3
                }
            }

            sharedPref.edit()
                .putInt(getString(R.string.preferences_plot_distance_key), id)
                .apply()
            AppPreferences.plotDistance = id
        }

        main_checkbox_speed.setOnClickListener {
            if (main_checkbox_speed.isChecked && !DataHolder.speedPlotLine.visible) {
                DataHolder.speedPlotLine.visible = true
            } else if (!main_checkbox_speed.isChecked && DataHolder.speedPlotLine.visible) {
                DataHolder.speedPlotLine.visible = false
            }

            sharedPref.edit()
                .putBoolean(getString(R.string.preferences_plot_speed_key), main_checkbox_speed.isChecked)
                .apply()
            AppPreferences.plotSpeed = main_checkbox_speed.isChecked

            main_consumption_plot.invalidate()
        }

        timerHandler = Handler(Looper.getMainLooper())
        timerHandler.post(updateActivityTask)
    }

    override fun onDestroy() {
        super.onDestroy()
        InAppLogger.log("MainActivity.onDestroy")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        InAppLogger.log("onRequestPermissionResult")
        if(grantResults[0]==PackageManager.PERMISSION_GRANTED)
        {
            finish()
            startActivity(starterIntent)
            resetStats()
        }
    }

    private fun updateActivity() {
        InAppLogger.logUIUpdate()
        /** Use data from DataHolder to Update MainActivity text */

        if (AppPreferences.consumptionPlot && main_consumption_plot_container.visibility == View.GONE) {
            main_consumption_plot_container.visibility = View.VISIBLE
        } else if (!AppPreferences.consumptionPlot && main_consumption_plot_container.visibility == View.VISIBLE) {
            main_consumption_plot_container.visibility = View.GONE
        }

        main_consumption_plot.invalidate()

        chargePortConnectedTextView.text = DataHolder.chargePortConnected.toString()
        if (DataHolder.currentPowermW > 0) {
            currentPowerTextView.setTextColor(Color.RED)
            currentInstConsTextView.setTextColor(Color.RED)
        }
        else {
            currentPowerTextView.setTextColor(Color.GREEN)
            currentInstConsTextView.setTextColor(Color.GREEN)
        }
        currentPowerTextView.text = String.format("%.1f kW", DataHolder.currentPowermW / 1000000)
        if (AppPreferences.consumptionUnit) {
            usedEnergyTextView.text = String.format("%d Wh", DataHolder.usedEnergy.toInt())
        } else {
            usedEnergyTextView.text = String.format("%.1f kWh", DataHolder.usedEnergy / 1000)
        }
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
        finish()
        startActivity(intent)
        InAppLogger.log("MainActivity.resetStats")
        main_consumption_plot.reset()
        //DataHolder.consumptionPlotLine.addDataPoint(0f)
        //DataHolder.speedPlotLine.addDataPoint(DataHolder.currentSpeed * 3.6f)
        firstPlotValueAdded = false
        DataHolder.traveledDistance = 0F
        traveledDistanceTextView.text = String.format("%.3f km", DataHolder.traveledDistance / 1000)
        DataHolder.usedEnergy = 0F
        usedEnergyTextView.text = String.format("%d Wh", DataHolder.usedEnergy.toInt())
        DataHolder.averageConsumption = 0F
        averageConsumptionTextView.text = String.format("%d Wh/km", DataHolder.averageConsumption.toInt())
    }
}
