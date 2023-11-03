package com.coderax.carStatsViewer.ui.plot.objects

import android.util.Log
import com.coderax.carStatsViewer.ui.plot.enums.PlotDimensionX
import com.coderax.carStatsViewer.ui.plot.enums.PlotDimensionY
import com.coderax.carStatsViewer.ui.plot.enums.PlotHighlightMethod
import com.coderax.carStatsViewer.ui.plot.enums.PlotLineMarkerType
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.absoluteValue

class PlotLine(
    val Configuration: PlotLineConfiguration,
    var Visible: Boolean = true
) {
    companion object {
        var plotLineItemLimit = 300
    }

    private val dataPoints: ConcurrentHashMap<Int, PlotLineItem> = ConcurrentHashMap()

    var baseLineAt: ArrayList<Float> = ArrayList()

    var alignZero: Boolean = false

    var zeroAt: Float? = null

    fun getDataPointsSize() = dataPoints.size

    fun lastItem() : PlotLineItem? = when {
        dataPoints.isEmpty() -> null
        else -> dataPoints[dataPoints.size - 1]
    }

    fun addDataPoint(item: Float, epochTime: Long, nanoTime: Long, distance: Float, stateOfCharge: Float, altitude: Float? = null, timeDelta: Long? = null, distanceDelta: Float? = null, stateOfChargeDelta: Float? = null, altitudeDelta: Float? = null, plotLineMarkerType: PlotLineMarkerType? = null, autoMarkerTimeDeltaThreshold: Long? = null): PlotLineItem? {
        val prev = when (dataPoints[dataPoints.size - 1]?.Marker ?: PlotLineMarkerType.BEGIN_SESSION) {
            PlotLineMarkerType.BEGIN_SESSION -> dataPoints[dataPoints.size - 1]
            else -> null
        }

        return addDataPoint(
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

    fun addDataPoint(dataPoint: PlotLineItem, autoMarkerTimeDeltaThreshold: Long? = null): PlotLineItem? {
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

        return when {
            dataPoint.Value.isFinite() -> {
                dataPoints[dataPoints.size] = dataPoint
                dataPoint
            }
            else -> null
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

    fun getDataPoints(dimension: PlotDimensionX, dimensionRestriction: Long? = null, dimensionShift: Long? = null, surplus: Boolean = false): List<PlotLineItem> {
        return when {
            dataPoints.isEmpty() || dimensionRestriction == null -> dataPoints.map { it.value }
            else ->  {
                val min = minDimension(dimension, dimensionRestriction, dimensionShift, surplus)
                val max = maxDimension(dimension, dimensionRestriction, dimensionShift, surplus)

                getDataPoints (dimension, min, max)
            }
        }
    }

    private fun combineDataPoints(dataPointLeft: PlotLineItem, dataPointRight: PlotLineItem): PlotLineItem {
        val newDistanceDelta = (dataPointLeft.DistanceDelta?:0.0f) + (dataPointRight.DistanceDelta?:0.0f)

        // This needs to be changed once we switch to energy instead of consumption as value!!!
        val newValue = when {
            (dataPointLeft.DistanceDelta?:0.0f) == 0.0f || (dataPointRight.DistanceDelta?:0.0f) == 0.0f -> 0.0f
            else -> {
                ((dataPointLeft.Value * dataPointLeft.DistanceDelta!!) + (dataPointRight.Value * dataPointRight.DistanceDelta!!)) / newDistanceDelta
            }
        }
        return dataPointRight.copy(
            TimeDelta = (dataPointLeft.TimeDelta?:0) + (dataPointRight.TimeDelta?:0),
            DistanceDelta = newDistanceDelta,
            StateOfChargeDelta = (dataPointRight.StateOfCharge - dataPointLeft.StateOfCharge),
            AltitudeDelta = (dataPointLeft.AltitudeDelta?:0.0f) + (dataPointRight.AltitudeDelta?:0.0f),
            Value = newValue,
            Marker = when {
                (dataPointLeft.Marker == PlotLineMarkerType.BEGIN_SESSION || dataPointRight.Marker == PlotLineMarkerType.BEGIN_SESSION) -> PlotLineMarkerType.BEGIN_SESSION
                (dataPointLeft.Marker == PlotLineMarkerType.END_SESSION || dataPointRight.Marker == PlotLineMarkerType.END_SESSION) -> PlotLineMarkerType.END_SESSION
                else -> null
            }
        )
    }

    private fun createLodDataPoints(dataPoints: List<PlotLineItem>, plotDimensionX: PlotDimensionX): List<PlotLineItem> {
        if (dataPoints.isEmpty()) return dataPoints
        Log.i("PLOT", "Create LoD data points ...")
        Log.i("PLOT", "Data points size: ${dataPoints.size}")

        return when (plotDimensionX) {
            PlotDimensionX.DISTANCE -> {
                val lodDataPoints: ArrayList<PlotLineItem> = arrayListOf()
                if (dataPoints.size > plotLineItemLimit) {
                    var index = 0
                    while (index < dataPoints.size - 1) {
                        lodDataPoints.add(combineDataPoints(dataPoints[index], dataPoints[index + 1]))
                        if (lodDataPoints.size >= 2) {
                            if (lodDataPoints.last().Marker == PlotLineMarkerType.BEGIN_SESSION) {
                                lodDataPoints[lodDataPoints.size - 2].Marker = PlotLineMarkerType.END_SESSION
                            }
                        }
                        index += 2
                    }
                    createLodDataPoints(lodDataPoints, plotDimensionX)
                } else {
                    dataPoints
                }
            }
            else -> {
                dataPoints
            }
        }
    }

    private fun getDataPoints(dimension: PlotDimensionX, min: Any?, max: Any?): List<PlotLineItem> {
        return when {
            dataPoints.isEmpty() || min == null || max == null -> dataPoints.map { it.value }
            else ->  when (dimension) {
                PlotDimensionX.INDEX -> dataPoints.filter { it.key in min as Int..max as Int }.map { it.value }
                PlotDimensionX.DISTANCE -> {
                    val filteredDataPoints = dataPoints.filter { it.value.Distance in min as Float..max as Float }.map { it.value }
                    createLodDataPoints(filteredDataPoints, PlotDimensionX.DISTANCE)
                }
                PlotDimensionX.TIME -> dataPoints.filter { it.value.EpochTime in min as Long..max as Long }.map { it.value }
                PlotDimensionX.STATE_OF_CHARGE -> dataPoints.filter { it.value.StateOfCharge in min as Float..max as Float }.map { it.value }
            }
        }
    }

    internal fun minDimension(dimension: PlotDimensionX, dimensionRestriction: Long? = null, dimensionShift: Long? = null, surplus: Boolean = false): Any? {
        return when {
            dataPoints.isEmpty() -> null
            else -> when (dimension) {
                PlotDimensionX.INDEX -> when(dimensionRestriction) {
                    null -> dataPoints.minOf { it.key }
                    else -> maxDimension(dimension, dimensionRestriction, dimensionShift) as Int - (if (surplus) 2 * dimensionRestriction else dimensionRestriction)
                }
                PlotDimensionX.DISTANCE -> when(dimensionRestriction) {
                    null -> dataPoints.minOf { it.value.Distance }
                    else -> maxDimension(dimension, dimensionRestriction, dimensionShift) as Float - (if (surplus) 2 * dimensionRestriction else dimensionRestriction)
                }
                PlotDimensionX.TIME -> when(dimensionRestriction) {
                    null -> dataPoints.minOf { it.value.EpochTime }
                    else -> dataPoints.minOf { it.value.EpochTime } + (dimensionShift ?: 0L)
                }
                PlotDimensionX.STATE_OF_CHARGE -> 0f
            }
        }
    }

    internal fun maxDimension(dimension: PlotDimensionX, dimensionRestriction: Long? = null, dimensionShift: Long? = null, surplus: Boolean = false): Any? {
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
                    else -> minDimension(dimension, dimensionRestriction, dimensionShift) as Long + (if (surplus) 2 * dimensionRestriction else dimensionRestriction)
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

        val dataPointsNonNull = dataPoints.map { Pair(it, it.byDimensionY(secondaryDimension)) }.filter { it.second != null }
        if (dataPointsNonNull.isEmpty()) return null

        if (dataPointsNonNull.all { (it.second ?: 0f) == 0f }) return null
        if (dataPointsNonNull.size == 1) return dataPointsNonNull.map { it.second }.first()

        val averageValue = when (averageMethod) {
            PlotHighlightMethod.AVG_BY_INDEX -> dataPointsNonNull.mapNotNull { it.second }.average().toFloat()
            PlotHighlightMethod.AVG_BY_DISTANCE -> {
                val value = dataPointsNonNull.map { (it.first.DistanceDelta ?: 0f) * (it.second ?: 0f) }.sum()
                val distance = dataPointsNonNull.map { (it.first.DistanceDelta ?: 0f) }.sum()

                when {
                    distance != 0f -> value / distance
                    else -> null
                }
            }
            PlotHighlightMethod.AVG_BY_TIME -> {
                val value = dataPointsNonNull.map { (it.first.TimeDelta ?: 0L) * (it.second ?: 0f) }.sum()
                val distance = dataPointsNonNull.sumOf { (it.first.TimeDelta ?: 0L) }

                when {
                    distance != 0L -> value / distance
                    else -> null
                }
            }
            PlotHighlightMethod.AVG_BY_STATE_OF_CHARGE -> {
                val value = dataPointsNonNull.map { (it.first.StateOfChargeDelta ?: 0f) * (it.second ?: 0f) }.sum()
                val distance = dataPointsNonNull.map { (it.first.StateOfChargeDelta ?: 0f) }.sum()

                when {
                    distance != 0f -> value / distance
                    else -> null
                }
            }
            else -> null
        }

        if (averageValue != null) return averageValue

        val nonNull = dataPointsNonNull.mapNotNull { it.second }

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

