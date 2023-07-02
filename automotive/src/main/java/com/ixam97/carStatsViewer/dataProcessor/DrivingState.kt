package com.ixam97.carStatsViewer.dataProcessor

object DrivingState {
    const val UNKNOWN = -1
    const val PARKED = 0
    const val DRIVE = 1
    const val CHARGE = 2

    val nameMap = mapOf<Int, String>(
        UNKNOWN to "UNKNOWN",
        PARKED to "PARKED",
        DRIVE to "DRIVE",
        CHARGE to "CHARGE"
    )
}