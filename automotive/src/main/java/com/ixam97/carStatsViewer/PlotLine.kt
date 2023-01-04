package com.ixam97.carStatsViewer

import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

class PlotLine(
    var DefaultMinY: Float,
    var DefaultMaxY: Float,

    var Divider: Float,

    var LabelFormat: String,
    var HighlightFormat: String,

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

    var PlotPaint: PlotPaint? = null
    var displayItemCount: Int? = null
        set(value) {
            field = value
            calculate()
        }

    fun addDataPoint(item: Float?) {
        if (item != null) {
            dataPoints.add(PlotLineItem(item, Calendar.getInstance()))
        }
        calculate()
    }

    fun addDataPoints(items: ArrayList<PlotLineItem>) {
        if (items != null) {
            for (item in items) {
                if (item == null) return
                dataPoints.add(item)
            }
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
        var limit = dataPoints.size - (displayItemCount ?: 0)
        if (dataPoints.size == limit) return dataPoints
        return dataPoints.filterIndexed { index, s -> index > limit }
    }

    private fun calculate() {
        var dataPoints = getDataPoints()
        maxCalculated = maxCalculate(dataPoints)
        minCalculated = minCalculate(dataPoints)
        averageCalculated = averageCalculate(dataPoints)
        duration = durationCalculate(dataPoints)
    }

    private fun maxCalculate(dataPoints: List<PlotLineItem>): Float? {
        if (dataPoints.isEmpty()) return null
        return dataPoints.maxBy { it.Value }?.Value
    }

    private fun minCalculate(dataPoints: List<PlotLineItem>): Float? {
        if (dataPoints.isEmpty()) return null
        return dataPoints.minBy { it.Value }?.Value
    }

    private fun averageCalculate(dataPoints: List<PlotLineItem>): Float? {
        if (dataPoints.isEmpty()) return null
        var sum = 0f
        var itemCount = 0f
        for (f in dataPoints) {
            sum += f.Value
            itemCount++
        }
        return if (itemCount == 0f) null else sum / itemCount
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
        if (HighlightMethod == PlotHighlightMethod.AVG) return averageCalculated
        if (HighlightMethod == PlotHighlightMethod.MAX) return maxCalculated
        if (HighlightMethod == PlotHighlightMethod.MIN) return minCalculated
        return null
    }

    fun x(position: Int, margin: Int, maxX: Float): Float? {
        return x(position, displayItemCount ?: dataPoints.size, margin, maxX)
    }

    fun y(value: Float?, margin: Int, maxY: Float): Float? {
        if (value == null) return value
        val marginY: Float = maxY - 2 * margin
        val distance = marginY / (range()) * (value - min())
        return margin + abs(marginY - distance)
    }

    companion object {
        fun x(position: Int, items: Int, margin: Int, maxX: Float): Float? {
            return margin + (maxX - 2 * margin) / (items - 1) * position
        }
    }
}

class PlotLineItem (
    val Value: Float,
    val Calendar: Calendar
)

enum class PlotLabelPosition {
    LEFT, RIGHT, NONE
}

enum class PlotHighlightMethod {
    MAX, AVG, MIN, NONE
}