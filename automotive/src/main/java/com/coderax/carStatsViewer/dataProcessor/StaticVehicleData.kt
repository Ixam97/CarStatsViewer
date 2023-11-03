package com.coderax.carStatsViewer.dataProcessor

import com.coderax.carStatsViewer.utils.DistanceUnitEnum

data class StaticVehicleData(
    val batteryCapacity: Float? = null,
    val vehicleMake: String? = null,
    val modelName: String? = null,
    val distanceUnit: DistanceUnitEnum = DistanceUnitEnum.KM
) {
    fun isInitialized(): Boolean = batteryCapacity != null
}
