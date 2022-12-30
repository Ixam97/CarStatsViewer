package com.ixam97.carStatsViewer

import android.app.Activity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_settings.*
import android.content.Context
import android.util.Log

object AppPreferences {
    var notifications = false
    var consumptionUnit = false
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

        val preferencesNotifications = sharedPref.getBoolean(getString(R.string.preferences_notifications_key), false)
        val preferencesConsumptionUnit = sharedPref.getBoolean(getString(R.string.preferences_consumption_unit_key), false)

        settings_version_text.text = String.format("Car Stats Viewer Version %s", BuildConfig.VERSION_NAME)

        settings_switch_notifications.isChecked = preferencesNotifications
        AppPreferences.notifications = preferencesNotifications
        settings_switch_consumption_unit.isChecked = preferencesConsumptionUnit
        AppPreferences.consumptionUnit = preferencesConsumptionUnit

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
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("Settings", "Destroyed")
    }
}