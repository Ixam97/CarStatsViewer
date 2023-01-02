package com.ixam97.carStatsViewer

import android.app.Activity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_settings.*
import android.content.Context
import android.util.Log

object AppPreferences {
    var debug = false
    var notifications = false
    var consumptionUnit = false
    var consumptionPlot = false
}

class SettingsActivity : Activity() {

    private lateinit var context : Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        context = applicationContext

        val sharedPref = context.getSharedPreferences(
            getString(R.string.preferences_file_key), Context.MODE_PRIVATE
        )
        settings_switch_notifications.isChecked = AppPreferences.notifications
        settings_switch_consumption_unit.isChecked = AppPreferences.consumptionUnit
        settings_switch_consumption_plot.isChecked = AppPreferences.consumptionPlot

        settings_version_text.text = String.format("Car Stats Viewer Version %s (Build %d)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)

        settings_button_back.setOnClickListener() {
            finish()
        }

        settings_switch_notifications.setOnClickListener {
            sharedPref.edit()
                .putBoolean(getString(R.string.preferences_notifications_key), settings_switch_notifications.isChecked)
                .apply()
            AppPreferences.notifications = settings_switch_notifications.isChecked
        }

        settings_switch_consumption_unit.setOnClickListener {
            sharedPref.edit()
                .putBoolean(getString(R.string.preferences_consumption_unit_key), settings_switch_consumption_unit.isChecked)
                .apply()
            AppPreferences.consumptionUnit = settings_switch_consumption_unit.isChecked
        }

        settings_switch_debug.setOnClickListener {
            sharedPref.edit()
                .putBoolean(getString(R.string.preferences_debug_key), settings_switch_debug.isChecked)
                .apply()
            AppPreferences.debug = settings_switch_debug.isChecked
        }

        settings_switch_consumption_plot.setOnClickListener {
            sharedPref.edit()
                .putBoolean(getString(R.string.preferences_consumption_plot_key), settings_switch_consumption_plot.isChecked)
                .apply()
            AppPreferences.consumptionPlot = settings_switch_consumption_plot.isChecked
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("Settings", "Destroyed")
    }
}