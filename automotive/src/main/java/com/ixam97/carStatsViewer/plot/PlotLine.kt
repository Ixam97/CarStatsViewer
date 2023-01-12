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

    var alignZero: Boolean = false

    var zeroAt: Float? = null

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

    fun getDataPoints(dimension: PlotDimension, dimensionRestriction: Long?, dimensionSmoothing: Long? = null): List<PlotLineItem> {
        return when {
            dataPoints.isEmpty() || dimensionRestriction == null -> dataPoints.map { it.value }
            else -> when (dimension) {
                PlotDimension.INDEX -> when {
                    dataPoints.size > dimensionRestriction -> dataPoints.filter { x -> x.key >= dimensionRestriction }.map { it.value }
                    else -> dataPoints.map { it.value }
                }
                PlotDimension.DISTANCE -> {
                    var min = dataPoints[dataPoints.size - 1]!!.Distance - dimensionRestriction
                    if (dimensionSmoothing != null) {
                        min -= 2 * dimensionSmoothing
                    }

                    return dataPoints.filter { it.value.Distance >= min }.map { it.value }
                }
                PlotDimension.TIME -> {
                    var min = dataPoints[dataPoints.size - 1]!!.Time - dimensionRestriction
                    if (dimensionSmoothing != null) {
                        min -= 2 * dimensionSmoothing
                    }

                    return dataPoints.filter { it.value.Time >= min }.map { it.value }
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

    fun distanceDimension(dimension: PlotDimension, dimensionRestriction: Long?, dataPoints: List<PlotLineItem>): Float {
        return when (dimension) {
            PlotDimension.TIME -> (maxDimension(dimension, dataPoints) as Long - minDimension(dimension, dimensionRestriction, dataPoints) as Long).toFloat()
            else -> maxDimension(dimension, dataPoints) as Float - minDimension(dimension, dimensionRestriction, dataPoints) as Float
        }
    }

    fun maxValue(dataPoints: List<PlotLineItem>): Float? {
        val max : Float? = when {
            dataPoints.isEmpty() -> null
            else -> {
                val min = (dataPoints.maxBy { it.Value }?.Value ?: Range.minPositive).coerceAtLeast(Range.minPositive)
                when {
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
                when {
                    Range.maxNegative != null -> max.coerceAtLeast(Range.maxNegative)
                    else -> max
                }
            }
        }

        val minSmooth = when {
            min == null -> null
            Range.smoothAxis != null -> when (min % Range.smoothAxis) {
                0f -> min
                else -> min - (min % Range.smoothAxis) - Range.smoothAxis
            }
            else -> min
        }

        val zeroAtCopy = zeroAt
        when {
            zeroAtCopy == null || zeroAtCopy <= 0f || zeroAtCopy >= 1f -> return minSmooth
            else -> {
                val max = maxValue(dataPoints) ?: return minSmooth
                return -(max /  (1f - zeroAtCopy) * (zeroAtCopy))
            }
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

    fun toPlotLineItemPointCollection(dataPoints: List<PlotLineItem>, dimension: PlotDimension, dimensionRestriction: Long?, dimensionSmoothing: Long?): ArrayList<ArrayList<PlotLineItemPoint>> {
        val result = ArrayList<ArrayList<PlotLineItemPoint>>()
        var group = ArrayList<PlotLineItemPoint>()

        for (index in dataPoints.indices) {
            val item = dataPoints[index]

            group.add(
                PlotLineItemPoint(
                    when (dimension) {
                        PlotDimension.INDEX -> x(index.toFloat(), dimension, dimensionRestriction, dataPoints)
                        PlotDimension.DISTANCE -> x(item.Distance, dimension, dimensionRestriction, dataPoints)
                        PlotDimension.TIME -> x(item.Time, dimension, dimensionRestriction, dataPoints)
                    },
                    item,
                    item.group(index, dimension, dimensionSmoothing)
                )
            )

            if ((item.Marker ?: PlotMarker.BEGIN_SESSION) != PlotMarker.BEGIN_SESSION) {
                result.add(group)
                group = ArrayList()
            }
        }

        if (!group.isEmpty()) {
            result.add(group)
        }

        return result
    }
}

