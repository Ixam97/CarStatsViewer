package com.ixam97.carStatsViewer.database.tripData

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TripDaoTest {

    private lateinit var database: TripDataDatabase
    private lateinit var tripDao: TripDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TripDataDatabase::class.java
        ).allowMainThreadQueries().build()
        tripDao = database.tripDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    /** Inserts points with the epoch times 1..[count] (offset by [startTime] - 1). */
    private fun insertDrivingPoints(count: Int, startTime: Long = 1L) {
        for (i in 0 until count) {
            tripDao.upsertDrivingPoint(
                DrivingPoint(
                    driving_point_epoch_time = startTime + i,
                    energy_delta = 1f,
                    distance_delta = 25f,
                    point_marker_type = null,
                    state_of_charge = 0.5f,
                    lat = null,
                    lon = null,
                    alt = null
                )
            )
        }
    }

    @Test
    fun getLatestDrivingPointsSince_returnsNewestPointsInAscendingOrder() {
        insertDrivingPoints(3_000)

        val points = tripDao.getLatestDrivingPointsSince(startTime = 1L, limit = 2_000)

        assertEquals(2_000, points.size)
        assertEquals(1_001L, points.first().driving_point_epoch_time)
        assertEquals(3_000L, points.last().driving_point_epoch_time)
        assertTrue(points.zipWithNext().all { (a, b) -> a.driving_point_epoch_time < b.driving_point_epoch_time })
    }

    @Test
    fun getLatestDrivingPointsSince_lastPointMatchesLatestDrivingPoint() {
        insertDrivingPoints(3_000)

        val points = tripDao.getLatestDrivingPointsSince(startTime = 1L, limit = 2_000)

        assertEquals(tripDao.getLatestDrivingPoint(), points.last())
    }

    @Test
    fun getLatestDrivingPointsSince_ignoresPointsBeforeStartTime() {
        insertDrivingPoints(3_000)

        val points = tripDao.getLatestDrivingPointsSince(startTime = 2_501L, limit = 2_000)

        assertEquals(500, points.size)
        assertEquals(2_501L, points.first().driving_point_epoch_time)
        assertEquals(3_000L, points.last().driving_point_epoch_time)
    }

    @Test
    fun getLatestDrivingPointsSince_includesPointAtStartTime() {
        insertDrivingPoints(10, startTime = 100L)

        val points = tripDao.getLatestDrivingPointsSince(startTime = 100L, limit = 2_000)

        assertEquals((100L..109L).toList(), points.map { it.driving_point_epoch_time })
    }

    @Test
    fun getLatestDrivingPointsSince_returnsEmptyListWithoutPoints() {
        assertTrue(tripDao.getLatestDrivingPointsSince(startTime = 0L, limit = 2_000).isEmpty())
    }

    @Test
    fun localTripDataSource_getLatestDrivingPointsSince_delegatesToDao() = runBlocking {
        insertDrivingPoints(3_000)
        val dataSource = LocalTripDataSource(tripDao)

        val points = dataSource.getLatestDrivingPointsSince(startTime = 1L, limit = 2_000)

        assertEquals(tripDao.getLatestDrivingPointsSince(startTime = 1L, limit = 2_000), points)
    }
}
