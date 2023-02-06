package com.ixam97.carStatsViewer.activities

import android.app.Activity
import android.app.AlertDialog
import android.car.VehicleGear
import android.content.*
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.SeekBar
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.appPreferences.AppPreferences
import com.ixam97.carStatsViewer.dataManager.TripData
import com.ixam97.carStatsViewer.plot.enums.*
import com.ixam97.carStatsViewer.plot.graphics.*
import com.ixam97.carStatsViewer.plot.objects.*
import com.ixam97.carStatsViewer.dataManager.DataCollector
import com.ixam97.carStatsViewer.dataManager.DataManager
import com.ixam97.carStatsViewer.dataManager.DataManagers
import com.ixam97.carStatsViewer.utils.StringFormatters
import com.ixam97.carStatsViewer.views.PlotView
import kotlinx.android.synthetic.main.activity_summary.*
import java.util.concurrent.TimeUnit

class SummaryActivity: Activity() {

    private var primaryColor: Int? = null

    private var chargePlotLine = PlotLine(
        PlotLineConfiguration(
            PlotRange(0f, 20f, 0f, 160f, 20f),
            PlotLineLabelFormat.NUMBER,
            PlotLabelPosition.LEFT,
            PlotHighlightMethod.AVG_BY_TIME,
            "kW"
        )
    )

    private lateinit var consumptionPlotLine: PlotLine

    private lateinit var tripData: TripData

    private lateinit var appPreferences: AppPreferences

    private lateinit var disabledTint: PorterDuffColorFilter
    private lateinit var enabledTint: PorterDuffColorFilter

    private val seekBarChangeListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            setVisibleChargeCurve(progress)
        }
        override fun onStartTrackingTouch(seekBar: SeekBar?) {}
        override fun onStopTrackingTouch(seekBar: SeekBar?) {}
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                getString(R.string.gear_update_broadcast) -> {
                    updateDistractionOptimization(true)
                }
                else -> {}
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_summary)

        appPreferences = AppPreferences(applicationContext)

        //val tripDataFileName = intent.getStringExtra("FileName").toString()
        // tripData = if (tripDataFileName != "null") DataManager.getTripData(tripDataFileName)
        // else DataManager.getTripData()

        val dataManager :DataManager = if (intent.hasExtra("dataManager")) {
            DataManagers.values()[intent.getIntExtra("dataManager", 0)].dataManager
        } else DataManagers.CURRENT_TRIP.dataManager

        tripData = dataManager.tripData!!
        consumptionPlotLine = dataManager.consumptionPlotLine

        val typedValue = TypedValue()
        applicationContext.theme.resolveAttribute(android.R.attr.colorControlActivated, typedValue, true)
        primaryColor = typedValue.data

        disabledTint = PorterDuffColorFilter(getColor(R.color.disabled_tint), PorterDuff.Mode.SRC_IN)
        enabledTint = PorterDuffColorFilter(getColor(android.R.color.white), PorterDuff.Mode.SRC_IN)

        summary_button_back.setOnClickListener {
            finish()
        }

        summary_button_reset.setOnClickListener {
            createResetDialog()
        }

        summary_trip_date_text.text = getString(R.string.summary_trip_start_date).format(StringFormatters.getDateString(tripData.tripStartDate))

        summary_button_show_consumption_container.isSelected = true

        updateDistractionOptimization()

        summary_button_show_consumption_container.setOnClickListener {
            switchToConsumptionLayout()
        }

        summary_button_show_charge_container.setOnClickListener {
            switchToChargeLayout()
        }

        setupConsumptionLayout()
        setupChargeLayout()

        registerReceiver(broadcastReceiver, IntentFilter(getString(R.string.gear_update_broadcast)))
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadcastReceiver)
    }

    private fun setupConsumptionLayout() {
        val plotMarkers = PlotMarkers()
        plotMarkers.addMarkers(tripData.markers)
        summary_consumption_plot.addPlotLine(consumptionPlotLine)
        summary_consumption_plot.sessionGapRendering = PlotSessionGapRendering.JOIN
        summary_consumption_plot.secondaryDimension = PlotSecondaryDimension.SPEED
        summary_consumption_plot.dimension = PlotDimension.DISTANCE
        summary_consumption_plot.dimensionRestriction = ((tripData.traveledDistance / MainActivity.DISTANCE_TRIP_DIVIDER).toInt() + 1) * MainActivity.DISTANCE_TRIP_DIVIDER + 1
        summary_consumption_plot.dimensionRestrictionMin = MainActivity.DISTANCE_TRIP_DIVIDER
        summary_consumption_plot.dimensionSmoothingPercentage = 0.02f
        summary_consumption_plot.setPlotMarkers(plotMarkers)
        summary_consumption_plot.visibleMarkerTypes.add(PlotMarkerType.CHARGE)
        summary_consumption_plot.visibleMarkerTypes.add(PlotMarkerType.PARK)

        summary_consumption_plot.invalidate()

        summary_distance_value_text.text = StringFormatters.getTraveledDistanceString(tripData.traveledDistance)
        summary_used_energy_value_text.text = StringFormatters.getEnergyString(tripData.usedEnergy)
        summary_avg_consumption_value_text.text = StringFormatters.getAvgConsumptionString(tripData.usedEnergy, tripData.traveledDistance)
        summary_travel_time_value_text.text = StringFormatters.getElapsedTimeString(tripData.travelTime)
    }

    private fun setupChargeLayout() {
        summary_charge_plot_sub_title_curve.text = "%s (%d/%d, %s)".format(
            getString(R.string.settings_sub_title_last_charge_plot),
            tripData.chargeCurves.size,
            tripData.chargeCurves.size,
            StringFormatters.getDateString(tripData.chargeCurves.last().chargeStartDate))

        chargePlotLine.plotPaint = PlotPaint.byColor(getColor(R.color.charge_plot_color), PlotView.textSize)
        chargePlotLine.secondaryPlotPaint = when {
            appPreferences.chargePlotSecondaryColor -> PlotPaint.byColor(getColor(R.color.secondary_plot_color_alt), PlotView.textSize)
            else -> PlotPaint.byColor(getColor(R.color.secondary_plot_color), PlotView.textSize)
        }
        chargePlotLine.reset()
        if (tripData.chargeCurves.isNotEmpty()) {
            chargePlotLine.addDataPoints(tripData.chargeCurves.last().chargePlotLine)
            summary_charge_plot_button_next.isEnabled = false
            summary_charge_plot_button_next.colorFilter = disabledTint
            summary_charge_plot_button_prev.isEnabled = true
            summary_charge_plot_button_prev.colorFilter = enabledTint

            summary_charged_energy_value_text.text = StringFormatters.getEnergyString(tripData.chargeCurves.last().chargedEnergy)
            summary_charge_time_value_text.text = StringFormatters.getElapsedTimeString(tripData.chargeCurves.last().chargeTime)
            summary_charge_ambient_temp.text = StringFormatters.getTemperatureString(tripData.chargeCurves.last().ambientTemperature)
            summary_charge_plot_view.dimensionRestriction = TimeUnit.MINUTES.toMillis((TimeUnit.MILLISECONDS.toMinutes(tripData.chargeCurves.last().chargeTime) / 5) + 1) * 5 + 1
            summary_charge_plot_view.dimensionRestrictionMin = TimeUnit.MINUTES.toMillis(5L)
        }
        if (tripData.chargeCurves.size < 2){
            summary_charge_plot_button_next.isEnabled = false
            summary_charge_plot_button_next.colorFilter = disabledTint
            summary_charge_plot_button_prev.isEnabled = false
            summary_charge_plot_button_prev.colorFilter = disabledTint
        }
        summary_charge_plot_view.addPlotLine(chargePlotLine)

        summary_charge_plot_view.dimension = PlotDimension.TIME
        // summary_charge_plot_view.dimensionSmoothingPercentage = 0.01f
        summary_charge_plot_view.sessionGapRendering = PlotSessionGapRendering.GAP
        summary_charge_plot_view.secondaryDimension = PlotSecondaryDimension.STATE_OF_CHARGE
        summary_charge_plot_view.invalidate()

        summary_charge_plot_seek_bar.max = (tripData.chargeCurves.size - 1).coerceAtLeast(0)
        summary_charge_plot_seek_bar.progress = (tripData.chargeCurves.size - 1).coerceAtLeast(0)
        summary_charge_plot_seek_bar.setOnSeekBarChangeListener(seekBarChangeListener)

        summary_charge_plot_button_next.setOnClickListener {
            val newProgress = summary_charge_plot_seek_bar.progress + 1
            if (newProgress <= (tripData.chargeCurves.size - 1)) {
                summary_charge_plot_seek_bar.progress = newProgress
            }
        }

        summary_charge_plot_button_prev.setOnClickListener {
            val newProgress = summary_charge_plot_seek_bar.progress - 1
            if (newProgress >= 0) {
                summary_charge_plot_seek_bar.progress = newProgress
            }
        }
    }

    private fun setVisibleChargeCurve(progress: Int) {
        summary_charge_plot_sub_title_curve.text = "%s (%d/%d, %s)".format(
            getString(R.string.settings_sub_title_last_charge_plot),
            tripData.chargeCurves.size,
            tripData.chargeCurves.size,
            StringFormatters.getDateString(tripData.chargeCurves.last().chargeStartDate))

        if (tripData.chargeCurves.size - 1 == 0) {
            summary_charge_plot_sub_title_curve.text = "%s (0/0)".format(
                getString(R.string.settings_sub_title_last_charge_plot))

            summary_charge_plot_button_next.isEnabled = false
            summary_charge_plot_button_next.colorFilter = disabledTint
            summary_charge_plot_button_prev.isEnabled = false
            summary_charge_plot_button_prev.colorFilter = disabledTint

        } else {
            summary_charge_plot_sub_title_curve.text = "%s (%d/%d, %s)".format(
                getString(R.string.settings_sub_title_last_charge_plot),
                progress + 1,
                tripData.chargeCurves.size,
                StringFormatters.getDateString(tripData.chargeCurves.last().chargeStartDate))

            when (progress) {
                0 -> {
                    summary_charge_plot_button_prev.isEnabled = false
                    summary_charge_plot_button_prev.colorFilter = disabledTint
                    summary_charge_plot_button_next.isEnabled = true
                    summary_charge_plot_button_next.colorFilter = enabledTint
                }
                tripData.chargeCurves.size - 1 -> {
                    summary_charge_plot_button_next.isEnabled = false
                    summary_charge_plot_button_next.colorFilter = disabledTint
                    summary_charge_plot_button_prev.isEnabled = true
                    summary_charge_plot_button_prev.colorFilter = enabledTint
                }
                else -> {
                    summary_charge_plot_button_next.isEnabled = true
                    summary_charge_plot_button_next.colorFilter = enabledTint
                    summary_charge_plot_button_prev.isEnabled = true
                    summary_charge_plot_button_prev.colorFilter = enabledTint
                }
            }
        }

        summary_charged_energy_value_text.text = StringFormatters.getEnergyString(tripData.chargeCurves[progress].chargedEnergy)
        summary_charge_time_value_text.text = StringFormatters.getElapsedTimeString(tripData.chargeCurves[progress].chargeTime)
        summary_charge_ambient_temp.text = StringFormatters.getTemperatureString(tripData.chargeCurves[progress].ambientTemperature)

        chargePlotLine.reset()
        chargePlotLine.addDataPoints(tripData.chargeCurves[progress].chargePlotLine)
        summary_charge_plot_view.dimensionRestriction = TimeUnit.MINUTES.toNanos((TimeUnit.MILLISECONDS.toMinutes(tripData.chargeCurves[progress].chargeTime) / 5) + 1) * 5 + TimeUnit.MILLISECONDS.toNanos(1)
        summary_charge_plot_view.invalidate()
    }

    private fun switchToConsumptionLayout() {
        summary_consumption_container.visibility = View.VISIBLE
        summary_charge_container.visibility = View.GONE
        summary_consumption_container.scrollTo(0,0)
        summary_button_show_charge_container.isSelected = false
        summary_button_show_consumption_container.isSelected = true

    }

    private fun switchToChargeLayout() {
        summary_consumption_container.visibility = View.GONE
        summary_charge_container.visibility = View.VISIBLE
        summary_charge_container.scrollTo(0,0)
        summary_button_show_consumption_container.isSelected = false
        summary_button_show_charge_container.isSelected = true
    }

    private fun createResetDialog() {
        val builder = AlertDialog.Builder(this@SummaryActivity)
        builder.setTitle(getString(R.string.dialog_reset_title))
            .setMessage(getString(R.string.dialog_reset_message))
            .setCancelable(true)
            .setPositiveButton(getString(R.string.dialog_reset_do_save)) { _, _ ->
                DataCollector.CurrentTripDataManager.reset()
                sendBroadcast(Intent(getString(R.string.save_trip_data_broadcast)))
                this@SummaryActivity.finish()
            }
            .setNegativeButton(R.string.dialog_reset_no_save) { _, _ ->
                // DataCollector.CurrentTripDataManager.reset()
                enumValues<DataManagers>().forEach { it.dataManager.reset() }
                sendBroadcast(Intent(getString(R.string.save_trip_data_broadcast)))
                this@SummaryActivity.finish()
            }
            .setNeutralButton(getString(R.string.dialog_reset_cancel)) { dialog, _ ->
                dialog.cancel()
            }
        val alert = builder.create()
        alert.show()
        alert.getButton(DialogInterface.BUTTON_POSITIVE).isEnabled = false
        alert.getButton(DialogInterface.BUTTON_NEGATIVE).setBackgroundColor(getColor(R.color.bad_red))

    }

    private fun updateDistractionOptimization(update: Boolean = false) {
        if (update) {
            finish()
            startActivity(intent)
            return
        }
        summary_parked_warning.visibility =
            if (DataCollector.CurrentTripDataManager.currentGear != VehicleGear.GEAR_PARK) View.VISIBLE
            else View.GONE
    }
}