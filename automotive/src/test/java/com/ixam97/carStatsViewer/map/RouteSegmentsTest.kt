package com.ixam97.carStatsViewer.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RouteSegmentsTest {

    /** Points along the equator, about 111 m apart. */
    private fun points(vararg speeds: Float?) = speeds.mapIndexed { index, speed ->
        MapboxInterface.MapPoint(lon = index * 0.001, lat = 0.0, speed = speed)
    }

    private fun build(points: List<MapboxInterface.MapPoint>) =
        RouteSegments.build(points, { it.speed }, bucketSize = 10f)

    @Test
    fun sameBucketIsMergedIntoOneSegment() {
        val segments = build(points(null, 11f, 12f, 19f))

        assertEquals(1, segments.size)
        assertEquals(4, segments[0].points.size)
        assertEquals(14f, segments[0].value!!, 0.01f)
    }

    @Test
    fun segmentsAreConnected() {
        val points = points(null, 5f, 5f, 15f, 15f, 25f)
        val segments = build(points)

        assertEquals(3, segments.size)
        for (i in 1 until segments.size) {
            assertEquals(segments[i - 1].points.last(), segments[i].points.first())
        }
        assertEquals(points.first(), segments.first().points.first())
        assertEquals(points.last(), segments.last().points.last())
        assertEquals(listOf(5f, 15f, 25f), segments.map { it.value })
    }

    @Test
    fun unknownValuesGetTheirOwnSegment() {
        val segments = build(points(null, 5f, null, null, 5f))

        assertEquals(3, segments.size)
        assertNull(segments[1].value)
        assertEquals(3, segments[1].points.size)
    }

    @Test
    fun valueIsDistanceWeighted() {
        val points = listOf(
            MapboxInterface.MapPoint(lon = 0.0, lat = 0.0),
            MapboxInterface.MapPoint(lon = 0.003, lat = 0.0, speed = 11f),  // ~333 m
            MapboxInterface.MapPoint(lon = 0.004, lat = 0.0, speed = 19f),  // ~111 m
        )
        val segments = build(points)

        assertEquals(1, segments.size)
        assertEquals(13f, segments[0].value!!, 0.01f)
    }

    @Test
    fun tooFewPointsGiveNoSegments() {
        assertEquals(0, build(points()).size)
        assertEquals(0, build(points(5f)).size)
    }
}
