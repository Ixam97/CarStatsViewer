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

        markers.add(PlotMarker(plotMarkerType, StartTime = time, StartDistance = distance))
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

        this.markers.clear()
        this.markers.addAll(current.union(markers).sortedBy { it.StartTime })
    }

    fun endMarker(time: Long, distance: Float) {
        if (markers.isNotEmpty() && markers.last().EndTime == null) {
            markers.last().EndTime = time
        }

        if (markers.isNotEmpty() && markers.last().EndDistance == null) {
            markers.last().EndDistance = distance
        }
    }
}

class PlotMarker (
    val MarkerType: PlotMarkerType,
    val StartTime: Long,
    var EndTime: Long? = null,
    val StartDistance: Float,
    var EndDistance: Float? = null,
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
