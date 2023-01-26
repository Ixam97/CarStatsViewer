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
import com.ixam97.carStatsViewer.plot.*
import com.ixam97.carStatsViewer.views.PlotView
import kotlin.system.exitProcess

class SettingsActivity : Activity() {

    private lateinit var context : Context
    private lateinit var appPreferences: AppPreferences

    private lateinit var disabledTint: PorterDuffColorFilter
    private lateinit var enabledTint: PorterDuffColorFilter

    var chargePlotLine = PlotLine(
        PlotLineConfiguration(
            PlotRange(0f, 20f, 0f, 160f, 20f),
            PlotLineLabelFormat.NUMBER,
            PlotLabelPosition.LEFT,
            PlotHighlightMethod.AVG_BY_TIME,
            "kW"
        ),
        hashMapOf(
            PlotSecondaryDimension.TIME to PlotLineConfiguration(
                PlotRange(backgroundZero = 0f),
                PlotLineLabelFormat.TIME,
                PlotLabelPosition.RIGHT,
                PlotHighlightMethod.MAX,
                "Time"
            ),
            PlotSecondaryDimension.STATE_OF_CHARGE to PlotLineConfiguration(
                PlotRange(0f, 100f, backgroundZero = 0f),
                PlotLineLabelFormat.PERCENTAGE,
                PlotLabelPosition.RIGHT,
                PlotHighlightMethod.MAX,
                "% SoC"
            )
        )
    )

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                getString(R.string.ui_update_plot_broadcast) -> {
                    settings_consumption_plot_view.invalidate()
                    settings_charge_plot_view.invalidate()
                }
                getString(R.string.gear_update_broadcast) -> setEnableByGear(DataHolder.currentGear)
            }
        }
    }

    private val seekBarChangeListener = object : OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            setVisibleChargeCurve(progress)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {
        }

        override fun onStopTrackingTouch(seekBar: SeekBar?) {
        }


    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        InAppLogger.log("SettingsActivity.onCreate")

        context = applicationContext
        appPreferences = AppPreferences(context)

        setContentView(R.layout.activity_settings)

        disabledTint = PorterDuffColorFilter(getColor(R.color.disabled_tint), PorterDuff.Mode.SRC_IN)
        enabledTint = PorterDuffColorFilter(getColor(android.R.color.white), PorterDuff.Mode.SRC_IN)

        setupSettingsMaster()
        setupSettingsConsumptionPlot()
        setupSettingsChargePlot()

        registerReceiver(broadcastReceiver, IntentFilter(getString(R.string.ui_update_plot_broadcast)))
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
        settings_switch_experimental_layout.isChecked = appPreferences.experimentalLayout

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

        settings_switch_experimental_layout.setOnClickListener {
            appPreferences.experimentalLayout = settings_switch_experimental_layout.isChecked
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

        settings_consumption_plot_view.addPlotLine(DataHolder.consumptionPlotLine)
        settings_consumption_plot_view.dimension

        settings_consumption_plot_view.dimension = PlotDimension.DISTANCE
        settings_consumption_plot_view.dimensionRestriction = ((DataHolder.traveledDistance / MainActivity.DISTANCE_TRIP_DIVIDER).toInt() + 1) * MainActivity.DISTANCE_TRIP_DIVIDER + 1
        settings_consumption_plot_view.dimensionSmoothingPercentage = 0.02f
        settings_consumption_plot_view.setPlotMarkers(DataHolder.plotMarkers)
        settings_consumption_plot_view.visibleMarkerTypes.add(PlotMarkerType.CHARGE)
        settings_consumption_plot_view.visibleMarkerTypes.add(PlotMarkerType.PARK)
        settings_consumption_plot_view.dimensionShiftTouchInterval = 1_000L
        settings_consumption_plot_view.dimensionRestrictionTouchInterval = 5_000L

        settings_consumption_plot_view.secondaryDimension = if (appPreferences.plotSpeed) PlotSecondaryDimension.SPEED else null

        settings_consumption_plot_view.invalidate()

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
                else ->PlotPaint.byColor(getColor(R.color.secondary_plot_color), PlotView.textSize)
            }
            settings_consumption_plot_view.invalidate()
        }

        settings_consumption_plot_speed_switch.setOnClickListener {
            appPreferences.plotSpeed = settings_consumption_plot_speed_switch.isChecked
            settings_consumption_plot_view.secondaryDimension = when (settings_consumption_plot_speed_switch.isChecked) {
                true -> PlotSecondaryDimension.SPEED
                else -> null
            }
            settings_consumption_plot_view.invalidate()
        }

        settings_consumption_plot_switch_visible_gages.setOnClickListener {
            appPreferences.consumptionPlotVisibleGages = settings_consumption_plot_switch_visible_gages.isChecked
        }
        
        settings_consumption_plot_switch_single_motor.setOnClickListener {
            appPreferences.consumptionPlotSingleMotor = settings_consumption_plot_switch_single_motor.isChecked
        }
    }

    private fun setupSettingsChargePlot() {
        settings_charge_plot_sub_title_curve.text = "%s (%d/%d)".format(
            getString(R.string.settings_sub_title_last_charge_plot),
            DataHolder.chargeCurves.size,
            DataHolder.chargeCurves.size)

        chargePlotLine.plotPaint = PlotPaint.byColor(getColor(R.color.charge_plot_color), PlotView.textSize)
        chargePlotLine.secondaryPlotPaint = when {
            appPreferences.chargePlotSecondaryColor -> PlotPaint.byColor(getColor(R.color.secondary_plot_color_alt), PlotView.textSize)
            else -> PlotPaint.byColor(getColor(R.color.secondary_plot_color), PlotView.textSize)
        }

        chargePlotLine.reset()
        if (DataHolder.chargeCurves.isNotEmpty()) {
            chargePlotLine.addDataPoints(DataHolder.chargeCurves[DataHolder.chargeCurves.size - 1].chargePlotLine)
            settings_charge_plot_button_next.isEnabled = false
            settings_charge_plot_button_next.colorFilter = disabledTint
            settings_charge_plot_button_prev.isEnabled = true
            settings_charge_plot_button_prev.colorFilter = enabledTint
        }
        if (DataHolder.chargeCurves.size < 2){
            settings_charge_plot_button_next.isEnabled = false
            settings_charge_plot_button_next.colorFilter = disabledTint
            settings_charge_plot_button_prev.isEnabled = false
            settings_charge_plot_button_prev.colorFilter = disabledTint
        }
        settings_charge_plot_view.addPlotLine(chargePlotLine)

        settings_charge_plot_view.dimension = appPreferences.chargePlotDimension
        settings_charge_plot_view.dimensionRestriction = null
        settings_charge_plot_view.dimensionSmoothingPercentage = 0.01f
        settings_charge_plot_view.secondaryDimension = when (appPreferences.chargePlotDimension) {
            PlotDimension.TIME -> PlotSecondaryDimension.STATE_OF_CHARGE
            else -> null
        }

        settings_charge_plot_view.invalidate()

        settings_charge_plot_switch_secondary_color.isChecked = appPreferences.chargePlotSecondaryColor
        settings_charge_plot_switch_state_of_charge_dimension.isChecked = appPreferences.chargePlotDimension == PlotDimension.STATE_OF_CHARGE

        settings_charge_plot_button_back.setOnClickListener {
            gotoMaster(settings_charge_plot_layout)
        }

        settings_charge_plot_seek_bar.max = (DataHolder.chargeCurves.size - 1).coerceAtLeast(0)
        settings_charge_plot_seek_bar.progress = (DataHolder.chargeCurves.size - 1).coerceAtLeast(0)

        settings_charge_plot_seek_bar.setOnSeekBarChangeListener(seekBarChangeListener)

        settings_charge_plot_button_next.setOnClickListener {
            val newProgress = settings_charge_plot_seek_bar.progress + 1
            if (newProgress <= (DataHolder.chargeCurves.size - 1)) {
                settings_charge_plot_seek_bar.progress = newProgress
            }
        }

        settings_charge_plot_button_prev.setOnClickListener {
            val newProgress = settings_charge_plot_seek_bar.progress - 1
            if (newProgress >= 0) {
                settings_charge_plot_seek_bar.progress = newProgress
            }
        }

        settings_charge_plot_switch_secondary_color.setOnClickListener {
            appPreferences.chargePlotSecondaryColor = settings_charge_plot_switch_secondary_color.isChecked
            val plotPaint = when {
                appPreferences.chargePlotSecondaryColor -> PlotPaint.byColor(getColor(R.color.secondary_plot_color_alt), PlotView.textSize)
                else -> PlotPaint.byColor(getColor(R.color.secondary_plot_color), PlotView.textSize)
            }
            chargePlotLine.secondaryPlotPaint = plotPaint
            DataHolder.chargePlotLine.secondaryPlotPaint = plotPaint
            settings_charge_plot_view.invalidate()
        }

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

    private fun setVisibleChargeCurve(progress: Int) {

        settings_charge_plot_sub_title_curve.text = "%s (%d/%d)".format(
            getString(R.string.settings_sub_title_last_charge_plot),
            DataHolder.chargeCurves.size,
            DataHolder.chargeCurves.size)

        if (DataHolder.chargeCurves.size - 1 == 0) {
            settings_charge_plot_sub_title_curve.text = "%s (0/0)".format(
                getString(R.string.settings_sub_title_last_charge_plot))

            settings_charge_plot_button_next.isEnabled = false
            settings_charge_plot_button_next.colorFilter = disabledTint
            settings_charge_plot_button_prev.isEnabled = false
            settings_charge_plot_button_prev.colorFilter = disabledTint

        } else {
            settings_charge_plot_sub_title_curve.text = "%s (%d/%d)".format(
                getString(R.string.settings_sub_title_last_charge_plot),
                progress + 1,
                DataHolder.chargeCurves.size)

            when (progress) {
                0 -> {
                    settings_charge_plot_button_prev.isEnabled = false
                    settings_charge_plot_button_prev.colorFilter = disabledTint
                    settings_charge_plot_button_next.isEnabled = true
                    settings_charge_plot_button_next.colorFilter = enabledTint
                }
                DataHolder.chargeCurves.size - 1 -> {
                    settings_charge_plot_button_next.isEnabled = false
                    settings_charge_plot_button_next.colorFilter = disabledTint
                    settings_charge_plot_button_prev.isEnabled = true
                    settings_charge_plot_button_prev.colorFilter = enabledTint
                }
                else -> {
                    settings_charge_plot_button_next.isEnabled = true
                    settings_charge_plot_button_next.colorFilter = enabledTint
                    settings_charge_plot_button_prev.isEnabled = true
                    settings_charge_plot_button_prev.colorFilter = enabledTint
                }
            }
        }

        chargePlotLine.reset()
        chargePlotLine.addDataPoints(DataHolder.chargeCurves[progress].chargePlotLine)
        settings_charge_plot_view.invalidate()
    }
}