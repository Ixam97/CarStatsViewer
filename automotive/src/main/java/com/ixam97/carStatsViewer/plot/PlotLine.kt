package com.ixam97.carStatsViewer.plot

import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min

class PlotLine(
    internal val Range: PlotRange,

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

    fun getDataPoints(dimension: PlotDimension, dimensionRestriction: Long?): List<PlotLineItem> {
        val clone = dataPoints.map { it.value }

        return when {
            clone.isEmpty() -> clone
            dimensionRestriction == null -> clone
            else -> when (dimension) {
                PlotDimension.INDEX -> when {
                    clone.size > dimensionRestriction -> clone.filterIndexed { index, _ -> index >= dimensionRestriction }
                    else -> clone
                }
                PlotDimension.DISTANCE -> {
                    val min = clone.last().Distance - dimensionRestriction
                    return clone.filter { it.Distance >= min }
                }
                PlotDimension.TIME -> {
                    val min = clone.last().Time - dimensionRestriction
                    return clone.filter { it.Time >= min }
                }
            }
        }
    }

    private fun minDimension(dimension: PlotDimension, dimensionRestriction: Long?): Any {
        return minDimension(dimension, dimensionRestriction, getDataPoints(dimension, dimensionRestriction))
    }

    private fun minDimension(dimension: PlotDimension, dimensionRestriction: Long?, dataPoints: List<PlotLineItem>): Any {
        return when (dimension) {
            PlotDimension.INDEX -> max(dataPoints.size - 1, 0).toFloat()
            PlotDimension.DISTANCE -> when {
                dataPoints.isEmpty() -> -(dimensionRestriction ?: 0L).toFloat()
                else -> min(dataPoints.first().Distance,dataPoints.last().Distance - (dimensionRestriction ?: 0L))
            }
            PlotDimension.TIME -> when {
                dataPoints.isEmpty() -> -(dimensionRestriction ?: 0L)
                else -> min(dataPoints.first().Time, dataPoints.last().Time - (dimensionRestriction?:0L))
            }
        }
    }

    private fun maxDimension(dimension: PlotDimension, dimensionRestriction: Long?): Any {
        return maxDimension(dimension, getDataPoints(dimension, dimensionRestriction))
    }

    private fun maxDimension(dimension: PlotDimension, dataPoints: List<PlotLineItem>): Any {
        return when (dimension) {
            PlotDimension.INDEX -> (dataPoints.size - 1).toFloat()
            PlotDimension.DISTANCE -> when {
                dataPoints.isEmpty() -> return 0f
                else -> dataPoints.last().Distance
            }
            PlotDimension.TIME -> when {
                dataPoints.isEmpty() -> return 0L
                else -> dataPoints.last().Time
            }
        }
    }

    fun distanceDimension(dimension: PlotDimension, dimensionRestriction: Long?): Float {
        return when (dimension) {
            PlotDimension.TIME -> (maxDimension(dimension, dimensionRestriction) as Long - minDimension(dimension, dimensionRestriction) as Long).toFloat()
            else -> maxDimension(dimension, dimensionRestriction) as Float - minDimension(dimension, dimensionRestriction) as Float
        }
    }


    fun maxValue(dataPoints: List<PlotLineItem>): Float? {
        val max : Float? = when {
            dataPoints.isEmpty() -> null
            else -> {
                val min = (dataPoints.maxBy { it.Value }?.Value ?: Range.minPositive).coerceAtLeast(Range.minPositive)
                return when {
                    Range.maxPositive != null -> min.coerceAtMost(Range.maxPositive)
                    else -> min
                }
            }
        }

        return when {
            max == null -> null
            Range.smoothAxis != null -> when (max % Range.smoothAxis) {
                0f -> max
                else -> max + (Range.smoothAxis - max % Range.smoothAxis)
            }
            else -> max
        }
    }

    fun minValue(dataPoints: List<PlotLineItem>): Float? {
        val min : Float? = when {
            dataPoints.isEmpty() -> null
            else -> {
                val max = (dataPoints.minBy { it.Value }?.Value ?: Range.minNegative).coerceAtMost(Range.minNegative)
                return when {
                    Range.maxNegative != null -> max.coerceAtLeast(Range.maxNegative)
                    else -> max
                }
            }
        }

        return when {
            min == null -> null
            Range.smoothAxis != null -> when (min % Range.smoothAxis) {
                0f -> min
                else -> min - (min % Range.smoothAxis) - Range.smoothAxis
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
                val value = dataPoints.map { (it.DistanceDelta?:0f) * it.Value }.sum()
                val distance = dataPoints.map { (it.DistanceDelta?:0f) }.sum()

                return when {
                    distance != 0f -> value / distance
                    else -> 0f
                }
            }
            PlotHighlightMethod.AVG_BY_TIME -> {
                val value = dataPoints.map { (it.TimeDelta?:0L) * it.Value }.sum()
                val distance = dataPoints.map { (it.TimeDelta?:0L) }.sum()

                return when {
                    distance != 0L -> value / distance
                    else -> 0f
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

    fun x(index: Float, dimension: PlotDimension, dimensionRestriction: Long?, dataPoints: List<PlotLineItem>) : Float {
        return when(dimension) {
            PlotDimension.DISTANCE -> PlotLineItem.cord(index, minDimension(dimension, dimensionRestriction, dataPoints) as Float, maxDimension(dimension, dataPoints) as Float)
            PlotDimension.INDEX -> index
            else -> 0f
        }
    }

    fun x(index: Long, dimension: PlotDimension, dimensionRestriction: Long?, dataPoints: List<PlotLineItem>) : Float {
        return when(dimension) {
            PlotDimension.TIME -> PlotLineItem.cord(index, minDimension(dimension, dimensionRestriction, dataPoints) as Long, maxDimension(dimension, dataPoints) as Long)
            else -> 0f
        }
    }
}

