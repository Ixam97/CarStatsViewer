package com.ixam97.carStatsViewer.database.tripData

object TripType {
    val MANUAL = 1
    val AUTO = 2
    val SINCE_CHARGE = 3
    val MONTH = 4

    val tripTypesNameMap = mapOf(
        MANUAL to "manual",
        AUTO to "auto",
        SINCE_CHARGE to "since charge",
        MONTH to "monthly"
    )
}