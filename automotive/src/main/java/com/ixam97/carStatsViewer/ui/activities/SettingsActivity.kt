package com.ixam97.carStatsViewer.ui.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.firebase.Firebase
import com.google.firebase.app
import com.ixam97.carStatsViewer.BuildConfig
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.databinding.ActivitySettingsBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SettingsActivity : FragmentActivity() {

    private lateinit var context : Context
    private lateinit var binding: ActivitySettingsBinding
    private val appPreferences = CarStatsViewer.appPreferences

    private var versionClickCounter = 0

    private var moving = false

    override fun startActivity(intent: Intent?) {
        super.startActivity(intent)
        // if (intent?.hasExtra("noTransition") == false)
        if (BuildConfig.FLAVOR_aaos != "carapp")
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    override fun onResume() {
        super.onResume()
        binding.settingsSwitchConsumptionUnit.text = getString(R.string.settings_consumption_unit, appPreferences.distanceUnit.unit())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        context = applicationContext
        if (CarStatsViewer.appPreferences.colorTheme > 0) setTheme(R.style.ColorTestTheme)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        val view = binding.root

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                CarStatsViewer.dataProcessor.realTimeDataFlow.collectLatest {
                    setDistractionOptimization((it.speed?:0f) > 0)
                }
            }
        }
        setContentView(view)

        setupSettingsMaster()
    }

    private fun setDistractionOptimization(doOptimize: Boolean) {
        if (moving == doOptimize) return
        with(binding) {
            settingsMainViewWidget.isEnabled = !doOptimize
            // settingsVehicleWidget.isEnabled = !doOptimize
            settingsApisWidget.isEnabled = !doOptimize
            settingsAboutWidget.isEnabled = !doOptimize
        }
        moving = doOptimize
    }

    private fun setupSettingsMaster() {
        with(binding){
            settingsSwitchNotifications.isChecked = appPreferences.notifications
            if (getString(R.string.useFirebase) == "true") {
                settingsSwitchAnalytics.isChecked = Firebase.app.isDataCollectionDefaultEnabled
            } else {
                settingsSwitchAnalytics.visibility = View.GONE
                settingsSwitchAnalyticsDivider.visibility = View.GONE
            }
            settingsSwitchConsumptionUnit.isChecked = appPreferences.consumptionUnit
            settingsSwitchUseLocation.isChecked = appPreferences.useLocation
            settingsSwitchAutostart.isChecked = appPreferences.autostart
            settingsSwitchPhoneReminder.isChecked = appPreferences.phoneNotification
            settingsSwitchAltLayout.isChecked = appPreferences.altLayout
            settingsSwitchTheme.isChecked = when (appPreferences.colorTheme) {
                1 -> true
                else -> false
            }

            settingsVersionText.text = "Car Stats Viewer %s (%s)".format(
                BuildConfig.VERSION_NAME,
                BuildConfig.APPLICATION_ID
            )

            settingsButtonBack.setOnClickListener() {
                finish()
                if (BuildConfig.FLAVOR_aaos != "carapp")
                    overridePendingTransition(R.anim.stay_still, R.anim.slide_out_right)
            }

            settingsSwitchNotifications.setSwitchClickListener {
                appPreferences.notifications = settingsSwitchNotifications.isChecked
            }

            settingsSwitchAnalytics.setSwitchClickListener {
                if (getString(R.string.useFirebase) == "true") {
                    Firebase.app.setDataCollectionDefaultEnabled(settingsSwitchAnalytics.isChecked)
                }
            }

            settingsSwitchConsumptionUnit.setSwitchClickListener {
                appPreferences.consumptionUnit = settingsSwitchConsumptionUnit.isChecked
            }

            settingsSwitchUseLocation.setSwitchClickListener {
                appPreferences.useLocation = settingsSwitchUseLocation.isChecked
                CarStatsViewer.watchdog.triggerWatchdog()
            }

            settingsSwitchAutostart.setSwitchClickListener {
                appPreferences.autostart = settingsSwitchAutostart.isChecked
                CarStatsViewer.setupRestartAlarm(
                    CarStatsViewer.appContext,
                    "termination",
                    10_000,
                    !appPreferences.autostart,
                    extendedLogging = true
                )
            }

            settingsSwitchPhoneReminder.setSwitchClickListener {
                appPreferences.phoneNotification = settingsSwitchPhoneReminder.isChecked
            }

            // if (emulatorMode) settings_switch_distance_unit.visibility = View.VISIBLE
            // settings_switch_distance_unit.setOnClickListener {
            //     appPreferences.distanceUnit = when (settings_switch_distance_unit.isChecked) {
            //         true -> DistanceUnitEnum.MILES
            //         else -> DistanceUnitEnum.KM
            //     }
            //     PlotGlobalConfiguration.updateDistanceUnit(appPreferences.distanceUnit)
            // }

            settingsSwitchAltLayout.setSwitchClickListener {
                appPreferences.altLayout = settingsSwitchAltLayout.isChecked
            }

            settingsMainViewWidget.setOnRowClickListener {
                startActivity(Intent(this@SettingsActivity, SettingsMainViewActivity::class.java))
            }

            // settingsVehicleWidget.setOnRowClickListener {
            //     startActivity(Intent(this@SettingsActivity, SettingsVehicleActivity::class.java))
            // }

            settingsApisWidget.setOnRowClickListener {
                startActivity(Intent(this@SettingsActivity, SettingsApisActivity::class.java))
            }

            settingsAboutWidget.setOnRowClickListener {
                startActivity(Intent(this@SettingsActivity, AboutActivity::class.java))
            }

            settingsVersionText.setOnClickListener {
                versionClickCounter++
                if (versionClickCounter >= 10 || BuildConfig.FLAVOR_version == "dev") {
                    versionClickCounter = 0
                    startActivity(Intent(this@SettingsActivity, DebugActivity::class.java))
                    if (BuildConfig.FLAVOR_aaos != "carapp")
                        overridePendingTransition(R.anim.slide_in_up, R.anim.stay_still)
                }
            }

            settingsSwitchThemeContainer.setOnClickListener {
                settingsSwitchTheme.isChecked = !settingsSwitchTheme.isChecked
                appPreferences.colorTheme = if (settingsSwitchTheme.isChecked) 1 else 0
                finish()
            }

            settingsSwitchTheme.setOnClickListener {
                appPreferences.colorTheme = if (settingsSwitchTheme.isChecked) 1 else 0
                finish()
            }
        }
    }
}
