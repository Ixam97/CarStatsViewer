package com.ixam97.carStatsViewer.utils

import com.ixam97.carStatsViewer.database.tripData.DrivingPoint

enum class RouteMetric { SPEED, CONSUMPTION }

/**
 * Derives smoothed per-point values (speed, consumption) from the deltas stored in [DrivingPoint]s.
 * Each point describes the segment since its predecessor. Pure logic, no Android dependencies.
 */
object DrivingPointMetrics {
    /** Time gap between two points that is treated as a break (segment boundary). */
    const val MAX_GAP_MS = 60_000L

    /** Speeds above this are treated as implausible. 70 m/s = 252 km/h. */
    const val MAX_SPEED_MPS = 70f

    /** Points covering less distance than this get no consumption value (division by almost 0). */
    const val MIN_CONSUMPTION_DISTANCE_M = 5f

    const val MIN_CONSUMPTION_WH_KM = -300f
    const val MAX_CONSUMPTION_WH_KM = 600f

    private const val SPEED_WINDOW = 2
    private const val CONSUMPTION_WINDOW = 4

    /**
     * Returns true if the point does not continue the previous segment: first point, segment marker or break.
     * Such a point has no value and the smoothing window does not extend across it.
     */
    fun isSegmentBoundary(points: List<DrivingPoint>, index: Int): Boolean {
        if (index == 0) return true
        val point = points[index]
        if (point.point_marker_type != null) return true
        return point.driving_point_epoch_time - points[index - 1].driving_point_epoch_time > MAX_GAP_MS
    }

    /** Returns the smoothed value per point in base units (m/s or Wh/km), null = unknown. */
    fun compute(points: List<DrivingPoint>, metric: RouteMetric): List<Float?> {
        if (points.isEmpty()) return listOf()

        val boundary = BooleanArray(points.size) { isSegmentBoundary(points, it) }
        val dt = LongArray(points.size) { if (it == 0) 0L else points[it].driving_point_epoch_time - points[it - 1].driving_point_epoch_time }
        val window = when (metric) {
            RouteMetric.SPEED -> SPEED_WINDOW
            RouteMetric.CONSUMPTION -> CONSUMPTION_WINDOW
        }

        // Index of the first point of the segment each point belongs to.
        val segmentStart = IntArray(points.size)
        for (i in points.indices) {
            segmentStart[i] = if (boundary[i]) i else segmentStart[i - 1]
        }

        return points.indices.map { i ->
            if (boundary[i] || !isPointValid(points[i], dt[i], metric)) return@map null

            // The boundary point itself carries the delta across the stop, so it is excluded.
            val from = maxOf(i - window, segmentStart[i] + 1)
            var to = i
            while (to + 1 < points.size && to + 1 <= i + window && !boundary[to + 1]) to++

            var numerator = 0.0
            var denominator = 0.0
            for (j in from..to) {
                if (metric == RouteMetric.SPEED && dt[j] <= 0) continue
                when (metric) {
                    RouteMetric.SPEED -> {
                        numerator += points[j].distance_delta
                        denominator += dt[j] / 1000.0
                    }
                    RouteMetric.CONSUMPTION -> {
                        numerator += points[j].energy_delta
                        denominator += points[j].distance_delta / 1000.0
                    }
                }
            }
            if (denominator <= 0.0) return@map null
            val value = (numerator / denominator).toFloat()

            when (metric) {
                RouteMetric.SPEED -> value.takeIf { it <= MAX_SPEED_MPS }
                RouteMetric.CONSUMPTION -> value.coerceIn(MIN_CONSUMPTION_WH_KM, MAX_CONSUMPTION_WH_KM)
            }
        }
    }

    private fun isPointValid(point: DrivingPoint, dt: Long, metric: RouteMetric): Boolean = when (metric) {
        RouteMetric.SPEED -> dt > 0 && point.distance_delta / (dt / 1000f) <= MAX_SPEED_MPS
        RouteMetric.CONSUMPTION -> point.distance_delta >= MIN_CONSUMPTION_DISTANCE_M
    }
}
