package com.ixam97.carStatsViewer.plot

class PlotLineItem (
    val Value: Float,

    val Time: Long,
    val Distance: Float,

    val TimeDelta: Long?,
    val DistanceDelta: Float?,

    val Marker: PlotMarker?
){
    fun group(index: Int, dimension: PlotDimension, dimensionSmoothing: Long?): Long {
        val value = when(dimension) {
            PlotDimension.INDEX -> index.toLong()
            PlotDimension.DISTANCE -> Distance.toLong()
            PlotDimension.TIME -> Time
        }

        return when (dimensionSmoothing) {
            null -> value
            0L -> value
            else -> value / dimensionSmoothing
        }
    }

    companion object {
        fun cord(index: Float?, min: Float, max: Float) : Float? {
            return when (index) {
                null -> null
                else -> cord(index, min, max)
            }
        }

        fun cord(index: Float, min: Float, max: Float) : Float {
            return 1f / (max - min) * (index - min)
        }

        fun cord(index: Long, min: Long, max: Long) : Float {
            return 1f / (max - min) * (index - min)
        }
    }
}