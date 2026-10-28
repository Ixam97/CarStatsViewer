package com.ixam97.carStatsViewer.database.tripData

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ixam97.carStatsViewer.TestAppEnvironment
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TripDaoNoteTest {

    private lateinit var environment: TestAppEnvironment
    private lateinit var tripDao: TripDao
    private lateinit var dataSource: LocalTripDataSource

    @Before
    fun setUp() {
        // LocalTripDataSource logs via InAppLogger, which needs the app environment
        environment = TestAppEnvironment()
        tripDao = environment.tripDatabase.tripDao()
        dataSource = environment.tripDataSource
    }

    @After
    fun tearDown() {
        environment.close()
    }

    private fun insertSession(
        note: String = "",
        endTime: Long = 0L,
        lastEdited: Long = 1_000L
    ): Long = tripDao.upsertDrivingSession(
        DrivingSession(
            start_epoch_time = 1_000L,
            end_epoch_time = endTime,
            session_type = TripType.MANUAL,
            drive_time = 60_000L,
            used_energy = 1_500.0,
            used_soc = 0.02,
            used_soc_energy = 1_500.0,
            driven_distance = 10_000.0,
            note = note,
            last_edited_epoch_time = lastEdited
        )
    )

    @Test
    fun updateDrivingSessionNote_updatesOnlyNoteAndEditTime() {
        val sessionId = insertSession(endTime = 5_000L)
        val before = tripDao.getDrivingSessionById(sessionId)!!

        val updatedRows = tripDao.updateDrivingSessionNote(sessionId, "Holiday trip", editedAt = 9_999L)

        assertEquals(1, updatedRows)
        val after = tripDao.getDrivingSessionById(sessionId)!!
        assertEquals(
            before.copy(note = "Holiday trip", last_edited_epoch_time = 9_999L),
            after
        )
    }

    @Test
    fun updateDrivingSessionNote_doesNotTouchOtherSessions() {
        val sessionId = insertSession(note = "first")
        val otherSessionId = insertSession(note = "second")

        tripDao.updateDrivingSessionNote(sessionId, "changed", editedAt = 2_000L)

        assertEquals("changed", tripDao.getDrivingSessionById(sessionId)!!.note)
        assertEquals("second", tripDao.getDrivingSessionById(otherSessionId)!!.note)
        assertEquals(1_000L, tripDao.getDrivingSessionById(otherSessionId)!!.last_edited_epoch_time)
    }

    @Test
    fun updateDrivingSessionNote_returnsZeroForUnknownSession() {
        assertEquals(0, tripDao.updateDrivingSessionNote(42L, "note", editedAt = 2_000L))
    }

    @Test
    fun updateDrivingSessionNote_canClearNote() {
        val sessionId = insertSession(note = "old note")

        tripDao.updateDrivingSessionNote(sessionId, "", editedAt = 2_000L)

        assertEquals("", tripDao.getDrivingSessionById(sessionId)!!.note)
    }

    @Test
    fun updateDrivingSessionNote_keepsMultiLineNotes() {
        val sessionId = insertSession()

        tripDao.updateDrivingSessionNote(sessionId, "line 1\nline 2", editedAt = 2_000L)

        assertEquals("line 1\nline 2", tripDao.getDrivingSessionById(sessionId)!!.note)
    }

    @Test
    fun localTripDataSource_updateDrivingSessionNote_reportsWhetherSessionExists() = runBlocking {
        val sessionId = insertSession()

        assertTrue(dataSource.updateDrivingSessionNote(sessionId, "note"))
        assertFalse(dataSource.updateDrivingSessionNote(sessionId + 1, "note"))
        assertEquals("note", dataSource.getDrivingSession(sessionId)!!.note)
    }

    @Test
    fun localTripDataSource_updateDrivingSessionNote_setsLastEditedTime() = runBlocking {
        val sessionId = insertSession(lastEdited = 1_000L)
        val before = System.currentTimeMillis()

        dataSource.updateDrivingSessionNote(sessionId, "note")

        assertTrue(dataSource.getDrivingSession(sessionId)!!.last_edited_epoch_time >= before)
    }
}
