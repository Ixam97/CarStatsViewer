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

    fun addDataPoint(dataPoint: PlotLineItem) {
        dataPoints[dataPoints.size] = dataPoint
    }

    fun addDataPoints(dataPoints: List<PlotLineItem>) {
        for (dataPoint in dataPoints) {
            addDataPoint(dataPoint)
        }
    }

    fun reset() {
        dataPoints.clear()
    }

    fun getDataPoints(dimension: PlotDimension, dimensionRestriction: Long? = null, dimensionShift: Long? = null): List<PlotLineItem> {
        return when {
            dataPoints.isEmpty() || dimensionRestriction == null -> dataPoints.map { it.value }
            else -> when (dimension) {
                PlotDimension.INDEX -> {
                    var max = dataPoints.size - 1 - (dimensionShift ?: 0L)
                    var min = max - dimensionRestriction

                    dataPoints.filter { it.key in min..max }.map { it.value }
                }
                PlotDimension.DISTANCE -> {
                    var max = dataPoints[dataPoints.size - 1]!!.Distance - (dimensionShift ?: 0L)
                    var min = max - dimensionRestriction

                    dataPoints.filter { it.value.Distance in min..max }.map { it.value }
                }
                PlotDimension.TIME -> {
                    var max = dataPoints[dataPoints.size - 1]!!.Time - (dimensionShift ?: 0L)
                    var min = max - dimensionRestriction

                    dataPoints.filter { it.value.Time in min..max }.map { it.value }
                }
            }
        }
    }

    internal fun minDimension(dataPoints: List<PlotLineItem>, dimension: PlotDimension): Any {
        return when (dimension) {
            PlotDimension.INDEX -> 0f
            PlotDimension.DISTANCE -> when {
                dataPoints.isEmpty() -> return 0f
                else -> dataPoints.first().Distance
            }
            PlotDimension.TIME -> when {
                dataPoints.isEmpty() -> return 0L
                else -> dataPoints.first().Time
            }
        }
    }

    internal fun maxDimension(dataPoints: List<PlotLineItem>, dimension: PlotDimension): Any {
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

    fun distanceDimension(dimension: PlotDimension): Float {
        return distanceDimension(getDataPoints(dimension), dimension)
    }

    fun distanceDimension(dataPoints: List<PlotLineItem>, dimension: PlotDimension): Float {
        return when (dimension) {
            PlotDimension.TIME -> (maxDimension(dataPoints, dimension) as Long - minDimension(dataPoints, dimension) as Long).toFloat()
            else -> maxDimension(dataPoints, dimension) as Float - minDimension(dataPoints, dimension) as Float
        }
    }

    fun maxValue(dataPoints: List<PlotLineItem>): Float? {
        val max : Float? = when {
            dataPoints.isEmpty() -> Range.minPositive
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
            dataPoints.isEmpty() -> Range.minNegative
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

    fun x(index: Float, min: Any, max: Any) : Float {
        return PlotLineItem.cord(
            index,
            min as Float,
            max as Float
        )
    }

    fun x(index: Long, min: Any, max: Any) : Float {
        return PlotLineItem.cord(
            index,
            min as Long,
            max as Long
        )
    }

    fun toPlotLineItemPointCollection(dataPoints: List<PlotLineItem>, dimension: PlotDimension, dimensionSmoothing: Long?, min: Any, max: Any): ArrayList<ArrayList<PlotLineItemPoint>> {
        val result = ArrayList<ArrayList<PlotLineItemPoint>>()
        var group = ArrayList<PlotLineItemPoint>()

        for (index in dataPoints.indices) {
            val item = dataPoints[index]

            group.add(
                PlotLineItemPoint(
                    when (dimension) {
                        PlotDimension.INDEX -> x(index.toFloat(), min, max)
                        PlotDimension.DISTANCE -> x(item.Distance, min, max)
                        PlotDimension.TIME -> x(item.Time, min, max)
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

