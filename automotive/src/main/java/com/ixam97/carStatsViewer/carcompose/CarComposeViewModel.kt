package com.ixam97.carStatsViewer.carcompose

import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.utils.InAppLogger
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

enum class MainScreenTab {
    Dashboard,
    History,
    Settings;
}

enum class VehicleModel {
    Polestar2,
    Polestar3,
    Polestar4,
    Other
}

class CarComposeViewModel: ViewModel() {

    data class CarComposeState(
        val uiTypeIndex: Int,
        val vehicleModel: VehicleModel = VehicleModel.Other,
        val isLoading: Boolean = false,
        val selectedMainScreenTab: MainScreenTab = MainScreenTab.Dashboard
    )

    var movingState by mutableStateOf(false)
        private set

    var carComposeState by mutableStateOf(
        value = when {
            Build.MODEL == "PS4" || Build.DEVICE == "lemon_x86_64" -> {
                CarComposeState(
                    uiTypeIndex = 1,
                    vehicleModel = VehicleModel.Polestar4
                )
            }
            Build.MODEL == "Polestar" && Build.DEVICE == "moose" -> {
                CarComposeState(
                    uiTypeIndex = 1,
                    vehicleModel = VehicleModel.Polestar3
                )
            }
            ((Build.MODEL == "Polestar" && Build.DEVICE == "ihu_abl_car") || Build.MODEL == "Polestar 2") -> {
                CarComposeState(
                    uiTypeIndex = 2,
                    vehicleModel = VehicleModel.Polestar2
                )
            }
            else -> CarComposeState(
                uiTypeIndex = 0
            )
        }
    )
        private set

    init {
        viewModelScope.launch {
            CarStatsViewer.dataProcessor.realTimeDataFlow.collect { realTimeData ->
                movingState = (realTimeData.speed != null && realTimeData.speed.absoluteValue > 0)
                if (movingState && carComposeState.selectedMainScreenTab != MainScreenTab.Dashboard) {
                    carComposeState = carComposeState.copy(selectedMainScreenTab = MainScreenTab.Dashboard)
                }
            }
        }
    }

    fun setUiTypeIndex(index: Int) {
        val newIndex = if (index > 3) 3 else index
        carComposeState = carComposeState.copy(
            uiTypeIndex = newIndex
        )
    }

    fun setLoading(isLoading: Boolean) {
        carComposeState = carComposeState.copy(
            isLoading = isLoading
        )
        InAppLogger.v("Setting Loading state to $isLoading")
    }

    fun setSelectedMainScreenTabIndex(tabIndex: Int) {
        if (MainScreenTab.entries.size - 1 < tabIndex)
            throw RuntimeException("Invalid MainScreenTab with index $tabIndex!")
        carComposeState = carComposeState.copy(
            selectedMainScreenTab = MainScreenTab.entries[tabIndex]
        )
    }
}