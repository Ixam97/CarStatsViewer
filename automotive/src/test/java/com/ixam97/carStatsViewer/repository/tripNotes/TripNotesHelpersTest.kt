package com.ixam97.carStatsViewer.repository.tripNotes

import com.ixam97.carStatsViewer.database.tripData.ChargingSession
import com.ixam97.carStatsViewer.database.tripData.DrivingPoint
import com.ixam97.carStatsViewer.database.tripData.DrivingSession
import com.ixam97.carStatsViewer.database.tripData.TripType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class TripNotesHelpersTest {

    private fun drivingSession(id: Long, note: String = "", type: Int = TripType.MANUAL) = DrivingSession(
        driving_session_id = id,
        start_epoch_time = 1_000L,
        end_epoch_time = 0L,
        session_type = type,
        drive_time = 0L,
        used_energy = 0.0,
        used_soc = 0.0,
        used_soc_energy = 0.0,
        driven_distance = 0.0,
        note = note,
        last_edited_epoch_time = 1_000L
    )

    @Test
    fun sanitizeNote_trimsWhitespace() {
        assertEquals("Holiday trip", TripNotesRepository.sanitizeNote("  Holiday trip \n"))
    }

    @Test
    fun sanitizeNote_returnsEmptyStringForBlankNote() {
        assertEquals("", TripNotesRepository.sanitizeNote(" \n\t "))
    }

    @Test
    fun sanitizeNote_keepsInnerLineBreaks() {
        assertEquals("line 1\nline 2", TripNotesRepository.sanitizeNote("line 1\nline 2\n"))
    }

    @Test
    fun sanitizeNote_limitsLength() {
        val sanitized = TripNotesRepository.sanitizeNote("a".repeat(TripNotesRepository.MAX_NOTE_LENGTH + 100))
        assertEquals(TripNotesRepository.MAX_NOTE_LENGTH, sanitized.length)
    }

    @Test
    fun sanitizeNote_doesNotEndWithWhitespaceAfterCut() {
        val note = "a".repeat(TripNotesRepository.MAX_NOTE_LENGTH - 1) + " b"
        assertEquals("a".repeat(TripNotesRepository.MAX_NOTE_LENGTH - 1), TripNotesRepository.sanitizeNote(note))
    }

    @Test
    fun singleLineNote_joinsLines() {
        assertEquals("line 1 line 2 line 3", TripNotesRepository.singleLineNote("line 1\nline 2\r\n  line 3"))
    }

    @Test
    fun singleLineNote_collapsesEmptyLines() {
        assertEquals("line 1 line 2", TripNotesRepository.singleLineNote("\nline 1\n\n\nline 2\n"))
    }

    @Test
    fun singleLineNote_keepsSingleLineNoteUnchanged() {
        assertEquals("Holiday trip", TripNotesRepository.singleLineNote("Holiday trip"))
    }

    @Test
    fun withNote_keepsFieldsOutsideOfConstructor() {
        val drivingPoints = listOf(DrivingPoint(1L, 1f, 25f, null, 0.5f, null, null, null))
        val chargingSessions = listOf(ChargingSession(1L, 1L, 2L, 1.0, 0.1f, 20f, null, null))
        val session = drivingSession(1L).apply {
            this.drivingPoints = drivingPoints
            this.chargingSessions = chargingSessions
            this.deleteMarker = true
        }

        val updatedSession = session.withNote("new note")

        assertEquals("new note", updatedSession.note)
        assertEquals(session.copy(note = "new note"), updatedSession)
        assertSame(drivingPoints, updatedSession.drivingPoints)
        assertSame(chargingSessions, updatedSession.chargingSessions)
        assertTrue(updatedSession.deleteMarker)
    }

    @Test
    fun replaceNote_updatesOnlyMatchingSession() {
        val drivingPoints = listOf(DrivingPoint(1L, 1f, 25f, null, 0.5f, null, null, null))
        val sessions = mutableListOf(
            drivingSession(1L, note = "one").apply { this.drivingPoints = drivingPoints },
            drivingSession(2L, note = "two", type = TripType.SINCE_CHARGE)
        )

        val updatedSession = sessions.replaceNote(1L, "changed")

        assertEquals("changed", updatedSession?.note)
        assertSame(updatedSession, sessions[0])
        assertSame(drivingPoints, sessions[0].drivingPoints)
        assertEquals("two", sessions[1].note)
    }

    @Test
    fun replaceNote_returnsNullForPastSession() {
        val sessions = mutableListOf(drivingSession(1L, note = "one"))

        assertNull(sessions.replaceNote(99L, "changed"))
        assertEquals("one", sessions[0].note)
    }
}
