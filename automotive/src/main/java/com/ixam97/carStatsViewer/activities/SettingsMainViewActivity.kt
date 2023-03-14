package com.ixam97.carStatsViewer.activities

import android.app.Activity
import android.os.Bundle
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.dataManager.DataManagers
import kotlinx.android.synthetic.main.activity_settings_main_view.*

class SettingsMainViewActivity: Activity() {

    private val appPreferences = CarStatsViewer.appPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings_main_view)

        settings_consumption_plot_switch_secondary_color.isChecked = appPreferences.chargePlotSecondaryColor
        settings_consumption_plot_switch_visible_gages.isChecked = appPreferences.consumptionPlotVisibleGages
        settings_charge_plot_switch_secondary_color.isChecked = appPreferences.chargePlotSecondaryColor
        settings_charge_plot_switch_visible_gages.isChecked = appPreferences.chargePlotVisibleGages

        settings_main_view_back.setOnClickListener {
            finish()
        }

        settings_multiselect_trip.entries = arrayListOf<String>().apply {
            DataManagers.values().forEach {
                add(getString(resources.getIdentifier(it.dataManager.printableName, "string", packageName)))
            }
        }
        settings_multiselect_trip.selectedIndex = appPreferences.mainViewTrip
        settings_multiselect_trip.setOnIndexChangedListener {
            appPreferences.mainViewTrip = settings_multiselect_trip.selectedIndex
        }

        settings_consumption_plot_switch_secondary_color.setOnClickListener {
            appPreferences.consumptionPlotSecondaryColor = settings_consumption_plot_switch_secondary_color.isChecked
        }
        settings_consumption_plot_switch_visible_gages.setOnClickListener {
            appPreferences.consumptionPlotVisibleGages = settings_consumption_plot_switch_visible_gages.isChecked
        }
        settings_charge_plot_switch_secondary_color.setOnClickListener {
            appPreferences.chargePlotSecondaryColor = settings_charge_plot_switch_secondary_color.isChecked
        }
        settings_charge_plot_switch_visible_gages.setOnClickListener {
            appPreferences.chargePlotVisibleGages = settings_charge_plot_switch_visible_gages.isChecked
        }
    }
}