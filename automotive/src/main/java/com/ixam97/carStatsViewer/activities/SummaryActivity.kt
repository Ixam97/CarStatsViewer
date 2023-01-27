package com.ixam97.carStatsViewer.activities

import android.app.Activity
import android.app.AlertDialog
import android.car.VehicleGear
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.objects.AppPreferences
import com.ixam97.carStatsViewer.objects.DataHolder
import com.ixam97.carStatsViewer.objects.TripData
import com.ixam97.carStatsViewer.plot.PlotDimension
import com.ixam97.carStatsViewer.plot.PlotMarkerType
import com.ixam97.carStatsViewer.plot.PlotSecondaryDimension
import kotlinx.android.synthetic.main.activity_summary.*
import java.util.*
import java.util.concurrent.TimeUnit

class SummaryActivity: Activity() {

    private var primaryColor: Int? = null

    private lateinit var tripData: TripData

    private lateinit var appPreferences: AppPreferences

    init {
    }

    private fun getUsedEnergyString(): String {
        if (!appPreferences.consumptionUnit) {
            return "%.1f kWh".format(
                Locale.ENGLISH,
                DataHolder.usedEnergy / 1000)
        }
        return "${DataHolder.usedEnergy.toInt()} Wh"
    }

    private fun getTraveledDistanceString(): String {
        return "%.1f km".format(Locale.ENGLISH, DataHolder.traveledDistance / 1000)
    }

    private fun getAvgConsumptionString(): String {
        val unitString = if (appPreferences.consumptionUnit) "Wh/km" else "kWh/100km"
        if (DataHolder.traveledDistance <= 0) {
            return "-/- $unitString"
        }
        if (!appPreferences.consumptionUnit) {
            return "%.1f %s".format(
                Locale.ENGLISH,
                (DataHolder.usedEnergy /(DataHolder.traveledDistance /1000))/10,
                unitString)
        }
        return "${(DataHolder.usedEnergy /(DataHolder.traveledDistance /1000)).toInt()} $unitString"
    }

    private fun getElapsedTimeString(elapsedTime: Long): String {
        return String.format("%02d:%02d:%02d",
            TimeUnit.MILLISECONDS.toHours(elapsedTime),
            TimeUnit.MILLISECONDS.toMinutes(elapsedTime) % TimeUnit.HOURS.toMinutes(1),
            TimeUnit.MILLISECONDS.toSeconds(elapsedTime) % TimeUnit.MINUTES.toSeconds(1))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_summary)

        appPreferences = AppPreferences(applicationContext)

        val tripDataFileName = intent.getStringExtra("FileName").toString()
        tripData = if (tripDataFileName != "null") DataHolder.getTripData(tripDataFileName)
        else DataHolder.getTripData()

        val typedValue = TypedValue()
        applicationContext.theme.resolveAttribute(android.R.attr.colorControlActivated, typedValue, true)
        primaryColor = typedValue.data

        summary_distance_value_text.text = getTraveledDistanceString()
        summary_used_energy_value_text.text = getUsedEnergyString()
        summary_avg_consumption_value_text.text = getAvgConsumptionString()
        summary_travel_time_value_text.text = getElapsedTimeString(DataHolder.travelTimeMillis)

        summary_button_back.setOnClickListener {
            finish()
        }

        summary_button_reset.setOnClickListener {
            createResetDialog()
        }

        if (DataHolder.currentGear != VehicleGear.GEAR_PARK) {
            summary_consumption_container.visibility = View.GONE
            summary_button_show_consumption_container.isEnabled = false
            summary_button_show_charge_container.isEnabled = false
            summary_parked_warning.visibility = View.VISIBLE
        } else {
            summary_button_show_consumption_container.isSelected = true
        }

        summary_button_show_consumption_container.setOnClickListener {
            switchToConsumptionLayout()
        }

        summary_button_show_charge_container.setOnClickListener {
            switchToChargeLayout()
        }

        setupConsumptionLayout()
        setupChargeLayout()
    }

    private fun setupConsumptionLayout() {
        summary_consumption_plot.addPlotLine(DataHolder.consumptionPlotLine)
        summary_consumption_plot.secondaryDimension = PlotSecondaryDimension.SPEED
        summary_consumption_plot.dimension = PlotDimension.DISTANCE
        summary_consumption_plot.dimensionRestriction = ((DataHolder.traveledDistance / MainActivity.DISTANCE_TRIP_DIVIDER).toInt() + 1) * MainActivity.DISTANCE_TRIP_DIVIDER + 1
        summary_consumption_plot.dimensionSmoothingPercentage = 0.02f
        summary_consumption_plot.setPlotMarkers(DataHolder.plotMarkers)
        summary_consumption_plot.visibleMarkerTypes.add(PlotMarkerType.CHARGE)
        summary_consumption_plot.visibleMarkerTypes.add(PlotMarkerType.PARK)
        summary_consumption_plot.dimensionShiftTouchInterval = 1_000L
        summary_consumption_plot.dimensionRestrictionTouchInterval = 5_000L

        summary_consumption_plot.invalidate()
    }

    private fun setupChargeLayout() {
        summary_charge_plot.dimension = PlotDimension.TIME
        summary_charge_plot.dimensionRestriction = null
        summary_charge_plot.dimensionSmoothingPercentage = 0.01f

        summary_charge_plot.addPlotLine(DataHolder.chargePlotLine)
        summary_charge_plot.secondaryDimension = PlotSecondaryDimension.STATE_OF_CHARGE
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
                DataHolder.resetDataHolder()
                sendBroadcast(Intent(getString(R.string.save_trip_data_broadcast)))
                this@SummaryActivity.finish()
            }
            .setNegativeButton(R.string.dialog_reset_no_save) { _, _ ->
                DataHolder.resetDataHolder()
                sendBroadcast(Intent(getString(R.string.save_trip_data_broadcast)))
                this@SummaryActivity.finish()
            }
            .setNeutralButton(getString(R.string.dialog_reset_cancel)) { dialog, _ ->
                dialog.cancel()
            }
        val alert = builder.create()
        alert.show()
        alert.getButton(DialogInterface.BUTTON_NEGATIVE).setBackgroundColor(getColor(R.color.bad_red))

    }

}