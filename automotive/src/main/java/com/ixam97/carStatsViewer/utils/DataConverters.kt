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

    fun consumptionPlotLineFromDrivingPoints(drivingPoints: List<DrivingPoint>): List<PlotLineItem> {
        val plotLine = mutableListOf<PlotLineItem>()

        drivingPoints.forEachIndexed() { index, drivingPoint ->
            if (index == 0) plotLine.add(consumptionPlotLineItemFromDrivingPoint(drivingPoint, null))
            else {
                if ((drivingPoint.point_marker_type == 2 && plotLine[index - 1].Marker == PlotLineMarkerType.END_SESSION))
                    plotLine.add(consumptionPlotLineItemFromDrivingPoint(drivingPoint.copy(point_marker_type = 0), plotLine[index - 1]))
                else
                    plotLine.add(consumptionPlotLineItemFromDrivingPoint(drivingPoint, plotLine[index - 1]))
            }
        }

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
        var prevDrivingPoint: DrivingPoint? = null
        var distanceSum = 0f

        session.drivingPoints?.forEachIndexed { index, drivingPoint ->

            if (index == 0) {
                // Add charging marker if there is one before the drive started
                val chargingSessions = session.chargingSessions?.filter {
                    it.start_epoch_time >= session.start_epoch_time && it.end_epoch_time != null && it.end_epoch_time <= drivingPoint.driving_point_epoch_time
                }

                var chargeTime = 0L

                if (chargingSessions?.isNotEmpty() == true) {
                    chargingSessions.forEachIndexed { chargingSessionIndex, chargingSession ->
                        chargeTime += (chargingSession.end_epoch_time!! - chargingSession.start_epoch_time)
                        plotMarkers.add(
                            PlotMarker(
                            MarkerType = PlotMarkerType.CHARGE,
                            MarkerVersion = 1,
                            StartTime = chargingSession.start_epoch_time,
                            EndTime = chargingSession.end_epoch_time,
                            StartDistance = distanceSum,
                            EndDistance = distanceSum
                        )
                        )

                        if (chargingSessionIndex == chargingSessions.size - 1) {

                            val parkTime = chargingSessions[0].start_epoch_time + chargeTime

                            plotMarkers.add(
                                PlotMarker(
                                MarkerType = PlotMarkerType.PARK,
                                MarkerVersion = 1,
                                StartTime = parkTime,
                                EndTime = drivingPoint.driving_point_epoch_time,
                                StartDistance = distanceSum,
                                EndDistance = distanceSum
                            )
                            )
                        }
                    }
                }
            }

            if (drivingPoint.point_marker_type == PlotLineMarkerType.BEGIN_SESSION.int && prevDrivingPoint?.point_marker_type == PlotLineMarkerType.END_SESSION.int) {

                val chargingSessions = session.chargingSessions?.filter { it.start_epoch_time >= prevDrivingPoint!!.driving_point_epoch_time && it.end_epoch_time != null && it.end_epoch_time <= drivingPoint.driving_point_epoch_time && it.end_epoch_time > 0 }

                val markerType = if (chargingSessions?.isNotEmpty() == true) PlotMarkerType.CHARGE
                    else PlotMarkerType.PARK

                plotMarkers.add(
                    PlotMarker(
                    MarkerType = markerType,
                    MarkerVersion = 1,
                    StartTime = prevDrivingPoint!!.driving_point_epoch_time,
                    EndTime = drivingPoint.driving_point_epoch_time,
                    StartDistance = distanceSum,
                    EndDistance = distanceSum
                )
                )
            }
            distanceSum += drivingPoint.distance_delta
            if (drivingPoint.point_marker_type == PlotLineMarkerType.END_SESSION.int) {
                // Only end session data points are relevant for marker generation
                prevDrivingPoint = drivingPoint
            }
        }

        return PlotMarkers().apply{ addMarkers(plotMarkers) }
    }
}