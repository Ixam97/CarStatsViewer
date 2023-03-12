package com.ixam97.carStatsViewer.abrpLiveData

data class AbrpDataSet(
    val stateOfCharge: Int,
    val power: Float,
    val isCharging: Boolean,
    val speed: Float,
    val isParked: Boolean,
    val lat: Double?,
    val lon: Double?,
    val alt: Double?,
    val temp: Float
)
