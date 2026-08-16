package com.ixam97.carStatsViewer.carCompose.components

import androidx.compose.runtime.Composable
import com.ixam97.carStatsViewer.compose.screens.ChargingPlot
import com.ixam97.carStatsViewer.database.tripData.ChargingSession
import com.ixam97.carStatsViewer.ui.plot.enums.PlotHighlightMethod
import com.ixam97.carStatsViewer.ui.plot.enums.PlotLineLabelFormat
import com.ixam97.carStatsViewer.ui.plot.objects.PlotLine
import com.ixam97.carStatsViewer.ui.plot.objects.PlotLineConfiguration
import com.ixam97.carStatsViewer.ui.plot.objects.PlotRange
import com.ixam97.carStatsViewer.utils.DataConverters

@Composable
fun ChargeCurveGraph(
    chargingSession: ChargingSession
) {
    val chargePlotLine = PlotLine(
        PlotLineConfiguration(
            PlotRange(0f, 20f, 0f, 400f, 20f),
            PlotLineLabelFormat.FLOAT,
            PlotHighlightMethod.AVG_BY_TIME,
            "kW"
        )
    )

    val plotPoints = DataConverters.chargePlotLineFromChargingPoints((chargingSession.chargingPoints?: listOf()).filter { it.power < -500_00 })

    chargePlotLine.addDataPoints(plotPoints)

    if (chargingSession.end_epoch_time != null) {
        //TODO: Temporary, replace with new compose based plot.
        ChargingPlot(
            plotLine = chargePlotLine,
            plotLinePaint = GraphValues.chargePlotPaint,
            limitedHeight = false,
            time = chargingSession.end_epoch_time - chargingSession.start_epoch_time
        )
    }
}