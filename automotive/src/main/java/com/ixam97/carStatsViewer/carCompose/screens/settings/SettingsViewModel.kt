package com.ixam97.carStatsViewer.carCompose.screens.settings

import android.util.Patterns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.app
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.liveDataApi.ConnectionStatus
import com.ixam97.carStatsViewer.utils.DistanceUnitEnum
import com.ixam97.carStatsViewer.utils.InAppLogger
import com.ixam97.carStatsViewer.utils.ScreenshotService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsGeneralState(
    val autoAppStart: Boolean = CarStatsViewer.appPreferences.autostart,
    val phoneReminder: Boolean = CarStatsViewer.appPreferences.phoneNotification,
    val detailedNotification: Boolean = CarStatsViewer.appPreferences.notifications
)
data class SettingsAppearanceState(
    val altConsumptionUnit: Boolean = CarStatsViewer.appPreferences.consumptionUnit,
    val showPowerBar: Boolean = CarStatsViewer.appPreferences.consumptionPlotVisibleGages,
    val altSecPowerPlotColor: Boolean = CarStatsViewer.appPreferences.consumptionPlotSecondaryColor,
    val showChargeBar: Boolean = CarStatsViewer.appPreferences.chargePlotVisibleGages,
    val altSecChargePlotColor: Boolean = CarStatsViewer.appPreferences.chargePlotSecondaryColor
)
data class SettingsPrivacyState(
    val locationTracking: Boolean = CarStatsViewer.appPreferences.useLocation,
    val analytics: Boolean? = null
)

data class SettingsApisState(
    val exportMailAddress: String = CarStatsViewer.appPreferences.dataExportAddress,
    val validExportMailAddress: Boolean? = validateEmailAddress(exportMailAddress),
    val exportEnabled: Boolean = CarStatsViewer.appPreferences.dataExportEnabled,
    val abrpConnectionStatus: ConnectionStatus = ConnectionStatus.UNUSED,
    val restConnectionStatus: ConnectionStatus = ConnectionStatus.UNUSED
)
data class SettingsDevState(
    val loadingDelays: Boolean = CarStatsViewer.appPreferences.debugDelays,
    val additionalColorSchemes: Boolean = CarStatsViewer.appPreferences.debugColors,
    val milesAsDistanceUnit: DistanceUnitEnum = CarStatsViewer.appPreferences.distanceUnit,
    val userId: String = CarStatsViewer.appPreferences.debugUserID,
    val userMail: String = CarStatsViewer.appPreferences.debugScreenshotReceiver,
    val userMailValid: Boolean? = validateEmailAddress(userMail),
    val numberOfScreenshots: Int = ScreenshotService.screenshotServiceState.value.numberOfScreenshots,
    val screenshotServiceRunning: Boolean = ScreenshotService.screenshotServiceState.value.isServiceRunning
)

class SettingsViewModel: ViewModel() {

    val appPreferences = CarStatsViewer.appPreferences

    var selectedSettingsTabKey: SettingsTabKeys by mutableStateOf(SettingsTabKeys.General)
        private set

    fun setSettingsTabKey(key: SettingsTabKeys) {
        // TODO: Implement check for valid keys depending on dev mode and flavor.
        selectedSettingsTabKey = key
    }

    //region General Settings
    private var _settingsGeneralState = MutableStateFlow(SettingsGeneralState())
    val settingsGeneralState = _settingsGeneralState.asStateFlow()

    fun setAutoAppStartEnabled(enabled: Boolean) {
        appPreferences.autostart = enabled
        _settingsGeneralState.update { it.copy(autoAppStart = appPreferences.autostart) }
        CarStatsViewer.setupRestartAlarm(
            context = CarStatsViewer.appContext,
            reason = "termination",
            delay = 9_500,
            cancel = !appPreferences.autostart,
            extendedLogging = true)
    }

    fun setPhoneReminderEnabled(enabled: Boolean) {
        appPreferences.phoneNotification = enabled
        _settingsGeneralState.update { it.copy(phoneReminder = appPreferences.phoneNotification) }
    }

    fun setDetailedNotificationEnabled(enabled: Boolean) {
        appPreferences.notifications = enabled
        _settingsGeneralState.update { it.copy(detailedNotification = appPreferences.notifications) }

    }
    //endregion

    //region Appearance Settings
    private var _settingsAppearanceState = MutableStateFlow(SettingsAppearanceState())
    val settingsAppearanceState = _settingsAppearanceState.asStateFlow()

    fun setAltConsumptionUnit(value: Boolean) {
        appPreferences.consumptionUnit = value
        _settingsAppearanceState.update { it.copy(altConsumptionUnit = appPreferences.consumptionUnit) }
    }
    fun setShowPowerBar(value: Boolean) {
        appPreferences.consumptionPlotVisibleGages = value
        _settingsAppearanceState.update { it.copy(showPowerBar = appPreferences.consumptionPlotVisibleGages) }
    }
    fun setAltSecPowerPlotColor(value: Boolean) {
        appPreferences.consumptionPlotSecondaryColor = value
        _settingsAppearanceState.update { it.copy(altSecPowerPlotColor = appPreferences.consumptionPlotSecondaryColor) }
    }
    fun setShowChargeBar(value: Boolean) {
        appPreferences.chargePlotVisibleGages = value
        _settingsAppearanceState.update { it.copy(showChargeBar = appPreferences.chargePlotVisibleGages) }
    }
    fun setAltSecChargePlotColor(value: Boolean) {
        appPreferences.chargePlotSecondaryColor = value
        _settingsAppearanceState.update { it.copy(altSecChargePlotColor = appPreferences.chargePlotSecondaryColor) }
    }
    //endregion

    //region Privacy and Location Settings
    private var _settingsPrivacyState = MutableStateFlow(SettingsPrivacyState())
    val settingsPrivacyState = _settingsPrivacyState.asStateFlow()

    fun setLocationTracking(value: Boolean) {
        appPreferences.useLocation = value
        _settingsPrivacyState.update { it.copy(locationTracking = appPreferences.useLocation) }
    }

    fun setAnalytics(value: Boolean) {
        try {
            Firebase.app.setDataCollectionDefaultEnabled(value)
        } catch (_: Throwable) {
            _settingsPrivacyState.update { it.copy(analytics = null) }
        } finally {
            _settingsPrivacyState.update { it.copy(analytics = value) }
        }
    }
    //endregion

    //region APIs Settings
    private val _settingsApisState = MutableStateFlow(SettingsApisState())
    val settingsApisState = _settingsApisState.asStateFlow()

    fun setExportEnabled(enabled: Boolean) {
        appPreferences.dataExportEnabled = enabled
        _settingsApisState.update { it.copy(exportEnabled = appPreferences.dataExportEnabled) }
    }

    fun setExportMailAddress(address: String) {
        val validAddress = validateEmailAddress(address)

        when (validAddress) {
            true -> {
                appPreferences.dataExportAddress = address
            }
            false -> {
                appPreferences.dataExportEnabled = false
                appPreferences.dataExportAddress = address
            }
            null -> {
                appPreferences.dataExportAddress = ""
                appPreferences.dataExportEnabled = false
            }
        }

        _settingsApisState.update { it.copy(
            exportMailAddress = appPreferences.dataExportAddress,
            validExportMailAddress = validAddress,
            exportEnabled = appPreferences.dataExportEnabled
        ) }
    }
    //endregion

    //region Developer Settings
    private var _settingsDevState = MutableStateFlow(SettingsDevState())
    val settingsDevState = _settingsDevState.asStateFlow()
    //endregion

    init {

        try {
            val analyticsEnabled = Firebase.app.isDataCollectionDefaultEnabled
            _settingsPrivacyState.update { it.copy(analytics = analyticsEnabled) }
        } catch (_: Throwable) {
            InAppLogger.w("Firebase is not enabled in this build!")
        }

        viewModelScope.launch {
            ScreenshotService.screenshotServiceState.collect { serviceState ->
                _settingsDevState.update { it.copy(
                    screenshotServiceRunning = serviceState.isServiceRunning,
                    numberOfScreenshots = serviceState.numberOfScreenshots
                ) }
            }
        }

        viewModelScope.launch {
            CarStatsViewer.watchdog.watchdogStateFlow.collect { watchdogState ->
                _settingsApisState.update { it.copy(
                    abrpConnectionStatus = ConnectionStatus.fromInt(watchdogState.apiState[CarStatsViewer.liveDataApis[0].apiIdentifier]?:0),
                    restConnectionStatus = ConnectionStatus.fromInt(watchdogState.apiState[CarStatsViewer.liveDataApis[1].apiIdentifier]?:0),
                ) }
            }
        }
    }
}

internal fun validateEmailAddress(address: String): Boolean? {
    if (address.isBlank()) return null
    return Patterns.EMAIL_ADDRESS.matcher(address).matches()
}