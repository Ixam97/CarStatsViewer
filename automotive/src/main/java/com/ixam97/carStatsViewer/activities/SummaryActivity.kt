package com.ixam97.carStatsViewer.activities

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.objects.DataHolder
import com.ixam97.carStatsViewer.plot.PlotDimension
import com.ixam97.carStatsViewer.plot.PlotMarkerType
import com.ixam97.carStatsViewer.plot.PlotSecondaryDimension
import kotlinx.android.synthetic.main.activity_summary.*

class SummaryActivity: Activity() {

    private var primaryColor: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_summary)

        val typedValue = TypedValue()
        applicationContext.theme.resolveAttribute(android.R.attr.colorControlActivated, typedValue, true)
        primaryColor = typedValue.data

        summary_button_back.setOnClickListener {
            finish()
        }

        summary_button_reset.setOnClickListener {
            createResetDialog()
        }

        summary_button_show_consumption_container.setBackgroundColor(primaryColor!!)
        summary_button_show_consumption_container.setOnClickListener {
            switchToConsumptionLayout()
        }

        summary_button_show_charge_container.background = getDrawable(R.color.button_background_inactive)
        summary_button_show_charge_container.setTextColor(getColor(R.color.button_text_inactive))
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
        summary_button_show_charge_container.background = getDrawable(R.color.button_background_inactive)
        summary_button_show_charge_container.setTextColor(getColor(R.color.button_text_inactive))
        summary_button_show_consumption_container.setBackgroundColor(primaryColor!!)
        summary_button_show_consumption_container.setTextColor(Color.WHITE)

    }

    private fun switchToChargeLayout() {
        summary_consumption_container.visibility = View.GONE
        summary_charge_container.visibility = View.VISIBLE
        summary_charge_container.scrollTo(0,0)
        summary_button_show_consumption_container.background = getDrawable(R.color.button_background_inactive)
        summary_button_show_consumption_container.setTextColor(getColor(R.color.button_text_inactive))
        summary_button_show_charge_container.setBackgroundColor(primaryColor!!)
        summary_button_show_charge_container.setTextColor(Color.WHITE)
    }

    private fun createResetDialog() {
        val builder = AlertDialog.Builder(this@SummaryActivity)
        builder.setTitle(getString(R.string.dialog_reset_title))
            .setMessage(getString(R.string.dialog_reset_message))
            .setCancelable(true)
            .setPositiveButton(getString(R.string.dialog_reset_do_save)) { _, _ ->
                DataHolder.resetDataHolder()
                sendBroadcast(Intent(getString(R.string.save_trip_data_broadcast)))
            }
            .setNegativeButton(R.string.dialog_reset_no_save) { _, _ ->
                DataHolder.resetDataHolder()
                sendBroadcast(Intent(getString(R.string.save_trip_data_broadcast)))
            }
            .setNeutralButton(getString(R.string.dialog_reset_cancel)) { dialog, _ ->
                dialog.cancel()
            }
        val alert = builder.create()
        alert.show()
        alert.getButton(DialogInterface.BUTTON_NEGATIVE).setBackgroundColor(getColor(R.color.bad_red))

    }

}