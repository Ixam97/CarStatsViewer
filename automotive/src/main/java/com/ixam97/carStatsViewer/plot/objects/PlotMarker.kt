package com.ixam97.carStatsViewer.plot.objects

import com.ixam97.carStatsViewer.plot.enums.PlotDimension
import com.ixam97.carStatsViewer.plot.enums.PlotMarkerType
import kotlin.math.roundToInt

class PlotMarkers {
    val markers : ArrayList<PlotMarker> = ArrayList()

    fun reset() {
        markers.clear()
    }

    fun addMarker(plotMarkerType: PlotMarkerType, time: Long, distance: Float) {
        when {
            markers.isNotEmpty() -> {
                val last = markers.maxByOrNull { it.StartTime }!!
                when {
                    last.MarkerType == plotMarkerType && last.EndTime == null -> return
                    last.MarkerType == plotMarkerType && last.EndTime == time -> {
                        last.EndTime = null
                        last.EndDistance = null
                        return
                    }
                    last.MarkerType == plotMarkerType && last.EndDistance == distance -> {
                        last.EndTime = null
                        last.EndDistance = null
                        return
                    }
                    else -> endMarker(time, distance)
                }
            }
        }

        markers.add(PlotMarker(plotMarkerType, StartTime = time, StartDistance = distance, MarkerVersion = PlotGlobalConfiguration.DataVersion))
    }

    fun addMarkers(markers: List<PlotMarker>) {
        val current = this.markers.map { it }

        if (current.isNotEmpty()) {
            for (marker in markers.filter { it.EndTime == null }) {
                marker.EndTime = marker.StartTime
            }
            for (marker in markers.filter { it.EndDistance == null }) {
                marker.EndDistance = marker.StartDistance
            }
        }

        // Version Migrations
        val provided : ArrayList<PlotMarker> = ArrayList()
        for (marker in markers) {
            // old version of markers before switch to System.currentTimeMillis()
            if (marker.MarkerVersion == null) continue

            provided.add(marker)
        }

        this.markers.clear()
        this.markers.addAll(current.union(provided).sortedBy { it.StartTime })
    }

    fun endMarker(time: Long, distance: Float) {
        if (markers.isEmpty()) return

        val last = markers.last()

        if (last.EndTime == null) {
            last.EndTime = when (last.MarkerType) {
                PlotMarkerType.PARK, PlotMarkerType.CHARGE -> time
                else -> last.StartTime
            }
        }

        if (last.EndDistance == null) {
            last.EndDistance = when (last.MarkerType) {
                PlotMarkerType.PARK, PlotMarkerType.CHARGE -> last.StartDistance
                else -> distance
            }
        }
    }
}

class PlotMarker (
    val MarkerType: PlotMarkerType,
    val MarkerVersion: Int? = null,
    val StartTime: Long,
    var EndTime: Long? = null,
    val StartDistance: Float,
    var EndDistance: Float? = null
) {
    fun group(dimension: PlotDimension, dimensionSmoothing: Float?): Float? {
        val value = when(dimension) {
            PlotDimension.DISTANCE -> StartDistance
            PlotDimension.TIME -> StartTime.toFloat()
            else -> null
        } ?: return null

        return when (dimensionSmoothing) {
            null -> value
            0f -> value
            else -> (value / dimensionSmoothing).roundToInt().toFloat()
        }
    }

    fun startByDimension(dimension: PlotDimension) : Any? {
        return when (dimension) {
            PlotDimension.TIME -> StartTime
            PlotDimension.DISTANCE -> StartDistance
            else -> null
        }
    }

    fun endByDimension(dimension: PlotDimension) : Any? {
        return when (dimension) {
            PlotDimension.TIME -> EndTime
            PlotDimension.DISTANCE -> EndDistance
            else -> null
        }
    }


}
