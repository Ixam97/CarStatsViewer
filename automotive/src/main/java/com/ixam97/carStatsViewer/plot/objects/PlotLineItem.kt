package com.ixam97.carStatsViewer.plot.objects

import com.ixam97.carStatsViewer.plot.enums.*
import com.ixam97.carStatsViewer.utils.Exclude
import java.time.LocalDate
import java.util.*
import kotlin.math.roundToInt

class PlotLineItem (
    var Value: Float,
    val EpochTime: Long,
    @Exclude
    val NanoTime: Long?,
    val Distance: Float,
    var StateOfCharge: Float,
    var TimeDelta: Long?,
    var DistanceDelta: Float?,
    var StateOfChargeDelta: Float?,

    var Marker: PlotLineMarkerType?
){
    fun group(index: Int, dimension: PlotDimension, dimensionSmoothing: Float?): Any {
        val value = when(dimension) {
            PlotDimension.INDEX -> index
            PlotDimension.DISTANCE -> Distance
            PlotDimension.TIME -> EpochTime
            PlotDimension.STATE_OF_CHARGE -> StateOfCharge
        }

        return when (dimensionSmoothing) {
            null -> value
            0f -> value
            else -> when(dimension) {
                PlotDimension.INDEX -> (value as Int / dimensionSmoothing).roundToInt()
                PlotDimension.DISTANCE, PlotDimension.STATE_OF_CHARGE -> (value as Float / dimensionSmoothing).roundToInt()
                PlotDimension.TIME ->  (value as Long / dimensionSmoothing).roundToInt()
            }
        }
    }

    fun bySecondaryDimension(secondaryDimension: PlotSecondaryDimension? = null): Float? {
        return when (secondaryDimension) {
            PlotSecondaryDimension.SPEED -> {
                when {
                    (TimeDelta?:0L) <= 0 -> null
                    else -> (DistanceDelta ?: 0f) / ((TimeDelta ?: 1L) / 1_000_000_000f) * 3.6f
                }
            }
            PlotSecondaryDimension.DISTANCE -> Distance
            PlotSecondaryDimension.TIME -> EpochTime.toFloat()
            PlotSecondaryDimension.STATE_OF_CHARGE -> StateOfCharge?:0f
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

        fun cord(index: Long?, min: Long, max: Long) : Float? {
            return when (index) {
                null -> null
                else -> cord(index, min, max)
            }
        }

        fun cord(index: Long, min: Long, max: Long) : Float {
            return 1f / (max - min) * (index - min)
        }
    }
}