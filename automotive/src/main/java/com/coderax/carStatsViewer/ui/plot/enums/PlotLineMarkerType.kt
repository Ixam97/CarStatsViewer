package com.coderax.carStatsViewer.ui.plot.enums

enum class PlotLineMarkerType(val int: Int) {
    BEGIN_SESSION(1), END_SESSION(2), SINGLE_SESSION(3);

    companion object {
        fun getType(type: Int?): PlotLineMarkerType? = when (type) {
            1 -> BEGIN_SESSION
            2 -> END_SESSION
            3 -> SINGLE_SESSION
            else -> null // throw(Exception("Unknown marker type"))
        }
    }
}