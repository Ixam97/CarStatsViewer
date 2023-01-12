package com.ixam97.carStatsViewer.objects

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.ixam97.carStatsViewer.R

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
    var singleMotor: Boolean
        get() {
            return getPreference(AppPreference.SINGLE_MOTOR) as Boolean
        }
        set(value) {
            setPreference(AppPreference.SINGLE_MOTOR, value)
        }



    private val keyMap = hashMapOf<AppPreference, String>(
        AppPreference.DEBUG to context.getString(R.string.preferences_debug_key),
        AppPreference.NOTIFICATIONS to context.getString(R.string.preferences_notifications_key),
        AppPreference.CONSUMPTION_UNIT to context.getString(R.string.preferences_consumption_unit_key),
        AppPreference.EXPERIMENTAL_LAYOUT to context.getString(R.string.preferences_experimental_layout_key),
        AppPreference.DEEP_LOG to context.getString(R.string.preferences_deep_log_key),
        AppPreference.PLOT_SHOW_SPEED to context.getString(R.string.preferences_plot_speed_key),
        AppPreference.PLOT_DISTANCE to context.getString(R.string.preferences_plot_distance_key),
        AppPreference.SINGLE_MOTOR to context.getString(R.string.preferences_single_motor_key)
    )

    private var typeMap = mapOf<AppPreference, Any>(
        AppPreference.DEBUG to false,
        AppPreference.NOTIFICATIONS to false,
        AppPreference.CONSUMPTION_UNIT to false,
        AppPreference.EXPERIMENTAL_LAYOUT to false,
        AppPreference.DEEP_LOG to false,
        AppPreference.PLOT_SHOW_SPEED to false,
        AppPreference.PLOT_DISTANCE to 1,
        AppPreference.SINGLE_MOTOR to false
    )

    fun getPreference(appPreference: AppPreference): Any? {
        if (typeMap.containsKey(appPreference)) {
            if (typeMap[appPreference] is Boolean) {
                return sharedPref.getBoolean(keyMap[appPreference], false)
            }
            if (typeMap[appPreference] is Int) {
                return sharedPref.getInt(keyMap[appPreference], 1)
            }
        }
        throw java.lang.Exception("AppPreferences.setPreference: Unknown Preference!")
    }

    fun setPreference(appPreference: AppPreference, value: Any) {
        if (typeMap.containsKey(appPreference)) {
            if (typeMap[appPreference] is Boolean && value is Boolean) {
                sharedPref.edit().putBoolean(keyMap[appPreference], value).apply()
                return
            }
            if (typeMap[appPreference] is Int && value is Int) {
                sharedPref.edit().putInt(keyMap[appPreference], value).apply()
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
    SINGLE_MOTOR
}