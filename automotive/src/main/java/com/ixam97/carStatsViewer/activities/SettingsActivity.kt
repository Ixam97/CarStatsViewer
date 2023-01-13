package com.ixam97.carStatsViewer.activities

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import com.ixam97.carStatsViewer.*
import com.ixam97.carStatsViewer.objects.*
import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_settings.*
import android.content.Context
import android.content.Intent
import android.view.View
import kotlin.system.exitProcess

class SettingsActivity : Activity() {

    private lateinit var context : Context
    private lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        InAppLogger.log("SettingsActivity.onCreate")

        context = applicationContext
        appPreferences = AppPreferences(context)

        setContentView(R.layout.activity_settings)

        setupSettingsMaster()
        setupSettingsConsumptionPlot()

    }

    override fun onDestroy() {
        super.onDestroy()
        InAppLogger.log("SettingsActivity.onDestroy")
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
                    exitProcess(-1)
                }
                .setNegativeButton(getString(R.string.dialog_dismiss)) { dialog, id ->
                    // Dismiss the dialog
                    dialog.dismiss()
                }
            val alert = builder.create()
            alert.show()
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

        settings_version_text.setOnClickListener {
            startActivity(Intent(this, LogActivity::class.java))
        }
    }

    private fun setupSettingsConsumptionPlot() {

        settings_consumption_plot_switch_secondary_color.isChecked = appPreferences.consumptionPlotSecondaryColor
        settings_consumption_plot_switch_visible_gages.isChecked = appPreferences.consumptionPlotVisibleGages
        settings_consumption_plot_switch_single_motor.isChecked = appPreferences.consumptionPlotSingleMotor

        settings_consumption_plot_button_back.setOnClickListener {
            gotoMaster()
        }

        settings_consumption_plot_switch_secondary_color.setOnClickListener {
            appPreferences.consumptionPlotSecondaryColor = settings_consumption_plot_switch_secondary_color.isChecked
        }

        settings_consumption_plot_switch_visible_gages.setOnClickListener {
            appPreferences.consumptionPlotVisibleGages = settings_consumption_plot_switch_visible_gages.isChecked
        }
        
        settings_consumption_plot_switch_single_motor.setOnClickListener {
            appPreferences.consumptionPlotSingleMotor = settings_consumption_plot_switch_single_motor.isChecked
        }
    }

    private fun setupSettingsChargePlot() {

    }

    private fun gotoMaster(){
        animateTransition(settings_consumption_plot_layout, settings_master_layout)
        // settings_consumption_plot_layout.visibility = View.GONE
        // settings_master_layout.visibility = View.VISIBLE
    }

    private fun gotoConsumptionPlot() {
        animateTransition(settings_master_layout, settings_consumption_plot_layout)
        // settings_master_layout.visibility = View.GONE
        // settings_consumption_plot_layout.visibility = View.VISIBLE
    }

    private fun animateTransition(fromView: View, toView: View) {
        toView.apply{
            alpha = 0f
            visibility = View.VISIBLE
            animate()
                .alpha(1f)
                .setDuration(200)
                .setListener(null)
        }
        fromView.apply {
            animate()
                .alpha(0f)
                .setDuration(200)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        fromView.visibility = View.GONE
                        fromView.alpha = 1f
                    }
                })
        }

        InAppLogger.log(resources.getInteger(android.R.integer.config_shortAnimTime).toString())
    }

}