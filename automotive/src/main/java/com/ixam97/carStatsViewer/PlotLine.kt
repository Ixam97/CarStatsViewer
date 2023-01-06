package com.ixam97.carStatsViewer

import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

class PlotLine(
    private val DefaultMinY: Float,
    private val DefaultMaxY: Float,

    val SmoothAxle: Float? = null,

    var Divider: Float,

    var LabelFormat: String,
    var HighlightFormat: String,

    var Unit: String,

    val LabelPosition: PlotLabelPosition,
    val HighlightMethod: PlotHighlightMethod
) {
    private var maxCalculated: Float? = null
    private var minCalculated: Float? = null
    private var averageCalculated: Float? = null

    var duration: Long? = null
        private set

    private val dataPoints: ArrayList<PlotLineItem> = ArrayList()

    var visible: Boolean = true

    var baseLineAt: ArrayList<Float> = ArrayList()

    var plotPaint: PlotPaint? = null

    var displayItemCount: Int? = null
        set(value) {
            field = value
            calculate()
        }

    fun addDataPoint(item: Float?, calculate: Boolean = true) {
        if (item != null) {
            dataPoints.add(PlotLineItem(item, Calendar.getInstance()))
        }
        if (calculate) calculate()
    }

    fun addDataPoints(items: ArrayList<PlotLineItem>) {
        for (item in items) {
            dataPoints.add(item)
        }
        calculate()
    }

    fun setDataPoints(items: ArrayList<PlotLineItem>) {
        dataPoints.clear()
        addDataPoints(items)
    }

    fun reset() {
        dataPoints.clear()
        calculate()
    }

    fun getDataPoints(): List<PlotLineItem> {
        val limit = dataPoints.size - (displayItemCount ?: 0)

        return when (dataPoints.size) {
            limit -> dataPoints
            else -> dataPoints.filterIndexed { index, s -> index >= limit }
        }
    }

    private fun calculate() {
        val dataPoints = getDataPoints()
        maxCalculated = maxCalculate(dataPoints)
        minCalculated = minCalculate(dataPoints)
        averageCalculated = averageCalculate(dataPoints)
        duration = durationCalculate(dataPoints)
    }

    private fun maxCalculate(dataPoints: List<PlotLineItem>): Float? {
        if (dataPoints.isEmpty()) return null
        val max = dataPoints.maxBy { it.Value }?.Value

        return when {
            max == null -> null
            SmoothAxle != null -> max + (SmoothAxle - max % SmoothAxle)
            else -> max
        }
    }

    private fun minCalculate(dataPoints: List<PlotLineItem>): Float? {
        if (dataPoints.isEmpty()) return null
        val min = dataPoints.minBy { it.Value }?.Value

        return when {
            min == null -> null
            SmoothAxle != null -> min - (min % SmoothAxle) - SmoothAxle
            else -> min
        }
    }

    private fun averageCalculate(dataPoints: List<PlotLineItem>): Float? {
        if (dataPoints.isEmpty()) return null
        return dataPoints.map { it.Value }.average().toFloat()
    }

    private fun durationCalculate(dataPoints: List<PlotLineItem>): Long? {
        if (dataPoints.isEmpty()) return null
        return TimeUnit.SECONDS.convert(
            abs(dataPoints.last().Calendar.timeInMillis - dataPoints.first().Calendar.timeInMillis),
            TimeUnit.MILLISECONDS
        )
    }

    fun min(): Float {
        return floor((minCalculated ?: DefaultMinY).coerceAtMost(DefaultMinY))
    }

    fun max(): Float {
        return ceil((maxCalculated ?: DefaultMaxY).coerceAtLeast(DefaultMaxY))
    }

    fun range(): Float {
        return max() - min()
    }

    fun isEmpty(): Boolean {
        return dataPoints.isEmpty()
    }

    fun highlight(): Float? {
        return when (HighlightMethod) {
            PlotHighlightMethod.AVG -> averageCalculated
            PlotHighlightMethod.MAX -> maxCalculated
            PlotHighlightMethod.MIN -> minCalculated
            else -> null
        }
    }

    fun x(index: Calendar) : Float {
        return PlotLineItem.x(index, dataPoints.first().Calendar, dataPoints.last().Calendar)
    }

    fun y(index: Float?) : Float? {
        return PlotLineItem.cord(index, min(), max())
    }
}

class PlotLineItem (
    val Value: Float,
    val Calendar: Calendar
){
    companion object {
        fun x(index: Calendar, min: Calendar, max: Calendar) : Float {
            return 1f / (max.timeInMillis - min.timeInMillis) * (index.timeInMillis - min.timeInMillis)
        }

        fun cord(index: Float?, min: Float, max: Float) : Float? {
            return when (index) {
                null -> null
                else -> 1f / (max - min) * (index - min)
            }
        }

        fun cord(index: Int, min: Int, max: Int) : Float {
            return 1f / (max - min) * (index - min)
        }
    }
}


enum class PlotLabelPosition {
    LEFT, RIGHT, NONE
}

enum class PlotHighlightMethod {
    MAX, AVG, MIN, NONE
}