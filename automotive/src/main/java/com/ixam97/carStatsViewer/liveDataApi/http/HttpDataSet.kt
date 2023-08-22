package com.ixam97.carStatsViewer.liveDataApi.http

import com.ixam97.carStatsViewer.database.tripData.ChargingSession
import com.ixam97.carStatsViewer.database.tripData.DrivingPoint

data class HttpDataSet(
    val apiVersion: String = "2.1",
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
    val abrpPackage: String? = null,

    val drivingPoints: List<DrivingPoint>? = null,
    val chargingSessions: List<ChargingSession>? = null
)
