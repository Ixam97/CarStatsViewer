package com.ixam97.carStatsViewer.carCompose.screens.settings

import android.util.Patterns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.ixam97.carStatsViewer.CarStatsViewer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SettingsApisState(
    val exportMailAddress: String = CarStatsViewer.appPreferences.dataExportAddress,
    val validExportMailAddress: Boolean? = validateEmailAddress(exportMailAddress),
    val exportEnabled: Boolean = CarStatsViewer.appPreferences.dataExportEnabled
)

data class SettingsGeneralState(
    val autoAppStartEnabled: Boolean = CarStatsViewer.appPreferences.autostart,
    val phoneReminderEnabled: Boolean = CarStatsViewer.appPreferences.phoneNotification,
    val detailedNotificationEnabled: Boolean = CarStatsViewer.appPreferences.notifications
)

data class SettingsAppearanceState(
    val altConsumptionUnit: Boolean = CarStatsViewer.appPreferences.consumptionUnit,
    val showPowerBar: Boolean = CarStatsViewer.appPreferences.consumptionPlotVisibleGages,
    val altSecPowerPlotColor: Boolean = CarStatsViewer.appPreferences.consumptionPlotSecondaryColor,
    val showChargeBar: Boolean = CarStatsViewer.appPreferences.chargePlotVisibleGages,
    val altSecChargePlotColor: Boolean = CarStatsViewer.appPreferences.chargePlotSecondaryColor
)

class SettingsViewModel: ViewModel() {

    var selectedSettingsTabKey: SettingsTabKeys by mutableStateOf(SettingsTabKeys.General)
        private set

    fun setSettingsTabKey(key: SettingsTabKeys) {
        // TODO: Implement check for valid keys depending on dev mode and flavor.
        selectedSettingsTabKey = key
    }

    private val _settingsApisState = MutableStateFlow(SettingsApisState())
    val settingsApisState = _settingsApisState.asStateFlow()

    fun setExportEnabled(enabled: Boolean) {
        CarStatsViewer.appPreferences.dataExportEnabled = enabled
        _settingsApisState.update { it.copy(exportEnabled = CarStatsViewer.appPreferences.dataExportEnabled) }
    }

    fun setExportMailAddress(address: String) {
        val validAddress = validateEmailAddress(address)

        when (validAddress) {
            true -> {
                CarStatsViewer.appPreferences.dataExportAddress = address
            }
            false -> {
                CarStatsViewer.appPreferences.dataExportEnabled = false
                CarStatsViewer.appPreferences.dataExportAddress = address
            }
            null -> {
                CarStatsViewer.appPreferences.dataExportAddress = ""
                CarStatsViewer.appPreferences.dataExportEnabled = false
            }
        }

        _settingsApisState.update { it.copy(
            exportMailAddress = CarStatsViewer.appPreferences.dataExportAddress,
            validExportMailAddress = validAddress,
            exportEnabled = CarStatsViewer.appPreferences.dataExportEnabled
        ) }
    }

    /**
     * General Settings
     */
    private var _settingsGeneralState = MutableStateFlow(SettingsGeneralState())
    val settingsGeneralState = _settingsGeneralState.asStateFlow()

    fun setAutoAppStartEnabled(enabled: Boolean) {
        CarStatsViewer.appPreferences.autostart = enabled
        _settingsGeneralState.update { it.copy(autoAppStartEnabled = CarStatsViewer.appPreferences.autostart) }
        CarStatsViewer.setupRestartAlarm(
            context = CarStatsViewer.appContext,
            reason = "termination",
            delay = 9_500,
            cancel = !CarStatsViewer.appPreferences.autostart,
            extendedLogging = true)
    }

    fun setPhoneReminderEnabled(enabled: Boolean) {
        CarStatsViewer.appPreferences.phoneNotification = enabled
        _settingsGeneralState.update { it.copy(phoneReminderEnabled = CarStatsViewer.appPreferences.phoneNotification) }
    }

    fun setDetailedNotificationEnabled(enabled: Boolean) {
        CarStatsViewer.appPreferences.notifications = enabled
        _settingsGeneralState.update { it.copy(detailedNotificationEnabled = CarStatsViewer.appPreferences.notifications) }

    }

    /**
     * Appearance Settings
     */
    private var _settingsAppearanceState = MutableStateFlow(SettingsAppearanceState())
    val settingsAppearanceState = _settingsAppearanceState.asStateFlow()

    fun setAltConsumptionUnit(value: Boolean) {
        CarStatsViewer.appPreferences.consumptionUnit = value
        _settingsAppearanceState.update { it.copy(altConsumptionUnit = CarStatsViewer.appPreferences.consumptionUnit) }
    }
    fun setShowPowerBar(value: Boolean) {
        CarStatsViewer.appPreferences.consumptionPlotVisibleGages = value
        _settingsAppearanceState.update { it.copy(showPowerBar = CarStatsViewer.appPreferences.consumptionPlotVisibleGages) }
    }
    fun setAltSecPowerPlotColor(value: Boolean) {
        CarStatsViewer.appPreferences.consumptionPlotSecondaryColor = value
        _settingsAppearanceState.update { it.copy(altSecPowerPlotColor = CarStatsViewer.appPreferences.consumptionPlotSecondaryColor) }
    }
    fun setShowChargeBar(value: Boolean) {
        CarStatsViewer.appPreferences.chargePlotVisibleGages = value
        _settingsAppearanceState.update { it.copy(showChargeBar = CarStatsViewer.appPreferences.chargePlotVisibleGages) }
    }
    fun setAltSecChargePlotColor(value: Boolean) {
        CarStatsViewer.appPreferences.chargePlotSecondaryColor = value
        _settingsAppearanceState.update { it.copy(altSecChargePlotColor = CarStatsViewer.appPreferences.chargePlotSecondaryColor) }
    }
}

internal fun validateEmailAddress(address: String): Boolean? {
    if (address.isBlank()) return null
    return Patterns.EMAIL_ADDRESS.matcher(address).matches()
}