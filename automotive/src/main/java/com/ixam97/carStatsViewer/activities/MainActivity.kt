package com.ixam97.carStatsViewer.activities

import com.ixam97.carStatsViewer.*
import com.ixam97.carStatsViewer.dataManager.*
import android.app.Activity
import android.app.PendingIntent
import android.car.VehicleGear
import android.car.VehiclePropertyIds
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.format.DateFormat
import android.view.View
import android.widget.Toast
import com.ixam97.carStatsViewer.appPreferences.AppPreferences
import com.ixam97.carStatsViewer.plot.enums.*
import com.ixam97.carStatsViewer.plot.graphics.PlotPaint
import com.ixam97.carStatsViewer.views.PlotView
import com.ixam97.carStatsViewer.dataManager.DataManagers
import com.ixam97.carStatsViewer.services.LocCollector
import com.ixam97.carStatsViewer.utils.StringFormatters
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

var emulatorMode = false
var emulatorPowerSign = -1

class MainActivity : Activity() {
    companion object {
        private const val UI_UPDATE_INTERVAL = 1000L
        const val DISTANCE_TRIP_DIVIDER = 5_000L
        // lateinit var context: Context
        lateinit var appPreferences: AppPreferences
    }

    /** values and variables */

    private lateinit var timerHandler: Handler
    private lateinit var starterIntent: Intent
    private lateinit var context: Context
    // private lateinit var appPreferences: AppPreferences

    private var selectedDataManager = DataManagers.CURRENT_TRIP.dataManager

    private var updateUi = false
    private var lastPlotUpdate: Long = 0L

    private val updateActivityTask = object : Runnable {
        override fun run() {
            updateActivity()
            if (updateUi) timerHandler.postDelayed(this, UI_UPDATE_INTERVAL)
        }
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                getString(R.string.ui_update_plot_broadcast) -> updatePlots()
                getString(R.string.ui_update_gages_broadcast) -> updateGages()
            }
        }
    }

    /** Overrides */

    override fun onResume() {
        super.onResume()

        // InAppLogger.log("MainActivity.onResume")

        if (appPreferences.consumptionUnit) {
            selectedDataManager.consumptionPlotLine.Configuration.Unit = "Wh/km"
            selectedDataManager.consumptionPlotLine.Configuration.LabelFormat = PlotLineLabelFormat.NUMBER
            selectedDataManager.consumptionPlotLine.Configuration.Divider = 1f
        } else {
            selectedDataManager.consumptionPlotLine.Configuration.Unit = "kWh/100km"
            selectedDataManager.consumptionPlotLine.Configuration.LabelFormat = PlotLineLabelFormat.FLOAT
            selectedDataManager.consumptionPlotLine.Configuration.Divider = 10f
        }

        main_power_gage.maxValue = if (appPreferences.consumptionPlotSingleMotor) 170f else 300f
        main_power_gage.minValue = if (appPreferences.consumptionPlotSingleMotor) -100f else -150f

        main_power_gage.barVisibility = appPreferences.consumptionPlotVisibleGages
        main_consumption_gage.barVisibility = appPreferences.consumptionPlotVisibleGages
        main_SoC_gage.barVisibility = appPreferences.chargePlotVisibleGages
        main_charge_gage.barVisibility = appPreferences.chargePlotVisibleGages

        main_checkbox_speed.isChecked = appPreferences.plotSpeed
        main_consumption_plot.secondaryDimension = when (appPreferences.plotSpeed) {
            true -> PlotSecondaryDimension.SPEED
            else -> null
        }

        main_button_speed.text = when {
            main_consumption_plot.secondaryDimension != null -> getString(R.string.main_button_hide_speed)
            else -> getString(R.string.main_button_show_speed)
        }
        selectedDataManager.consumptionPlotLine.plotPaint = PlotPaint.byColor(getColor(R.color.primary_plot_color), PlotView.textSize)
        selectedDataManager.consumptionPlotLine.secondaryPlotPaint = when {
            appPreferences.consumptionPlotSecondaryColor -> PlotPaint.byColor(getColor(R.color.secondary_plot_color_alt), PlotView.textSize)
            else -> PlotPaint.byColor(getColor(R.color.secondary_plot_color), PlotView.textSize)
        }

        selectedDataManager.chargePlotLine.plotPaint = PlotPaint.byColor(getColor(R.color.charge_plot_color), PlotView.textSize)
        selectedDataManager.chargePlotLine.secondaryPlotPaint = when {
            appPreferences.chargePlotSecondaryColor -> PlotPaint.byColor(getColor(R.color.secondary_plot_color_alt), PlotView.textSize)
            else -> PlotPaint.byColor(getColor(R.color.secondary_plot_color), PlotView.textSize)
        }

        main_consumption_plot.invalidate()

        enableUiUpdates()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // InAppLogger.log("MainActivity.onCreate")

        context = applicationContext

        appPreferences = AppPreferences(context)
        StringFormatters.appPreferences = appPreferences
        StringFormatters.dateFormat = DateFormat.getDateFormat(context)
        StringFormatters.timeFormat = DateFormat.getTimeFormat(context)

        startForegroundService(Intent(this, DataCollector::class.java))
        startService(Intent(this, LocCollector::class.java))

        DataCollector.mainActivityPendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        setContentView(R.layout.activity_main)
        setupDefaultUi()
        setUiEventListeners()

        timerHandler = Handler(Looper.getMainLooper())

        registerReceiver(
            broadcastReceiver,
            IntentFilter(getString(R.string.ui_update_plot_broadcast))
        )
        registerReceiver(
            broadcastReceiver,
            IntentFilter(getString(R.string.ui_update_gages_broadcast))
        )

        main_button_performance.isEnabled = false
        main_button_performance.colorFilter = PorterDuffColorFilter(getColor(R.color.disabled_tint), PorterDuff.Mode.SRC_IN)

        enableUiUpdates()
    }

    override fun onDestroy() {
        super.onDestroy()
        disableUiUpdates()
        unregisterReceiver(broadcastReceiver)
        // InAppLogger.log("MainActivity.onDestroy")
    }

    override fun onPause() {
        super.onPause()
        disableUiUpdates()
        // InAppLogger.log("MainActivity.onPause")
    }

    /** Private functions */

    private fun updateActivity() {
        // InAppLogger.logUIUpdate()
        /** Use data from DataManager to Update MainActivity text */

        setUiVisibilities()
        updateGages()

        main_gage_avg_consumption_text_view.text = "  Ø %s".format(StringFormatters.getAvgConsumptionString(selectedDataManager.usedEnergy, selectedDataManager.traveledDistance))
        main_gage_distance_text_view.text = "  %s".format(StringFormatters.getTraveledDistanceString(selectedDataManager.traveledDistance))
        main_gage_used_power_text_view.text = "  %s".format(StringFormatters.getEnergyString(selectedDataManager.usedEnergy))
        main_gage_time_text_view.text = "  %s".format(getElapsedTimeString(selectedDataManager.travelTime))
        main_gage_charged_energy_text_view.text = "  %s".format(StringFormatters.getEnergyString(selectedDataManager.chargedEnergy))
        main_gage_charge_time_text_view.text = "  %s".format(getElapsedTimeString(selectedDataManager.chargeTime))
        main_gage_ambient_temperature_text_view.text = "  %.1f° C".format(selectedDataManager.ambientTemperature)
    }

    private fun setUiVisibilities() {

        if (main_button_dismiss_charge_plot.isEnabled == selectedDataManager.chargePortConnected)
            main_button_dismiss_charge_plot.isEnabled = !selectedDataManager.chargePortConnected

        if (main_charge_layout.visibility == View.GONE && selectedDataManager.chargePortConnected) {
            main_consumption_layout.visibility = View.GONE
            main_charge_layout.visibility = View.VISIBLE
        }
    }

    private fun updateGages() {
        if ((selectedDataManager.currentPower / 1_000_000).absoluteValue > 10 && true) { // Add Setting!
            val newValue = (selectedDataManager.currentPower / 1_000_000).toInt()
            main_power_gage.setValue(newValue)
        } else {
            main_power_gage.setValue(selectedDataManager.currentPower / 1_000_000)
        }
        main_charge_gage.setValue(-selectedDataManager.currentPower / 1_000_000)
        main_SoC_gage.setValue((100f / selectedDataManager.maxBatteryLevel * selectedDataManager.batteryLevel).roundToInt())

        var consumptionValue: Float? = null

        if (appPreferences.consumptionUnit) {
            main_consumption_gage.gageUnit = "Wh/km"
            main_consumption_gage.minValue = -300f
            main_consumption_gage.maxValue = 600f
            if (selectedDataManager.currentSpeed * 3.6 > 3) {
                main_consumption_gage.setValue(((selectedDataManager.currentPower / 1000) / (selectedDataManager.currentSpeed * 3.6)).toInt())
            } else {
                main_consumption_gage.setValue(consumptionValue)
            }

        } else {
            main_consumption_gage.gageUnit = "kWh/100km"
            main_consumption_gage.minValue = -30f
            main_consumption_gage.maxValue = 60f
            if (selectedDataManager.currentSpeed * 3.6 > 3) {
                main_consumption_gage.setValue(((selectedDataManager.currentPower / 1000) / (selectedDataManager.currentSpeed * 3.6f))/10)
            } else {
                main_consumption_gage.setValue(consumptionValue)
            }
        }
    }

    private fun updatePlots(){
        // if (appPreferences.plotDistance == 3) main_consumption_plot.dimensionRestriction = dimensionRestrictionById(appPreferences.plotDistance)

        main_charge_plot.dimensionRestriction = TimeUnit.MINUTES.toNanos((TimeUnit.MILLISECONDS.toMinutes(selectedDataManager.chargeTime) / 5) + 1) * 5 + TimeUnit.MILLISECONDS.toNanos(1)

        if (SystemClock.elapsedRealtime() - lastPlotUpdate > 1_000L) {
            if (main_consumption_layout.visibility == View.VISIBLE) {
                main_consumption_plot.invalidate()
            }

            if (main_charge_layout.visibility == View.VISIBLE) {
                main_charge_plot.invalidate()
            }

            lastPlotUpdate = SystemClock.elapsedRealtime()
        }
    }

    private fun getElapsedTimeString(elapsedTime: Long): String {
        return String.format("%02d:%02d:%02d",
            TimeUnit.MILLISECONDS.toHours(elapsedTime),
            TimeUnit.MILLISECONDS.toMinutes(elapsedTime) % TimeUnit.HOURS.toMinutes(1),
            TimeUnit.MILLISECONDS.toSeconds(elapsedTime) % TimeUnit.MINUTES.toSeconds(1))
    }

    private fun resetStats() {
        finish()
        startActivity(intent)
        // InAppLogger.log("MainActivity.resetStats")
        selectedDataManager.reset()
        sendBroadcast(Intent(getString(R.string.save_trip_data_broadcast)))
    }

    private fun setupDefaultUi() {

        var plotDistanceId = when (appPreferences.plotDistance) {
            1 -> main_radio_10.id
            2 -> main_radio_25.id
            3 -> main_radio_50.id
            else -> main_radio_10.id
        }

        main_radio_group_distance.check(plotDistanceId)

        main_consumption_plot.reset()
        main_consumption_plot.addPlotLine(selectedDataManager.consumptionPlotLine)

        main_button_speed.text = when {
            main_consumption_plot.secondaryDimension != null -> getString(R.string.main_button_hide_speed)
            else -> getString(R.string.main_button_show_speed)
        }

        selectedDataManager.consumptionPlotLine.secondaryPlotPaint = when {
            appPreferences.consumptionPlotSecondaryColor -> PlotPaint.byColor(getColor(R.color.secondary_plot_color_alt), PlotView.textSize)
            else -> PlotPaint.byColor(getColor(R.color.secondary_plot_color), PlotView.textSize)
        }

        main_consumption_plot.dimension = PlotDimension.DISTANCE
        main_consumption_plot.dimensionRestriction = 10_001L
        main_consumption_plot.dimensionSmoothingPercentage = 0.02f
        main_consumption_plot.sessionGapRendering = PlotSessionGapRendering.JOIN
        main_consumption_plot.secondaryDimension = when (appPreferences.plotSpeed) {
            true -> PlotSecondaryDimension.SPEED
            else -> null
        }

        main_consumption_plot.invalidate()

        main_charge_plot.reset()
        main_charge_plot.addPlotLine(selectedDataManager.chargePlotLine)

        selectedDataManager.chargePlotLine.secondaryPlotPaint = when {
            appPreferences.chargePlotSecondaryColor -> PlotPaint.byColor(getColor(R.color.secondary_plot_color_alt), PlotView.textSize)
            else -> PlotPaint.byColor(getColor(R.color.secondary_plot_color), PlotView.textSize)
        }

        main_charge_plot.dimension = PlotDimension.TIME
        main_charge_plot.dimensionRestriction = null
        main_charge_plot.sessionGapRendering = PlotSessionGapRendering.GAP
        main_charge_plot.secondaryDimension = PlotSecondaryDimension.STATE_OF_CHARGE
        main_charge_plot.invalidate()

        main_power_gage.gageName = getString(R.string.main_gage_power)
        main_power_gage.gageUnit = "kW"
        main_power_gage.primaryColor = getColor(R.color.polestar_orange)
        main_power_gage.maxValue = if (appPreferences.consumptionPlotSingleMotor) 170f else 300f
        main_power_gage.minValue = if (appPreferences.consumptionPlotSingleMotor) -100f else -150f
        main_power_gage.setValue(0f)

        main_consumption_gage.gageName = getString(R.string.main_gage_consumption)
        main_consumption_gage.gageUnit = "kWh/100km"
        main_consumption_gage.primaryColor = getColor(R.color.polestar_orange)
        main_consumption_gage.minValue = -30f
        main_consumption_gage.maxValue = 60f
        main_consumption_gage.setValue(0f)

        main_charge_gage.gageName = getString(R.string.main_gage_charging_power)
        main_charge_gage.gageUnit = "kW"
        main_charge_gage.primaryColor = getColor(R.color.charge_plot_color)
        main_charge_gage.minValue = 0f
        main_charge_gage.maxValue = 160f
        main_charge_gage.setValue(0f)

        main_SoC_gage.gageName = getString(R.string.main_gage_SoC)
        main_SoC_gage.gageUnit = "%"
        main_SoC_gage.primaryColor = getColor(R.color.charge_plot_color)
        main_SoC_gage.minValue = 0f
        main_SoC_gage.maxValue = 100f
        main_SoC_gage.setValue(0f)
    }

    private fun setUiEventListeners() {

        main_title.setOnClickListener {
            if (emulatorMode) {
                val gearSimulationIntent = Intent(getString(R.string.VHAL_emulator_broadcast)).apply {
                    putExtra(
                        EmulatorIntentExtras.PROPERTY_ID,
                        VehiclePropertyIds.GEAR_SELECTION
                    )
                    putExtra(EmulatorIntentExtras.TYPE, EmulatorIntentExtras.TYPE_INT)
                    if (selectedDataManager.currentGear == VehicleGear.GEAR_PARK) {
                        putExtra(
                            EmulatorIntentExtras.VALUE,
                            VehicleGear.GEAR_DRIVE
                        )
                        Toast.makeText(applicationContext, "Drive", Toast.LENGTH_SHORT).show()
                    }
                    else {
                        putExtra(EmulatorIntentExtras.VALUE, VehicleGear.GEAR_PARK)
                        Toast.makeText(applicationContext, "Park", Toast.LENGTH_SHORT).show()
                    }
                }
                sendBroadcast(gearSimulationIntent)
                sendBroadcast(Intent(getString(R.string.save_trip_data_broadcast)))
            }
        }
        main_title_icon.setOnClickListener {
            if (emulatorMode) {
                emulatorPowerSign = if (emulatorPowerSign < 0) 1
                else -1
                Toast.makeText(this, "Power sign: ${if(emulatorPowerSign<0) "-" else "+"}", Toast.LENGTH_SHORT).show()
            }
        }

        main_button_settings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        main_button_speed.setOnClickListener {
            main_consumption_plot.secondaryDimension = when (main_consumption_plot.secondaryDimension) {
                null -> PlotSecondaryDimension.SPEED
                else -> null
            }

            appPreferences.plotSpeed = main_consumption_plot.secondaryDimension != null
            main_consumption_plot.invalidate()
            main_button_speed.text = when {
                main_consumption_plot.secondaryDimension != null -> getString(R.string.main_button_hide_speed)
                else -> getString(R.string.main_button_show_speed)
            }
        }

        main_button_summary.setOnClickListener {
            val summaryIntent = Intent(this, SummaryActivity::class.java)
            summaryIntent.putExtra("dataManager", DataManagers.values().indexOf(DataManagers.CURRENT_TRIP))
            startActivity(summaryIntent)
        }

        main_button_summary_charge.setOnClickListener {
            val summaryIntent = Intent(this, SummaryActivity::class.java)
            summaryIntent.putExtra("dataManager", DataManagers.values().indexOf(DataManagers.CURRENT_TRIP))
            startActivity(summaryIntent)
        }

        main_button_dismiss_charge_plot.setOnClickListener {
            main_charge_layout.visibility = View.GONE
            main_consumption_layout.visibility = View.VISIBLE
            main_consumption_plot.invalidate()
            // DataManager.chargedEnergy = 0f
            // DataManager.chargeTime = 0L
        }
    }

    private fun enableUiUpdates() {
        // InAppLogger.log("Enabling UI updates")
        updateUi = true
        if (this::timerHandler.isInitialized) {
            timerHandler.removeCallbacks(updateActivityTask)
            timerHandler.post(updateActivityTask)
        }

    }

    private fun disableUiUpdates() {
        // InAppLogger.log("Disabling UI Updates")
        updateUi = false
        if (this::timerHandler.isInitialized) {
            timerHandler.removeCallbacks(updateActivityTask)
        }
    }

}