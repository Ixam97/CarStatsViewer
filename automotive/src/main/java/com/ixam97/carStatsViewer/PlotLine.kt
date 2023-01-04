package com.ixam97.carStatsViewer

import java.util.Collections.max
import java.util.Collections.min
import kotlin.math.abs

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

    private val dataPoints: ArrayList<Float> = ArrayList()

    var BaseLineAt: ArrayList<Float> = ArrayList()

    var PlotPaint: PlotPaint? = null
    var displayItemCount: Int? = null
        set(value) {
            field = value
            calculate()
        }

    fun addDataPoint(item: Float?) {
        if (item != null) {
            dataPoints.add(item)
        }
        calculate()
    }

    fun addDataPoints(items: ArrayList<Float?>) {
        if (items != null) {
            for (item in items) {
                if (item == null) return
                dataPoints.add(item)
            }
        }
        calculate()
    }

    fun setDataPoints(items: ArrayList<Float?>) {
        dataPoints.clear()
        addDataPoints(items)
    }

    fun reset() {
        dataPoints.clear()
        calculate()
    }

    private fun calculate() {
        maxCalculated = maxCalculate()
        minCalculated = minCalculate()
        averageCalculated = averageCalculate()
    }

    fun getDataPoints(): List<Float> {
        var limit = dataPoints.size - (displayItemCount ?: 0);
        if (dataPoints.size == limit) return dataPoints;
        return dataPoints.filterIndexed { index, s -> index > limit }
    }

    private fun maxCalculate(): Float? {
        if (dataPoints.isEmpty()) return null;
        return max(getDataPoints())
    }

    private fun minCalculate(): Float? {
        if (dataPoints.isEmpty()) return null;
        return min(getDataPoints())
    }

    private fun averageCalculate(): Float? {
        if (dataPoints.isEmpty()) return null
        var sum = 0f
        var itemCount = 0f
        for (f in getDataPoints()) {
            sum += f
            itemCount++
        }
        return if (itemCount == 0f) null else sum / itemCount
    }

    fun isEmpty(): Boolean {
        return dataPoints.isEmpty();
    }

    fun min(): Float {
        return Math.floor(Math.min(minCalculated ?: DefaultMinY, DefaultMinY).toDouble()).toFloat()
    }

    fun max(): Float {
        return Math.ceil(Math.max(maxCalculated ?: DefaultMaxY, DefaultMaxY).toDouble()).toFloat()
    }

    fun highlight(): Float? {
        if (HighlightMethod == PlotHighlightMethod.AVG) return averageCalculated;
        if (HighlightMethod == PlotHighlightMethod.MAX) return maxCalculated;
        if (HighlightMethod == PlotHighlightMethod.MIN) return minCalculated;
        return null;
    }

    fun x(position: Int, margin: Int, maxX: Float): Float? {
        return x(position, displayItemCount ?: dataPoints.size, margin, maxX)
    }

    fun y(value: Float?, margin: Int, maxY: Float): Float? {
        if (value == null) return value
        val marginY: Float = maxY - 2 * margin
        val distance = marginY / (max() - min()) * (value - min())
        return margin + abs(marginY - distance)
    }

    companion object {
        fun x(position: Int, items: Int, margin: Int, maxX: Float): Float? {
            return margin + (maxX - 2 * margin) / (items - 1) * position
        }
    }
}

enum class PlotLabelPosition {
    LEFT, RIGHT, NONE
}

enum class PlotHighlightMethod {
    MAX, AVG, MIN, NONE
}