package com.ixam97.carStatsViewer.ui.plot.graphics

import com.ixam97.carStatsViewer.ui.plot.enums.PlotDimensionY

class PlotLinePaint(
    private val xAxis : PlotPaint,
    private val yAxisNormal : PlotPaint,
    private val yAxisAlternative : PlotPaint,
    private var useYAxisAlternative: () -> Boolean
) {
    private val xAxisByY : HashSet<PlotDimensionY> = hashSetOf(
        PlotDimensionY.CONSUMPTION
    )

    fun bySecondaryDimension(secondaryDimension: PlotDimensionY?) : PlotPaint {
        return when {
            xAxisByY.contains(secondaryDimension) -> xAxis
            secondaryDimension != null -> when {
                useYAxisAlternative.invoke() -> yAxisAlternative
                else -> yAxisNormal
            }
            else -> xAxis
        }
    }
}