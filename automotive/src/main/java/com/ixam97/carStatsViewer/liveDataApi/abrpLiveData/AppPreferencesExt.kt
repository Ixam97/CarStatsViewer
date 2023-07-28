package com.ixam97.carStatsViewer.liveDataApi.abrpLiveData

/**
 * This file extends the AppPreferences to contain keys used by the API implementation. The rest of
 * the app does not need to see them.
 */

import com.ixam97.carStatsViewer.appPreferences.AppPreference
import com.ixam97.carStatsViewer.appPreferences.AppPreferences

val AppPreferences.AbrpGenericToken: AppPreference<String>
    get() = AppPreference<String>("preference_abrp_generic_token", "", sharedPref)
val AppPreferences.AbrpUseApi: AppPreference<Boolean>
    get() = AppPreference<Boolean>("preference_abrp_use", false, sharedPref)
val AppPreferences.AbrpUseLocation: AppPreference<Boolean>
    get() = AppPreference<Boolean>("preference_abrp_location", true, sharedPref)

var AppPreferences.abrpGenericToken: String get() = AbrpGenericToken.value; set(value) {AbrpGenericToken.value = value}
var AppPreferences.abrpUseApi: Boolean get() = AbrpUseApi.value; set(value) {AbrpUseApi.value = value}
var AppPreferences.abrpUseLocation: Boolean get() = AbrpUseLocation.value; set(value) {AbrpUseLocation.value = value}