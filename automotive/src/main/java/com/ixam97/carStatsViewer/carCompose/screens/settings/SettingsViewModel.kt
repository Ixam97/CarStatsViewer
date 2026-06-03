package com.ixam97.carStatsViewer.carCompose.screens.settings

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Patterns
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.app
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.liveDataApi.ConnectionStatus
import com.ixam97.carStatsViewer.repository.logSubmit.LogSubmitRepository
import com.ixam97.carStatsViewer.utils.DistanceUnitEnum
import com.ixam97.carStatsViewer.utils.InAppLogger
import com.ixam97.carStatsViewer.utils.ScreenshotService
import com.ixam97.carStatsViewer.utils.logLength
import com.ixam97.carStatsViewer.utils.logLevel
import de.ixam97.carcompose.components.layout.CarSnackBarConfig
import de.ixam97.carcompose.components.layout.CarSnackBarHostState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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
    val distanceUnit: DistanceUnitEnum = CarStatsViewer.appPreferences.distanceUnit,
    val userId: String = CarStatsViewer.appPreferences.debugUserID,
    val userMail: String = CarStatsViewer.appPreferences.debugScreenshotReceiver,
    val userMailValid: Boolean? = validateEmailAddress(userMail),
    val numberOfScreenshots: Int = ScreenshotService.screenshotServiceState.value.numberOfScreenshots,
    val screenshotServiceRunning: Boolean = ScreenshotService.screenshotServiceState.value.isServiceRunning,
    val logLevelKey: LogLevelKey = LogLevelKey.entries[CarStatsViewer.appPreferences.logLevel],
    val logLengthKey: LogLengthKey = LogLengthKey.entries[CarStatsViewer.appPreferences.logLength]
)

enum class LogLevelKey {
    Verbose, Debug, Info, Warning, Error
}

enum class LogLengthKey(val length: Int) {
    All(0), L500(500), L1000(1000), L2000(2000), L5000(5000), L10000(10000)
}

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

    //region About
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
    //endregion

    //region Developer Settings
    private var _settingsDevState = MutableStateFlow(SettingsDevState())
    val settingsDevState = _settingsDevState.asStateFlow()

    fun setLoadingDelays(value: Boolean) {
        appPreferences.debugDelays = value
        _settingsDevState.update { it.copy(loadingDelays = appPreferences.debugDelays) }
    }

    fun setAdditionalColorSchemes(value: Boolean) {
        appPreferences.debugColors = value
        _settingsDevState.update { it.copy(additionalColorSchemes = appPreferences.debugColors) }
    }

    fun setDistanceUnit(value: DistanceUnitEnum) {
        appPreferences.distanceUnit = value
        _settingsDevState.update { it.copy(distanceUnit = appPreferences.distanceUnit) }
    }

    fun debugCrash(context: Context) {
        AlertDialog.Builder(context).apply {
            setTitle("Debug Crash")
            setMessage("This action will deliberately crash the App. Use for debugging purposes only!")
            setPositiveButton("Confirm") {_,_ ->
                CarStatsViewer.debugCrash()
            }
            setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
            create()
        }.show()
    }

    fun scanAvailableFonts() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                File("/product/fonts").apply {
                    if (exists() && isDirectory) {
                        var fontsList = "Found product fonts:\n\r"
                        this.listFiles()?.forEach { file ->
                            fontsList += "    ${file.name}\n\r"
                        }
                        InAppLogger.d(fontsList)
                    }
                }
                File("/system/fonts").apply {
                    if (exists() && isDirectory) {
                        var fontsList = "Found system fonts:\n\r"
                        this.listFiles()?.forEach { file ->
                            fontsList += "    ${file.name}\n\r"
                        }
                        InAppLogger.d(fontsList)
                    }
                }
            }
        }
    }

    fun setDevUserId(userId: String) {
        appPreferences.debugUserID = userId
        _settingsDevState.update { it.copy(
            userId = userId
        ) }
    }

    fun setDevUserMail(address: String) {
        val validAddress = validateEmailAddress(address)

        when (validAddress) {
            true -> {
                appPreferences.debugScreenshotReceiver = address
            }
            null -> {
                appPreferences.debugScreenshotReceiver = ""
            }
            else -> { } //Invalid address
        }

        _settingsDevState.update { it.copy(
            userMail = address,
            userMailValid = validAddress
        ) }
    }

    fun submitScreenshots(
        snackBarState: CarSnackBarHostState
    ) {
        val submittedScreenshots = settingsDevState.value.numberOfScreenshots
        if (submittedScreenshots == 0) return

        val snackBarIdentifier = "SubmitScreenshotsSnackBar"
        snackBarState.showSnackBar(CarSnackBarConfig(
            identifier = snackBarIdentifier,
            content = { Text("Submitting screenshots...") },
            drawableResId = R.drawable.ic_camera,
            duration = 0,
            continuousLoading = true
        ))

        viewModelScope.launch(Dispatchers.IO) {
            var resultMsg: String?
            try {
                resultMsg = LogSubmitRepository.uploadImage(
                    bitmaps = ScreenshotService.screenshotsList,
                    additionalAddress = appPreferences.debugScreenshotReceiver.ifBlank { null }
                )
            } catch (e: Exception) {
                InAppLogger.e("Failed to submit screenshots: ${e.message}\n\r${e.stackTraceToString()}")
                resultMsg = e.message
            }
            delay(500)
            withContext(Dispatchers.Main) {
                if (resultMsg == null) {
                    snackBarState.showSnackBar(CarSnackBarConfig(
                        identifier = snackBarIdentifier,
                        content = {Text("$submittedScreenshots screenshots submitted successfully.") },
                        duration = 3000,
                        drawableResId = R.drawable.ic_checkmark
                    ))
                    ScreenshotService.clearScreenshots()
                } else {
                    snackBarState.showSnackBar(CarSnackBarConfig(
                        identifier = snackBarIdentifier,
                        content = { Text(resultMsg) },
                        isError = true,
                        duration = 5000,
                        drawableResId = R.drawable.ic_error
                    ))
                }
            }
        }
    }

    fun setLogLevel(levelKey: LogLevelKey?) {
        appPreferences.logLevel = levelKey?.ordinal?:appPreferences.logLevel
        _settingsDevState.update { it.copy(
            logLevelKey = LogLevelKey.entries[appPreferences.logLevel]
        ) }
    }

    fun setLogLength(lengthKey: LogLengthKey?) {
        appPreferences.logLength = lengthKey?.ordinal?:appPreferences.logLength
        _settingsDevState.update { it.copy(
            logLengthKey = LogLengthKey.entries[appPreferences.logLength]
        ) }
    }

    fun clearLog(context: Context) {
        AlertDialog.Builder(context).apply {
            setTitle("Delete log")
            setMessage("Are you sure you want to delete the debug log?")
            setPositiveButton("Confirm") {_,_ ->
                viewModelScope.launch {
                    withContext(Dispatchers.IO) {
                        InAppLogger.resetLog()
                    }
                }
            }
            setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
            create()
        }.show()
    }

    fun submitLog(snackBarState: CarSnackBarHostState) {
        val snackBarIdentifier = "SubmitLogSnackBar"
        snackBarState.showSnackBar(CarSnackBarConfig(
            identifier = snackBarIdentifier,
            content = { Text("Submitting debug logs...") },
            drawableResId = R.drawable.ic_debug,
            duration = 0,
            continuousLoading = true
        ))
        viewModelScope.launch {
            withContext(Dispatchers.IO){
                val resultMessage = LogSubmitRepository.submitLog(
                    additionalAddress = appPreferences.debugScreenshotReceiver.ifBlank { null }
                )
                delay(500)
                withContext(Dispatchers.Main) {
                    if (resultMessage == null) {
                        snackBarState.showSnackBar(CarSnackBarConfig(
                            identifier = snackBarIdentifier,
                            content = { Text("Debug Logs submitted successfully.") },
                            drawableResId = R.drawable.ic_checkmark,
                            duration = 3000
                        ))
                    } else {
                        snackBarState.showSnackBar(CarSnackBarConfig(
                            identifier = snackBarIdentifier,
                            content = { Text("Failed to submit log!\n$resultMessage") },
                            drawableResId = R.drawable.ic_error,
                            duration = 5000
                        ))
                    }
                }
            }
        }
    }

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

internal fun validateUrlAddress(url: String): Boolean? {
    if (url.isBlank()) return null
    return Patterns.WEB_URL.matcher(url).matches()
}