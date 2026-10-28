package com.ixam97.carStatsViewer.utils

import com.ixam97.carStatsViewer.database.tripData.DrivingPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DrivingPointMetricsTest {

    /** Builds points from (dt in ms, distance in m, energy in Wh, marker) tuples, the first point at t = 0. */
    private fun points(vararg deltas: Delta): List<DrivingPoint> {
        var time = 1_000_000L
        return deltas.map {
            time += it.dt
            DrivingPoint(
                driving_point_epoch_time = time,
                energy_delta = it.energy,
                distance_delta = it.distance,
                point_marker_type = it.marker,
                state_of_charge = 0.5f,
                lat = null,
                lon = null,
                alt = null
            )
        }
    }

    private data class Delta(val dt: Long, val distance: Float, val energy: Float, val marker: Int? = null)

    private fun constant(count: Int, dt: Long = 2_500L, distance: Float = 25f, energy: Float = 4f) =
        List(count) { Delta(dt, distance, energy) }

    @Test
    fun constantDrivingGivesConstantValues() {
        val points = points(Delta(0, 0f, 0f, 1), *constant(20).toTypedArray())

        val speeds = DrivingPointMetrics.compute(points, RouteMetric.SPEED)
        val consumptions = DrivingPointMetrics.compute(points, RouteMetric.CONSUMPTION)

        assertNull(speeds[0])
        assertNull(consumptions[0])
        for (i in 1 until points.size) {
            assertEquals(10f, speeds[i]!!, 0.001f)          // 25 m / 2.5 s
            assertEquals(160f, consumptions[i]!!, 0.001f)   // 4 Wh / 0.025 km
        }
    }

    @Test
    fun smoothingAveragesOverNeighbours() {
        // Alternating speeds of 10 and 20 m/s are smoothed towards the mean.
        val deltas = List(20) { if (it % 2 == 0) Delta(2_500L, 25f, 4f) else Delta(1_250L, 25f, 4f) }
        val speeds = DrivingPointMetrics.compute(points(*deltas.toTypedArray()), RouteMetric.SPEED)
        val middle = speeds[10]!!
        assert(middle > 10f && middle < 20f) { "Expected smoothed value, got $middle" }
    }

    @Test
    fun breakAndMarkerAreBoundaries() {
        val deltas = constant(10, energy = 2f) +
            Delta(DrivingPointMetrics.MAX_GAP_MS + 1, 25f, 2f) +
            constant(10, energy = 8f) +
            Delta(2_500L, 25f, 8f, marker = 1) +
            constant(10, energy = 4f)
        val points = points(*deltas.toTypedArray())

        val consumptions = DrivingPointMetrics.compute(points, RouteMetric.CONSUMPTION)

        assertNull(consumptions[10])  // break
        assertNull(consumptions[21])  // marker
        // Values right next to the boundaries are not averaged across them.
        assertEquals(80f, consumptions[9]!!, 0.001f)
        assertEquals(320f, consumptions[11]!!, 0.001f)
        assertEquals(320f, consumptions[20]!!, 0.001f)
        assertEquals(160f, consumptions[22]!!, 0.001f)
    }

    @Test
    fun outliersHaveNoValue() {
        val deltas = constant(5) +
            Delta(100L, 25f, 4f) +           // 250 m/s
            constant(5) +
            Delta(2_500L, 2f, 1f) +          // < 5 m
            Delta(0L, 25f, 4f) +             // dt = 0
            constant(5)
        val points = points(*deltas.toTypedArray())

        val speeds = DrivingPointMetrics.compute(points, RouteMetric.SPEED)
        val consumptions = DrivingPointMetrics.compute(points, RouteMetric.CONSUMPTION)

        assertNull(speeds[5])
        assertNull(consumptions[11])
        assertNull(speeds[12])
        assertNotNull(speeds[4])
        assertNotNull(consumptions[12])
    }

    @Test
    fun negativeConsumptionStaysNegativeAndIsClamped() {
        val regeneration = DrivingPointMetrics.compute(points(*constant(10, energy = -2f).toTypedArray()), RouteMetric.CONSUMPTION)
        assertEquals(-80f, regeneration[5]!!, 0.001f)

        val high = DrivingPointMetrics.compute(points(*constant(10, energy = 50f).toTypedArray()), RouteMetric.CONSUMPTION)
        assertEquals(DrivingPointMetrics.MAX_CONSUMPTION_WH_KM, high[5]!!, 0.001f)

        val low = DrivingPointMetrics.compute(points(*constant(10, energy = -50f).toTypedArray()), RouteMetric.CONSUMPTION)
        assertEquals(DrivingPointMetrics.MIN_CONSUMPTION_WH_KM, low[5]!!, 0.001f)
    }

    @Test
    fun emptyListGivesEmptyResult() {
        assertEquals(0, DrivingPointMetrics.compute(listOf(), RouteMetric.SPEED).size)
    }
}
