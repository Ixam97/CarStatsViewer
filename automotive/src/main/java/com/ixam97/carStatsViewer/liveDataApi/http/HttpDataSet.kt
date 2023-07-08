package com.ixam97.carStatsViewer.liveDataApi.http

data class HttpDataSet(
    val apiVersion: Int = 2,
    val appVersion: String,
    val timestamp: Long,
    val speed: Float,
    val power: Float,
    val selectedGear: String,
    val ignitionState: String,
    val chargePortConnected: Boolean,
    val batteryLevel: Float,
    val stateOfCharge: Float,
    val ambientTemperature: Float,
    val lat: Float?,
    val lon: Float?,
    val alt: Float?,

    // ABRP debug
    val abrpPackage: String? = null
)
