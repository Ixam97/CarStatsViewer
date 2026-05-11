package com.ixam97.carStatsViewer.appPreferences

import android.content.SharedPreferences

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
                else -> throw Exception("Unsupported Preference Type!")
            }
        }
        set(value) {
            when (default) {
                is Boolean -> sharedPref.edit().putBoolean(key, value as Boolean).apply()
                is Int -> sharedPref.edit().putInt(key, value as Int).apply()
                is Long -> sharedPref.edit().putLong(key, value as Long).apply()
                is String -> sharedPref.edit().putString(key, value as String).apply()
                else -> throw Exception("Unsupported Preference Type!")
            }
        }
}

