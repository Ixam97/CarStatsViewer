package com.ixam97.carStatsViewer.objects

import android.content.Context
import android.content.SharedPreferences
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.plot.enums.PlotDimension

class AppPreferences(context: Context) {

    private var sharedPref: SharedPreferences

    init {
        sharedPref = context.getSharedPreferences(
            context.getString(R.string.preferences_file_key), Context.MODE_PRIVATE
        )
    }

    var debug: Boolean
        get() {
            return getPreference(AppPreference.DEBUG) as Boolean
        }
        set(value) {
            setPreference(AppPreference.DEBUG, value)
        }
    var notifications: Boolean
        get() {
            return getPreference(AppPreference.NOTIFICATIONS) as Boolean
        }
        set(value) {
            setPreference(AppPreference.NOTIFICATIONS, value)
        }
    var consumptionUnit: Boolean
        get() {
            return getPreference(AppPreference.CONSUMPTION_UNIT) as Boolean
        }
        set(value) {
            setPreference(AppPreference.CONSUMPTION_UNIT, value)
        }
    var experimentalLayout: Boolean
        get() {
            return getPreference(AppPreference.EXPERIMENTAL_LAYOUT) as Boolean
        }
        set(value) {
            setPreference(AppPreference.EXPERIMENTAL_LAYOUT, value)
        }
    var deepLog: Boolean
        get() {
            return getPreference(AppPreference.DEEP_LOG) as Boolean
        }
        set(value) {
            setPreference(AppPreference.DEEP_LOG, value)
        }
    var plotSpeed: Boolean
        get() {
            return getPreference(AppPreference.PLOT_SHOW_SPEED) as Boolean
        }
        set(value) {
            setPreference(AppPreference.PLOT_SHOW_SPEED, value)
        }
    var plotDistance: Int
        get() {
            return getPreference(AppPreference.PLOT_DISTANCE) as Int
        }
        set(value) {
            setPreference(AppPreference.PLOT_DISTANCE, value)
        }
    var consumptionPlotSingleMotor: Boolean
        get() {
            return getPreference(AppPreference.CONSUMPTION_PLOT_SINGLE_MOTOR) as Boolean
        }
        set(value) {
            setPreference(AppPreference.CONSUMPTION_PLOT_SINGLE_MOTOR, value)
        }
    var consumptionPlotSecondaryColor: Boolean
        get() {
            return getPreference(AppPreference.CONSUMPTION_PLOT_SECONDARY_COLOR) as Boolean
        }
        set(value) {
            setPreference(AppPreference.CONSUMPTION_PLOT_SECONDARY_COLOR, value)
        }
    var consumptionPlotVisibleGages: Boolean
        get() {
            return getPreference(AppPreference.CONSUMPTION_PLOT_VISIBLE_GAGES) as Boolean
        }
        set(value) {
            setPreference(AppPreference.CONSUMPTION_PLOT_VISIBLE_GAGES, value)
        }
    var chargePlotSecondaryColor: Boolean
        get() {
            return getPreference(AppPreference.CHARGE_PLOT_SECONDARY_COLOR) as Boolean
        }
        set(value) {
            setPreference(AppPreference.CHARGE_PLOT_SECONDARY_COLOR, value)
        }
    var chargePlotVisibleGages: Boolean
        get() {
            return getPreference(AppPreference.CHARGE_PLOT_VISIBLE_GAGES) as Boolean
        }
        set(value) {
            setPreference(AppPreference.CHARGE_PLOT_VISIBLE_GAGES, value)
        }

    var chargePlotDimension: PlotDimension
        get() {
            return getPreference(AppPreference.CHARGE_PLOT_DIMENSION) as PlotDimension
        }
        set(value) {
            setPreference(AppPreference.CHARGE_PLOT_DIMENSION, value)
        }


    private val keyMap = hashMapOf<AppPreference, String>(
        AppPreference.DEBUG to context.getString(R.string.preferences_debug_key),
        AppPreference.NOTIFICATIONS to context.getString(R.string.preferences_notifications_key),
        AppPreference.CONSUMPTION_UNIT to context.getString(R.string.preferences_consumption_unit_key),
        AppPreference.EXPERIMENTAL_LAYOUT to context.getString(R.string.preferences_experimental_layout_key),
        AppPreference.DEEP_LOG to context.getString(R.string.preferences_deep_log_key),
        AppPreference.PLOT_SHOW_SPEED to context.getString(R.string.preferences_plot_speed_key),
        AppPreference.PLOT_DISTANCE to context.getString(R.string.preferences_plot_distance_key),
        AppPreference.CONSUMPTION_PLOT_SINGLE_MOTOR to context.getString(R.string.preferences_consumption_plot_single_motor_key),
        AppPreference.CONSUMPTION_PLOT_SECONDARY_COLOR to context.getString(R.string.preference_consumption_plot_secondary_color_key),
        AppPreference.CONSUMPTION_PLOT_VISIBLE_GAGES to context.getString(R.string.preference_consumption_plot_visible_gages_key),
        AppPreference.CHARGE_PLOT_SECONDARY_COLOR to context.getString(R.string.preference_charge_plot_secondary_color_key),
        AppPreference.CHARGE_PLOT_VISIBLE_GAGES to context.getString(R.string.preference_charge_plot_visible_gages_key),
        AppPreference.CHARGE_PLOT_DIMENSION to context.getString(R.string.preference_charge_plot_dimension_key)
    )

    private var typeMap = mapOf<AppPreference, Any>( // Also contains default values
        AppPreference.DEBUG to false,
        AppPreference.NOTIFICATIONS to false,
        AppPreference.CONSUMPTION_UNIT to false,
        AppPreference.EXPERIMENTAL_LAYOUT to false,
        AppPreference.DEEP_LOG to false,
        AppPreference.PLOT_SHOW_SPEED to false,
        AppPreference.PLOT_DISTANCE to 1,
        AppPreference.CONSUMPTION_PLOT_SINGLE_MOTOR to false,
        AppPreference.CONSUMPTION_PLOT_SECONDARY_COLOR to false,
        AppPreference.CONSUMPTION_PLOT_VISIBLE_GAGES to true,
        AppPreference.CHARGE_PLOT_SECONDARY_COLOR to false,
        AppPreference.CHARGE_PLOT_VISIBLE_GAGES to true,
        AppPreference.CHARGE_PLOT_DIMENSION to PlotDimension.TIME
    )

    private fun getPreference(appPreference: AppPreference): Any {
        if (typeMap.containsKey(appPreference)) {
            if (typeMap[appPreference] is Boolean) {
                return sharedPref.getBoolean(keyMap[appPreference], typeMap[appPreference] as Boolean)
            }
            if (typeMap[appPreference] is Int) {
                return sharedPref.getInt(keyMap[appPreference], typeMap[appPreference] as Int)
            }
            if (typeMap[appPreference] is PlotDimension) {
                return PlotDimension.valueOf(sharedPref.getString(keyMap[appPreference], (typeMap[appPreference] as PlotDimension).name) ?: PlotDimension.TIME.name)
            }
        }
        throw java.lang.Exception("AppPreferences.setPreference: Unknown Preference!")
    }

    private fun setPreference(appPreference: AppPreference, value: Any) {
        if (typeMap.containsKey(appPreference)) {
            if (typeMap[appPreference] is Boolean && value is Boolean) {
                sharedPref.edit().putBoolean(keyMap[appPreference], value).apply()
                return
            }
            if (typeMap[appPreference] is Int && value is Int) {
                sharedPref.edit().putInt(keyMap[appPreference], value).apply()
                return
            }
            if (typeMap[appPreference] is PlotDimension && value is PlotDimension) {
                sharedPref.edit().putString(keyMap[appPreference], value.name).apply()
                return
            }
            throw java.lang.Exception("AppPreferences.setPreference: Unsupported type!")
        }
        throw java.lang.Exception("AppPreferences.setPreference: Unknown Preference!")
    }
}

enum class AppPreference {
    DEBUG,
    NOTIFICATIONS,
    CONSUMPTION_UNIT,
    EXPERIMENTAL_LAYOUT,
    DEEP_LOG,
    PLOT_SHOW_SPEED,
    PLOT_DISTANCE,
    CONSUMPTION_PLOT_SINGLE_MOTOR,
    CONSUMPTION_PLOT_SECONDARY_COLOR,
    CONSUMPTION_PLOT_VISIBLE_GAGES,
    CHARGE_PLOT_SECONDARY_COLOR,
    CHARGE_PLOT_VISIBLE_GAGES,
    CHARGE_PLOT_DIMENSION
}