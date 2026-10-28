package com.ixam97.carStatsViewer.carCompose.screens.tripHistory

import com.ixam97.carStatsViewer.utils.DistanceUnitEnum
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Anchor points of the distance filter slider in the display unit (km or mi). They are spread
 * evenly over the slider track, values between two anchors are interpolated linearly. Slider
 * positions above the last anchor mean "no upper limit".
 */
val DISTANCE_FILTER_ANCHORS = listOf(0, 1, 2, 5, 10, 20, 30, 50, 75, 100, 150, 200, 300, 500, 750, 1000, 2000, 5000)

/** Slider position representing "no upper limit". */
val DISTANCE_FILTER_UNBOUNDED_POSITION = DISTANCE_FILTER_ANCHORS.size.toFloat()

private val lastAnchor = DISTANCE_FILTER_ANCHORS.last()

/** Selected distance range in the display unit (km or mi); max == null means "no upper limit". */
data class DistanceRange(val min: Int, val max: Int?) {
    val isUnbounded get() = min == 0 && max == null

    /** True if [distanceMeters] lies within this range, using the current display unit. */
    fun contains(distanceMeters: Double, unit: DistanceUnitEnum): Boolean {
        // Truncated to one decimal, as shown in the trip row (StringFormatters.getTraveledDistanceString)
        val distanceInUnit = (unit.toUnit(distanceMeters.toFloat()).toInt() / 100) / 10.0
        return distanceInUnit >= min && (max == null || distanceInUnit <= max)
    }

    val canDecrementMin get() = min > 0
    val canIncrementMin get() = min < (max ?: lastAnchor)
    val canDecrementMax get() = max == null || max > min
    val canIncrementMax get() = max != null

    fun decrementMin() = if (canDecrementMin) copy(min = distanceStepDown(min)) else this
    fun incrementMin() = if (canIncrementMin) copy(min = minOf(distanceStepUp(min) ?: lastAnchor, max ?: lastAnchor)) else this
    fun decrementMax() = if (canDecrementMax) copy(max = maxOf(distanceStepDown(max), min)) else this
    fun incrementMax() = if (canIncrementMax) copy(max = distanceStepUp(max!!)) else this
}

/**
 * Increment used for rounding and for the steppers. At a range boundary, the increment of the
 * range the value moves into is used: 20 −1 → 19, 20 +5 → 25.
 */
fun distanceIncrement(value: Int, up: Boolean): Int {
    val v = if (up) value else value - 1
    return when {
        v < 20 -> 1
        v < 100 -> 5
        v < 500 -> 10
        v < 2000 -> 50
        else -> 100
    }
}

/** Rounds [value] to the grid of the increment of its range. */
fun roundToGrid(value: Float): Int {
    val increment = distanceIncrement(floor(value).toInt(), up = true)
    return (value / increment).roundToInt() * increment
}

/** Next larger value on the increment grid, null ("no upper limit") above the last anchor. */
fun distanceStepUp(value: Int): Int? {
    if (value >= lastAnchor) return null
    val increment = distanceIncrement(value, up = true)
    return (value / increment + 1) * increment
}

/** Next smaller value on the increment grid; from "no upper limit" (null) to the last anchor. */
fun distanceStepDown(value: Int?): Int {
    if (value == null) return lastAnchor
    if (value <= 0) return 0
    val increment = distanceIncrement(value, up = false)
    return ((value + increment - 1) / increment - 1) * increment
}

/** Maps a slider position to a value on the increment grid; null in the last segment (∞). */
fun positionToValue(position: Float): Int? {
    if (position > DISTANCE_FILTER_ANCHORS.lastIndex) return null
    val index = floor(position).toInt().coerceIn(0, DISTANCE_FILTER_ANCHORS.lastIndex)
    if (index == DISTANCE_FILTER_ANCHORS.lastIndex) return lastAnchor
    val lower = DISTANCE_FILTER_ANCHORS[index]
    val upper = DISTANCE_FILTER_ANCHORS[index + 1]
    return roundToGrid(lower + (position - index) * (upper - lower))
}

/** Maps a value to its slider position; null ("no upper limit") to the end of the track. */
fun valueToPosition(value: Int?): Float {
    if (value == null) return DISTANCE_FILTER_UNBOUNDED_POSITION
    if (value >= lastAnchor) return DISTANCE_FILTER_ANCHORS.lastIndex.toFloat()
    val index = DISTANCE_FILTER_ANCHORS.indexOfLast { it <= value }.coerceAtLeast(0)
    val lower = DISTANCE_FILTER_ANCHORS[index]
    val upper = DISTANCE_FILTER_ANCHORS[index + 1]
    return index + (value - lower).toFloat() / (upper - lower)
}
