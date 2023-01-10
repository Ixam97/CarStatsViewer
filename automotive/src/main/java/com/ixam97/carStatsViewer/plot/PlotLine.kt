package com.ixam97.carStatsViewer.plot

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.math.floor

class PlotLine(
    private val MinValueDefault: Float,
    private val MaxValueDefault: Float,

    val SmoothAxle: Float? = null,

    var Divider: Float,

    var LabelFormat: String,
    var HighlightFormat: String,

    var Unit: String,

    var LabelPosition: PlotLabelPosition,
    var HighlightMethod: PlotHighlightMethod,

    var Visible: Boolean = true
) {
    private val dataPoints: ConcurrentHashMap<Int, PlotLineItem> = ConcurrentHashMap()

    var baseLineAt: ArrayList<Float> = ArrayList()

    var plotPaint: PlotPaint? = null

    fun addDataPoint(item: Float, time: Long, distance: Float, timeDelta: Long? = null, distanceDelta: Float? = null, plotMarker: PlotMarker? = null) {
        val prev = dataPoints[dataPoints.size - 1]

        dataPoints[dataPoints.size] = PlotLineItem(
            item,
            time,
            distance,
            timeDelta?:(time - (prev?.Time ?: time)),
            distanceDelta?:(distance - (prev?.Distance ?: distance)),
            plotMarker
        )
    }

    fun reset() {
        dataPoints.clear()
    }

    fun getDataPoints(dimension: PlotDimension, dimensionRestriction: Float?): List<PlotLineItem> {
        var clone = dataPoints.map { it.value }
        return when (dimensionRestriction) {
            null -> clone
            else -> when (dimension) {
                PlotDimension.INDEX -> when (clone.size > dimensionRestriction) {
                    true -> clone.filterIndexed { index, _ -> index >= dimensionRestriction }
                    else -> clone
                }
                PlotDimension.DISTANCE -> {
                    val min = clone.last().Distance - dimensionRestriction
                    return clone.filter { it.Distance >= min }
                }
                PlotDimension.TIME -> {
                    val min = clone.last().timeInSeconds - dimensionRestriction
                    return clone.filter { it.timeInSeconds >= min }
                }
            }
        }
    }

    fun minDimension(dimension: PlotDimension): Float {
        if (dataPoints.isEmpty()) return 0f
        return when (dimension) {
            PlotDimension.INDEX -> 0f
            PlotDimension.DISTANCE -> dataPoints[0]?.DistanceDelta?:0f
            PlotDimension.TIME -> dataPoints[0]?.timeInSeconds?:0f
        }
    }

    fun maxDimension(dimension: PlotDimension): Float {
        if (dataPoints.isEmpty()) return 0f
        val index = dataPoints.size - 1
        return when (dimension) {
            PlotDimension.INDEX -> index.toFloat()
            PlotDimension.DISTANCE -> dataPoints[index]?.DistanceDelta?:0f
            PlotDimension.TIME -> dataPoints[index]?.timeInSeconds?:0f
        }
    }

    fun maxValue(dataPoints: List<PlotLineItem>): Float? {
        val max = when (dataPoints.isEmpty()) {
            true -> null
            false -> ceil((dataPoints.maxBy { it.Value }?.Value ?: MaxValueDefault).coerceAtLeast(MaxValueDefault))
        }

        return when {
            max == null -> null
            SmoothAxle != null -> when (max % SmoothAxle) {
                0f -> max
                else -> max + (SmoothAxle - max % SmoothAxle)
            }
            else -> max
        }
    }

    fun minValue(dataPoints: List<PlotLineItem>): Float? {
        val min = when (dataPoints.isEmpty()) {
            true -> null
            false -> floor((dataPoints.minBy { it.Value }?.Value ?: MinValueDefault).coerceAtMost(MinValueDefault))
        }

        return when {
            min == null -> null
            SmoothAxle != null -> when (min % SmoothAxle) {
                0f -> min
                else -> min - (min % SmoothAxle) - SmoothAxle
            }
            else -> min
        }
    }

    fun averageValue(dataPoints: List<PlotLineItem>, dimension: PlotDimension): Float? {
        return when(dimension) {
            PlotDimension.INDEX -> averageValue(dataPoints, PlotHighlightMethod.AVG_BY_INDEX)
            PlotDimension.DISTANCE -> averageValue(dataPoints, PlotHighlightMethod.AVG_BY_DISTANCE)
            PlotDimension.TIME -> averageValue(dataPoints, PlotHighlightMethod.AVG_BY_TIME)
        }
    }

    private fun averageValue(dataPoints: List<PlotLineItem>, averageMethod: PlotHighlightMethod): Float? {
        if (dataPoints.isEmpty()) return null
        if (dataPoints.size == 1) return dataPoints.first().Value

        return when (averageMethod) {
            PlotHighlightMethod.AVG_BY_INDEX -> dataPoints.map { it.Value }.average().toFloat()
            PlotHighlightMethod.AVG_BY_DISTANCE -> {
                var value = dataPoints.map { (it.DistanceDelta?:0f) * it.Value }.sum()
                var distance = dataPoints.map { (it.DistanceDelta?:0f) }.sum()

                return when(distance != 0f) {
                    true -> value / distance
                    else -> distance
                }
            }
            PlotHighlightMethod.AVG_BY_TIME -> {
                var value = dataPoints.map { (it.TimeDelta?:0L) * it.Value }.sum()
                var distance = dataPoints.map { (it.TimeDelta?:0L) }.sum()

                return when(distance != 0L) {
                    true -> value / distance
                    else -> distance.toFloat()
                }
            }
            else -> null
        }
    }

    fun isEmpty(): Boolean {
        return dataPoints.isEmpty()
    }

    fun byHighlightMethod(dataPoints: List<PlotLineItem>): Float? {
        return when (HighlightMethod) {
            PlotHighlightMethod.MIN -> minValue(dataPoints)
            PlotHighlightMethod.MAX -> maxValue(dataPoints)
            PlotHighlightMethod.FIRST -> dataPoints.first().Value
            PlotHighlightMethod.LAST -> dataPoints.last().Value
            PlotHighlightMethod.AVG_BY_INDEX -> averageValue(dataPoints, HighlightMethod)
            PlotHighlightMethod.AVG_BY_DISTANCE -> averageValue(dataPoints, HighlightMethod)
            PlotHighlightMethod.AVG_BY_TIME -> averageValue(dataPoints, HighlightMethod)
            else -> null
        }
    }

    fun x(index: Float, dimension: PlotDimension, dimensionRestriction: Float?, dataPoints: List<PlotLineItem>) : Float {
        return when(dimension) {
            PlotDimension.DISTANCE -> {
                val max = dataPoints.last().Distance
                val min = when (dimensionRestriction) {
                    null -> dataPoints.first().Distance
                    else -> max - dimensionRestriction
                }
                PlotLineItem.cord(index, min, max)
            }
            PlotDimension.TIME -> {
                val max = dataPoints.last().timeInSeconds
                val min = when (dimensionRestriction) {
                    null -> dataPoints.first().timeInSeconds
                    else -> max - dimensionRestriction
                }
                PlotLineItem.cord(index, min, max)
            }
            PlotDimension.INDEX -> index
        }
    }
}

class PlotLineItem (
    val Value: Float,

    val Time: Long,
    val Distance: Float,

    val TimeDelta: Long?,
    val DistanceDelta: Float?,

    val Marker: PlotMarker?
){
    val timeInSeconds: Float
        get() {
            return TimeUnit.MILLISECONDS.convert(Time, TimeUnit.NANOSECONDS) / 1_000f
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
    }
}

enum class PlotDimension {
    INDEX, DISTANCE, TIME
}

enum class PlotLabelPosition {
    LEFT, RIGHT, NONE
}

enum class PlotHighlightMethod {
    MIN, MAX, FIRST, LAST, AVG_BY_INDEX, AVG_BY_DISTANCE, AVG_BY_TIME, NONE
}

enum class PlotMarker {
    BEGIN_SESSION, END_SESSION, SINGLE_SESSION
}