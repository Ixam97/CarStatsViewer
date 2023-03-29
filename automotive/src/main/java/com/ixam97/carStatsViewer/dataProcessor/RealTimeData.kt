package com.ixam97.carStatsViewer.dataProcessor

import android.car.VehicleIgnitionState
import com.ixam97.carStatsViewer.dataManager.DrivingState

data class RealTimeData(
    val speed: Float = 0f,
    val power: Float = 0f,
    val selectedGear: Int = 0,
    val ignitionState: Int = 0,
    val chargePortConnected: Boolean = false,
    val batteryLevel: Float = 0f,
    val stateOfCharge: Float = 0f,
    val ambientTemperature: Float = 0f,
    val lat: Float? = null,
    val lon: Float? = null,
    val alt: Float? = null
) {
    val drivingState: Int get() = getDriveState()
    val instConsumption: Float? get() = getInstCons()
    val optimizeDistraction: Boolean get() = speed > 0.0

    private fun getDriveState(): Int {
        return if (chargePortConnected) DrivingState.CHARGE
        else if (ignitionState == VehicleIgnitionState.START) DrivingState.DRIVE
        else if (ignitionState != VehicleIgnitionState.UNDEFINED) DrivingState.PARKED
        else DrivingState.UNKNOWN
    }

    private fun getInstCons(): Float? {
        val instCons = (power / 1_000f) / (speed * 3.6f)
        return if (instCons.isFinite()) instCons else null
    }
}
