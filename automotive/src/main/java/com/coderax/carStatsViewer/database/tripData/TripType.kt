package com.coderax.carStatsViewer.database.tripData

object TripType {
    const val MANUAL = 1
    const val SINCE_CHARGE = 2
    const val AUTO = 3
    const val MONTH = 4

    val tripTypesNameMap = mapOf(
        MANUAL to "manual",
        AUTO to "auto",
        SINCE_CHARGE to "since charge",
        MONTH to "monthly"
    )
}