package com.ixam97.carStatsViewer.carCompose.screens.tripHistory

import com.ixam97.carStatsViewer.utils.DistanceUnitEnum
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DistanceFilterTest {

    @Test
    fun anchors_roundTripThroughPosition() {
        DISTANCE_FILTER_ANCHORS.forEach { anchor ->
            assertEquals(anchor, positionToValue(valueToPosition(anchor)))
        }
    }

    @Test
    fun valuesBetweenAnchors_roundTripThroughPosition() {
        listOf(3, 15, 25, 40, 85, 120, 240, 350, 600, 800, 1500, 3000, 4900).forEach { value ->
            assertEquals(value, positionToValue(valueToPosition(value)))
        }
    }

    @Test
    fun positionToValue_interpolatesBetweenAnchorsOnTheGrid() {
        // between the anchors 200 and 300 the slider resolves in 10 steps
        assertEquals(250, positionToValue(11.5f))
        assertEquals(240, positionToValue(11.42f))
        // between 10 and 20 in steps of 1
        assertEquals(13, positionToValue(4.3f))
    }

    @Test
    fun positionToValue_lastSegmentIsUnbounded() {
        val lastIndex = DISTANCE_FILTER_ANCHORS.lastIndex.toFloat()
        assertEquals(5000, positionToValue(lastIndex))
        assertNull(positionToValue(lastIndex + 0.01f))
        assertNull(positionToValue(DISTANCE_FILTER_UNBOUNDED_POSITION))
    }

    @Test
    fun valueToPosition_unboundedAndAboveLastAnchor() {
        assertEquals(DISTANCE_FILTER_UNBOUNDED_POSITION, valueToPosition(null))
        assertEquals(DISTANCE_FILTER_ANCHORS.lastIndex.toFloat(), valueToPosition(8000))
    }

    @Test
    fun stepsAtRangeBoundaries() {
        listOf(
            19 to 20, 20 to 25,
            95 to 100, 100 to 110,
            490 to 500, 500 to 550,
            1950 to 2000, 2000 to 2100,
        ).forEach { (lower, upper) ->
            assertEquals(upper, distanceStepUp(lower))
            assertEquals(lower, distanceStepDown(upper))
        }
    }

    @Test
    fun stepsSnapOffGridValuesToTheGrid() {
        assertEquals(25, distanceStepUp(23))
        assertEquals(20, distanceStepDown(23))
        assertEquals(25, roundToGrid(23.4f))
        assertEquals(240, roundToGrid(243f))
    }

    @Test
    fun distanceIncrement_usesRangeTheValueMovesInto() {
        assertEquals(5, distanceIncrement(20, up = true))
        assertEquals(1, distanceIncrement(20, up = false))
        assertEquals(100, distanceIncrement(2000, up = true))
        assertEquals(50, distanceIncrement(2000, up = false))
    }

    @Test
    fun upperStepper_switchesBetweenLastAnchorAndUnbounded() {
        assertEquals(DistanceRange(0, null), DistanceRange(0, 5000).incrementMax())
        assertEquals(DistanceRange(0, 5000), DistanceRange(0, null).decrementMax())
        assertFalse(DistanceRange(0, null).canIncrementMax)
    }

    @Test
    fun lowerStepper_cannotGoBelowZero() {
        val range = DistanceRange(0, 100)
        assertFalse(range.canDecrementMin)
        assertEquals(range, range.decrementMin())
    }

    @Test
    fun steppers_keepMinNotAboveMax() {
        val equal = DistanceRange(42, 42)
        assertFalse(equal.canIncrementMin)
        assertFalse(equal.canDecrementMax)
        assertEquals(equal, equal.incrementMin())
        assertEquals(equal, equal.decrementMax())
        // a step that would pass the other value is clamped, min == max is allowed
        assertEquals(DistanceRange(24, 24), DistanceRange(23, 24).incrementMin())
        assertEquals(DistanceRange(23, 23), DistanceRange(23, 24).decrementMax())
    }

    @Test
    fun steppers_fineAdjustRange() {
        var range = DistanceRange(200, 300)
        repeat(6) { range = range.decrementMax() }
        assertEquals(DistanceRange(200, 240), range)
    }

    @Test
    fun contains_usesInclusiveBoundsOnDisplayedValue() {
        val range = DistanceRange(200, 240)
        assertTrue(range.contains(240_040.0, DistanceUnitEnum.KM)) // shown as 240.0 km
        assertFalse(range.contains(240_400.0, DistanceUnitEnum.KM)) // shown as 240.4 km
        assertTrue(range.contains(200_000.0, DistanceUnitEnum.KM))
        assertFalse(range.contains(199_900.0, DistanceUnitEnum.KM))
    }

    @Test
    fun contains_openUpperLimit() {
        val range = DistanceRange(200, null)
        assertTrue(range.contains(10_000_000.0, DistanceUnitEnum.KM))
        assertFalse(range.contains(150_000.0, DistanceUnitEnum.KM))
    }

    @Test
    fun contains_convertsToMiles() {
        val range = DistanceRange(100, 150)
        assertTrue(range.contains(200_000.0, DistanceUnitEnum.MILES)) // 124.3 mi
        assertFalse(range.contains(100_000.0, DistanceUnitEnum.MILES)) // 62.1 mi
        assertFalse(range.contains(250_000.0, DistanceUnitEnum.MILES)) // 155.3 mi
    }

    @Test
    fun isUnbounded() {
        assertTrue(DistanceRange(0, null).isUnbounded)
        assertFalse(DistanceRange(1, null).isUnbounded)
        assertFalse(DistanceRange(0, 5000).isUnbounded)
    }
}
