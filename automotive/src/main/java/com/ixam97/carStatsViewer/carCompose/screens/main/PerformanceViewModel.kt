package com.ixam97.carStatsViewer.carCompose.screens.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.dataProcessor.IgnitionState
import com.ixam97.carStatsViewer.utils.StringFormatters
import kotlinx.coroutines.launch
import java.text.DecimalFormat

class PerformanceViewModel: ViewModel() {
    data class PerformanceState(
        val speed: Float? = null,
        val power: Float? = null,
        val selectedGear: Int? = null,
        val ignitionState: Int? = null,
        val chargePortConnected: Boolean? = null,
        val batteryLevel: Float? = null,
        val stateOfCharge: Float? = null,
        val ambientTemperature: Float? = null,
        val avgBatteryTemperature: Float? = null,
    ) {
        private val decFormat = DecimalFormat("#.##")
        fun getDataStateStrings(): List<String> {
            return listOf(
                "Speed: ${
                    if (speed == null) "Unavailable"
                    else decFormat.format(speed * 3.6f) + " km/h"
                }",
                "Power: ${
                    if (power == null) "Unavailable"
                    else decFormat.format(power / 1_000_000f) + " kW"
                }",
                "Selected gear: ${
                    if (selectedGear == null) "Unavailable" 
                    else StringFormatters.getGearString(selectedGear)
                }",
                "Ignition State: ${IgnitionState.nameMap[ignitionState] ?: "Unavailable"}",
                "Charge port connected: ${chargePortConnected ?: "Unavailable"}",
                "Battery level: ${
                    if (batteryLevel == null) "Unavailable"
                    else "$batteryLevel Wh"
                }",
                "State of Charge: ${
                    if (stateOfCharge == null) "Unavailable"
                    else decFormat.format(stateOfCharge * 100) + "%"
                }",
                "Ambient temperature: ${
                    if (ambientTemperature == null) "Unavailable"
                    else decFormat.format(ambientTemperature) + "°C"
                }",
                "Average battery temperature: ${
                    if (avgBatteryTemperature == null) "Unavailable"
                    else decFormat.format(ambientTemperature) + "°C"
                }"
            )
        }
    }

    var performanceState by mutableStateOf(PerformanceState())
        private set

    init {
        viewModelScope.launch {
            CarStatsViewer.dataProcessor.realTimeDataFlow.collect {
                performanceState = performanceState.copy(
                    speed = it.speed,
                    power = it.power,
                    selectedGear = it.selectedGear,
                    ignitionState = it.ignitionState,
                    chargePortConnected = it.chargePortConnected,
                    batteryLevel = it.batteryLevel,
                    stateOfCharge = it.stateOfCharge,
                    ambientTemperature = it.ambientTemperature,
                    avgBatteryTemperature = it.avgBatteryTemperature
                )
            }
        }
    }
}