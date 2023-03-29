package com.ixam97.carStatsViewer.dataProcessor

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class TripDataManager {

    private var drivingTripData = DrivingTripData()
        set(value) {
            field = value
            _drivingTripDataFlow.value = value
        }

    private var chargingTripData = ChargingTripData()
        set(value) {
            field = value
            _chargingTripDataFlow.value = field
        }

    private val _drivingTripDataFlow = MutableStateFlow(drivingTripData)
    val drivingTripDataFlow = _drivingTripDataFlow.asStateFlow()

    private val _chargingTripDataFlow = MutableStateFlow(chargingTripData)
    val chargingTripDataFlow = _chargingTripDataFlow.asStateFlow()

    fun newDrivingState(drivingState: Int) {

    }

    fun newDrivingDeltas(distanceDelta: Double, energyDelta: Double) {
        val currentDrivingDistance = drivingTripData.drivenDistance + distanceDelta
        val currentDrivingEnergy = drivingTripData.usedEnergy + energyDelta

        drivingTripData = drivingTripData.copy(
            drivenDistance = currentDrivingDistance,
            usedEnergy = currentDrivingEnergy
        )

        // Update database
    }

    fun newChargingDeltas(energyDelta: Double) {
        val currentChargingEnergy = chargingTripData.chargedEnergy + energyDelta

        chargingTripData = chargingTripData.copy(
            chargedEnergy = currentChargingEnergy
        )

        // Update database
    }
}