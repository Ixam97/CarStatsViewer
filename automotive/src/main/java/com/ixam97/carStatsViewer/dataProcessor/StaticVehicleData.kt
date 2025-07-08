package com.ixam97.carStatsViewer.dataProcessor

import com.ixam97.carStatsViewer.utils.DistanceUnitEnum

data class StaticVehicleData(
    val batteryCapacity: Float? = null,
    val vehicleMake: String? = null,
    val modelName: String? = null,
    val distanceUnit: DistanceUnitEnum? = null
) {
    fun isInitialized(): Boolean =
        isEssentialInitialized() && isOptionalInitialized()

    fun isEssentialInitialized(): Boolean =
        batteryCapacity != null

    fun isOptionalInitialized(): Boolean =
        vehicleMake != null
                && modelName != null
                && distanceUnit != null
}
