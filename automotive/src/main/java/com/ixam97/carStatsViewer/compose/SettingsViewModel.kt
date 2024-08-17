package com.ixam97.carStatsViewer.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ixam97.carStatsViewer.BuildConfig
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.utils.InAppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel: ViewModel() {

    data class SettingsState(
        val isInitialized: Boolean = false,
        val detailedNotifications: Boolean = false,
        val altConsumptionUnit: Boolean = false,
        val showConsumptionGages: Boolean = false,
        val showChargingGages: Boolean = false,
        val locationTracking: Boolean = false,
        val autoAppStart: Boolean = false,
        val phoneNotification: Boolean = false,
        val selectedTripType: Int = 0,
        val selectedConnection: Int = 0,
        val primaryPlotColor: Int = 0,
        val secondaryConsumptionPlotColor: Int = 0,
        val secondaryChargePlotColor: Int = 0
    )

    private val _themeSettingState = MutableStateFlow<Int>(0)
    val themeSettingStateFLow = _themeSettingState.asStateFlow()

    private val _finishActivityLiveData = MutableLiveData<Event<Boolean>>()
    val finishActivityLiveData: LiveData<Event<Boolean>> = _finishActivityLiveData

    // private val _settingsStateFlow = MutableStateFlow(SettingsState())
    // val settingsStateFlow = _settingsStateFlow.asStateFlow()

    var isDevEnabled by mutableStateOf(BuildConfig.FLAVOR_version != "dev")
        private set

    var settingsState by mutableStateOf(SettingsState())
        // private set

    private var versionClickedNum: Int = 0

    init {
        val preferences = CarStatsViewer.appPreferences
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                // _settingsStateFlow.update {
                //     it.copy(
                //         isInitialized = true,
                //         detailedNotifications = preferences.notifications,
                //         altConsumptionUnit = preferences.consumptionUnit,
                //         showChargingGages = preferences.chargePlotVisibleGages,
                //         showConsumptionGages = preferences.chargePlotVisibleGages,
                //         locationTracking = preferences.useLocation,
                //         autoAppStart = preferences.autostart,
                //         phoneNotification = preferences.phoneNotification,
                //         selectedTripType = preferences.mainViewTrip,
                //         selectedConnection = preferences.mainViewConnectionApi,
                //         primaryPlotColor = 0,
                //         secondaryPlotColor = if (preferences.consumptionPlotSecondaryColor) 1 else 0
                //     )
                // }
                settingsState = settingsState.copy(
                    isInitialized = true,
                    detailedNotifications = preferences.notifications,
                    altConsumptionUnit = preferences.consumptionUnit,
                    showChargingGages = preferences.chargePlotVisibleGages,
                    showConsumptionGages = preferences.chargePlotVisibleGages,
                    locationTracking = preferences.useLocation,
                    autoAppStart = preferences.autostart,
                    phoneNotification = preferences.phoneNotification,
                    selectedTripType = preferences.mainViewTrip,
                    selectedConnection = preferences.mainViewConnectionApi,
                    primaryPlotColor = 0,
                    secondaryConsumptionPlotColor = if (preferences.consumptionPlotSecondaryColor) 1 else 0,
                    secondaryChargePlotColor = if (preferences.chargePlotSecondaryColor) 1 else 0
                )
            }
        }
    }

    fun finishActivity() = _finishActivityLiveData.postValue(Event(true))

    fun setTheme(themeIndex: Int) {
        _themeSettingState.update { themeIndex }
    }

    override fun onCleared() {
        InAppLogger.d("ViewModel cleared!")
        super.onCleared()
    }

    fun versionClick() {
        versionClickedNum++
        if (versionClickedNum >= 7) {
            isDevEnabled = true
        }
    }

}