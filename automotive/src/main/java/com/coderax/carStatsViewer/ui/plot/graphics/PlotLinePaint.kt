package com.coderax.carStatsViewer.ui.plot.graphics

import com.coderax.carStatsViewer.ui.plot.enums.PlotDimensionY

class PlotLinePaint(
    private val xAxis : PlotPaint,
    private val yAxisNormal : PlotPaint,
    private val yAxisAlternative : PlotPaint,
    private var useYAxisAlternative: () -> Boolean
) {
    fun bySecondaryDimension(secondaryDimension: PlotDimensionY?) : PlotPaint {
        return when {
            secondaryDimension != null -> when {
                useYAxisAlternative.invoke() -> yAxisAlternative
                else -> yAxisNormal
            }
            else -> xAxis
        }
    }
}