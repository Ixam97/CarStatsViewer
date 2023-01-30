package com.ixam97.carStatsViewer.objects

import com.ixam97.carStatsViewer.plot.objects.PlotLineItem
import com.ixam97.carStatsViewer.plot.enums.PlotLineMarkerType
import com.ixam97.carStatsViewer.plot.objects.PlotMarker
import java.util.*

data class TripData(
    var appVersion: String,
    var tripStartDate: Date,
    var usedEnergy: Float,
    var traveledDistance: Float,
    var travelTime: Long,
    var chargedEnergy: Float,
    var chargeTime: Long,
    var consumptionPlotLine: List<PlotLineItem>,
    var chargePlotLine: List<PlotLineItem>,
    var chargeCurves: List<ChargeCurve>,
    var markers: List<PlotMarker>
)
