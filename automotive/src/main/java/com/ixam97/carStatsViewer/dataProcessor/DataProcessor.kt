package com.ixam97.carStatsViewer.dataProcessor

import android.util.Log
import com.ixam97.carStatsViewer.carPropertiesClient.CarProperties
import com.ixam97.carStatsViewer.carPropertiesClient.CarPropertiesData
import com.ixam97.carStatsViewer.carPropertiesClient.CarProperty
import com.ixam97.carStatsViewer.utils.InAppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.absoluteValue

class DataProcessor {
    val carPropertiesData = CarPropertiesData()
    var drivenDistance: Double = 0.0

    var staticVehicleData = StaticVehicleData()
    var realTimeData = RealTimeData()
        set(value) {
            field = value
            _realTimeDataFlow.value = value
        }

    private val _realTimeDataFlow = MutableStateFlow<RealTimeData>(realTimeData)
    val realTimeDataFlow = _realTimeDataFlow.asStateFlow()

    fun processProperty(carProperty: Int) {

        realTimeData = realTimeData.copy(
            speed = (carPropertiesData.CurrentSpeed.value as Float?)?: 0f,
            power = (carPropertiesData.CurrentPower.value as Float?)?: 0f,
            batteryLevel = (carPropertiesData.BatteryLevel.value as Float?)?: 0f,
            stateOfCharge = ((carPropertiesData.BatteryLevel.value as Float?)?: 0f) / staticVehicleData.batteryCapacity!!,
            ambientTemperature = (carPropertiesData.CurrentAmbientTemperature.value as Float?)?: 0f,
            selectedGear = (carPropertiesData.CurrentGear.value as Int?)?: 0,
            ignitionState = (carPropertiesData.CurrentIgnitionState.value as Int?)?: 0,
            chargePortConnected = (carPropertiesData.ChargePortConnected.value as Boolean?)?: false
        )

        when (carProperty) {
            CarProperties.PERF_VEHICLE_SPEED -> {
                if (carPropertiesData.CurrentSpeed.timeDelta > 0) {
                    val distanceDelta = (carPropertiesData.CurrentSpeed.value as Float).absoluteValue * (carPropertiesData.CurrentSpeed.timeDelta / 1_000_000_000f)
                    drivenDistance += distanceDelta
                    //Log.v("Driven Distance","${carPropertiesData.CurrentSpeed.value as Float}m/s * ${(carPropertiesData.CurrentSpeed.timeDelta.toFloat() / 1_000_000_000f)}s = ${drivenDistance.toFloat()}m")
                    // Log.v("Neo", "${distanceDelta}m, ${carPropertiesData.CurrentSpeed.timeDelta / 1_000_000_000f}s")
                    // Log.v("Neo", "${drivenDistance}m")
                }
            }
        }
    }
}