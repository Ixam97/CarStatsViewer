package com.ixam97.carStatsViewer.activities

import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.ixam97.carStatsViewer.*
import com.ixam97.carStatsViewer.enums.DistanceUnitEnum
import com.ixam97.carStatsViewer.plot.objects.PlotGlobalConfiguration
import com.ixam97.carStatsViewer.utils.InAppLogger
import kotlinx.android.synthetic.main.activity_settings.*
import kotlin.system.exitProcess

class SettingsActivity : Activity() {

    private lateinit var context : Context
    private val appPreferences = CarStatsViewer.appPreferences

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                getString(R.string.distraction_optimization_broadcast) -> setDistractionOptimization(appPreferences.doDistractionOptimization)
            }
        }
    }

    override fun startActivity(intent: Intent?) {
        super.startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        context = applicationContext

        setContentView(R.layout.activity_settings)

        setupSettingsMaster()

        registerReceiver(broadcastReceiver, IntentFilter(getString(R.string.distraction_optimization_broadcast)))

        setDistractionOptimization(appPreferences.doDistractionOptimization)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadcastReceiver)
    }

    private fun setDistractionOptimization(doOptimize: Boolean) {
        if (doOptimize) {
            setMenuRowIsEnabled(false, settings_button_main_view)
            setMenuRowIsEnabled(false, settings_button_vehicle)
            setMenuRowIsEnabled(false, settings_button_apis)
            setMenuRowIsEnabled(false, settings_button_about)
        } else {
            setMenuRowIsEnabled(true, settings_button_main_view)
            setMenuRowIsEnabled(true, settings_button_vehicle)
            setMenuRowIsEnabled(true, settings_button_apis)
            setMenuRowIsEnabled(true, settings_button_about)
        }
    }

    private fun setupSettingsMaster() {
        settings_switch_notifications.isChecked = appPreferences.notifications
        settings_switch_consumption_unit.isChecked = appPreferences.consumptionUnit
        settings_switch_use_location.isChecked = appPreferences.useLocation
        settings_switch_autostart.isChecked = appPreferences.autostart
        settings_switch_distance_unit.isChecked = appPreferences.distanceUnit == DistanceUnitEnum.MILES

        settings_version_text.text = "Car Stats Viewer Version %s (%s)".format(BuildConfig.VERSION_NAME, BuildConfig.APPLICATION_ID)

        settings_button_back.setOnClickListener() {
            finish()
            overridePendingTransition(R.anim.stay_still, R.anim.slide_out_right)
        }

        settings_button_kill.setOnClickListener {

            val builder = AlertDialog.Builder(this@SettingsActivity)
            builder.setTitle(getString(R.string.quit_dialog_title))
                .setMessage(getString(R.string.quit_dialog_message))
                .setCancelable(true)
                .setPositiveButton(getString(R.string.dialog_confirm)) { dialog, id ->
                    InAppLogger.w("App killed from Settings")
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

        settings_switch_use_location.setOnClickListener {
            appPreferences.useLocation = settings_switch_use_location.isChecked
        }

        settings_switch_autostart.setOnClickListener {
            appPreferences.autostart = settings_switch_autostart.isChecked
        }

        if (emulatorMode) settings_switch_distance_unit.visibility = View.VISIBLE
        settings_switch_distance_unit.setOnClickListener {
            appPreferences.distanceUnit = when (settings_switch_distance_unit.isChecked) {
                true -> DistanceUnitEnum.MILES
                else -> DistanceUnitEnum.KM
            }
            PlotGlobalConfiguration.updateDistanceUnit(appPreferences.distanceUnit)
        }

        settings_button_main_view.setOnClickListener {
            startActivity(Intent(this, SettingsMainViewActivity::class.java))
        }

        settings_button_vehicle.setOnClickListener {
            startActivity(Intent(this, SettingsVehicleActivity::class.java))
        }

        settings_version_text.setOnClickListener {
            startActivity(Intent(this, LogActivity::class.java))
            overridePendingTransition(R.anim.slide_in_up, R.anim.stay_still)
        }

        settings_button_about.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }

        settings_button_apis.setOnClickListener {
            startActivity(Intent(this, SettingsApisActivity::class.java))
        }
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
}
