package com.ixam97.carStatsViewer

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

    val LabelPosition: PlotLabelPosition,
    val HighlightMethod: PlotHighlightMethod
) {
    private val dataPoints: ArrayList<PlotLineItem> = ArrayList()

    var visible: Boolean = true

    var baseLineAt: ArrayList<Float> = ArrayList()

    var plotPaint: PlotPaint? = null

     fun addDataPoint(item: Float, timestamp: Float, distance: Float) {
        dataPoints.add(PlotLineItem(item, timestamp, distance))
    }

    fun addDataPoints(items: ArrayList<PlotLineItem>) {
        for (item in items) {
            dataPoints.add(item)
        }
    }

    fun setDataPoints(items: ArrayList<PlotLineItem>) {
        dataPoints.clear()
        addDataPoints(items)
    }

    fun reset() {
        dataPoints.clear()
    }

    fun getDataPoints(dimension: PlotDimension, dimensionRestriction: Float?): List<PlotLineItem> {
        return when (dimensionRestriction) {
            null -> dataPoints
            else -> when (dimension) {
                PlotDimension.INDEX -> when (dataPoints.size > dimensionRestriction) {
                   true -> dataPoints.filterIndexed { index, s -> index >= dimensionRestriction }
                   else -> dataPoints
                }
                PlotDimension.DISTANCE -> {
                    val min = dataPoints.last().Distance - dimensionRestriction
                    return dataPoints.filter { it.Distance >= min }
                }
                PlotDimension.TIME -> {
                    val min = dataPoints.last().Timestamp - dimensionRestriction
                    return dataPoints.filter { it.Timestamp >= min }
                }
            }
        }
    }

    fun minDimension(dimension: PlotDimension, dataPoints: List<PlotLineItem>? = null): Float {
        return when (dimension) {
            PlotDimension.INDEX -> 0f
            PlotDimension.DISTANCE -> (dataPoints?:this.dataPoints).first().Distance
            PlotDimension.TIME -> (dataPoints?:this.dataPoints).first().Timestamp
        }
    }

    fun maxDimension(dimension: PlotDimension, dataPoints: List<PlotLineItem>? = null): Float {
        return when (dimension) {
            PlotDimension.INDEX -> ((dataPoints?:this.dataPoints).size - 1).toFloat()
            PlotDimension.DISTANCE -> (dataPoints?:this.dataPoints).last().Distance
            PlotDimension.TIME -> (dataPoints?:this.dataPoints).last().Timestamp
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

    fun averageValue(dataPoints: List<PlotLineItem>, averageMethod: PlotHighlightMethod): Float? {
        if (dataPoints.isEmpty()) return null
        return when (averageMethod) {
            PlotHighlightMethod.AVG -> dataPoints.map { it.Value }.average().toFloat()
            PlotHighlightMethod.AVG_BY_DIMENSION -> (dataPoints.last().Distance - dataPoints.first().Distance) / (dataPoints.last().Timestamp - dataPoints.first().Timestamp) * 3.6f
            else -> null
        }
    }

    fun isEmpty(): Boolean {
        return dataPoints.isEmpty()
    }

    fun byHighlightMethod(dataPoints: List<PlotLineItem>): Float? {
        return when (HighlightMethod) {
            PlotHighlightMethod.AVG -> averageValue(dataPoints, HighlightMethod)
            PlotHighlightMethod.AVG_BY_DIMENSION -> averageValue(dataPoints, HighlightMethod)
            PlotHighlightMethod.MAX -> maxValue(dataPoints)
            PlotHighlightMethod.MIN -> minValue(dataPoints)
            else -> null
        }
    }

    fun x(index: Float, dimension: PlotDimension, dimensionRestriction: Float?, dataPoints: List<PlotLineItem>? = null) : Float {
        return when(dimension) {
            PlotDimension.DISTANCE -> {
                val max = (dataPoints ?: this.dataPoints).last().Distance
                val min = when (dimensionRestriction) {
                    null -> (dataPoints ?: this.dataPoints).first().Distance
                    else -> max - dimensionRestriction
                }
                PlotLineItem.cord(index, min, max)
            }
            PlotDimension.TIME -> {
                val max = (dataPoints ?: this.dataPoints).last().Timestamp
                val min = when (dimensionRestriction) {
                    null -> (dataPoints ?: this.dataPoints).first().Timestamp
                    else -> max - dimensionRestriction
                }
                PlotLineItem.cord(index, min, max)
            }
            else -> index
        }
    }
}

class PlotLineItem (
    val Value: Float,
    val Timestamp: Float,
    val Distance: Float
){
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
    MAX, AVG, MIN, AVG_BY_DIMENSION, NONE
}