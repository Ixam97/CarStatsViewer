package com.ixam97.carStatsViewer.abrpLiveData

import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.appPreferences.AppPreference
import com.ixam97.carStatsViewer.appPreferences.AppPreferences

val AppPreferences.AbrpGenericToken: AppPreference<String>
    get() = AppPreference<String>("preference_abrp_generic_token", "", sharedPref)

var AppPreferences.abrpGenericToken: String get() = AbrpGenericToken.value; set(value) {AbrpGenericToken.value = value}