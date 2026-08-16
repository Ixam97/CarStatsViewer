package com.ixam97.carStatsViewer.carCompose.components

import androidx.core.graphics.toColorInt
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.ui.plot.graphics.PlotLinePaint
import com.ixam97.carStatsViewer.ui.plot.graphics.PlotPaint

object GraphValues {
    val chargePlotPaint = PlotLinePaint(
        xAxis = PlotPaint.byColor("#2693FF".toColorInt(), 10f),
        yAxisNormal = PlotPaint.byColor("#00C400".toColorInt(), 10f),
        yAxisAlternative = PlotPaint.byColor("#CCCCCC".toColorInt(), 10f),
        useYAxisAlternative = { CarStatsViewer.appPreferences.chargePlotSecondaryColor }
    )

    val consumptionPlotPaint = PlotLinePaint(
        xAxis = PlotPaint.byColor("#FFD96C00".toColorInt(), 10f),
        yAxisNormal = PlotPaint.byColor("#FF00C400".toColorInt(), 10f),
        yAxisAlternative = PlotPaint.byColor("#FFCCCCCC".toColorInt(), 10f),
        useYAxisAlternative = { CarStatsViewer.appPreferences.consumptionPlotSecondaryColor }
    )
}