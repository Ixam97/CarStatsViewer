package com.ixam97.carStatsViewer.liveData.http

import java.util.*

data class HttpDataSet(
    val timestamp: Long,
    val currentSpeed: Float,
    val currentPower: Float,
    val currentGear: Int,
    val chargePortConnected: Boolean,
    val batteryLevel: Float,
    val stateOfCharge: Int,
    val currentIgnitionState: Int,
    val instConsumption: Float?,
    val avgConsumption: Float?,
    val avgSpeed: Float,
    val travelTime: Long,
    val chargeTime: Long,
    val driveState: Int,
    val ambientTemperature: Float,
    val maxBatteryLevel: Float,
    val tripStartDate: Date,
    val usedEnergy: Float,
    val traveledDistance: Float,
    val chargeStartDate: Date,
    val chargedEnergy: Float,
    val lat: Double?,
    val lon: Double?,
    val alt: Double?,

    // Helpers
    val isCharging: Boolean,
    val isParked: Boolean,
    val isFastCharging: Boolean,

    // ABRP debug
    val abrpPackage: String? = null
)
