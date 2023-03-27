package com.ixam97.carStatsViewer.dataProcessor

data class RealTimeData(
    val speed: Float = 0f,
    val power: Float = 0f,
    val instConsumption: Float? = null,
    val selectedGear: Int = 0,
    val ignitionState: Int = 0,
    val driveState: Int = 0,
    val chargePortConnected: Boolean = false,
    val batteryLevel: Float = 0f,
    val stateOfCharge: Float = 0f,
    val ambientTemperature: Float = 0f,
    val lat: Float? = null,
    val lon: Float? = null,
    val alt: Float? = null
)
