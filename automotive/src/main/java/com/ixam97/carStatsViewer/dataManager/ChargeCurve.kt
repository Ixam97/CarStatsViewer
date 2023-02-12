package com.ixam97.carStatsViewer.dataManager

import com.ixam97.carStatsViewer.plot.objects.PlotLineItem
import java.util.*

data class ChargeCurve(
    val chargePlotLine: List<PlotLineItem>,
    val chargeTime: Long,
    val chargedEnergy: Float,
    val ambientTemperature: Float? = null,
    val chargeStartDate: Date? = null
) {}