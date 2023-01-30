package com.ixam97.carStatsViewer.objects

import com.ixam97.carStatsViewer.plot.objects.PlotLineItem

data class ChargeCurve(
    var chargePlotLine: List<PlotLineItem>,
    var chargeTime: Long,
    var chargedEnergy: Float
) {}