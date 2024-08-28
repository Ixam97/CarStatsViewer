package com.ixam97.carStatsViewer.ui.activities
/*
import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentActivity
import com.ixam97.carStatsViewer.BuildConfig
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.utils.VehicleDefinitions
import kotlinx.android.synthetic.main.activity_settings_vehicle.settings_vehicle_button_back
import kotlinx.android.synthetic.main.activity_settings_vehicle.settings_vehicle_code
import kotlinx.android.synthetic.main.activity_settings_vehicle.settings_vehicle_multiselect_drivetrain
import kotlinx.android.synthetic.main.activity_settings_vehicle.settings_vehicle_multiselect_model_year
import kotlinx.android.synthetic.main.activity_settings_vehicle.settings_vehicle_scrollview
import kotlinx.android.synthetic.main.activity_settings_vehicle.settings_vehicle_string
import kotlinx.android.synthetic.main.activity_settings_vehicle.settings_vehicle_switch_bst
import kotlinx.android.synthetic.main.activity_settings_vehicle.settings_vehicle_switch_performance
import kotlinx.android.synthetic.main.activity_settings_vehicle.settings_vehicle_switch_plus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SettingsVehicleActivity : FragmentActivity() {

    val appPreferences = CarStatsViewer.appPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentViewAndTheme(this, R.layout.activity_settings_vehicle)

        settings_vehicle_button_back.setOnClickListener {
            finish()
            if (BuildConfig.FLAVOR_aaos != "carapp") {
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            }
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
            3 -> {
                settings_vehicle_switch_bst.isChecked = false
                settings_vehicle_switch_bst.isEnabled = false
                settings_vehicle_switch_plus.isEnabled = true
                settings_vehicle_multiselect_drivetrain.isEnabled = true
                if (settings_vehicle_multiselect_drivetrain.selectedIndex != 2) {
                    settings_vehicle_switch_performance.isEnabled = false
                    settings_vehicle_switch_performance.isChecked = false
                } else {
                    settings_vehicle_switch_performance.isEnabled = true
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
*/