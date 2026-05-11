package com.ixam97.carStatsViewer.carCompose

import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ixam97.carStatsViewer.BuildConfig
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.utils.InAppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

enum class VehicleModel {
    Polestar2,
    Polestar3,
    Polestar4,
    Volvo,
    Other
}

enum class UiType {
    Auto, Classic, Modern, Club, Generic, Volvo
}

enum class UiBrightnessMode {
    Auto, Dark, Bright
}

internal fun getVehicleModel(): VehicleModel {
    return when {
        Build.MODEL == "PS4" || Build.DEVICE == "lemon_x86_64" -> VehicleModel.Polestar4
        Build.MODEL == "Polestar" && Build.DEVICE == "moose" -> VehicleModel.Polestar3
        ((Build.MODEL == "Polestar" && Build.DEVICE == "ihu_abl_car") || Build.MODEL == "Polestar 2") -> VehicleModel.Polestar2
        Build.BRAND == "VolvoCars" -> VehicleModel.Volvo
        else -> VehicleModel.Other
    }
}

internal fun getDefaultBrightnessMode(): UiBrightnessMode {
    return when {
        Build.BRAND == "VolvoCars" && (Build.DEVICE == "ihu_abl_car" || Build.DEVICE == "ihu_emulator") -> UiBrightnessMode.Dark
        else -> UiBrightnessMode.Auto
    }
}

val vehicleUiTypeMap = mapOf(
    VehicleModel.Polestar4 to UiType.Modern,
    VehicleModel.Polestar3 to UiType.Modern,
    VehicleModel.Polestar2 to UiType.Classic,
    VehicleModel.Volvo to UiType.Volvo,
).withDefault { UiType.Generic }

data class GlobalState(
    val vehicleModel: VehicleModel = getVehicleModel(),
    val uiType: UiType = if (CarStatsViewer.appPreferences.carComposeTheme == UiType.Auto) vehicleUiTypeMap.getValue(vehicleModel) else CarStatsViewer.appPreferences.carComposeTheme,
    val devModeEnabled: Boolean = BuildConfig.FLAVOR_version == "dev",
    val isLoading: Boolean = false,
    val uiSupportsBrightMode: Boolean = false,
    val uiBrightnessMode: UiBrightnessMode = getDefaultBrightnessMode()
)

class CarComposeGlobalViewModel: ViewModel() {

    private var _globalState = MutableStateFlow(GlobalState())
    val globalState = _globalState.asStateFlow()

    var movingState by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            CarStatsViewer.dataProcessor.realTimeDataFlow.collect { realTimeData ->
                movingState = (realTimeData.speed != null && realTimeData.speed.absoluteValue > 0)
//                if (movingState && carComposeState.selectedMainScreenTab != MainScreenTab.Dashboard) {
//                    carComposeState = carComposeState.copy(selectedMainScreenTab = MainScreenTab.Dashboard)
//                }
            }
        }
    }

    fun setUiType(uiType: UiType?) {
        CarStatsViewer.appPreferences.carComposeTheme = uiType?: UiType.Auto
        _globalState.update { it.copy(uiType = uiType?:vehicleUiTypeMap.getValue(it.vehicleModel)) }
    }

    fun setLoading(isLoading: Boolean) {
        _globalState.update { it.copy(isLoading = isLoading) }
        InAppLogger.v("Setting Loading state to $isLoading")
    }

    fun setDevModeEnabled(enabled: Boolean) {
        _globalState.update { it.copy(devModeEnabled = enabled) }
    }

    fun setUiSupportsBrightMode(value: Boolean) {
        _globalState.update { it.copy(uiSupportsBrightMode = value) }
    }

    fun setUiBrightnessMode(value: UiBrightnessMode) {
        _globalState.update { it.copy(uiBrightnessMode = value) }
    }

//    fun setSelectedMainScreenTabIndex(tabIndex: Int) {
//        if (MainScreenTab.entries.size - 1 < tabIndex)
//            throw RuntimeException("Invalid MainScreenTab with index $tabIndex!")
//        carComposeState = carComposeState.copy(
//            selectedMainScreenTab = MainScreenTab.entries[tabIndex]
//        )
//    }
}