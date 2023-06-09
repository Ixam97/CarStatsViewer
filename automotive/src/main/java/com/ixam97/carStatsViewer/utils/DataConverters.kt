package com.ixam97.carStatsViewer.utils

import com.ixam97.carStatsViewer.database.tripData.DrivingPoint
import com.ixam97.carStatsViewer.plot.enums.PlotLineMarkerType
import com.ixam97.carStatsViewer.plot.objects.PlotLineItem

object DataConverters {

    fun consumptionPlotLineFromDrivingPoints(drivingPoints: List<DrivingPoint>): List<PlotLineItem> {
        val plotLine = mutableListOf<PlotLineItem>()

        drivingPoints.forEachIndexed() { index, drivingPoint ->
            if (index == 0) plotLine.add(consumptionPlotLineItemFromDrivingPoint(drivingPoint, null))
            else plotLine.add(consumptionPlotLineItemFromDrivingPoint(drivingPoint, plotLine[index - 1]))
        }

        return plotLine
    }

    fun consumptionPlotLineItemFromDrivingPoint(drivingPoint: DrivingPoint, lastPlotLineItem: PlotLineItem? = null): PlotLineItem {
        return PlotLineItem(
            Value = drivingPoint.energy_delta / (drivingPoint.distance_delta / 1000) ,
            EpochTime = drivingPoint.driving_point_epoch_time,
            NanoTime = null,
            Distance = if (lastPlotLineItem == null) {
                drivingPoint.distance_delta
            } else {
                lastPlotLineItem.Distance + drivingPoint.distance_delta
            },
            StateOfCharge = drivingPoint.state_of_charge * 100,
            Altitude = drivingPoint.alt,
            TimeDelta = if (lastPlotLineItem == null) 0 else {
                (drivingPoint.driving_point_epoch_time - lastPlotLineItem.EpochTime) * 1_000_000
            },
            DistanceDelta = drivingPoint.distance_delta,
            StateOfChargeDelta = if (lastPlotLineItem == null) 0f else {
                drivingPoint.state_of_charge*100 - lastPlotLineItem.StateOfCharge
            },
            AltitudeDelta = null,
            Marker = PlotLineMarkerType.getType(drivingPoint.point_marker_type)
        )
    }
}