package com.ixam97.carStatsViewer.dataProcessor

data class DrivingTripData(
    val drivenDistance: Double = 0.0,
    val usedEnergy: Double = 0.0,
    val avgConsumption: Float = 0f,
    val driveTime: Long = 0L,
    val remainingRange: Double = 0.0,
    val selectedTripType: Int = 1,
    val usedStateOfCharge: Double = 0.0,
    val usedStateOfChargeEnergy: Double = 0.0
)
