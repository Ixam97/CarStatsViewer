package com.ixam97.carStatsViewer.compose

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.app
import com.google.gson.Gson
import com.ixam97.carStatsViewer.BuildConfig
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.database.log.LogEntry
import com.ixam97.carStatsViewer.liveDataApi.ConnectionStatus
import com.ixam97.carStatsViewer.repository.logSubmit.LogSubmitBody
import com.ixam97.carStatsViewer.ui.views.SnackbarWidget
import com.ixam97.carStatsViewer.utils.DistanceUnitEnum
import com.ixam97.carStatsViewer.utils.InAppLogger
import com.ixam97.carStatsViewer.utils.logLength
import com.ixam97.carStatsViewer.utils.logLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel:
    ViewModel()
{
    companion object {
        val logLengths = arrayOf(0, 500, 1000, 2000, 5000, 10_000)
    }

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

    data class DevSettingsState(
        val distanceUnit: DistanceUnitEnum = DistanceUnitEnum.KM,
        val showScreenshotButton: Boolean = false,
        val loggingLevel: Int = 0,
        val logLength: Int = 0,
        val debugDelays: Boolean = false,
        val debugColors: Boolean = false,
    )

    data class ApiSettingsState(
        val abrpStatus: ConnectionStatus = ConnectionStatus.UNUSED,
        val httpStatus: ConnectionStatus = ConnectionStatus.UNUSED
    )

    private val _themeSettingState = MutableStateFlow<Int>(preferences.colorTheme)
    val themeSettingStateFLow = _themeSettingState.asStateFlow()

    private val _finishActivityLiveData = MutableLiveData<Event<Boolean>>()
    val finishActivityLiveData: LiveData<Event<Boolean>> = _finishActivityLiveData

    var isDevEnabled by mutableStateOf(BuildConfig.FLAVOR_version == "dev")
        private set

    var settingsState by mutableStateOf(SettingsState())
        private set

    var devSettingsState by mutableStateOf(DevSettingsState())
        private set

    var log by mutableStateOf<List<LogEntry>?>(null)
        private set

    var apiSettingsState by mutableStateOf(ApiSettingsState())
        private set

    private var versionClickedNum: Int = 0

    fun initStates() {
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
                devSettingsState = devSettingsState.copy(
                    distanceUnit = preferences.distanceUnit,
                    showScreenshotButton = preferences.showScreenshotButton,
                    loggingLevel = preferences.logLevel,
                    logLength = preferences.logLength,
                    debugDelays = preferences.debugDelays,
                    debugColors = preferences.debugColors
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

    init {
        initStates()

        viewModelScope.launch {
            CarStatsViewer.watchdog.watchdogStateFlow.collect {
                apiSettingsState = apiSettingsState.copy(
                    abrpStatus = ConnectionStatus.fromInt(it.apiState[CarStatsViewer.liveDataApis[0].apiIdentifier]?:0),
                    httpStatus = ConnectionStatus.fromInt(it.apiState[CarStatsViewer.liveDataApis[1].apiIdentifier]?:0),
                )
            }
        }

    }

    fun finishActivity() = _finishActivityLiveData.postValue(Event(true))

    fun setTheme(themeIndex: Int) {
        _themeSettingState.update { themeIndex }
        preferences.colorTheme = themeIndex
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

    fun openGitHubLink(context: Context) {
        val url = context.getString(R.string.readme_link)
        openLink(context, url)
    }

    fun openGitHubIssuesLink(context: Context) {
        val url = context.getString(R.string.github_issues_link)
        openLink(context, url)
    }

    fun openClubLink(context: Context) {
        val url = context.getString(R.string.polestar_fans_link)
        openLink(context, url)
    }

    fun openForumsLink(context: Context) {
        val url = context.getString(R.string.polestar_forum_link)
        openLink(context, url)
    }

    fun openPrivacyLink(context: Context) {
        val url = context.getString(R.string.privacy_policy_link)
        openLink(context, url)
    }

    private fun openLink(context: Context, url: String) {
        context.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(url)
            )
        )
    }

    fun setDebugDelays(newState: Boolean) {
        preferences.debugDelays = newState
        devSettingsState = devSettingsState.copy(
            debugDelays = preferences.debugDelays
        )
    }

    fun setDebugColors(newState: Boolean) {
        preferences.debugColors = newState
        devSettingsState = devSettingsState.copy(
            debugColors = preferences.debugColors
        )
    }

    fun setDistanceUnit(newState: Boolean) {
        preferences.distanceUnit = when (newState) {
            true -> DistanceUnitEnum.MILES
            else -> DistanceUnitEnum.KM
        }
        devSettingsState = devSettingsState.copy(
            distanceUnit = preferences.distanceUnit
        )
    }

    fun setShowScreenshotButton(newState: Boolean) {
        preferences.showScreenshotButton = newState
        devSettingsState = devSettingsState.copy(
            showScreenshotButton = preferences.showScreenshotButton
        )
    }

    fun setLoggingLevel(index: Int) {
        preferences.logLevel = index
        devSettingsState = devSettingsState.copy(
            loggingLevel = preferences.logLevel
        )
    }

    fun setLogLength(index: Int) {
        preferences.logLength = index
        devSettingsState = devSettingsState.copy(
            logLength = preferences.logLength
        )
    }

    fun submitLog() {
        viewModelScope.launch {
            withContext(Dispatchers.IO){

                val submitMap = mutableMapOf<Long, String>()

                InAppLogger.getLogEntries(
                    logLevel = preferences.logLevel + 2,
                    logLength = logLengths[preferences.logLength]
                ).forEach { logEntry ->
                    submitMap[logEntry.epochTime] = "${InAppLogger.typeSymbol(logEntry.type)}: ${logEntry.message}"
                }
                Log.d("Log submit debug", Gson().toJson(LogSubmitBody(submitMap)))
                // LogSubmitRepository.submitLog(LogSubmitBody(submitMap))
            }
        }
    }

    fun loadLog() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                log = null
                log = InAppLogger.getLogEntries(
                    logLevel = preferences.logLevel + 2, // add 2 to align with log level definitions
                    logLength = logLengths[preferences.logLength]
                )
            }
        }
    }

    fun clearLog() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                InAppLogger.resetLog()
            }
        }
    }

}