package com.ixam97.carStatsViewer.repository.tripNotes

import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.database.tripData.DrivingSession
import com.ixam97.carStatsViewer.database.tripData.TripDataSource

/**
 * Central entry point for saving trip notes, so the UI does not need to know about active trips
 * being kept in memory by the DataProcessor.
 *
 * @param updateActiveSession Updates the in-memory copy of an active session. Must be a no-op for
 * past sessions. Without it, the DataProcessor would overwrite the note with the old value on its
 * next periodic database write.
 */
class TripNotesRepository(
    private val tripDataSource: TripDataSource = CarStatsViewer.tripDataSource,
    private val updateActiveSession: (sessionId: Long, note: String) -> Unit = CarStatsViewer.dataProcessor::updateNote
) {

    /**
     * Sanitizes and saves the note of a driving session.
     * @return The note as it has been saved, or null if the session does not exist.
     */
    suspend fun saveNote(sessionId: Long, note: String): String? {
        val sanitizedNote = sanitizeNote(note)
        // Update the memory first: a periodic write of the DataProcessor in between then already
        // contains the new note instead of reverting it.
        updateActiveSession(sessionId, sanitizedNote)
        if (!tripDataSource.updateDrivingSessionNote(sessionId, sanitizedNote)) return null
        return sanitizedNote
    }

    companion object {
        const val MAX_NOTE_LENGTH = 500

        private val lineBreakRegex = Regex("\\s*\\R\\s*")

        /** Trims surrounding whitespace and limits the note to [MAX_NOTE_LENGTH] characters. */
        fun sanitizeNote(note: String): String = note.trim().take(MAX_NOTE_LENGTH).trimEnd()

        /** Joins all lines of a note into a single line, e.g. for list previews or CSV comments. */
        fun singleLineNote(note: String): String = note.trim().replace(lineBreakRegex, " ")
    }
}

/**
 * Copy of a driving session with a new note. Unlike [DrivingSession.copy] this keeps the fields
 * that are not part of the constructor (drivingPoints, chargingSessions, deleteMarker).
 */
fun DrivingSession.withNote(note: String): DrivingSession {
    val drivingSession = this
    return copy(note = note).apply {
        drivingPoints = drivingSession.drivingPoints
        chargingSessions = drivingSession.chargingSessions
        deleteMarker = drivingSession.deleteMarker
    }
}

/**
 * Replaces the note of the session with [sessionId] in place.
 * @return The updated session, or null if the list does not contain the session.
 */
fun MutableList<DrivingSession>.replaceNote(sessionId: Long, note: String): DrivingSession? {
    val index = indexOfFirst { it.driving_session_id == sessionId }
    if (index < 0) return null
    this[index] = this[index].withNote(note)
    return this[index]
}
