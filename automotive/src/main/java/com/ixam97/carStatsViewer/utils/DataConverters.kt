package com.ixam97.carStatsViewer.utils

import com.ixam97.carStatsViewer.database.tripData.ChargingPoint
import com.ixam97.carStatsViewer.database.tripData.DrivingPoint
import com.ixam97.carStatsViewer.database.tripData.DrivingSession
import com.ixam97.carStatsViewer.ui.plot.enums.PlotLineMarkerType
import com.ixam97.carStatsViewer.ui.plot.enums.PlotMarkerType
import com.ixam97.carStatsViewer.ui.plot.objects.PlotLineItem
import com.ixam97.carStatsViewer.ui.plot.objects.PlotMarker
import com.ixam97.carStatsViewer.ui.plot.objects.PlotMarkers

object DataConverters {

    private data class DrivingPointWithDistanceSum(
        val drivingPoint: DrivingPoint,
        val distanceSum: Float,
    )

    fun consumptionPlotLineFromDrivingPoints(drivingPoints: List<DrivingPoint>, maxDistance: Float? = null): List<PlotLineItem> {
        val plotLine = mutableListOf<PlotLineItem>()
        var distanceSum = 0f
        var startIndex = 0

        var startTime = System.nanoTime()
        if (maxDistance != null ){
            var currentIndex = drivingPoints.size - 1
            while (distanceSum < maxDistance + 200f) {
                distanceSum += drivingPoints[currentIndex].distance_delta
                if (currentIndex <= 0 ) break
                currentIndex--
            }
            startIndex = currentIndex
            InAppLogger.d("Start index set to $startIndex / ${drivingPoints.size}")
            InAppLogger.d("Start index found in ${System.nanoTime() - startTime} ns")
        }
        startTime = System.nanoTime()
        var loopCount = 0
        drivingPoints.drop(startIndex).forEachIndexed { index, drivingPoint ->
            loopCount++
            if (index == 0) plotLine.add(consumptionPlotLineItemFromDrivingPoint(drivingPoint, null))
            else {
                if ((drivingPoint.point_marker_type == 2 && plotLine[index - 1].Marker == PlotLineMarkerType.END_SESSION))
                    plotLine.add(consumptionPlotLineItemFromDrivingPoint(drivingPoint.copy(point_marker_type = 0), plotLine[index - 1]))
                else
                    plotLine.add(consumptionPlotLineItemFromDrivingPoint(drivingPoint, plotLine[index - 1]))
            }
        }
        InAppLogger.i("Plot line construction took ${System.nanoTime() - startTime} ns, $loopCount loops")
        return plotLine
    }

    fun consumptionPlotLineItemFromDrivingPoint(drivingPoint: DrivingPoint, prevPlotLineItem: PlotLineItem? = null): PlotLineItem {

        val markerType = PlotLineMarkerType.getType(drivingPoint.point_marker_type)

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

    fun chargePlotLineFromChargingPoints(chargingPoints: List<ChargingPoint>): List<PlotLineItem> {
        val plotLine = mutableListOf<PlotLineItem>()

        chargingPoints.forEachIndexed() { index, chargingPoint ->
            if (index == 0) plotLine.add(chargePlotLineItemFromChargingPoint(chargingPoint, null))
            else plotLine.add(chargePlotLineItemFromChargingPoint(chargingPoint, plotLine[index - 1]))
        }

        return plotLine
    }

    fun chargePlotLineItemFromChargingPoint(chargingPoint: ChargingPoint, prevPlotLineItem: PlotLineItem? = null): PlotLineItem {
        val markerType = PlotLineMarkerType.getType(chargingPoint.point_marker_type)

        return PlotLineItem(
            Value = -chargingPoint.power / 1_000_000,
            EpochTime = chargingPoint.charging_point_epoch_time,
            NanoTime = null,
            Distance = 0f,
            StateOfCharge = chargingPoint.state_of_charge * 100,
            Altitude = null,
            TimeDelta = if (prevPlotLineItem == null) 0 else {
                (chargingPoint.charging_point_epoch_time - prevPlotLineItem.EpochTime) * 1_000_000
            },
            DistanceDelta = 0f,
            StateOfChargeDelta = if (prevPlotLineItem == null) 0f else {
                chargingPoint.state_of_charge * 100 - prevPlotLineItem.StateOfCharge
            },
            AltitudeDelta = null,
            Marker = markerType
        )
    }

    fun plotMarkersFromSession(session: DrivingSession): PlotMarkers {
        val plotMarkers = mutableListOf<PlotMarker>()
        var distanceSum = 0f

        val markedDrivingPoints = mutableListOf<DrivingPointWithDistanceSum>()

        /** Create a list of driving points with markers and calculate distance sums. */
        session.drivingPoints?.forEach { drivingPoint ->
            distanceSum += drivingPoint.distance_delta
            if ((drivingPoint.point_marker_type?:0) != 0) {
                markedDrivingPoints.add(DrivingPointWithDistanceSum(drivingPoint, distanceSum))
            }
        }

        /** Create markers for charging sessions, making use of split sessions. */
        session.chargingSessions?.forEachIndexed { index, chargingSession ->
            if (chargingSession.end_epoch_time != null) {

                if (chargingSession.start_epoch_time <= (markedDrivingPoints.firstOrNull()?.drivingPoint?.driving_point_epoch_time?:0L) && index == 0) {
                    plotMarkers.add(PlotMarker(
                        MarkerType = PlotMarkerType.CHARGE,
                        MarkerVersion = 1,
                        StartTime = chargingSession.start_epoch_time,
                        EndTime = chargingSession.end_epoch_time,
                        StartDistance = 0f,
                        EndDistance = 0f
                    ))
                } else {
                    val startDrivingPoint = markedDrivingPoints.lastOrNull { it.drivingPoint.driving_point_epoch_time <= chargingSession.start_epoch_time }
                    val endDrivingPoint = markedDrivingPoints.firstOrNull { it.drivingPoint.driving_point_epoch_time >= chargingSession.end_epoch_time }

                    if (startDrivingPoint != null && endDrivingPoint != null) {
                        plotMarkers.add(PlotMarker(
                            MarkerType = PlotMarkerType.CHARGE,
                            MarkerVersion = 1,
                            StartTime = startDrivingPoint.drivingPoint.driving_point_epoch_time,
                            EndTime = endDrivingPoint.drivingPoint.driving_point_epoch_time,
                            StartDistance = startDrivingPoint.distanceSum,
                            EndDistance = endDrivingPoint.distanceSum
                        ))
                    }
                }
            }
        }

        /** Get regular park markers. */
        markedDrivingPoints.forEachIndexed { index, markedDrivingPoint ->
            if (markedDrivingPoint.drivingPoint.point_marker_type == 1 && index >= 1) {
                plotMarkers.add(PlotMarker(
                    MarkerType = PlotMarkerType.PARK,
                    MarkerVersion = 1,
                    StartTime = markedDrivingPoints[index-1].drivingPoint.driving_point_epoch_time,
                    EndTime = markedDrivingPoint.drivingPoint.driving_point_epoch_time,
                    StartDistance = markedDrivingPoint.distanceSum,
                    EndDistance = markedDrivingPoint.distanceSum
                ))
            }
        }

        /** Filter out park markers withing charge markers. */
        plotMarkers.filter { it.MarkerType == PlotMarkerType.CHARGE }.forEach { chargeMarker ->
            if (chargeMarker.EndTime != null)
                plotMarkers.removeIf {
                    it.MarkerType == PlotMarkerType.PARK
                    && it.StartTime >= chargeMarker.StartTime
                    && it.EndTime != null
                    && it.EndTime!! <= chargeMarker.EndTime!!
                }
        }

        return PlotMarkers().apply{ addMarkers(plotMarkers) }
    }
}