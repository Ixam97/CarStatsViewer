package com.ixam97.carStatsViewer.ui.plot.objects

import com.ixam97.carStatsViewer.ui.plot.enums.PlotDimensionX
import com.ixam97.carStatsViewer.ui.plot.enums.PlotDimensionY
import com.ixam97.carStatsViewer.ui.plot.enums.PlotLineMarkerType
import com.ixam97.carStatsViewer.utils.Exclude
import kotlin.math.roundToInt

class PlotLineItem (
    var Value: Float,
    val EpochTime: Long,
    @Exclude
    val NanoTime: Long?,
    val Distance: Float,
    var StateOfCharge: Float,
    var Altitude: Float?,
    var TimeDelta: Long?,
    var DistanceDelta: Float?,
    var StateOfChargeDelta: Float?,
    var AltitudeDelta: Float?,

    var Marker: PlotLineMarkerType?
){
    fun group(index: Int, dimension: PlotDimensionX, dimensionSmoothing: Float?): Any {
        val value = when(dimension) {
            PlotDimensionX.INDEX -> index
            PlotDimensionX.DISTANCE -> Distance
            PlotDimensionX.TIME -> EpochTime
            PlotDimensionX.STATE_OF_CHARGE -> StateOfCharge
        }

        return when (dimensionSmoothing) {
            null -> value
            0f -> value
            else -> when(dimension) {
                PlotDimensionX.INDEX -> (value as Int / dimensionSmoothing).roundToInt()
                PlotDimensionX.DISTANCE, PlotDimensionX.STATE_OF_CHARGE -> (value as Float / dimensionSmoothing).roundToInt()
                PlotDimensionX.TIME -> value as Long / dimensionSmoothing.toLong()
            }
        }
    }

    fun byDimensionY(dimensionY: PlotDimensionY? = null): Float? {
        return when (dimensionY) {
            PlotDimensionY.SPEED -> {
                when {
                    (DistanceDelta ?: 0f) == 0f || (TimeDelta ?: 0L) == 0L -> null
                    else -> (DistanceDelta ?: 0f) / ((TimeDelta ?: 0L) / 1_000_000_000f) * 3.6f
                }
            }
            PlotDimensionY.CONSUMPTION -> {
                when {
                    Value == 0f || (DistanceDelta ?: 0f) == 0f -> null
                    else -> Value / ((DistanceDelta ?: 0f) / 1000)
                }
            }
            PlotDimensionY.DISTANCE -> Distance
            PlotDimensionY.TIME -> EpochTime.toFloat()
            PlotDimensionY.STATE_OF_CHARGE -> StateOfCharge
            PlotDimensionY.ALTITUDE -> Altitude
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

        fun byDimensionY(dataPoints: List<PlotLineItem>, dimensionY: PlotDimensionY? = null): Float? {
            if (dataPoints.isEmpty()) return null

            return when (dimensionY) {
                PlotDimensionY.SPEED -> {
                    val distance = dataPoints.map { (it.DistanceDelta ?: 0f) }.sum()
                    val time = dataPoints.sumOf { (it.TimeDelta ?: 0L) }

                    when {
                        distance == 0f || time == 0L -> null
                        else -> distance / (time / 1_000_000_000f) * 3.6f
                    }
                }
                PlotDimensionY.CONSUMPTION -> {
                    val value = dataPoints.map { it.Value }.sum()
                    val distance  = dataPoints.map { (it.DistanceDelta ?: 0f) }.sum()

                    when {
                        value == 0f || distance == 0f -> null
                        else -> value / (distance / 1000)
                    }
                }
                else -> null
            }
        }
    }
}
