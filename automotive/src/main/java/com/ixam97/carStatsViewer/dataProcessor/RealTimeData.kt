package com.ixam97.carStatsViewer.dataProcessor

import android.car.VehicleGear
import android.car.VehicleIgnitionState

data class RealTimeData(
    val speed: Float? = null,
    val power: Float? = null,
    val selectedGear: Int? = null,
    val ignitionState: Int? = null,
    val chargePortConnected: Boolean? = null,
    val batteryLevel: Float? = null,
    val stateOfCharge: Float? = null,
    val ambientTemperature: Float? = null,
    val lat: Float? = null,
    val lon: Float? = null,
    val alt: Float? = null
) {
    val drivingState: Int get() = getDriveState()
    val instConsumption: Float? get() = getInstCons()
    val optimizeDistraction: Boolean get() = speed != null && speed > 0.0

    fun isInitialized(): Boolean {
        return speed != null
                && power != null
                && selectedGear != null
                && ignitionState != null
                && chargePortConnected != null
                && batteryLevel != null
                && stateOfCharge != null
                && ambientTemperature  != null
    }

    private fun getDriveState(): Int {
        return if (chargePortConnected == true) DrivingState.CHARGE
        else if (ignitionState == VehicleIgnitionState.START || (ignitionState == VehicleIgnitionState.ON && selectedGear == VehicleGear.GEAR_DRIVE)) DrivingState.DRIVE
        else if (ignitionState != VehicleIgnitionState.UNDEFINED && ignitionState != null) DrivingState.PARKED
        else DrivingState.UNKNOWN
    }

    private fun getInstCons(): Float? {
        if (power == null || speed == null) return null
        val instCons = (power / 1_000f) / (speed * 3.6f)
        return if (instCons.isFinite()) instCons else null
    }
}
