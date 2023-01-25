package com.ixam97.carStatsViewer.activities

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import androidx.core.graphics.drawable.DrawableCompat
import com.ixam97.carStatsViewer.InAppLogger
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.objects.DataHolder
import com.ixam97.carStatsViewer.plot.PlotDimension
import kotlinx.android.synthetic.main.activity_main.*
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
    }

    private fun setupConsumptionLayout() {
        summary_consumption_plot.dimension = PlotDimension.DISTANCE
        summary_consumption_plot.dimensionRestriction = 10_001L
        summary_consumption_plot.dimensionSmoothingPercentage = 0.02f
    }

    private fun setupChargeLayout() {

    }

    private fun switchToConsumptionLayout() {

    }

    private fun switchToChargeLayout() {

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
            .setNeutralButton(R.string.dialog_reset_no_save) { _, _ ->
                DataHolder.resetDataHolder()
                sendBroadcast(Intent(getString(R.string.save_trip_data_broadcast)))
            }
            .setNegativeButton(getString(R.string.dialog_reset_cancel)) { dialog, _ ->
                dialog.cancel()
            }
        val alert = builder.create()
        alert.show()
    }

}