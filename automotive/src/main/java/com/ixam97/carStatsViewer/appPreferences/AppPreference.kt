package com.ixam97.carStatsViewer.appPreferences

import android.content.SharedPreferences
import com.ixam97.carStatsViewer.ui.plot.enums.PlotDimensionX
import com.ixam97.carStatsViewer.utils.DistanceUnitEnum

class AppPreference<T>(
    private val key: String,
    private val default: T,
    private val sharedPref: SharedPreferences) {

    var value: T
        get() {
            return when (default) {
                is Boolean -> sharedPref.getBoolean(key, default) as T
                is Int -> sharedPref.getInt(key, default) as T
                is Long -> sharedPref.getLong(key, default) as T
                is String -> sharedPref.getString(key, default) as T
                is PlotDimensionX -> PlotDimensionX.valueOf(sharedPref.getString(key, default.name)!!) as T
                is DistanceUnitEnum -> DistanceUnitEnum.valueOf(sharedPref.getString(key, default.name)!!) as T
                else -> default
            }
        }
        set(value) {
            when (default) {
                is Boolean -> sharedPref.edit().putBoolean(key, value as Boolean).apply()
                is Int -> sharedPref.edit().putInt(key, value as Int).apply()
                is Long -> sharedPref.edit().putLong(key, value as Long).apply()
                is String -> sharedPref.edit().putString(key, value as String).apply()
                is PlotDimensionX -> sharedPref.edit().putString(key, (value as PlotDimensionX).name).apply()
                is DistanceUnitEnum ->sharedPref.edit().putString(key, (value as DistanceUnitEnum).name).apply()
                //else ->
            }
        }
}

