package com.ixam97.carStatsViewer.objects

import com.ixam97.carStatsViewer.PlotHighlightMethod
import com.ixam97.carStatsViewer.PlotLabelPosition
import com.ixam97.carStatsViewer.PlotLine

object DataHolder {
    var currentPowermW = 0F
    var currentSpeed = 0F
    var traveledDistance = 0F
    var usedEnergy = 0F
    var averageConsumption = 0F
    var currentBatteryCapacity = 0
    var chargePortConnected = false
    var maxBatteryCapacity = 0

    var consumptionPlotLine = PlotLine(
        -200f,
        600f,
        100f,
        1f,
        "%.0f",
        "%.0f",
        "Wh/km",
        PlotLabelPosition.LEFT,
        PlotHighlightMethod.AVG
    )

    var speedPlotLine = PlotLine(
        0f,
        120f,
        40f,
        1f,
        "%.0f",
        "Ø %.0f",
        "km/h",
        PlotLabelPosition.RIGHT,
        PlotHighlightMethod.AVG_BY_DIMENSION
    )
}