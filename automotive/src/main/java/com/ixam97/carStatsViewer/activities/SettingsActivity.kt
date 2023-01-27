package com.ixam97.carStatsViewer.activities

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import com.ixam97.carStatsViewer.*
import com.ixam97.carStatsViewer.objects.*
import android.app.Activity
import android.app.AlertDialog
import android.car.VehicleGear
import android.content.BroadcastReceiver
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_settings.*
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.View
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import com.ixam97.carStatsViewer.plot.enums.*
import com.ixam97.carStatsViewer.plot.objects.PlotLine
import com.ixam97.carStatsViewer.plot.objects.PlotLineConfiguration
import com.ixam97.carStatsViewer.plot.graphics.PlotPaint
import com.ixam97.carStatsViewer.plot.objects.PlotRange
import com.ixam97.carStatsViewer.views.PlotView
import kotlin.system.exitProcess

class SettingsActivity : Activity() {

    private lateinit var context : Context
    private lateinit var appPreferences: AppPreferences

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                getString(R.string.gear_update_broadcast) -> setEnableByGear(DataHolder.currentGear)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        InAppLogger.log("SettingsActivity.onCreate")

        context = applicationContext
        appPreferences = AppPreferences(context)

        setContentView(R.layout.activity_settings)

        setupSettingsMaster()
        setupSettingsConsumptionPlot()
        setupSettingsChargePlot()

        registerReceiver(broadcastReceiver, IntentFilter(getString(R.string.gear_update_broadcast)))

        setEnableByGear(DataHolder.currentGear)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadcastReceiver)
        InAppLogger.log("SettingsActivity.onDestroy")
    }

    private fun setEnableByGear(gear: Int) {
        if (gear != VehicleGear.GEAR_PARK) {
            if (settings_charge_plot_layout.visibility == View.VISIBLE) animateTransition(settings_charge_plot_layout, settings_master_layout)
            if (settings_consumption_plot_layout.visibility == View.VISIBLE) animateTransition(settings_consumption_plot_layout, settings_master_layout)
            setMenuRowIsEnabled(false, settings_charge_plot)
            setMenuRowIsEnabled(false, settings_consumption_plot)
        } else {
            setMenuRowIsEnabled(true, settings_charge_plot)
            setMenuRowIsEnabled(true, settings_consumption_plot)
        }
    }

    private fun setupSettingsMaster() {
        settings_switch_notifications.isChecked = appPreferences.notifications
        settings_switch_consumption_unit.isChecked = appPreferences.consumptionUnit

        settings_version_text.text = "Car Stats Viewer Version %s (%s)".format(BuildConfig.VERSION_NAME, BuildConfig.APPLICATION_ID)

        settings_button_back.setOnClickListener() {
            finish()
        }

        settings_button_kill.setOnClickListener {

            val builder = AlertDialog.Builder(this@SettingsActivity)
            builder.setTitle(getString(R.string.quit_dialog_title))
                .setMessage(getString(R.string.quit_dialog_message))
                .setCancelable(true)
                .setPositiveButton(getString(R.string.dialog_confirm)) { dialog, id ->
                    InAppLogger.log("App killed from Settings")
                    InAppLogger.copyToClipboard(this)
                    exitProcess(0)
                }
                .setNegativeButton(getString(R.string.dialog_dismiss)) { dialog, id ->
                    // Dismiss the dialog
                    dialog.dismiss()
                }
            val alert = builder.create()
            alert.show()
            alert.getButton(DialogInterface.BUTTON_POSITIVE).setBackgroundColor(getColor(R.color.bad_red))
        }

        settings_switch_notifications.setOnClickListener {
            appPreferences.notifications = settings_switch_notifications.isChecked
        }

        settings_switch_consumption_unit.setOnClickListener {
            appPreferences.consumptionUnit = settings_switch_consumption_unit.isChecked
        }
        
        settings_consumption_plot.setOnClickListener {
            gotoConsumptionPlot()
        }

        settings_charge_plot.setOnClickListener {
            gotoChargePlot()
        }

        settings_version_text.setOnClickListener {
            startActivity(Intent(this, LogActivity::class.java))
        }
    }

    private fun setupSettingsConsumptionPlot() {

        settings_consumption_plot_switch_secondary_color.isChecked = appPreferences.consumptionPlotSecondaryColor
        settings_consumption_plot_switch_visible_gages.isChecked = appPreferences.consumptionPlotVisibleGages
        settings_consumption_plot_switch_single_motor.isChecked = appPreferences.consumptionPlotSingleMotor
        settings_consumption_plot_speed_switch.isChecked = appPreferences.plotSpeed

        settings_consumption_plot_button_back.setOnClickListener {
            gotoMaster(settings_consumption_plot_layout)
        }

        settings_consumption_plot_switch_secondary_color.setOnClickListener {
            appPreferences.consumptionPlotSecondaryColor = settings_consumption_plot_switch_secondary_color.isChecked
            DataHolder.consumptionPlotLine.secondaryPlotPaint = when {
                appPreferences.consumptionPlotSecondaryColor -> PlotPaint.byColor(getColor(R.color.secondary_plot_color_alt), PlotView.textSize)
                else -> PlotPaint.byColor(getColor(R.color.secondary_plot_color), PlotView.textSize)
            }
        }

        settings_consumption_plot_speed_switch.setOnClickListener {
            appPreferences.plotSpeed = settings_consumption_plot_speed_switch.isChecked
        }

        settings_consumption_plot_switch_visible_gages.setOnClickListener {
            appPreferences.consumptionPlotVisibleGages = settings_consumption_plot_switch_visible_gages.isChecked
        }
        
        settings_consumption_plot_switch_single_motor.setOnClickListener {
            appPreferences.consumptionPlotSingleMotor = settings_consumption_plot_switch_single_motor.isChecked
        }
    }

    private fun setupSettingsChargePlot() {

        settings_charge_plot_switch_secondary_color.isChecked = appPreferences.chargePlotSecondaryColor
        settings_charge_plot_switch_state_of_charge_dimension.isChecked = appPreferences.chargePlotDimension == PlotDimension.STATE_OF_CHARGE

        settings_charge_plot_button_back.setOnClickListener {
            gotoMaster(settings_charge_plot_layout)
        }

        settings_charge_plot_switch_secondary_color.setOnClickListener {
            appPreferences.chargePlotSecondaryColor = settings_charge_plot_switch_secondary_color.isChecked
            val plotPaint = when {
                appPreferences.chargePlotSecondaryColor -> PlotPaint.byColor(getColor(R.color.secondary_plot_color_alt), PlotView.textSize)
                else -> PlotPaint.byColor(getColor(R.color.secondary_plot_color), PlotView.textSize)
            }
            DataHolder.chargePlotLine.secondaryPlotPaint = plotPaint
        }
/*
        settings_charge_plot_switch_state_of_charge_dimension.setOnClickListener {
            appPreferences.chargePlotDimension = when (settings_charge_plot_switch_state_of_charge_dimension.isChecked) {
                true -> PlotDimension.STATE_OF_CHARGE
                else -> PlotDimension.TIME
            }
            settings_charge_plot_view.dimension = appPreferences.chargePlotDimension
            settings_charge_plot_view.secondaryDimension = when (appPreferences.chargePlotDimension) {
                PlotDimension.TIME -> PlotSecondaryDimension.STATE_OF_CHARGE
                else -> null
            }
            settings_charge_plot_view.invalidate()
        }
 */
    }

    private fun gotoMaster(fromLayout: View){
        animateTransition(fromLayout, settings_master_layout)
    }

    private fun gotoConsumptionPlot() {
        animateTransition(settings_master_layout, settings_consumption_plot_layout)
    }

    private fun gotoChargePlot() {
        animateTransition(settings_master_layout, settings_charge_plot_layout)
    }

    private fun animateTransition(fromLayout: View, toView: View) {
        toView.apply{
            alpha = 0f
            visibility = View.VISIBLE
            animate()
                .alpha(1f)
                .setDuration(200)
                .setListener(null)
        }
        fromLayout.apply {
            animate()
                .alpha(0f)
                .setDuration(200)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        fromLayout.visibility = View.GONE
                        fromLayout.alpha = 1f
                    }
                })
        }

        InAppLogger.log(resources.getInteger(android.R.integer.config_shortAnimTime).toString())
    }

    private fun setMenuRowIsEnabled(enabled: Boolean, view: View) {
        view.isEnabled = enabled
        if (view is TextView) {
            if(!enabled){
                view.setTextAppearance(R.style.menu_button_row_style_disabled)
                for (drawable in view.compoundDrawablesRelative) {
                    if (drawable != null) {
                        drawable.colorFilter = PorterDuffColorFilter(getColor(R.color.disabled_tint), PorterDuff.Mode.SRC_IN)
                    }
                }
            } else {
                view.setTextAppearance(R.style.menu_button_row_style)
                for (drawable in view.compoundDrawablesRelative) {
                    if (drawable != null) {
                        drawable.colorFilter = PorterDuffColorFilter(getColor(android.R.color.white), PorterDuff.Mode.SRC_IN)
                    }
                }
            }
        }
    }
}