package com.ixam97.carStatsViewer.activities

import com.ixam97.carStatsViewer.*
import com.ixam97.carStatsViewer.objects.*
import com.ixam97.carStatsViewer.services.*
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
import android.os.SystemClock
import android.view.View
import com.ixam97.carStatsViewer.plot.PlotDimension
import kotlinx.android.synthetic.main.activity_main.*

var devMode = false

class MainActivity : Activity() {
    companion object {
        private val permissions = arrayOf(Car.PERMISSION_ENERGY, Car.PERMISSION_SPEED)
        private const val uiUpdateDelayMillis = 40L
    }
    private lateinit var timerHandler: Handler

    private var updateUi = false

    private val updateActivityTask = object : Runnable {
        override fun run() {
            // InAppLogger.deepLog("MainActivity.updateActivityTask")
            updateActivity()
            if (updateUi) timerHandler.postDelayed(this, uiUpdateDelayMillis)
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

        updateUi = true
        timerHandler.removeCallbacks(updateActivityTask)
        timerHandler.post(updateActivityTask)
        //checkPermissions()
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
        AppPreferences.experimentalLayout = sharedPref.getBoolean(getString(R.string.preferences_experimental_layout_key), false)
        AppPreferences.deepLog = sharedPref.getBoolean(getString(R.string.preferences_deep_log_key), false)
        AppPreferences.plotDistance = sharedPref.getInt(getString(R.string.preferences_plot_distance_key), 1)
        AppPreferences.plotSpeed = sharedPref.getBoolean(getString(R.string.preferences_plot_speed_key), false)

        dataCollectorIntent = Intent(this, DataCollector::class.java)
        starterIntent = intent
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        mainActivityPendingIntent = pendingIntent
        startForegroundService(dataCollectorIntent)

        checkPermissions()

        setContentView(R.layout.activity_main)

        main_checkbox_speed.isChecked = AppPreferences.plotSpeed
        var plotDistanceId = main_radio_10.id
        var plotDistanceValue = 10_001f
        when (AppPreferences.plotDistance) {
            1 -> {
                plotDistanceId = main_radio_10.id
                plotDistanceValue = 10_001f
            }
            2 -> {
                plotDistanceId = main_radio_25.id
                plotDistanceValue = 25_001f
            }
            3 -> {
                plotDistanceId = main_radio_50.id
                plotDistanceValue = 50_001f
            }
        }
        main_radio_group_distance.check(plotDistanceId)

        // if (AppPreferences.consumptionPlot) main_consumption_plot_container.visibility = View.VISIBLE

        if (AppPreferences.experimentalLayout) {
            legacy_layout.visibility = View.GONE
            gage_layout.visibility = View.VISIBLE
        }

        main_consumption_plot.reset()
        main_consumption_plot.addPlotLine(DataHolder.consumptionPlotLine)
        main_consumption_plot.addPlotLine(DataHolder.speedPlotLine)

        DataHolder.speedPlotLine.Visible = main_checkbox_speed.isChecked

        main_consumption_plot.dimension = PlotDimension.DISTANCE
        main_consumption_plot.dimensionRestriction = plotDistanceValue
        main_consumption_plot.invalidate()

        main_charge_plot.reset()
        main_charge_plot.addPlotLine(DataHolder.chargePlotLine)
        main_charge_plot.addPlotLine(DataHolder.stateOfChargePlotLine)

        main_charge_plot.dimension = PlotDimension.TIME
        main_charge_plot.dimensionRestriction = null
        main_charge_plot.invalidate()

        main_title.setOnClickListener {
            finish()
        }

        main_power_gage.gageName = getString(R.string.main_gage_power)
        main_power_gage.gageUnit = "kW"
        main_power_gage.maxValue = 300f
        main_power_gage.minValue = -150f
        main_power_gage.setValue(0f)

        main_consumption_gage.gageName = getString(R.string.main_gage_consumption)
        main_consumption_gage.gageUnit = "kWh/100km"
        main_consumption_gage.minValue = -30f
        main_consumption_gage.maxValue = 60f
        main_consumption_gage.setValue(0f)

/*      main_speed_gage.gageName = getString(R.string.main_gage_speed)
        main_speed_gage.gageUnit = "km/h"
        main_speed_gage.maxValue = 205f
        main_speed_gage.minValue = 0f
        main_speed_gage.setValue(0f)*/

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

        /** cycle through consumption plot distances when tapping the plot */
        main_consumption_plot.setOnClickListener {
            var plotDistanceId = main_radio_10.id

            AppPreferences.plotDistance++

            if (AppPreferences.plotDistance >= 4) AppPreferences.plotDistance = 1

            when (AppPreferences.plotDistance) {
                1 -> plotDistanceId = main_radio_10.id
                2 -> plotDistanceId = main_radio_25.id
                3 -> plotDistanceId = main_radio_50.id
            }
            sharedPref.edit()
                .putInt(getString(R.string.preferences_plot_distance_key), AppPreferences.plotDistance)
                .apply()
            main_radio_group_distance.check(plotDistanceId)
        }

        main_radio_group_distance.setOnCheckedChangeListener { group, checkedId ->
            var id = 1
            when (checkedId) {
                main_radio_10.id -> {
                    main_consumption_plot.dimensionRestriction = 10_001f
                    id = 1
                }
                main_radio_25.id -> {
                    main_consumption_plot.dimensionRestriction = 25_001f
                    id = 2
                }
                main_radio_50.id -> {
                    main_consumption_plot.dimensionRestriction = 50_001f
                    id = 3
                }
            }

            sharedPref.edit()
                .putInt(getString(R.string.preferences_plot_distance_key), id)
                .apply()
            AppPreferences.plotDistance = id
        }

        main_checkbox_speed.setOnClickListener {
            if (main_checkbox_speed.isChecked && !DataHolder.speedPlotLine.Visible) {
                DataHolder.speedPlotLine.Visible = true
            } else if (!main_checkbox_speed.isChecked && DataHolder.speedPlotLine.Visible) {
                DataHolder.speedPlotLine.Visible = false
            }

            sharedPref.edit()
                .putBoolean(getString(R.string.preferences_plot_speed_key), main_checkbox_speed.isChecked)
                .apply()
            AppPreferences.plotSpeed = main_checkbox_speed.isChecked

            main_consumption_plot.invalidate()
        }

        updateUi = true
        timerHandler = Handler(Looper.getMainLooper())
        timerHandler.post(updateActivityTask)
    }

    override fun onDestroy() {
        super.onDestroy()
        updateUi = false
        timerHandler.removeCallbacks(updateActivityTask)
        InAppLogger.log("MainActivity.onDestroy")
    }

    override fun onPause() {
        super.onPause()
        updateUi = false
        timerHandler.removeCallbacks(updateActivityTask)
        InAppLogger.log("MainActivity.onPause")
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

    private var lastPlotUpdate: Long = 0L

    private fun updateActivity() {
        InAppLogger.logUIUpdate()
        /** Use data from DataHolder to Update MainActivity text */

        if (AppPreferences.experimentalLayout && legacy_layout.visibility == View.VISIBLE) {
            gage_layout.visibility = View.VISIBLE
            legacy_layout.visibility = View.GONE
        } else if (!AppPreferences.experimentalLayout && legacy_layout.visibility == View.GONE) {
            gage_layout.visibility = View.GONE
            legacy_layout.visibility = View.VISIBLE
        }

        if (main_consumption_plot_container.visibility == View.GONE && !DataHolder.chargePortConnected) {
            main_consumption_plot_container.visibility = View.VISIBLE
        }
        else if (main_consumption_plot_container.visibility == View.VISIBLE && DataHolder.chargePortConnected) {
            main_consumption_plot_container.visibility = View.GONE
        }

        if (main_charge_plot_container.visibility == View.GONE && DataHolder.chargePortConnected) {
            main_charge_plot.reset()
            main_charge_plot_container.visibility = View.VISIBLE
        }
        else if (main_charge_plot_container.visibility == View.VISIBLE && !DataHolder.chargePortConnected) {
            main_charge_plot_container.visibility = View.GONE
        }

        if (SystemClock.elapsedRealtime() - lastPlotUpdate > 1_000L) {
            if (main_consumption_plot_container.visibility == View.VISIBLE) {
                main_consumption_plot.invalidate()
            }

            if (main_charge_plot_container.visibility == View.VISIBLE) {
                main_charge_plot.invalidate()
            }

            lastPlotUpdate = SystemClock.elapsedRealtime()
        }

        main_power_gage.setValue(DataHolder.currentPowerSmooth / 1000000)
        //main_speed_gage.setValue((DataHolder.currentSpeedSmooth * 3.6f).toInt())
        var consumptionValue: Float? = null

        if (AppPreferences.consumptionUnit) {
            /** Wh/km */
            main_consumption_gage.gageUnit = "Wh/km"
            main_consumption_gage.minValue = -300f
            main_consumption_gage.maxValue = 600f
            if (DataHolder.currentSpeedSmooth > 0) {
                main_consumption_gage.setValue(((DataHolder.currentPowerSmooth / 1000) / (DataHolder.currentSpeedSmooth * 3.6)).toInt())
            } else {
                main_consumption_gage.setValue(consumptionValue)
            }

        } else {
            /** kWh/100km */
            main_consumption_gage.gageUnit = "kWh/100km"
            main_consumption_gage.minValue = -30f
            main_consumption_gage.maxValue = 60f
            if (DataHolder.currentSpeedSmooth > 0) {
                main_consumption_gage.setValue(((DataHolder.currentPowerSmooth / 1000) / (DataHolder.currentSpeedSmooth * 3.6f))/10)
            } else {
                main_consumption_gage.setValue(consumptionValue)
            }
        }

        //var consumptionGageValue: Float? = (((DataHolder.currentPowermW / 1000) / (DataHolder.currentSpeed * 3.6))/10).toFloat()

        chargePortConnectedTextView.text = DataHolder.chargePortConnected.toString()
        if (DataHolder.currentPowerSmooth > 0) {
            currentPowerTextView.setTextColor(Color.RED)
            currentInstConsTextView.setTextColor(Color.RED)
        }
        else {
            currentPowerTextView.setTextColor(Color.GREEN)
            currentInstConsTextView.setTextColor(Color.GREEN)
        }
        currentPowerTextView.text = String.format("%.1f kW", DataHolder.currentPowerSmooth / 1000000)
        if (AppPreferences.consumptionUnit) {
            usedEnergyTextView.text = String.format("%d Wh", DataHolder.usedEnergy.toInt())
        } else {
            usedEnergyTextView.text = String.format("%.1f kWh", DataHolder.usedEnergy / 1000)
        }
        currentSpeedTextView.text = String.format("%d km/h", (DataHolder.currentSpeedSmooth *3.6).toInt())
        traveledDistanceTextView.text = String.format("%.3f km", DataHolder.traveledDistance / 1000)
        batteryEnergyTextView.text = String.format("%d/%d, %d%%",
            DataHolder.currentBatteryCapacity,
            DataHolder.maxBatteryCapacity,
            ((DataHolder.currentBatteryCapacity.toFloat()/ DataHolder.maxBatteryCapacity.toFloat())*100).toInt())

        if (AppPreferences.consumptionUnit) { // Use Wh/km
            if (DataHolder.currentSpeedSmooth > 0) currentInstConsTextView.text = String.format("%d Wh/km",
                ((DataHolder.currentPowerSmooth / 1000) / (DataHolder.currentSpeedSmooth * 3.6)).toInt())
            else currentInstConsTextView.text = "N/A"
            if (DataHolder.traveledDistance > 0) averageConsumptionTextView.text = String.format("%d Wh/km",
                (DataHolder.usedEnergy /(DataHolder.traveledDistance /1000)).toInt())
            else averageConsumptionTextView.text = "N/A"
        } else { // Use kWh/100km
            if (DataHolder.currentSpeedSmooth > 0) currentInstConsTextView.text = String.format("%.1f kWh/100km",
                ((DataHolder.currentPowerSmooth / 1000) / (DataHolder.currentSpeedSmooth * 3.6))/10)
            else currentInstConsTextView.text = "N/A"
            if (DataHolder.traveledDistance > 0) averageConsumptionTextView.text = String.format("%.1f kWh/100km",
                (DataHolder.usedEnergy /(DataHolder.traveledDistance /1000))/10)
            else averageConsumptionTextView.text = "N/A"
        }

        main_gage_avg_consumption_text_view.text = String.format("  Ø %s", averageConsumptionTextView.text)
        main_gage_distance_text_view.text = String.format("  %s", traveledDistanceTextView.text)
        main_gage_used_power_text_view.text = String.format("  %s", usedEnergyTextView.text)
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

        DataHolder.traveledDistance = 0F
        traveledDistanceTextView.text = String.format("%.3f km", DataHolder.traveledDistance / 1000)
        DataHolder.usedEnergy = 0F
        usedEnergyTextView.text = String.format("%d Wh", DataHolder.usedEnergy.toInt())
        DataHolder.averageConsumption = 0F
        averageConsumptionTextView.text = String.format("%d Wh/km", DataHolder.averageConsumption.toInt())
    }
}
