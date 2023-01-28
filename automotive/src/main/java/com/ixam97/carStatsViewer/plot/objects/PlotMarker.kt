package com.ixam97.carStatsViewer.plot.objects

import com.ixam97.carStatsViewer.plot.enums.PlotMarkerType

class PlotMarkers {
    val markers : ArrayList<PlotMarker> = ArrayList()

    fun addMarker(plotMarkerType: PlotMarkerType, time: Long) {
        when {
            markers.isNotEmpty() -> {
                val last = markers.last()
                when {
                    last.MarkerType == plotMarkerType && last.EndTime == null -> return
                    last.MarkerType == plotMarkerType && last.EndTime == time -> {
                        last.EndTime = null
                        return
                    }
                    else -> endMarker(time)
                }
            }
        }

        markers.add(PlotMarker(plotMarkerType, time))
    }

    fun addMarkers(markers: List<PlotMarker>) {
        val current = this.markers.map { it }

        if (current.isNotEmpty()) {
            for (marker in markers.filter { it.EndTime == null }) {
                marker.EndTime = marker.StartTime
            }
        }

        this.markers.clear()
        this.markers.addAll(current.union(markers).sortedBy { it.StartTime })
    }

    fun endMarker(time: Long) {
        if (markers.isNotEmpty() && markers.last().EndTime == null) {
            markers.last().EndTime = time
        }
    }
}

class PlotMarker (
    val MarkerType: PlotMarkerType,
    val StartTime: Long,
    var EndTime: Long? = null,
)
