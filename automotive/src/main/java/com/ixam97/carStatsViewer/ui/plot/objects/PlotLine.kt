package com.ixam97.carStatsViewer.ui.plot.objects

import com.ixam97.carStatsViewer.ui.plot.enums.PlotDimensionX
import com.ixam97.carStatsViewer.ui.plot.enums.PlotDimensionY
import com.ixam97.carStatsViewer.ui.plot.enums.PlotHighlightMethod
import com.ixam97.carStatsViewer.ui.plot.enums.PlotLineMarkerType
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.absoluteValue

class PlotLine(
    val Configuration: PlotLineConfiguration,
    var Visible: Boolean = true
) {
    private val dataPoints: ConcurrentHashMap<Int, PlotLineItem> = ConcurrentHashMap()

    var baseLineAt: ArrayList<Float> = ArrayList()

    var alignZero: Boolean = false

    var zeroAt: Float? = null

    fun getDataPointsSize() = dataPoints.size

    fun addDataPoint(item: Float, epochTime: Long, nanoTime: Long, distance: Float, stateOfCharge: Float, altitude: Float? = null, timeDelta: Long? = null, distanceDelta: Float? = null, stateOfChargeDelta: Float? = null, altitudeDelta: Float? = null, plotLineMarkerType: PlotLineMarkerType? = null, autoMarkerTimeDeltaThreshold: Long? = null) {
        val prev = when (dataPoints[dataPoints.size - 1]?.Marker ?: PlotLineMarkerType.BEGIN_SESSION) {
            PlotLineMarkerType.BEGIN_SESSION -> dataPoints[dataPoints.size - 1]
            else -> null
        }

        addDataPoint(
            PlotLineItem(
            item,
            epochTime,
            nanoTime,
            distance,
            stateOfCharge,
            altitude,
            timeDelta?:(nanoTime - (prev?.NanoTime ?: nanoTime)),
            distanceDelta?:(distance - (prev?.Distance ?: distance)),
            stateOfChargeDelta?:(stateOfCharge - (prev?.StateOfCharge ?: stateOfCharge)),
            altitudeDelta?:(if (altitude == null || prev?.Altitude == null) { null } else { altitude - prev.Altitude!! }),
            plotLineMarkerType
        ), autoMarkerTimeDeltaThreshold)
    }

    fun addDataPoint(dataPoint: PlotLineItem, autoMarkerTimeDeltaThreshold: Long? = null) {
        val prev = dataPoints[dataPoints.size - 1]

        if (dataPoint.Marker == PlotLineMarkerType.BEGIN_SESSION && prev?.Marker == null) {
            prev?.Marker = PlotLineMarkerType.END_SESSION
        }

        if (dataPoint.Marker == null && (prev == null || prev.Marker == PlotLineMarkerType.END_SESSION)) {
            dataPoint.Marker = PlotLineMarkerType.BEGIN_SESSION
        }

        if ((autoMarkerTimeDeltaThreshold ?: dataPoint.TimeDelta ?: 0L) < (dataPoint.TimeDelta ?: 0L)) {
            prev?.Marker = PlotLineMarkerType.END_SESSION
            dataPoint.Marker = PlotLineMarkerType.BEGIN_SESSION
        } else if (prev?.Marker == PlotLineMarkerType.BEGIN_SESSION) {
            if ((prev.StateOfCharge - dataPoint.StateOfCharge).absoluteValue > 1) {
                // Car gives an old value for SoC at the end of hibernation. just override that. Bit hacky though...
                prev.StateOfCharge = dataPoint.StateOfCharge
                dataPoint.StateOfChargeDelta = 0f
            }
            if (prev.Value < dataPoint.Value - 1_000) {
                prev.Value = dataPoint.Value
            }
        }

        if (dataPoint.Marker == PlotLineMarkerType.BEGIN_SESSION) {
            dataPoint.TimeDelta = 0L
            dataPoint.DistanceDelta = 0f
            dataPoint.StateOfChargeDelta = 0f
        }

        when {
            dataPoint.Value.isFinite() -> {
                dataPoints[dataPoints.size] = dataPoint
            }
        }
    }

    fun addDataPoints(dataPoints: List<PlotLineItem>) {
        if (dataPoints.isEmpty()) return

        val last = dataPoints.last()

        // make sure to close the last marker on restore, next item will then be a BEGIN
        last.Marker = when (last.Marker) {
            PlotLineMarkerType.BEGIN_SESSION -> PlotLineMarkerType.SINGLE_SESSION
            else -> PlotLineMarkerType.END_SESSION
        }

        for (dataPoint in dataPoints) {
            addDataPoint(dataPoint)
        }
    }

    fun reset() {
        dataPoints.clear()
    }

    fun getDataPoints(dimension: PlotDimensionX, dimensionRestriction: Long? = null, dimensionShift: Long? = null): List<PlotLineItem> {
        return when {
            dataPoints.isEmpty() || dimensionRestriction == null -> dataPoints.map { it.value }
            else ->  {
                val min = minDimension(dimension, dimensionRestriction, dimensionShift)
                val max = maxDimension(dimension, dimensionRestriction, dimensionShift)

                getDataPoints (dimension, min, max)
            }
        }
    }

    private fun getDataPoints(dimension: PlotDimensionX, min: Any?, max: Any?): List<PlotLineItem> {
        return when {
            dataPoints.isEmpty() || min == null || max == null -> dataPoints.map { it.value }
            else ->  when (dimension) {
                PlotDimensionX.INDEX -> dataPoints.filter { it.key in min as Int..max as Int }.map { it.value }
                PlotDimensionX.DISTANCE -> dataPoints.filter { it.value.Distance in min as Float..max as Float }.map { it.value }
                PlotDimensionX.TIME -> dataPoints.filter { it.value.EpochTime in min as Long..max as Long }.map { it.value }
                PlotDimensionX.STATE_OF_CHARGE -> dataPoints.filter { it.value.StateOfCharge in min as Float..max as Float }.map { it.value }
            }
        }
    }

    internal fun minDimension(dimension: PlotDimensionX, dimensionRestriction: Long? = null, dimensionShift: Long? = null): Any? {
        return when {
            dataPoints.isEmpty() -> null
            else -> when (dimension) {
                PlotDimensionX.INDEX -> when(dimensionRestriction) {
                    null -> dataPoints.minOf { it.key }
                    else -> maxDimension(dimension, dimensionRestriction, dimensionShift) as Int - dimensionRestriction
                }
                PlotDimensionX.DISTANCE -> when(dimensionRestriction) {
                    null -> dataPoints.minOf { it.value.Distance }
                    else -> maxDimension(dimension, dimensionRestriction, dimensionShift) as Float - dimensionRestriction
                }
                PlotDimensionX.TIME -> when(dimensionRestriction) {
                    null -> dataPoints.minOf { it.value.EpochTime }
                    else -> dataPoints.minOf { it.value.EpochTime } + (dimensionShift ?: 0L)
                }
                PlotDimensionX.STATE_OF_CHARGE -> 0f
            }
        }
    }

    internal fun maxDimension(dimension: PlotDimensionX, dimensionRestriction: Long? = null, dimensionShift: Long? = null): Any? {
        return when {
            dataPoints.isEmpty() -> null
            else -> when (dimension) {
                PlotDimensionX.INDEX -> when(dimensionRestriction) {
                    null -> dataPoints.maxOf { it.key }
                    else -> dataPoints.maxOf { it.key } - (dimensionShift ?: 0L)
                }
                PlotDimensionX.DISTANCE -> when(dimensionRestriction) {
                    null -> dataPoints.maxOf { it.value.Distance }
                    else -> dataPoints.maxOf { it.value.Distance } - (dimensionShift ?: 0L)
                }
                PlotDimensionX.TIME -> when(dimensionRestriction) {
                    null -> dataPoints.maxOf { it.value.EpochTime }
                    else -> minDimension(dimension, dimensionRestriction, dimensionShift) as Long + dimensionRestriction
                }
                PlotDimensionX.STATE_OF_CHARGE -> 100f
            }
        }
    }

    fun distanceDimension(dimension: PlotDimensionX, dimensionRestriction: Long? = null, dimensionShift: Long? = null): Float? {
        return distanceDimensionMinMax(
            dimension,
            minDimension(dimension, dimensionRestriction, dimensionShift),
            maxDimension(dimension, dimensionRestriction, dimensionShift)
        )
    }

    fun distanceDimensionMinMax(dimension: PlotDimensionX, min: Any?, max: Any?): Float? {
        if (min == null || max == null) return null

        return when (dimension) {
            PlotDimensionX.TIME -> (max as Long - min as Long).toFloat()
            else -> max as Float - min as Float
        }
    }

    fun maxValue(dataPoints: List<PlotLineItem>, dimensionY: PlotDimensionY? = null, applyRange: Boolean = true): Float? {
        val baseConfiguration = PlotGlobalConfiguration.DimensionYConfiguration[dimensionY] ?: Configuration
        val max : Float? = when {
            dataPoints.isEmpty() -> when {
                baseConfiguration.Range.minPositive == null || !applyRange -> null
                else -> baseConfiguration.Range.minPositive
            }
            else -> {
                var maxByData = dataPoints.mapNotNull { it.byDimensionY(dimensionY) }.maxOfOrNull { it }

                if (applyRange) {
                    if (baseConfiguration.Range.minPositive != null) maxByData = (maxByData?:0f).coerceAtLeast(baseConfiguration.Range.minPositive)
                    if (baseConfiguration.Range.maxPositive != null) maxByData = (maxByData?:0f).coerceAtMost(baseConfiguration.Range.maxPositive)
                }

                maxByData
            }
        }

        if (!applyRange) return max

        return when {
            max == null -> null
            baseConfiguration.Range.smoothAxis != null -> when (max % (baseConfiguration.Range.smoothAxis * baseConfiguration.UnitFactor)) {
                0f -> max
                else -> max + ((baseConfiguration.Range.smoothAxis * baseConfiguration.UnitFactor) - max % (baseConfiguration.Range.smoothAxis * baseConfiguration.UnitFactor))
            }
            else -> max
        }
    }

    fun minValue(dataPoints: List<PlotLineItem>, secondaryDimension: PlotDimensionY? = null, applyRange: Boolean = true): Float? {
        val baseConfiguration = PlotGlobalConfiguration.DimensionYConfiguration[secondaryDimension] ?: Configuration
        val min : Float? = when {
            dataPoints.isEmpty() -> when {
                baseConfiguration.Range.minNegative == null || !applyRange -> null
                else  -> baseConfiguration.Range.minNegative
            }
            else -> {
                var minByData = dataPoints.mapNotNull { it.byDimensionY(secondaryDimension) }.minOfOrNull { it }

                if (applyRange) {
                    if (baseConfiguration.Range.minNegative != null) minByData = (minByData?:0f).coerceAtMost(baseConfiguration.Range.minNegative)
                    if (baseConfiguration.Range.maxNegative != null) minByData = (minByData?:0f).coerceAtLeast(baseConfiguration.Range.maxNegative)
                }

                minByData
            }
        }

        if (!applyRange) return min

        val minSmooth = when {
            min == null -> null
            baseConfiguration.Range.smoothAxis != null -> when (min % (baseConfiguration.Range.smoothAxis * baseConfiguration.UnitFactor)) {
                0f -> min
                else -> min - (min % (baseConfiguration.Range.smoothAxis * baseConfiguration.UnitFactor)) - (baseConfiguration.Range.smoothAxis * baseConfiguration.UnitFactor)
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

    fun averageValue(dataPoints: List<PlotLineItem>, dimension: PlotDimensionX, secondaryDimension: PlotDimensionY? = null): Float? {
        return when(dimension) {
            PlotDimensionX.INDEX -> averageValue(dataPoints, PlotHighlightMethod.AVG_BY_INDEX, secondaryDimension)
            PlotDimensionX.DISTANCE -> averageValue(dataPoints, PlotHighlightMethod.AVG_BY_DISTANCE, secondaryDimension)
            PlotDimensionX.TIME -> averageValue(dataPoints, PlotHighlightMethod.AVG_BY_TIME, secondaryDimension)
            PlotDimensionX.STATE_OF_CHARGE -> averageValue(dataPoints, PlotHighlightMethod.AVG_BY_STATE_OF_CHARGE, secondaryDimension)
        }
    }

    private fun averageValue(dataPoints: List<PlotLineItem>, averageMethod: PlotHighlightMethod, secondaryDimension: PlotDimensionY? = null): Float? {
        if (dataPoints.isEmpty()) return null

        val filtered = dataPoints.filter { (it.byDimensionY(secondaryDimension) ?: 0f) != 0f }

        if (filtered.isEmpty()) return null
        if (filtered.size == 1) return filtered.first().byDimensionY(secondaryDimension)

        val averageValue = when (averageMethod) {
            PlotHighlightMethod.AVG_BY_INDEX -> filtered.mapNotNull { it.byDimensionY(secondaryDimension) }.average().toFloat()
            PlotHighlightMethod.AVG_BY_DISTANCE -> {
                val filteredAverageMethod = filtered.filter { (it.DistanceDelta ?: 0f) != 0f }
                val value = filteredAverageMethod.map { (it.DistanceDelta ?: 0f) * (it.byDimensionY(secondaryDimension) ?: 0f) }.sum()
                val distance = filteredAverageMethod.map { (it.DistanceDelta ?: 0f) }.sum()

                when {
                    distance != 0f -> value / distance
                    else -> null
                }
            }
            PlotHighlightMethod.AVG_BY_TIME -> {
                val filteredAverageMethod = filtered.filter { (it.TimeDelta ?: 0f) != 0f  }
                val value = filteredAverageMethod.map { (it.TimeDelta ?: 0L) * (it.byDimensionY(secondaryDimension) ?: 0f) }.sum()
                val distance = filteredAverageMethod.sumOf { (it.TimeDelta ?: 0L) }

                when {
                    distance != 0L -> value / distance
                    else -> null
                }
            }
            PlotHighlightMethod.AVG_BY_STATE_OF_CHARGE -> {
                val filteredAverageMethod = filtered.filter { (it.StateOfChargeDelta ?: 0f) != 0f }
                val value = filteredAverageMethod.map { (it.StateOfChargeDelta ?: 0f) * (it.byDimensionY(secondaryDimension) ?: 0f) }.sum()
                val distance = filteredAverageMethod.map { (it.StateOfChargeDelta ?: 0f) }.sum()

                when {
                    distance != 0f -> value / distance
                    else -> null
                }
            }
            else -> null
        }

        if (averageValue != null) return averageValue

        val nonNull = filtered.mapNotNull { it.byDimensionY(secondaryDimension) }

        return when {
            nonNull.isEmpty() -> null
            else -> nonNull.sum() / nonNull.size
        }
    }

    fun isEmpty(): Boolean {
        return dataPoints.isEmpty()
    }

    fun byHighlightMethod(dataPoints: List<PlotLineItem>, dimension: PlotDimensionX, secondaryDimension: PlotDimensionY? = null): Float? {
        if (dataPoints.isEmpty()) return null

        val configuration = when {
            secondaryDimension != null -> PlotGlobalConfiguration.DimensionYConfiguration[secondaryDimension]
            else -> Configuration
        } ?: return null

        return byHighlightMethod(configuration.HighlightMethod, dataPoints, dimension, secondaryDimension)
    }

    fun byHighlightMethod(highlightMethod: PlotHighlightMethod, dataPoints: List<PlotLineItem>, dimension: PlotDimensionX, secondaryDimension: PlotDimensionY? = null): Float? {
        if (dataPoints.isEmpty()) return null

        return when (val inlineHighlightMethod = when (highlightMethod) {
            PlotHighlightMethod.AVG_BY_DIMENSION -> when (dimension) {
                PlotDimensionX.INDEX -> PlotHighlightMethod.AVG_BY_INDEX
                PlotDimensionX.DISTANCE -> PlotHighlightMethod.AVG_BY_DISTANCE
                PlotDimensionX.TIME -> PlotHighlightMethod.AVG_BY_TIME
                PlotDimensionX.STATE_OF_CHARGE -> PlotHighlightMethod.AVG_BY_STATE_OF_CHARGE
            }
            else -> highlightMethod
        }) {
            PlotHighlightMethod.MIN -> minValue(dataPoints, secondaryDimension, false)
            PlotHighlightMethod.MAX -> maxValue(dataPoints, secondaryDimension, false)
            PlotHighlightMethod.FIRST -> dataPoints.first().byDimensionY(secondaryDimension)
            PlotHighlightMethod.LAST -> dataPoints.last().byDimensionY(secondaryDimension)
            PlotHighlightMethod.AVG_BY_INDEX,
            PlotHighlightMethod.AVG_BY_DISTANCE,
            PlotHighlightMethod.AVG_BY_TIME,
            PlotHighlightMethod.AVG_BY_STATE_OF_CHARGE -> averageValue(dataPoints, inlineHighlightMethod, secondaryDimension)
            else -> null
        }
    }

    private fun x(index: Float, min: Any, max: Any) : Float {
        return PlotLineItem.cord(
            index,
            min as Float,
            max as Float
        )
    }

    private fun x(index: Long, min: Any, max: Any) : Float {
        return PlotLineItem.cord(
            index,
            min as Long,
            max as Long
        )
    }

    fun toPlotLineItemPointCollection(dataPoints: List<PlotLineItem>, dimension: PlotDimensionX, dimensionSmoothing: Float?, min: Any, max: Any): ArrayList<ArrayList<PlotPoint>> {
        val result = ArrayList<ArrayList<PlotPoint>>()
        var group = ArrayList<PlotPoint>()

        for (index in dataPoints.indices) {
            val item = dataPoints[index]

            group.add(
                PlotPoint(
                    when (dimension) {
                        PlotDimensionX.INDEX -> x(index.toFloat(), min, max)
                        PlotDimensionX.DISTANCE -> x(item.Distance, min, max)
                        PlotDimensionX.TIME -> x(item.EpochTime, min, max)
                        PlotDimensionX.STATE_OF_CHARGE -> x(item.StateOfCharge, min, max)
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

