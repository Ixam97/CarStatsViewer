package com.ixam97.carStatsViewer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefinesTest {

    @Test
    fun mainViewDistance_mapsRestrictionIndexToRange() {
        assertEquals(20_000L, Defines.mainViewDistance(0))
        assertEquals(40_000L, Defines.mainViewDistance(1))
        assertEquals(100_000L, Defines.mainViewDistance(2))
    }

    @Test
    fun mainViewDistance_fallsBackToSmallestRangeForUnknownIndex() {
        assertEquals(20_000L, Defines.mainViewDistance(-1))
        assertEquals(20_000L, Defines.mainViewDistance(Defines.MAIN_VIEW_DISTANCES.size))
    }

    @Test
    fun drivingPointBuffer_coversLargestRangeInMilesWithReserve() {
        // 100 mi plus the 200 m rebuild margin of the plot, plus 10 % reserve for partial points
        val requiredMeters = Defines.MAIN_VIEW_DISTANCES.max() * 1.60934 + 200.0
        val requiredPoints = requiredMeters / Defines.PLOT_DISTANCE_INTERVAL * 1.1
        assertTrue(
            "MAIN_VIEW_DRIVING_POINTS = ${Defines.MAIN_VIEW_DRIVING_POINTS}, required ≈ $requiredPoints",
            Defines.MAIN_VIEW_DRIVING_POINTS >= requiredPoints.toInt()
        )
    }
}
