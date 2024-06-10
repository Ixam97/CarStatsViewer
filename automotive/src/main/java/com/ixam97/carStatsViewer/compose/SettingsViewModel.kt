package com.ixam97.carStatsViewer.compose

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ixam97.carStatsViewer.CarStatsViewer
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
        val locationTracking: Boolean = false,
        val autoAppStart: Boolean = false,
        val phoneNotification: Boolean = false,
        val selectedTripType: Int = 0,
        val selectedConnection: Int = 0,
        val primaryPlotColor: Int = 0,
        val secondaryPlotColor: Int = 0
    )

    private val _themeSettingState = MutableStateFlow<Int>(0)
    val themeSettingStateFLow = _themeSettingState.asStateFlow()

    private val _finishActivityLiveData = MutableLiveData<Event<Boolean>>()
    val finishActivityLiveData: LiveData<Event<Boolean>> = _finishActivityLiveData

    private val _settingsStateFlow = MutableStateFlow(SettingsState())
    val settingsStateFlow = _settingsStateFlow.asStateFlow()

    init {
        val preferences = CarStatsViewer.appPreferences
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                _settingsStateFlow.update {
                    it.copy(
                        isInitialized = true,
                        detailedNotifications = preferences.notifications,
                        altConsumptionUnit = preferences.consumptionUnit,
                        locationTracking = preferences.useLocation,
                        autoAppStart = preferences.autostart,
                        phoneNotification = preferences.phoneNotification,
                        selectedTripType = preferences.mainViewTrip,
                        selectedConnection = preferences.mainViewConnectionApi,
                        primaryPlotColor = 0,
                        secondaryPlotColor = if (preferences.consumptionPlotSecondaryColor) 1 else 0
                    )
                }
            }
        }
    }

    fun finishActivity() = _finishActivityLiveData.postValue(Event(true))
    fun setLocationTracking(locationTracking: Boolean) {
        _settingsStateFlow.update { it.copy(locationTracking = locationTracking) }
    }

    fun setAltConsumptionUnit(altConsumptionUnit: Boolean) {
        _settingsStateFlow.update { it.copy(altConsumptionUnit = altConsumptionUnit) }
    }

    fun setAutoAppStart(autoAppStart: Boolean) {
        _settingsStateFlow.update { it.copy(autoAppStart = autoAppStart) }
    }

    fun setTheme(themeIndex: Int) {
        _themeSettingState.update { themeIndex }
    }

}