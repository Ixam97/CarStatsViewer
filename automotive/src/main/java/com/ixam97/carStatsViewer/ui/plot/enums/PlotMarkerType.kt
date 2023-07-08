package com.ixam97.carStatsViewer.ui.plot.enums

enum class PlotMarkerType {
    CHARGE, PARK;

    fun getType(type: Int): PlotMarkerType = when (type) {
        1 -> CHARGE
        2 -> PARK
        else -> throw(Exception("Unknown marker type"))
    }

    fun getInt(type: PlotMarkerType): Int = when (type) {
        CHARGE -> 1
        PARK -> 2
    }
}