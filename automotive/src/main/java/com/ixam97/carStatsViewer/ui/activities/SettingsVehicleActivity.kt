package com.ixam97.carStatsViewer.ui.activities

import android.app.Activity
import android.os.Bundle
import android.view.View
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.utils.VehicleDefinitions
import com.ixam97.carStatsViewer.utils.applyTypeface
import kotlinx.android.synthetic.main.activity_settings_vehicle.*
import kotlinx.coroutines.*

class SettingsVehicleActivity : Activity() {

    val appPreferences = CarStatsViewer.appPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_settings_vehicle)

        CarStatsViewer.typefaceMedium?.let {
            applyTypeface(settings_vehicle_activity)
        }

        settings_vehicle_button_back.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        settings_vehicle_multiselect_drivetrain.entries = VehicleDefinitions.Polestar2.driveTrains
        settings_vehicle_multiselect_model_year.entries = VehicleDefinitions.Polestar2.modelYears

        settings_vehicle_multiselect_model_year.setOnIndexChangedListener { updateVehicleString() }
        settings_vehicle_multiselect_drivetrain.setOnIndexChangedListener { updateVehicleString() }
        settings_vehicle_switch_plus.setOnClickListener { updateVehicleString() }
        settings_vehicle_switch_performance.setOnClickListener { updateVehicleString() }
        settings_vehicle_switch_bst.setOnClickListener { updateVehicleString() }

        settings_vehicle_multiselect_model_year.selectedIndex = appPreferences.modelYear
        settings_vehicle_multiselect_drivetrain.selectedIndex = appPreferences.driveTrain
        settings_vehicle_switch_plus.isChecked = appPreferences.plusPack
        settings_vehicle_switch_performance.isChecked = appPreferences.performanceUpgrade
        settings_vehicle_switch_bst.isChecked = appPreferences.bstEdition

        CoroutineScope(Dispatchers.Default).launch {
            delay(200)
            runOnUiThread {
                updateVehicleString()
                settings_vehicle_scrollview.visibility = View.VISIBLE
            }
        }
    }

    private fun updateVehicleString() {

        when (settings_vehicle_multiselect_model_year.selectedIndex) {
            0 -> {
                settings_vehicle_switch_plus.isEnabled = false
                settings_vehicle_switch_plus.isChecked = false
                settings_vehicle_switch_performance.isEnabled = true
                settings_vehicle_switch_bst.isEnabled = false
                settings_vehicle_switch_bst.isChecked = false
                settings_vehicle_multiselect_drivetrain.selectedIndex = 2
                settings_vehicle_multiselect_drivetrain.isEnabled = false
            }
            1 -> {
                settings_vehicle_switch_plus.isEnabled = true
                settings_vehicle_switch_bst.isEnabled = false
                settings_vehicle_switch_bst.isChecked = false
                settings_vehicle_multiselect_drivetrain.isEnabled = true
                if (settings_vehicle_multiselect_drivetrain.selectedIndex != 2) {
                    settings_vehicle_switch_performance.isEnabled = false
                    settings_vehicle_switch_performance.isChecked = false
                } else {
                    settings_vehicle_switch_performance.isEnabled = true
                }
            }
            2 -> {
                if (settings_vehicle_switch_bst.isChecked) {
                    settings_vehicle_switch_plus.isEnabled = false
                    settings_vehicle_switch_plus.isChecked = true
                    settings_vehicle_switch_performance.isEnabled = false
                    settings_vehicle_switch_performance.isChecked = true
                    settings_vehicle_multiselect_drivetrain.selectedIndex = 2
                    settings_vehicle_multiselect_drivetrain.isEnabled = false
                } else {
                    settings_vehicle_switch_plus.isEnabled = true
                    settings_vehicle_switch_bst.isEnabled = true
                    settings_vehicle_multiselect_drivetrain.isEnabled = true
                    if (settings_vehicle_multiselect_drivetrain.selectedIndex != 2) {
                        settings_vehicle_switch_performance.isEnabled = false
                        settings_vehicle_switch_performance.isChecked = false
                    } else {
                        settings_vehicle_switch_performance.isEnabled = true
                    }
                }
            }
        }

        settings_vehicle_string.text = VehicleDefinitions.Polestar2.getVehicleString(
            settings_vehicle_multiselect_model_year.selectedIndex,
            settings_vehicle_multiselect_drivetrain.selectedIndex,
            settings_vehicle_switch_plus.isChecked,
            settings_vehicle_switch_performance.isChecked,
            settings_vehicle_switch_bst.isChecked
        )
        settings_vehicle_code.text = VehicleDefinitions.Polestar2.getVehicleCode(
            settings_vehicle_multiselect_model_year.selectedIndex,
            settings_vehicle_multiselect_drivetrain.selectedIndex,
            settings_vehicle_switch_plus.isChecked,
            settings_vehicle_switch_performance.isChecked,
            settings_vehicle_switch_bst.isChecked
        )

        appPreferences.modelYear = settings_vehicle_multiselect_model_year.selectedIndex
        appPreferences.driveTrain = settings_vehicle_multiselect_drivetrain.selectedIndex
        appPreferences.plusPack = settings_vehicle_switch_plus.isChecked
        appPreferences.performanceUpgrade = settings_vehicle_switch_performance.isChecked
        appPreferences.bstEdition = settings_vehicle_switch_bst.isChecked

    }
}