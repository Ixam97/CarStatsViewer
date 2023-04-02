package com.ixam97.carStatsViewer.plot.enums

enum class PlotDimensionY {
    SPEED, DISTANCE, TIME, STATE_OF_CHARGE, ALTITUDE;

    companion object {
        val IndexMap = mapOf(
            0 to null,
            1 to SPEED,
            2 to STATE_OF_CHARGE,
            3 to ALTITUDE
        )
    }
}