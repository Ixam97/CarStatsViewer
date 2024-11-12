package com.ixam97.carStatsViewer.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.liveDataApi.abrpLiveData.abrpGenericToken
import com.ixam97.carStatsViewer.liveDataApi.abrpLiveData.abrpUseApi
import com.ixam97.carStatsViewer.liveDataApi.abrpLiveData.abrpUseLocation
import com.ixam97.carStatsViewer.liveDataApi.http.httpLiveDataEnabled
import com.ixam97.carStatsViewer.liveDataApi.http.httpLiveDataLocation
import com.ixam97.carStatsViewer.liveDataApi.http.httpLiveDataPassword
import com.ixam97.carStatsViewer.liveDataApi.http.httpLiveDataSendABRPDataset
import com.ixam97.carStatsViewer.liveDataApi.http.httpLiveDataURL
import com.ixam97.carStatsViewer.liveDataApi.http.httpLiveDataUsername

class AbrpSettingsViewModel: ViewModel() {

    private val preferences = CarStatsViewer.appPreferences

    private var valuesChanged = false

    data class AbrpSettingsState(
        val token: String = "",
        val enabled: Boolean = false,
        val useLocation: Boolean = false
    )

    var abrpSettingsState by mutableStateOf(AbrpSettingsState(
        token = preferences.abrpGenericToken,
        enabled = preferences.abrpUseApi,
        useLocation = preferences.abrpUseLocation
    ))
        private set

    fun setToken(newValue: String) {
        abrpSettingsState = abrpSettingsState.copy(
            token = newValue
        )
        valuesChanged = true
    }

    fun setEnabled(newValue: Boolean) {
        abrpSettingsState = abrpSettingsState.copy(
            enabled = newValue
        )
        valuesChanged = true
    }

    fun setUseLocation(newValue: Boolean) {
        abrpSettingsState = abrpSettingsState.copy(
            useLocation = newValue
        )
        valuesChanged = true
    }

    override fun onCleared() {
        if (valuesChanged) {
            abrpSettingsState.apply {
                preferences.abrpGenericToken = token
                preferences.abrpUseApi = enabled
                preferences.abrpUseLocation = useLocation
            }
        }
        super.onCleared()
    }

}

class WebhookSettingsViewModel: ViewModel() {

    private val preferences = CarStatsViewer.appPreferences

    private var valuesChanged = false

    data class WebhookSettingsState(
        val endpointUrl: String = "",
        val userName: String = "",
        val userPassword: String = "",
        val enabled: Boolean = false,
        val useLocation: Boolean = false,
        val debugAbrp: Boolean = false,
        val telemetryType: Int = 0
    )

    var webhookSettingsState by mutableStateOf(WebhookSettingsState(
        endpointUrl = preferences.httpLiveDataURL,
        userName = preferences.httpLiveDataUsername,
        userPassword = preferences.httpLiveDataPassword,
        enabled = preferences.httpLiveDataEnabled,
        useLocation = preferences.httpLiveDataLocation,
        debugAbrp = preferences.httpLiveDataSendABRPDataset,
        telemetryType = preferences.httpApiTelemetryType
    ))
        private set

    fun setUrl(newValue: String) {
        webhookSettingsState = webhookSettingsState.copy(
            endpointUrl = newValue
        )
        valuesChanged = true
    }

    fun setUserName(newValue: String) {
        webhookSettingsState = webhookSettingsState.copy(
            userName = newValue
        )
        valuesChanged = true
    }

    fun setUserPassword(newValue: String) {
        webhookSettingsState = webhookSettingsState.copy(
            userPassword = newValue
        )
        valuesChanged = true
    }

    fun setEnabled(newValue: Boolean) {
        webhookSettingsState = webhookSettingsState.copy(
            enabled = newValue
        )
        valuesChanged = true
    }

    fun setUseLocation(newValue: Boolean) {
        webhookSettingsState = webhookSettingsState.copy(
            useLocation = newValue
        )
        valuesChanged = true
    }

    fun setDebugAbrp(newValue: Boolean) {
        webhookSettingsState = webhookSettingsState.copy(
            debugAbrp = newValue
        )
        valuesChanged = true
    }

    fun setTelemetryType(newValue: Int) {
        webhookSettingsState = webhookSettingsState.copy(
            telemetryType = newValue
        )
        valuesChanged = true
    }

    override fun onCleared() {
        if (valuesChanged) {
            webhookSettingsState.apply {
                preferences.httpLiveDataURL = endpointUrl
                preferences.httpLiveDataUsername = userName
                preferences.httpLiveDataPassword = userPassword
                preferences.httpLiveDataEnabled = enabled
                preferences.httpLiveDataLocation = useLocation
                preferences.httpLiveDataSendABRPDataset = debugAbrp
                preferences.httpApiTelemetryType = telemetryType
            }
        }
        super.onCleared()
    }
}