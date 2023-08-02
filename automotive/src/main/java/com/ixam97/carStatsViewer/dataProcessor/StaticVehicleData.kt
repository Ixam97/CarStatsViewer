package com.ixam97.carStatsViewer.dataProcessor

import com.ixam97.carStatsViewer.utils.DistanceUnitEnum

data class StaticVehicleData(
    val batteryCapacity: Float? = null,
    val vehicleMake: String? = null,
    val modelName: String? = null,
    val distanceUnit: DistanceUnitEnum = DistanceUnitEnum.KM
) {
    fun isInitialized(): Boolean = batteryCapacity != null
}
