package com.ixam97.carStatsViewer.plot

import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

class PlotLine(
    val Configuration: PlotLineConfiguration,
    val SecondaryDimensionConfiguration: HashMap<PlotSecondaryDimension, PlotLineConfiguration> = HashMap(),
    var Visible: Boolean = true
) {
    private val dataPoints: ConcurrentHashMap<Int, PlotLineItem> = ConcurrentHashMap()

    var baseLineAt: ArrayList<Float> = ArrayList()

    var plotPaint: PlotPaint? = null
    var secondaryPlotPaint: PlotPaint? = null

    var alignZero: Boolean = false

    var zeroAt: Float? = null

    fun addDataPoint(item: Float, time: Long, distance: Float, stateOfCharge: Float, timeDelta: Long? = null, distanceDelta: Float? = null, stateOfChargeDelta: Float? = null, plotLineMarkerType: PlotLineMarkerType? = null) {
        val prev = dataPoints[dataPoints.size - 1]

        addDataPoint(PlotLineItem(
            item,
            time,
            distance,
            stateOfCharge,
            timeDelta?:(time - (prev?.Time ?: time)),
            distanceDelta?:(distance - (prev?.Distance ?: distance)),
            stateOfChargeDelta?:(stateOfCharge - (prev?.StateOfCharge ?: distance)),
            plotLineMarkerType
        ))
    }

    fun addDataPoint(dataPoint: PlotLineItem) {
        when {
            dataPoint.Value.isFinite() -> {
                dataPoints[dataPoints.size] = dataPoint
            }
            dataPoint.Marker == PlotLineMarkerType.END_SESSION -> {
                val lastPoint = dataPoints[dataPoints.size - 1]
                when {
                    lastPoint != null && lastPoint.Marker == null -> {
                        lastPoint.Marker = dataPoint.Marker
                    }
                }
            }
        }
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
                PlotDimension.STATE_OF_CHARGE -> {
                    var max = dataPoints[dataPoints.size - 1]!!.StateOfCharge - (dimensionShift ?: 0L)
                    var min = max - dimensionRestriction

                    dataPoints.filter { it.value.StateOfCharge in min..max }.map { it.value }
                }
            }
        }
    }

    internal fun minDimension(dataPoints: List<PlotLineItem>, dimension: PlotDimension, dimensionRestriction: Long?): Any {
        return when (dimension) {
            PlotDimension.INDEX -> 0f
            PlotDimension.DISTANCE -> when {
                dataPoints.isEmpty() -> 0f
                else -> (maxDimension(dataPoints, dimension) as Float - (dimensionRestriction ?: 0L))
                    .coerceAtMost(dataPoints.minOf { it.Distance })
            }
            PlotDimension.TIME -> when {
                dataPoints.isEmpty() -> 0L
                else -> (maxDimension(dataPoints, dimension) as Long - (dimensionRestriction ?: 0L))
                    .coerceAtMost(dataPoints.minOf { it.Time })
            }
            PlotDimension.STATE_OF_CHARGE -> when {
                dataPoints.isEmpty() -> 0f
                else -> {
                    val min = dataPoints.minOf { it.StateOfCharge }
                    (min - (min % 10f)).coerceAtLeast(0f)
                }
            }
        }
    }

    internal fun maxDimension(dataPoints: List<PlotLineItem>, dimension: PlotDimension): Any {
        return when (dimension) {
            PlotDimension.INDEX -> (dataPoints.size - 1).toFloat()
            PlotDimension.DISTANCE -> when {
                dataPoints.isEmpty() -> 0f
                else -> dataPoints.maxOf { it.Distance }
            }
            PlotDimension.TIME -> when {
                dataPoints.isEmpty() -> 0L
                else -> dataPoints.maxOf { it.Time }
            }
            PlotDimension.STATE_OF_CHARGE -> when {
                dataPoints.isEmpty() -> 0f
                else -> {
                    val max = dataPoints.maxOf { it.StateOfCharge }
                    max + (10f - max % 10f)
                }
            }
        }
    }

    fun distanceDimension(dimension: PlotDimension, dimensionRestriction: Long? = null): Float {
        return distanceDimension(getDataPoints(dimension), dimension, dimensionRestriction)
    }

    fun distanceDimension(dataPoints: List<PlotLineItem>, dimension: PlotDimension, dimensionRestriction: Long?): Float {
        return when (dimension) {
            PlotDimension.TIME -> (maxDimension(dataPoints, dimension) as Long - minDimension(dataPoints, dimension, dimensionRestriction) as Long).toFloat()
            else -> maxDimension(dataPoints, dimension) as Float - minDimension(dataPoints, dimension, dimensionRestriction) as Float
        }
    }

    fun maxValue(dataPoints: List<PlotLineItem>, secondaryDimension: PlotSecondaryDimension? = null, applyRange: Boolean = true): Float? {
        val baseConfiguration = SecondaryDimensionConfiguration[secondaryDimension] ?: Configuration
        val max : Float? = when {
            dataPoints.isEmpty() -> baseConfiguration.Range.minPositive ?: 0f
            else -> {
                var maxByData = dataPoints.maxOf { it.bySecondaryDimension(secondaryDimension) }

                if (!applyRange) return maxByData

                if (baseConfiguration.Range.minPositive != null) maxByData = maxByData.coerceAtLeast(baseConfiguration.Range.minPositive)
                if (baseConfiguration.Range.maxPositive != null) maxByData.coerceAtMost(baseConfiguration.Range.maxPositive)

                maxByData
            }
        }

        if (!applyRange) return max

        return when {
            max == null -> null
            baseConfiguration.Range.smoothAxis != null -> when (max % baseConfiguration.Range.smoothAxis) {
                0f -> max
                else -> max + (baseConfiguration.Range.smoothAxis - max % baseConfiguration.Range.smoothAxis)
            }
            else -> max
        }
    }

    fun minValue(dataPoints: List<PlotLineItem>, secondaryDimension: PlotSecondaryDimension? = null, applyRange: Boolean = true): Float? {
        val baseConfiguration = SecondaryDimensionConfiguration[secondaryDimension] ?: Configuration
        val min : Float? = when {
            dataPoints.isEmpty() -> baseConfiguration.Range.minNegative ?: 0f
            else -> {
                var minByData = dataPoints.minOf { it.bySecondaryDimension(secondaryDimension) }

                if (!applyRange) return minByData

                if (baseConfiguration.Range.minNegative != null) minByData = minByData.coerceAtMost(baseConfiguration.Range.minNegative)
                if (baseConfiguration.Range.maxNegative != null) minByData.coerceAtLeast(baseConfiguration.Range.maxNegative)

                minByData
            }
        }

        if (!applyRange) return min

        val minSmooth = when {
            min == null -> null
            baseConfiguration.Range.smoothAxis != null -> when (min % baseConfiguration.Range.smoothAxis) {
                0f -> min
                else -> min - (min % baseConfiguration.Range.smoothAxis) - baseConfiguration.Range.smoothAxis
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

    fun averageValue(dataPoints: List<PlotLineItem>, dimension: PlotDimension, secondaryDimension: PlotSecondaryDimension? = null): Float? {
        return when(dimension) {
            PlotDimension.INDEX -> averageValue(dataPoints, PlotHighlightMethod.AVG_BY_INDEX, secondaryDimension)
            PlotDimension.DISTANCE -> averageValue(dataPoints, PlotHighlightMethod.AVG_BY_DISTANCE, secondaryDimension)
            PlotDimension.TIME -> averageValue(dataPoints, PlotHighlightMethod.AVG_BY_TIME, secondaryDimension)
            PlotDimension.STATE_OF_CHARGE -> averageValue(dataPoints, PlotHighlightMethod.AVG_BY_STATE_OF_CHARGE, secondaryDimension)
        }
    }

    private fun averageValue(dataPoints: List<PlotLineItem>, averageMethod: PlotHighlightMethod, secondaryDimension: PlotSecondaryDimension? = null): Float? {
        if (dataPoints.isEmpty()) return null
        if (dataPoints.size == 1) return dataPoints.first().bySecondaryDimension(secondaryDimension)

        return when (averageMethod) {
            PlotHighlightMethod.AVG_BY_INDEX -> dataPoints.map { it.bySecondaryDimension(secondaryDimension) }.average().toFloat()
            PlotHighlightMethod.AVG_BY_DISTANCE -> {
                val value = dataPoints.filter { it.DistanceDelta != null }.map { (it.DistanceDelta?:0f) * it.bySecondaryDimension(secondaryDimension) }.sum()
                val distance = dataPoints.filter { it.DistanceDelta != null }.map { (it.DistanceDelta?:0f) }.sum()

                return when {
                    distance != 0f -> value / distance
                    else -> 0f
                }
            }
            PlotHighlightMethod.AVG_BY_TIME -> {
                val value = dataPoints.filter { it.TimeDelta != null }.map { (it.TimeDelta?:0L) * it.bySecondaryDimension(secondaryDimension) }.sum()
                val distance = dataPoints.filter { it.TimeDelta != null }.map { (it.TimeDelta?:0L) }.sum()

                return when {
                    distance != 0L -> value / distance
                    else -> 0f
                }
            }
            PlotHighlightMethod.AVG_BY_STATE_OF_CHARGE -> {
                val value = dataPoints.filter { it.StateOfChargeDelta != null }.map { (it.StateOfChargeDelta?:0f) * it.bySecondaryDimension(secondaryDimension) }.sum()
                val distance = dataPoints.filter { it.StateOfChargeDelta != null }.map { (it.StateOfChargeDelta?:0f) }.sum()

                return when {
                    distance != 0f -> value / distance
                    else -> 0f
                }
            }
            else -> null
        }
    }

    fun isEmpty(): Boolean {
        return dataPoints.isEmpty()
    }

    fun byHighlightMethod(dataPoints: List<PlotLineItem>, secondaryDimension: PlotSecondaryDimension? = null): Float? {
        if (dataPoints.isEmpty()) return null

        val configuration = when {
            secondaryDimension != null -> SecondaryDimensionConfiguration[secondaryDimension]
            else -> Configuration
        } ?: return null

        return when (configuration.HighlightMethod) {
            PlotHighlightMethod.MIN -> minValue(dataPoints, secondaryDimension, false)
            PlotHighlightMethod.MAX -> maxValue(dataPoints, secondaryDimension, false)
            PlotHighlightMethod.FIRST -> dataPoints.first().bySecondaryDimension(secondaryDimension)
            PlotHighlightMethod.LAST -> dataPoints.last().bySecondaryDimension(secondaryDimension)
            PlotHighlightMethod.AVG_BY_INDEX,
            PlotHighlightMethod.AVG_BY_DISTANCE,
            PlotHighlightMethod.AVG_BY_TIME,
            PlotHighlightMethod.AVG_BY_STATE_OF_CHARGE -> averageValue(dataPoints, configuration.HighlightMethod, secondaryDimension)
            else -> null
        }
    }

    fun x(dataPoints: List<PlotLineItem>, value: Long?, valueDimension: PlotDimension, targetDimension: PlotDimension, min: Any, max: Any) : Float? {
        if (dataPoints.isEmpty() || value == null) return null
        return when (targetDimension) {
            PlotDimension.DISTANCE -> when (valueDimension) {
                PlotDimension.TIME -> {
                    if (value !in dataPoints.first().Time .. dataPoints.last().Time) return null

                    val closePoint = dataPoints.minBy { abs(it.Time - value) }
                    when (closePoint.Marker) {
                        PlotLineMarkerType.BEGIN_SESSION -> x(closePoint.Distance - (closePoint.DistanceDelta ?: 0f), min, max)
                        else -> x(closePoint.Distance, min, max)
                    }
                }
                PlotDimension.DISTANCE -> x(value.toFloat(), min, max)
                else -> null
            }
            PlotDimension.TIME -> when (valueDimension) {
                PlotDimension.TIME -> x(value.toFloat(), min, max)
                PlotDimension.DISTANCE -> {
                    if (value.toFloat() !in dataPoints.first().Distance .. dataPoints.last().Distance) return null

                    val closePoint = dataPoints.minBy { abs(it.Distance - value) }
                    when (closePoint.Marker) {
                        PlotLineMarkerType.BEGIN_SESSION -> x(closePoint.Time - (closePoint.TimeDelta ?: 0L), min, max)
                        else -> x(closePoint.Distance, min, max)
                    }
                }
                else -> null
            }
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

    fun toPlotLineItemPointCollection(dataPoints: List<PlotLineItem>, dimension: PlotDimension, dimensionSmoothing: Float?, min: Any, max: Any): ArrayList<ArrayList<PlotLineItemPoint>> {
        val result = ArrayList<ArrayList<PlotLineItemPoint>>()
        var group = ArrayList<PlotLineItemPoint>()

        for (index in dataPoints.indices) {
            val item = dataPoints[index]

            if (item.Marker == PlotLineMarkerType.BEGIN_SESSION && index != 0) {
                group.add(
                    PlotLineItemPoint(
                        when (dimension) {
                            PlotDimension.INDEX -> x(index.toFloat(), min, max)
                            PlotDimension.DISTANCE -> x(item.Distance - (item.DistanceDelta ?: 0f), min, max)
                            PlotDimension.TIME -> x(item.Time - (item.TimeDelta ?: 0L), min, max)
                            PlotDimension.STATE_OF_CHARGE -> x(item.StateOfCharge - (item.StateOfChargeDelta ?: 0f), min, max)
                        },
                        item,
                        item.group(index, dimension, dimensionSmoothing)
                    )
                )
            }

            group.add(
                PlotLineItemPoint(
                    when (dimension) {
                        PlotDimension.INDEX -> x(index.toFloat(), min, max)
                        PlotDimension.DISTANCE -> x(item.Distance, min, max)
                        PlotDimension.TIME -> x(item.Time, min, max)
                        PlotDimension.STATE_OF_CHARGE -> x(item.StateOfCharge, min, max)
                    },
                    item,
                    item.group(index, dimension, dimensionSmoothing)
                )
            )

            if ((item.Marker ?: PlotLineMarkerType.BEGIN_SESSION) != PlotLineMarkerType.BEGIN_SESSION) {
                result.add(ArrayList(group.sortedBy { it.x }))
                group = ArrayList()
            }
        }

        if (group.isNotEmpty()) {
            result.add(ArrayList(group.sortedBy { it.x }))
        }

        return result
    }
}

