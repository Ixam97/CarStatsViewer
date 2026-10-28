package com.ixam97.carStatsViewer.map

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** A part of the route whose points all fall into the same value bucket. [value] is null if unknown. */
data class RouteSegment(
    val points: List<MapboxInterface.MapPoint>,
    val value: Float?
)

object RouteSegments {

    /** Bucket sizes in base units: 10 km/h and 20 Wh/km. */
    const val SPEED_BUCKET_SIZE = 10f / 3.6f
    const val CONSUMPTION_BUCKET_SIZE = 20f

    /**
     * Merges consecutive points of the same value bucket into segments. The line between point i-1 and i
     * is described by the value of point i. Each segment starts with the last point of its predecessor,
     * so there are no gaps in the route. The value of a segment is the distance-weighted mean of its points.
     */
    fun build(
        points: List<MapboxInterface.MapPoint>,
        value: (MapboxInterface.MapPoint) -> Float?,
        bucketSize: Float
    ): List<RouteSegment> {
        if (points.size < 2) return listOf()

        val segments = mutableListOf<RouteSegment>()
        var start = 1
        var currentBucket: Bucket? = bucketOf(value(points[1]), bucketSize)

        for (i in 2..points.size) {
            val bucket = if (i < points.size) bucketOf(value(points[i]), bucketSize) else null
            if (i == points.size || bucket != currentBucket) {
                segments.add(createSegment(points, start, i - 1, value))
                start = i
                currentBucket = bucket
            }
        }
        return segments
    }

    private fun bucketOf(value: Float?, bucketSize: Float): Bucket =
        if (value == null) Bucket.Unknown else Bucket.Known(floor(value / bucketSize).toInt())

    private sealed class Bucket {
        data object Unknown : Bucket()
        data class Known(val index: Int) : Bucket()
    }

    private fun createSegment(
        points: List<MapboxInterface.MapPoint>,
        first: Int,
        last: Int,
        value: (MapboxInterface.MapPoint) -> Float?
    ): RouteSegment {
        var weightedSum = 0.0
        var weightSum = 0.0
        var plainSum = 0.0
        var count = 0
        for (i in first..last) {
            val pointValue = value(points[i]) ?: continue
            val weight = distanceMeters(points[i - 1], points[i])
            weightedSum += pointValue * weight
            weightSum += weight
            plainSum += pointValue
            count++
        }
        val segmentValue = when {
            count == 0 -> null
            weightSum > 0.0 -> (weightedSum / weightSum).toFloat()
            else -> (plainSum / count).toFloat()
        }
        return RouteSegment(points.subList(first - 1, last + 1), segmentValue)
    }

    private const val EARTH_RADIUS_M = 6_371_000.0

    fun distanceMeters(a: MapboxInterface.MapPoint, b: MapboxInterface.MapPoint): Double {
        val lat1 = Math.toRadians(a.lat)
        val lat2 = Math.toRadians(b.lat)
        val dLat = lat2 - lat1
        val dLon = Math.toRadians(b.lon - a.lon)
        val h = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
        return 2 * EARTH_RADIUS_M * asin(sqrt(h.coerceIn(0.0, 1.0)))
    }
}
