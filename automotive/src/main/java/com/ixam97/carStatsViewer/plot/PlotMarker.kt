package com.ixam97.carStatsViewer.plot

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
        this.markers.addAll(markers)
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
