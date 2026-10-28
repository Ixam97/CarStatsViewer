package com.ixam97.carStatsViewer.carCompose.screens.tripDetails

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ixam97.carStatsViewer.TestAppEnvironment
import com.ixam97.carStatsViewer.database.tripData.TripType
import com.ixam97.carStatsViewer.repository.tripNotes.TripNotesRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TripDetailsViewModelNoteTest {

    private lateinit var environment: TestAppEnvironment
    private val activeSessionUpdates = mutableListOf<Pair<Long, String>>()

    @Before
    fun setUp() {
        environment = TestAppEnvironment()
    }

    @After
    fun tearDown() {
        environment.close()
    }

    private fun createSession(note: String = ""): Long = runBlocking {
        val sessionId = environment.tripDataSource.startDrivingSession(1_000L, TripType.MANUAL)
        environment.tripDataSource.endDrivingSession(5_000L, sessionId)
        if (note.isNotEmpty()) environment.tripDataSource.updateDrivingSessionNote(sessionId, note)
        sessionId
    }

    private fun createLoadedViewModel(sessionId: Long): TripDetailsViewModel {
        val viewModel = TripDetailsViewModel(
            sessionId = sessionId,
            tripNotesRepository = lazy {
                TripNotesRepository(environment.tripDataSource) { id, note -> activeSessionUpdates.add(id to note) }
            }
        )
        runBlocking {
            withTimeout(10_000) {
                while (viewModel.tripDetailsState.value.drivingSession == null) delay(10)
            }
        }
        return viewModel
    }

    private fun TripDetailsViewModel.saveNoteAndWait() = runBlocking {
        saveNote()?.join()
    }

    private fun noteInDatabase(sessionId: Long): String? = runBlocking {
        environment.tripDataSource.getDrivingSession(sessionId)?.note
    }

    @Test
    fun startEditNote_prefillsDraftWithExistingNote() {
        val viewModel = createLoadedViewModel(createSession(note = "existing note"))

        viewModel.startEditNote()

        val state = viewModel.tripDetailsState.value
        assertTrue(state.isEditingNote)
        assertEquals("existing note", state.noteDraft)
    }

    @Test
    fun saveNote_persistsNoteAndUpdatesState() {
        val sessionId = createSession()
        val viewModel = createLoadedViewModel(sessionId)

        viewModel.startEditNote()
        viewModel.setNoteDraft("  Trip to the lake\nwith friends  ")
        viewModel.saveNoteAndWait()

        val state = viewModel.tripDetailsState.value
        assertFalse(state.isEditingNote)
        assertFalse(state.isSavingNote)
        assertEquals("", state.noteDraft)
        assertEquals("Trip to the lake\nwith friends", state.drivingSession?.note)
        assertEquals("Trip to the lake\nwith friends", noteInDatabase(sessionId))
        assertEquals(listOf(sessionId to "Trip to the lake\nwith friends"), activeSessionUpdates)
    }

    @Test
    fun saveNote_keepsLoadedDrivingPoints() {
        val viewModel = createLoadedViewModel(createSession())
        val drivingPoints = viewModel.tripDetailsState.value.drivingSession?.drivingPoints

        viewModel.startEditNote()
        viewModel.setNoteDraft("note")
        viewModel.saveNoteAndWait()

        assertEquals(drivingPoints, viewModel.tripDetailsState.value.drivingSession?.drivingPoints)
    }

    @Test
    fun cancelEditNote_discardsDraft() {
        val sessionId = createSession(note = "existing note")
        val viewModel = createLoadedViewModel(sessionId)

        viewModel.startEditNote()
        viewModel.setNoteDraft("discarded")
        viewModel.cancelEditNote()

        val state = viewModel.tripDetailsState.value
        assertFalse(state.isEditingNote)
        assertEquals("", state.noteDraft)
        assertEquals("existing note", state.drivingSession?.note)
        assertEquals("existing note", noteInDatabase(sessionId))
    }

    @Test
    fun setNoteDraft_limitsLength() {
        val viewModel = createLoadedViewModel(createSession())

        viewModel.startEditNote()
        viewModel.setNoteDraft("a".repeat(TripNotesRepository.MAX_NOTE_LENGTH + 10))

        assertEquals(TripNotesRepository.MAX_NOTE_LENGTH, viewModel.tripDetailsState.value.noteDraft.length)
    }

    @Test
    fun saveNote_doesNothingWhenNotEditing() {
        val sessionId = createSession(note = "existing note")
        val viewModel = createLoadedViewModel(sessionId)

        assertNull(viewModel.saveNote())
        assertEquals("existing note", noteInDatabase(sessionId))
        assertTrue(activeSessionUpdates.isEmpty())
    }

    @Test
    fun saveNote_keepsEditingIfSessionWasDeleted() {
        val sessionId = createSession()
        val viewModel = createLoadedViewModel(sessionId)
        environment.tripDatabase.tripDao().deleteDrivingSessionByID(sessionId)

        viewModel.startEditNote()
        viewModel.setNoteDraft("note")
        viewModel.saveNoteAndWait()

        val state = viewModel.tripDetailsState.value
        assertTrue(state.isEditingNote)
        assertFalse(state.isSavingNote)
        assertEquals("note", state.noteDraft)
        assertEquals("", state.drivingSession?.note)
    }
}
