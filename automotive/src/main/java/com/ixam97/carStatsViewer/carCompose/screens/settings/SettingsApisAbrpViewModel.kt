package com.ixam97.carStatsViewer.carCompose.screens.settings

import androidx.lifecycle.ViewModel
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.liveDataApi.abrpLiveData.abrpGenericToken
import com.ixam97.carStatsViewer.liveDataApi.abrpLiveData.abrpUseApi
import com.ixam97.carStatsViewer.liveDataApi.abrpLiveData.abrpUseLocation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SettingsAbrpConfigState(
    val genericToken: String = CarStatsViewer.appPreferences.abrpGenericToken,
    val useApi: Boolean = CarStatsViewer.appPreferences.abrpUseApi,
    val locationTracking: Boolean = CarStatsViewer.appPreferences.abrpUseLocation,
    val showStatusIcon: Boolean = CarStatsViewer.appPreferences.mainViewConnectionApi == 0
)

class SettingsApisAbrpViewModel() : ViewModel() {

    val appPreferences = CarStatsViewer.appPreferences
    private val _abrpConfigState = MutableStateFlow(SettingsAbrpConfigState())
    val abrpConfigState = _abrpConfigState.asStateFlow()

    fun setAbrpGenericToken(token: String) {
        appPreferences.abrpGenericToken = token
        _abrpConfigState.update { it.copy(
            genericToken = appPreferences.abrpGenericToken
        ) }

        if (appPreferences.abrpGenericToken.isBlank()) {
            setAbrpEnabled(false)
        }
    }

    fun setAbrpEnabled(enabled: Boolean) {
        appPreferences.abrpUseApi = enabled
        _abrpConfigState.update { it.copy(
            useApi = appPreferences.abrpUseApi
        ) }
    }

    fun setAbrpUseLocation(enabled: Boolean) {
        appPreferences.abrpUseLocation = enabled
        _abrpConfigState.update { it.copy(
            locationTracking = appPreferences.abrpUseLocation
        ) }
    }
}