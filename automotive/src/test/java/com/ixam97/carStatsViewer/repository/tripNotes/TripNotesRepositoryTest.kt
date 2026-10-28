package com.ixam97.carStatsViewer.repository.tripNotes

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ixam97.carStatsViewer.TestAppEnvironment
import com.ixam97.carStatsViewer.database.tripData.DrivingSession
import com.ixam97.carStatsViewer.database.tripData.TripType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TripNotesRepositoryTest {

    private lateinit var environment: TestAppEnvironment

    /** Stand-in for the local session copies of the DataProcessor. */
    private val localSessions = mutableListOf<DrivingSession>()

    @Before
    fun setUp() {
        environment = TestAppEnvironment()
    }

    @After
    fun tearDown() {
        environment.close()
    }

    private fun startActiveSession(type: Int = TripType.SINCE_CHARGE): Long = runBlocking {
        val sessionId = environment.tripDataSource.startDrivingSession(1_000L, type)
        localSessions.add(environment.tripDataSource.getDrivingSession(sessionId)!!)
        sessionId
    }

    private fun startPastSession(): Long = runBlocking {
        val sessionId = environment.tripDataSource.startDrivingSession(1_000L, TripType.MANUAL)
        environment.tripDataSource.endDrivingSession(5_000L, sessionId)
        sessionId
    }

    /** Equivalent of DataProcessor.writeTripsToDatabase() */
    private fun writeLocalSessionsToDatabase() = runBlocking {
        localSessions.forEach { environment.tripDataSource.updateDrivingSession(it) }
    }

    private fun noteInDatabase(sessionId: Long): String? = runBlocking {
        environment.tripDataSource.getDrivingSession(sessionId)?.note
    }

    private fun repository(
        updateActiveSession: (Long, String) -> Unit = { sessionId, note -> localSessions.replaceNote(sessionId, note) }
    ) = TripNotesRepository(environment.tripDataSource, updateActiveSession)

    @Test
    fun saveNote_savesSanitizedNoteForPastSession() = runBlocking {
        val sessionId = startPastSession()

        val savedNote = repository().saveNote(sessionId, "  Visit at grandma's \n")

        assertEquals("Visit at grandma's", savedNote)
        assertEquals("Visit at grandma's", noteInDatabase(sessionId))
    }

    @Test
    fun saveNote_returnsNullForUnknownSession() = runBlocking {
        assertNull(repository().saveNote(42L, "note"))
    }

    @Test
    fun saveNote_canDeleteNote() = runBlocking {
        val sessionId = startPastSession()
        repository().saveNote(sessionId, "note")

        val savedNote = repository().saveNote(sessionId, "   ")

        assertEquals("", savedNote)
        assertEquals("", noteInDatabase(sessionId))
    }

    @Test
    fun saveNote_updatesMemoryBeforeDatabase() = runBlocking {
        val sessionId = startActiveSession()
        var noteInDatabaseDuringMemoryUpdate: String? = null
        val updates = mutableListOf<Pair<Long, String>>()

        repository { id, note ->
            noteInDatabaseDuringMemoryUpdate = noteInDatabase(id)
            updates.add(id to note)
        }.saveNote(sessionId, " note ")

        assertEquals(listOf(sessionId to "note"), updates)
        assertEquals("", noteInDatabaseDuringMemoryUpdate)
        assertEquals("note", noteInDatabase(sessionId))
    }

    @Test
    fun saveNote_noteOfActiveSessionSurvivesPeriodicWrite() = runBlocking {
        val sessionId = startActiveSession()
        startActiveSession(TripType.AUTO)

        repository().saveNote(sessionId, "Road trip")
        writeLocalSessionsToDatabase()

        assertEquals("Road trip", noteInDatabase(sessionId))
        assertEquals("Road trip", localSessions.first { it.driving_session_id == sessionId }.note)
    }

    @Test
    fun periodicWrite_revertsNoteIfMemoryIsNotUpdated() = runBlocking {
        // Documents the pitfall the in-memory update protects against.
        val sessionId = startActiveSession()

        repository { _, _ -> }.saveNote(sessionId, "Road trip")
        writeLocalSessionsToDatabase()

        assertEquals("", noteInDatabase(sessionId))
    }

    @Test
    fun saveNote_noteMovesWithSessionOnTripReset() = runBlocking {
        val sessionId = startActiveSession()
        repository().saveNote(sessionId, "Road trip")
        writeLocalSessionsToDatabase()

        val newSessionId = environment.tripDataSource.supersedeDrivingSession(sessionId, 9_000L)!!

        assertEquals("Road trip", noteInDatabase(sessionId))
        assertEquals("", noteInDatabase(newSessionId))
    }
}
