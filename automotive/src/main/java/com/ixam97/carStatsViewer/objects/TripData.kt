package com.ixam97.carStatsViewer.objects

import com.ixam97.carStatsViewer.plot.PlotLine
import com.ixam97.carStatsViewer.plot.PlotLineItem
import java.util.*
import kotlin.collections.ArrayList

data class TripData(
    var saveDate: Date,
    var traveledDistance: Float,
    var usedEnergy: Float,
    var averageConsumption: Float,
    var travelTimeMillis: Long,
    var consumptionPlotLine: List<PlotLineItem>,
    var speedPlotLine: List<PlotLineItem>,
    var chargeCurves: List<DataHolder.ChargeCurve>
) {

}
