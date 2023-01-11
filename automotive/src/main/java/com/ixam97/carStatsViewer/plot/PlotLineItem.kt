package com.ixam97.carStatsViewer.plot

class PlotLineItem (
    val Value: Float,

    val Time: Long,
    val Distance: Float,

    val TimeDelta: Long?,
    val DistanceDelta: Float?,

    val Marker: PlotMarker?
){
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