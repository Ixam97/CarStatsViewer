package com.ixam97.carStatsViewer.plot.objects

import com.ixam97.carStatsViewer.plot.graphics.*
import com.ixam97.carStatsViewer.plot.enums.*
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

    fun addDataPoint(item: Float, epochTime: Long, nanoTime: Long, distance: Float, stateOfCharge: Float, timeDelta: Long? = null, distanceDelta: Float? = null, stateOfChargeDelta: Float? = null, plotLineMarkerType: PlotLineMarkerType? = null, autoMarkerTimeDeltaThreshold: Long? = null) {
        val prev = when (dataPoints[dataPoints.size - 1]?.Marker ?: PlotLineMarkerType.BEGIN_SESSION) {
            PlotLineMarkerType.BEGIN_SESSION -> dataPoints[dataPoints.size - 1]
            else -> null
        }

        addDataPoint(PlotLineItem(
            item,
            epochTime,
            nanoTime,
            distance,
            stateOfCharge,
            timeDelta?:(nanoTime - (prev?.NanoTime ?: nanoTime)),
            distanceDelta?:(distance - (prev?.Distance ?: distance)),
            stateOfChargeDelta?:(stateOfCharge - (prev?.StateOfCharge ?: stateOfCharge)),
            plotLineMarkerType
        ), autoMarkerTimeDeltaThreshold)
    }

    fun addDataPoint(dataPoint: PlotLineItem, autoMarkerTimeDeltaThreshold: Long? = null) {
        val prev = dataPoints[dataPoints.size - 1]

        if (dataPoint.Marker == PlotLineMarkerType.BEGIN_SESSION && prev?.Marker == null) {
            prev?.Marker = PlotLineMarkerType.END_SESSION
        }
        
        if (dataPoint.Marker == null && (prev == null || prev?.Marker == PlotLineMarkerType.END_SESSION)) {
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

    fun getDataPoints(dimension: PlotDimension, dimensionRestriction: Long? = null, dimensionShift: Long? = null): List<PlotLineItem> {
        return when {
            dataPoints.isEmpty() || dimensionRestriction == null -> dataPoints.map { it.value }
            else ->  {
                val min = minDimension(dimension, dimensionRestriction, dimensionShift)
                val max = maxDimension(dimension, dimensionRestriction, dimensionShift)

                getDataPoints (dimension, min, max)
            }
        }
    }

    private fun getDataPoints(dimension: PlotDimension, min: Any?, max: Any?): List<PlotLineItem> {
        return when {
            dataPoints.isEmpty() || min == null || max == null -> dataPoints.map { it.value }
            else ->  when (dimension) {
                PlotDimension.INDEX -> dataPoints.filter { it.key in min as Int..max as Int }.map { it.value }
                PlotDimension.DISTANCE -> dataPoints.filter { it.value.Distance in min as Float..max as Float }.map { it.value }
                PlotDimension.TIME -> dataPoints.filter { it.value.EpochTime in min as Long..max as Long }.map { it.value }
                PlotDimension.STATE_OF_CHARGE -> dataPoints.filter { it.value.StateOfCharge in min as Float..max as Float }.map { it.value }
            }
        }
    }

    internal fun minDimension(dimension: PlotDimension, dimensionRestriction: Long? = null, dimensionShift: Long? = null): Any? {
        return when {
            dataPoints.isEmpty() -> null
            else -> when (dimension) {
                PlotDimension.INDEX -> when(dimensionRestriction) {
                    null -> dataPoints.minOf { it.key }
                    else -> maxDimension(dimension, dimensionRestriction, dimensionShift) as Int - dimensionRestriction
                }
                PlotDimension.DISTANCE -> when(dimensionRestriction) {
                    null -> dataPoints.minOf { it.value.Distance }
                    else -> maxDimension(dimension, dimensionRestriction, dimensionShift) as Float - dimensionRestriction
                }
                PlotDimension.TIME -> when(dimensionRestriction) {
                    null -> dataPoints.minOf { it.value.EpochTime }
                    else -> dataPoints.minOf { it.value.EpochTime } + (dimensionShift ?: 0L)
                }
                PlotDimension.STATE_OF_CHARGE -> 0f
            }
        }
    }

    internal fun maxDimension(dimension: PlotDimension, dimensionRestriction: Long? = null, dimensionShift: Long? = null): Any? {
        return when {
            dataPoints.isEmpty() -> null
            else -> when (dimension) {
                PlotDimension.INDEX -> when(dimensionRestriction) {
                    null -> dataPoints.maxOf { it.key }
                    else -> dataPoints.maxOf { it.key } - (dimensionShift ?: 0L)
                }
                PlotDimension.DISTANCE -> when(dimensionRestriction) {
                    null -> dataPoints.maxOf { it.value.Distance }
                    else -> dataPoints.maxOf { it.value.Distance } - (dimensionShift ?: 0L)
                }
                PlotDimension.TIME -> when(dimensionRestriction) {
                    null -> dataPoints.maxOf { it.value.EpochTime }
                    else -> minDimension(dimension, dimensionRestriction, dimensionShift) as Long + dimensionRestriction
                }
                PlotDimension.STATE_OF_CHARGE -> 100f
            }
        }
    }

    fun distanceDimension(dimension: PlotDimension, dimensionRestriction: Long? = null, dimensionShift: Long? = null): Float? {
        return distanceDimensionMinMax(
            dimension,
            minDimension(dimension, dimensionRestriction, dimensionShift),
            maxDimension(dimension, dimensionRestriction, dimensionShift)
        )
    }

    fun distanceDimensionMinMax(dimension: PlotDimension, min: Any?, max: Any?): Float? {
        if (min == null || max == null) return null
        
        return when (dimension) {
            PlotDimension.TIME -> (max as Long - min as Long).toFloat()
            else -> max as Float - min as Float
        }
    }

    fun maxValue(dataPoints: List<PlotLineItem>, secondaryDimension: PlotSecondaryDimension? = null, applyRange: Boolean = true): Float? {
        val baseConfiguration = PlotGlobalConfiguration.SecondaryDimensionConfiguration[secondaryDimension] ?: Configuration
        val max : Float? = when {
            dataPoints.isEmpty() -> when {
                baseConfiguration.Range.minPositive == null || !applyRange -> null
                else -> baseConfiguration.Range.minPositive
            }
            else -> {
                var maxByData = dataPoints.mapNotNull { it.bySecondaryDimension(secondaryDimension) }.maxOfOrNull { it }

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

    fun minValue(dataPoints: List<PlotLineItem>, secondaryDimension: PlotSecondaryDimension? = null, applyRange: Boolean = true): Float? {
        val baseConfiguration = PlotGlobalConfiguration.SecondaryDimensionConfiguration[secondaryDimension] ?: Configuration
        val min : Float? = when {
            dataPoints.isEmpty() -> when {
                baseConfiguration.Range.minNegative == null || !applyRange -> null
                else  -> baseConfiguration.Range.minNegative
            }
            else -> {
                var minByData = dataPoints.mapNotNull { it.bySecondaryDimension(secondaryDimension) }.minOfOrNull { it }

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

        val averageValue = when (averageMethod) {
            PlotHighlightMethod.AVG_BY_INDEX -> dataPoints.mapNotNull { it.bySecondaryDimension(secondaryDimension) }.average().toFloat()
            PlotHighlightMethod.AVG_BY_DISTANCE -> {
                val value = dataPoints.map { (it.DistanceDelta?:0f) * (it.bySecondaryDimension(secondaryDimension)?:0f) }.sum()
                val distance = dataPoints.map { (it.DistanceDelta?:0f) }.sum()

                when {
                    distance != 0f -> value / distance
                    else -> null
                }
            }
            PlotHighlightMethod.AVG_BY_TIME -> {
                val value = dataPoints.map { (it.TimeDelta?:0L) * (it.bySecondaryDimension(secondaryDimension)?:0f) }.sum()
                val distance = dataPoints.sumOf { (it.TimeDelta?:0L) }

                when {
                    distance != 0L -> value / distance
                    else -> null
                }
            }
            PlotHighlightMethod.AVG_BY_STATE_OF_CHARGE -> {
                val value = dataPoints.map { (it.StateOfChargeDelta?:0f) * (it.bySecondaryDimension(secondaryDimension)?:0f) }.sum()
                val distance = dataPoints.map { (it.StateOfChargeDelta?:0f) }.sum()

                when {
                    distance != 0f -> value / distance
                    else -> null
                }
            }
            else -> null
        }

        if (averageValue != null) return averageValue

        val nonNull = dataPoints.mapNotNull { it.bySecondaryDimension(secondaryDimension) }

        return when {
            nonNull.isEmpty() -> null
            else -> nonNull.sum() / nonNull.size
        }
    }

    fun isEmpty(): Boolean {
        return dataPoints.isEmpty()
    }

    fun byHighlightMethod(dataPoints: List<PlotLineItem>, dimension: PlotDimension, secondaryDimension: PlotSecondaryDimension? = null): Float? {
        if (dataPoints.isEmpty()) return null

        val configuration = when {
            secondaryDimension != null -> PlotGlobalConfiguration.SecondaryDimensionConfiguration[secondaryDimension]
            else -> Configuration
        } ?: return null

        val highlightMethod = when (configuration.HighlightMethod) {
            PlotHighlightMethod.AVG_BY_DIMENSION -> when (dimension) {
                PlotDimension.INDEX -> PlotHighlightMethod.AVG_BY_INDEX
                PlotDimension.DISTANCE -> PlotHighlightMethod.AVG_BY_DISTANCE
                PlotDimension.TIME -> PlotHighlightMethod.AVG_BY_TIME
                PlotDimension.STATE_OF_CHARGE -> PlotHighlightMethod.AVG_BY_STATE_OF_CHARGE
            }
            else -> configuration.HighlightMethod
        }

        return when (highlightMethod) {
            PlotHighlightMethod.MIN -> minValue(dataPoints, secondaryDimension, false)
            PlotHighlightMethod.MAX -> maxValue(dataPoints, secondaryDimension, false)
            PlotHighlightMethod.FIRST -> dataPoints.first().bySecondaryDimension(secondaryDimension)
            PlotHighlightMethod.LAST -> dataPoints.last().bySecondaryDimension(secondaryDimension)
            PlotHighlightMethod.AVG_BY_INDEX,
            PlotHighlightMethod.AVG_BY_DISTANCE,
            PlotHighlightMethod.AVG_BY_TIME,
            PlotHighlightMethod.AVG_BY_STATE_OF_CHARGE -> averageValue(dataPoints, highlightMethod, secondaryDimension)
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

    fun toPlotLineItemPointCollection(dataPoints: List<PlotLineItem>, dimension: PlotDimension, dimensionSmoothing: Float?, min: Any, max: Any): ArrayList<ArrayList<PlotPoint>> {
        val result = ArrayList<ArrayList<PlotPoint>>()
        var group = ArrayList<PlotPoint>()

        for (index in dataPoints.indices) {
            val item = dataPoints[index]

            group.add(
                PlotPoint(
                    when (dimension) {
                        PlotDimension.INDEX -> x(index.toFloat(), min, max)
                        PlotDimension.DISTANCE -> x(item.Distance, min, max)
                        PlotDimension.TIME -> x(item.EpochTime, min, max)
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

