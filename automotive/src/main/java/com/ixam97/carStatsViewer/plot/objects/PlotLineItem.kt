package com.ixam97.carStatsViewer.plot.objects

import com.ixam97.carStatsViewer.plot.enums.*
import kotlin.math.roundToInt

class PlotLineItem (
    val Value: Float,

    val Time: Long,
    val Distance: Float,
    var StateOfCharge: Float,

    val TimeDelta: Long?,
    val DistanceDelta: Float?,
    var StateOfChargeDelta: Float?,

    var Marker: PlotLineMarkerType?
){
    fun group(index: Int, dimension: PlotDimension, dimensionSmoothing: Float?): Float {
        val value = when(dimension) {
            PlotDimension.INDEX -> index.toFloat()
            PlotDimension.DISTANCE -> Distance
            PlotDimension.TIME -> Time.toFloat()
            PlotDimension.STATE_OF_CHARGE -> StateOfCharge
        }

        return when (dimensionSmoothing) {
            null -> value
            0f -> value
            else -> (value / dimensionSmoothing).roundToInt().toFloat()
        }
    }

    fun bySecondaryDimension(secondaryDimension: PlotSecondaryDimension? = null): Float {
        return when (secondaryDimension) {
            PlotSecondaryDimension.SPEED -> (DistanceDelta ?: 0f) / ((TimeDelta ?: 1L) / 1_000_000_000f) * 3.6f
            PlotSecondaryDimension.DISTANCE -> Distance
            PlotSecondaryDimension.TIME -> Time.toFloat()
            PlotSecondaryDimension.STATE_OF_CHARGE -> StateOfCharge ?: 0f
            else -> Value
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