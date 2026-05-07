package com.ixam97.carStatsViewer.carCompose.screens.settings

import androidx.lifecycle.ViewModel
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.liveDataApi.http.httpLiveDataEnabled
import com.ixam97.carStatsViewer.liveDataApi.http.httpLiveDataLocation
import com.ixam97.carStatsViewer.liveDataApi.http.httpLiveDataPassword
import com.ixam97.carStatsViewer.liveDataApi.http.httpLiveDataURL
import com.ixam97.carStatsViewer.liveDataApi.http.httpLiveDataUsername
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class SettingsWebhookTelemetryType {
    RealTime, DrivePoints, Both
}

data class SettingsWebhookConfigState(
    val endpointUrl: String = CarStatsViewer.appPreferences.httpLiveDataURL,
    val endpointUrlValid: Boolean? = validateUrlAddress(endpointUrl),
    val username: String = CarStatsViewer.appPreferences.httpLiveDataUsername,
    val password: String = CarStatsViewer.appPreferences.httpLiveDataPassword,
    val useApi: Boolean = CarStatsViewer.appPreferences.httpLiveDataEnabled,
    val locationTracking: Boolean = CarStatsViewer.appPreferences.httpLiveDataLocation,
    val telemetryType: SettingsWebhookTelemetryType = SettingsWebhookTelemetryType.entries[CarStatsViewer.appPreferences.httpApiTelemetryType],
    val showStatusIcon: Boolean = CarStatsViewer.appPreferences.mainViewConnectionApi == 1,
    val passwordVisible: Boolean = false
)

class SettingsApisWebhookViewModel() : ViewModel() {

    private val _webhookConfigState = MutableStateFlow(SettingsWebhookConfigState())
    val webhookConfigState = _webhookConfigState.asStateFlow()

    val appPreferences = CarStatsViewer.appPreferences

    fun setWebhookEndpointUrl(endpointUrl: String) {
        val valid = validateUrlAddress(endpointUrl)
        _webhookConfigState.update { it.copy(
            endpointUrl = endpointUrl,
            endpointUrlValid = valid
        ) }

        if (valid != false) {
            appPreferences.httpLiveDataURL = endpointUrl
        }

        if (valid != true) {
            setWebhookEnabled(false)
        }
    }

    fun setWebhookUserName(userName: String) {
        appPreferences.httpLiveDataUsername = userName
        _webhookConfigState.update { it.copy(
            username = appPreferences.httpLiveDataUsername
        ) }

        if (userName.isBlank()) {
            setWebhookEnabled(false)
        }
    }

    fun setWebhookPassword(password: String) {
        appPreferences.httpLiveDataPassword = password
        _webhookConfigState.update { it.copy(
            password = appPreferences.httpLiveDataPassword
        ) }

        if (password.isBlank()) {
            setWebhookEnabled(false)
        }
    }

    fun setWebhookPasswordVisible(visible: Boolean) {
        _webhookConfigState.update { it.copy(
            passwordVisible = visible
        ) }
    }

    fun setWebhookEnabled(enabled: Boolean) {
        _webhookConfigState.update { it.copy(
            useApi = enabled
        ) }
        appPreferences.httpLiveDataEnabled = enabled
    }

    fun setWebhookLocationTracking(enabled: Boolean) {
        _webhookConfigState.update { it.copy(
            locationTracking = enabled
        ) }
        appPreferences.httpLiveDataLocation = enabled
    }

    fun setWebhookTelemetryType(telemetryType: SettingsWebhookTelemetryType) {
        _webhookConfigState.update { it.copy(
            telemetryType = telemetryType
        ) }
        appPreferences.httpApiTelemetryType = when (telemetryType) {
            SettingsWebhookTelemetryType.RealTime -> 0
            SettingsWebhookTelemetryType.DrivePoints -> 1
            SettingsWebhookTelemetryType.Both -> 2
        }
    }

    fun setWebhookStatusIcon(enabled: Boolean) {
        if (enabled) {
            _webhookConfigState.update { it.copy(
                showStatusIcon = true
            ) }
            appPreferences.mainViewConnectionApi = 1 // TODO: This is ugly ... Rewrite logic for main screen status icon
        }
    }

}