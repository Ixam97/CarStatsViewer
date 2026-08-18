package com.ixam97.carStatsViewer.carCompose.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import com.ixam97.carStatsViewer.compose.screens.ConsumptionPlot
import com.ixam97.carStatsViewer.database.tripData.DrivingSession
import com.ixam97.carStatsViewer.ui.plot.enums.PlotHighlightMethod
import com.ixam97.carStatsViewer.ui.plot.enums.PlotLineLabelFormat
import com.ixam97.carStatsViewer.ui.plot.objects.PlotLine
import com.ixam97.carStatsViewer.ui.plot.objects.PlotLineConfiguration
import com.ixam97.carStatsViewer.ui.plot.objects.PlotRange
import com.ixam97.carStatsViewer.utils.DataConverters
import com.ixam97.carStatsViewer.utils.InAppLogger

data class ConsumptionGraphState(
    val secondaryDimension: Int,
    val distance: Float
)

@Composable
fun ConsumptionGraph(
    drivingSession: DrivingSession,
    consumptionGraphState: ConsumptionGraphState
) {

    val consumptionPlotLine = PlotLine(
        PlotLineConfiguration(
            PlotRange(-200f, 600f, -200f, 600f, 100f, 0f),
            PlotLineLabelFormat.NUMBER,
            PlotHighlightMethod.AVG_BY_DISTANCE,
            "Wh/km"
        ),
    )

    //TODO: Temporary, replace with new compose based plot.
    drivingSession.drivingPoints?.let { drivingPoints ->

        val plotPoints = DataConverters.consumptionPlotLineFromDrivingPoints(drivingPoints)
        val plotMarkers = DataConverters.plotMarkersFromSession(drivingSession)

        consumptionPlotLine.addDataPoints(plotPoints)

        val distance = if (consumptionGraphState.distance > 0) consumptionGraphState.distance else drivingSession.driven_distance.toFloat()

        InAppLogger.d("Consumption graph distance: ${consumptionGraphState.distance}")

        key(consumptionGraphState) {
            ConsumptionPlot(
                plotLine = consumptionPlotLine,
                plotMarkers = plotMarkers,
                plotLinePaint = GraphValues.consumptionPlotPaint,
                distance = distance,
                limitedHeight = false,
                secondaryDimension = consumptionGraphState.secondaryDimension
            )
        }

    }
}