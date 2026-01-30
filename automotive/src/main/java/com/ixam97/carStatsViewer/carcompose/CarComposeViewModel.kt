package com.ixam97.carStatsViewer.carcompose

import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.utils.InAppLogger
import de.ixam97.carcompose.components.layout.CarTabLayout
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

object TabOrientation {
    const val VERTICAL = 0
    const val HORIZONTAL = 1
}

object TabIndexes {
    const val TRIP = 0
    const val PERFORMANCE = 1
    const val HISTORY = 2
}

class CarComposeViewModel: ViewModel() {

    data class CarComposeState(
        val uiTypeIndex: Int,
        val tabLayoutOrientation: CarTabLayout.Orientation = CarTabLayout.Orientation.Vertical,
        val isLoading: Boolean = false,
        val selectedTab: Int = TabIndexes.TRIP

    )

    var movingState by mutableStateOf(false)
        private set

    var carComposeState by mutableStateOf(
        value = when {
            Build.MODEL == "PS4" || Build.DEVICE == "lemon_x86_64" -> {
                CarComposeState(
                    tabLayoutOrientation = CarTabLayout.Orientation.Vertical,
                    uiTypeIndex = 1
                )
            }
            Build.MODEL == "Polestar" && Build.DEVICE == "moose" -> {
                CarComposeState(
                    tabLayoutOrientation = CarTabLayout.Orientation.Horizontal,
                    uiTypeIndex = 1
                )
            }
            ((Build.MODEL == "Polestar" && Build.DEVICE == "ihu_abl_car") || Build.MODEL == "Polestar 2") -> {
                CarComposeState(
                    tabLayoutOrientation = CarTabLayout.Orientation.Horizontal,
                    uiTypeIndex = 2
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
                if (movingState && carComposeState.selectedTab > 1) {
                    carComposeState = carComposeState.copy(selectedTab = 0)
                }
            }
        }
    }

    fun setUiTypeIndex(index: Int) {
        carComposeState = carComposeState.copy(
            uiTypeIndex = index
        )
    }

    fun setLoading(isLoading: Boolean) {
        carComposeState = carComposeState.copy(
            isLoading = isLoading
        )
        InAppLogger.v("Setting Loading state to $isLoading")
    }

    fun setSelectedTab(tabIndex: Int) {
        carComposeState = carComposeState.copy(
            selectedTab = tabIndex
        )
    }

    fun setTabOrientationIndex(tabOrientationIndex: Int) {
        carComposeState = carComposeState.copy(
            tabLayoutOrientation = if (tabOrientationIndex == TabOrientation.VERTICAL) {
                CarTabLayout.Orientation.Vertical
            } else {
                CarTabLayout.Orientation.Horizontal
            }
        )
    }
}