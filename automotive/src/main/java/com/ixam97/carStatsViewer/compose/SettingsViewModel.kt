package com.ixam97.carStatsViewer.compose

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.app
import com.ixam97.carStatsViewer.BuildConfig
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.ui.views.SnackbarWidget
import com.ixam97.carStatsViewer.utils.InAppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel: ViewModel() {


    private val preferences = CarStatsViewer.appPreferences

    data class SettingsState(
        val isInitialized: Boolean = false,
        val detailedNotifications: Boolean = false,
        val altConsumptionUnit: Boolean = false,
        val showConsumptionGages: Boolean = false,
        val showChargingGages: Boolean = false,
        val locationTracking: Boolean = false,
        val analytics: Boolean = false,
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

    var isDevEnabled by mutableStateOf(BuildConfig.FLAVOR_version == "dev")
        private set

    var settingsState by mutableStateOf(SettingsState())
        private set

    private var versionClickedNum: Int = 0

    init {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
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
                try {
                    settingsState = settingsState.copy(
                        analytics = Firebase.app.isDataCollectionDefaultEnabled
                    )
                } catch (e: Exception) {
                    InAppLogger.i("Firebase is disabled")
                }
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

    fun versionClick(context: Context) {
        versionClickedNum++
        if (versionClickedNum >= 7 && !isDevEnabled) {
            isDevEnabled = true
            SnackbarWidget.Builder(context, "Developer Settings enabled!")
                .setDuration(3_000L)
                .show()
        }
    }


    fun setDetailedNotifications(value: Boolean) {
        settingsState = settingsState.copy(detailedNotifications = value)
        preferences.notifications = value
    }
    fun setAltConsumptionUnit(value: Boolean) {
        settingsState = settingsState.copy(altConsumptionUnit = value)
        preferences.consumptionUnit = value
    }
    fun setShowChargingGages(value: Boolean) {
        settingsState = settingsState.copy(showChargingGages = value)
        preferences.consumptionPlotVisibleGages = value
    }
    fun setShowConsumptionGages(value: Boolean) {
        settingsState = settingsState.copy(showConsumptionGages = value)
        preferences.chargePlotVisibleGages = value
    }
    fun setLocationTracking(value: Boolean) {
        settingsState = settingsState.copy(locationTracking = value)
        preferences.useLocation = value
    }
    fun setAutoAppStart(value: Boolean) {
        settingsState = settingsState.copy(autoAppStart = value)
        preferences.autostart = value
    }
    fun setPhoneNotification(value: Boolean) {
        settingsState = settingsState.copy(phoneNotification = value)
        preferences.phoneNotification = value
    }
    fun setSelectedTripType(value: Int) {
        settingsState = settingsState.copy(selectedTripType = value)
        preferences.mainViewTrip = value
    }
    fun setSelectedConnection(value: Int) {
        settingsState = settingsState.copy(selectedConnection = value)
        preferences.mainViewConnectionApi = value
    }
    fun setPrimaryPlotColor(value: Int) {
        settingsState = settingsState.copy(primaryPlotColor = value)

    }
    fun setSecondaryConsumptionPlotColor(value: Int) {
        settingsState = settingsState.copy(secondaryConsumptionPlotColor = value)
        preferences.consumptionPlotSecondaryColor = value > 0
    }
    fun setSecondaryChargePlotColor(value: Int) {
        settingsState = settingsState.copy(secondaryChargePlotColor = value)
        preferences.chargePlotSecondaryColor = value > 0
    }
    fun setAnalytics(value: Boolean) {
        settingsState = settingsState.copy(analytics = value)
        Firebase.app.setDataCollectionDefaultEnabled(value)
    }

}