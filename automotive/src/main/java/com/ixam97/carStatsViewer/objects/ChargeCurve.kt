package com.ixam97.carStatsViewer.objects

import com.ixam97.carStatsViewer.plot.PlotLineItem

data class ChargeCurve(
    var chargePlotLine: List<PlotLineItem>,
    var stateOfChargePlotLine: List<PlotLineItem>,
    var startTimeNanos: Long,
    var endTimeNanos: Long,
    var chargedEnergyWh: Float,
    var maxChargeRatemW: Float,
    var avgChargeRatemW: Float
) {}