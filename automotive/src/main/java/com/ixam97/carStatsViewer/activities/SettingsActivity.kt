package com.ixam97.carStatsViewer.activities

import com.ixam97.carStatsViewer.*
import com.ixam97.carStatsViewer.objects.*
import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_settings.*
import android.content.Context
import android.content.Intent
import kotlin.system.exitProcess

class SettingsActivity : Activity() {

    private lateinit var context : Context
    private lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        InAppLogger.log("SettingsActivity.onCreate")

        setContentView(R.layout.activity_settings)

        context = applicationContext

        // val sharedPref = context.getSharedPreferences(
        //     getString(R.string.preferences_file_key), Context.MODE_PRIVATE
        // )

        appPreferences = AppPreferences(context)

        settings_switch_notifications.isChecked = appPreferences.notifications
        settings_switch_consumption_unit.isChecked = appPreferences.consumptionUnit
        settings_switch_experimental_layout.isChecked = appPreferences.experimentalLayout
        settings_switch_single_motor.isChecked = appPreferences.singleMotor
        // settings_switch_consumption_plot.isChecked = AppPreferences.consumptionPlot

        settings_version_text.text = String.format("Car Stats Viewer Version %s (Build %d)",
            BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE
        )

        settings_button_back.setOnClickListener() {
            intent = Intent()
            setResult(RESULT_OK, intent);
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
            // sharedPref.edit()
            //     .putBoolean(getString(R.string.preferences_notifications_key), settings_switch_notifications.isChecked)
            //     .apply()
            appPreferences.notifications = settings_switch_notifications.isChecked
        }

        settings_switch_consumption_unit.setOnClickListener {
            // sharedPref.edit()
            //     .putBoolean(getString(R.string.preferences_consumption_unit_key), settings_switch_consumption_unit.isChecked)
            //     .apply()
            appPreferences.consumptionUnit = settings_switch_consumption_unit.isChecked
        }

        settings_switch_debug.setOnClickListener {
            // sharedPref.edit()
            //     .putBoolean(getString(R.string.preferences_debug_key), settings_switch_debug.isChecked)
            //     .apply()
            appPreferences.debug = settings_switch_debug.isChecked
        }

        settings_switch_experimental_layout.setOnClickListener {
            // sharedPref.edit()
            //     .putBoolean(getString(R.string.preferences_experimental_layout_key), settings_switch_experimental_layout.isChecked)
            //     .apply()
            appPreferences.experimentalLayout = settings_switch_experimental_layout.isChecked
        }

        settings_switch_single_motor.setOnClickListener {
            appPreferences.singleMotor = settings_switch_single_motor.isChecked
        }
/*
        settings_switch_consumption_plot.setOnClickListener {
            sharedPref.edit()
                .putBoolean(getString(R.string.preferences_consumption_plot_key), settings_switch_consumption_plot.isChecked)
                .apply()
            AppPreferences.consumptionPlot = settings_switch_consumption_plot.isChecked
        }
*/
        settings_version_text.setOnClickListener {
            startActivity(Intent(this, LogActivity::class.java))
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        InAppLogger.log("SettingsActivity.onDestroy")
    }

}