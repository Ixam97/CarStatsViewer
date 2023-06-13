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

    fun consumptionPlotLineItemFromDrivingPoint(drivingPoint: DrivingPoint, prevPlotLineItem: PlotLineItem? = null): PlotLineItem {

        var markerType: PlotLineMarkerType? = null

        if (prevPlotLineItem == null) {
            markerType = if (drivingPoint.point_marker_type == 2)
                PlotLineMarkerType.SINGLE_SESSION
            else
                PlotLineMarkerType.BEGIN_SESSION
        } else {
            if (drivingPoint.point_marker_type == 2)
                markerType = PlotLineMarkerType.END_SESSION
        }

        val pointValue = if (drivingPoint.distance_delta <= 0) 0f else drivingPoint.energy_delta / (drivingPoint.distance_delta / 1000)

        return PlotLineItem(
            Value = pointValue,
            EpochTime = drivingPoint.driving_point_epoch_time,
            NanoTime = null,
            Distance = if (prevPlotLineItem == null) {
                drivingPoint.distance_delta
            } else {
                prevPlotLineItem.Distance + drivingPoint.distance_delta
            },
            StateOfCharge = drivingPoint.state_of_charge * 100,
            Altitude = drivingPoint.alt,
            TimeDelta = if (prevPlotLineItem == null) 0 else {
                (drivingPoint.driving_point_epoch_time - prevPlotLineItem.EpochTime) * 1_000_000
            },
            DistanceDelta = drivingPoint.distance_delta,
            StateOfChargeDelta = if (prevPlotLineItem == null) 0f else {
                drivingPoint.state_of_charge*100 - prevPlotLineItem.StateOfCharge
            },
            AltitudeDelta = null,
            Marker = markerType
        )
    }
}