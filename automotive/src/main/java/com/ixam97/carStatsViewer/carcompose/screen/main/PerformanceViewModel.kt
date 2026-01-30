package com.ixam97.carStatsViewer.carcompose.screen.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ixam97.carStatsViewer.CarStatsViewer
import kotlinx.coroutines.launch

class PerformanceViewModel: ViewModel() {
    data class PerformanceState(
        val power: Float = 0f,
        val speed: Float = 0f
    )

    var performanceState by mutableStateOf(PerformanceState())
        private set

    init {
        viewModelScope.launch {
            CarStatsViewer.dataProcessor.realTimeDataFlow.collect {
                performanceState = performanceState.copy(
                    power = it.power?:0f,
                    speed = it.speed?:0f
                )
            }
        }
    }
}