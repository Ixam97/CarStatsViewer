package com.ixam97.carStatsViewer.ui.plot.enums

enum class PlotDimensionX {
    INDEX, DISTANCE, TIME, STATE_OF_CHARGE;

    fun toPlotDirection(): PlotDirection {
        return when (this) {
            TIME, STATE_OF_CHARGE -> PlotDirection.LEFT_TO_RIGHT
            else -> PlotDirection.RIGHT_TO_LEFT
        }
    }
}