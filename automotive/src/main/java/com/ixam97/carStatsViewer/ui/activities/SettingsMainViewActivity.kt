package com.ixam97.carStatsViewer.ui.activities

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.ixam97.carStatsViewer.BuildConfig
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.databinding.ActivitySettingsMainViewBinding

class SettingsMainViewActivity: FragmentActivity() {

    private lateinit var binding: ActivitySettingsMainViewBinding
    private val appPreferences = CarStatsViewer.appPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (CarStatsViewer.appPreferences.colorTheme > 0) setTheme(R.style.ColorTestTheme)
        binding = ActivitySettingsMainViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        with(binding){
            settingsConsumptionPlotSwitchSecondaryColor.isChecked =
                appPreferences.consumptionPlotSecondaryColor
            settingsConsumptionPlotSwitchVisibleGages.isChecked =
                appPreferences.consumptionPlotVisibleGages
            settingsChargePlotSwitchSecondaryColor.isChecked =
                appPreferences.chargePlotSecondaryColor
            settingsChargePlotSwitchVisibleGages.isChecked = appPreferences.chargePlotVisibleGages

            settingsMainViewBack.setOnClickListener {
                finish()
                if (BuildConfig.FLAVOR_aaos != "carapp")
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            }

            settingsMultiselectTrip.entries = ArrayList(
                resources.getStringArray(R.array.trip_type_names).toMutableList()
                    .apply { removeAt(0) })
            settingsMultiselectTrip.selectedIndex = appPreferences.mainViewTrip
            settingsMultiselectTrip.setOnIndexChangedListener {
                appPreferences.mainViewTrip = settingsMultiselectTrip.selectedIndex
                CarStatsViewer.dataProcessor.changeSelectedTrip(settingsMultiselectTrip.selectedIndex + 1)
            }
            // settingsMultiButtonTrip.selectedIndex = appPreferences.mainViewTrip
            // settingsMultiButtonTrip.setOnIndexChangedListener {
            //     appPreferences.mainViewTrip = settingsMultiButtonTrip.selectedIndex
            //     CarStatsViewer.dataProcessor.changeSelectedTrip(settingsMultiButtonTrip.selectedIndex + 1)
            // }

            settingsMultiselectConnectionSelector.entries =
                ArrayList(CarStatsViewer.liveDataApis.map { getString(it.apiNameStringId) })
            settingsMultiselectConnectionSelector.selectedIndex =
                appPreferences.mainViewConnectionApi
            settingsMultiselectConnectionSelector.setOnIndexChangedListener {
                appPreferences.mainViewConnectionApi =
                    settingsMultiselectConnectionSelector.selectedIndex
            }

            settingsConsumptionPlotSwitchSecondaryColor.setSwitchClickListener {
                appPreferences.consumptionPlotSecondaryColor =
                    settingsConsumptionPlotSwitchSecondaryColor.isChecked
            }
            settingsConsumptionPlotSwitchVisibleGages.setSwitchClickListener {
                appPreferences.consumptionPlotVisibleGages =
                    settingsConsumptionPlotSwitchVisibleGages.isChecked
            }
            settingsChargePlotSwitchSecondaryColor.setSwitchClickListener {
                appPreferences.chargePlotSecondaryColor =
                    settingsChargePlotSwitchSecondaryColor.isChecked
            }
            settingsChargePlotSwitchVisibleGages.setSwitchClickListener {
                appPreferences.chargePlotVisibleGages =
                    settingsChargePlotSwitchVisibleGages.isChecked
            }
        }
    }
}