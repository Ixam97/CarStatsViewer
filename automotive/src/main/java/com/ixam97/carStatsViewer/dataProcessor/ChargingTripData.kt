package com.ixam97.carStatsViewer.dataProcessor

data class ChargingTripData(
    val chargedEnergy: Double = 0.0,
    val chargeTime: Long = 0L,
    val chargingSessionId: Long = 0
)
