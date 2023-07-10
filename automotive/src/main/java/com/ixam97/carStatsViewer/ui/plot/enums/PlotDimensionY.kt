package com.ixam97.carStatsViewer.ui.plot.enums

enum class PlotDimensionY {
    SPEED, DISTANCE, TIME, STATE_OF_CHARGE, ALTITUDE, CONSUMPTION;

    companion object {
        val IndexMap = mapOf(
            0 to CONSUMPTION,
            1 to SPEED,
            2 to STATE_OF_CHARGE,
            3 to ALTITUDE
        )
    }
}