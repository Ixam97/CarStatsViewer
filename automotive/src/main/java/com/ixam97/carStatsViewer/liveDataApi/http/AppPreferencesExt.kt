package com.ixam97.carStatsViewer.liveDataApi.http

/**
 * This file extends the AppPreferences to contain keys used by the API implementation. The rest of
 * the app does not need to see them.
 */

import com.ixam97.carStatsViewer.appPreferences.AppPreference
import com.ixam97.carStatsViewer.appPreferences.AppPreferences

val AppPreferences.HTTPLiveDataURL: AppPreference<String>
    get() = AppPreference<String>("preference_http_live_data_url", "", sharedPref)

val AppPreferences.HTTPLiveDataUsername: AppPreference<String>
    get() = AppPreference<String>("preference_http_live_data_username", "", sharedPref)

val AppPreferences.HTTPLiveDataPassword: AppPreference<String>
    get() = AppPreference<String>("preference_http_live_data_password", "", sharedPref)

val AppPreferences.HTTPLiveDataEnabled: AppPreference<Boolean>
    get() = AppPreference<Boolean>("preference_http_live_data_enables", false, sharedPref)

val AppPreferences.HTTPLiveDataLocation: AppPreference<Boolean>
    get() = AppPreference<Boolean>("preference_http_live_data_location", true, sharedPref)

val AppPreferences.HTTPLiveDataSendABRPDataset: AppPreference<Boolean>
    get() = AppPreference<Boolean>("preference_http_live_data_abrp", false, sharedPref)

var AppPreferences.httpLiveDataURL: String get() = HTTPLiveDataURL.value; set(value) {HTTPLiveDataURL.value = value}
var AppPreferences.httpLiveDataUsername: String get() = HTTPLiveDataUsername.value; set(value) {HTTPLiveDataUsername.value = value}
var AppPreferences.httpLiveDataPassword: String get() = HTTPLiveDataPassword.value; set(value) {HTTPLiveDataPassword.value = value}
var AppPreferences.httpLiveDataEnabled: Boolean get() = HTTPLiveDataEnabled.value; set(value) {HTTPLiveDataEnabled.value = value}
var AppPreferences.httpLiveDataLocation: Boolean get() = HTTPLiveDataLocation.value; set(value) {HTTPLiveDataLocation.value = value}
var AppPreferences.httpLiveDataSendABRPDataset: Boolean get() = HTTPLiveDataSendABRPDataset.value; set(value) {HTTPLiveDataSendABRPDataset.value = value}