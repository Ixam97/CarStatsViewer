package com.ixam97.carStatsViewer.carCompose.screens.tripDetails

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ixam97.carStatsViewer.TestAppEnvironment
import com.ixam97.carStatsViewer.database.tripData.TripType
import com.ixam97.carStatsViewer.repository.tripNotes.TripNotesRepository
import de.ixam97.carcompose.theme.CarTheme
import de.ixam97.carcompose.theme.themes.PolestarModernThemeConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog
import java.io.File

/**
 * Renders the note editor dialog of the trip details. The buttons must be fully visible right
 * after opening the editor, and the editor must not depend on the height left for the section
 * (in portrait layouts the section had no height left while the on-screen keyboard was shown).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w1000dp-h700dp-land-mdpi")
class TripNoteEditorLayoutTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var environment: TestAppEnvironment

    @Before
    fun setUp() {
        environment = TestAppEnvironment()
    }

    @After
    fun tearDown() {
        environment.close()
    }

    private fun editingViewModel(): TripDetailsViewModel {
        val sessionId = runBlocking { environment.tripDataSource.startDrivingSession(1_000L, TripType.MANUAL) }
        val viewModel = TripDetailsViewModel(
            sessionId = sessionId,
            tripNotesRepository = lazy { TripNotesRepository(environment.tripDataSource) { _, _ -> } }
        )
        runBlocking {
            withTimeout(10_000) {
                while (viewModel.tripDetailsState.value.drivingSession == null) delay(10)
            }
        }
        viewModel.startEditNote()
        return viewModel
    }

    private fun renderEditor(width: Dp, height: Dp, noteEditingEnabled: Boolean = true) {
        val viewModel = editingViewModel()
        // The focused text field shows a blinking cursor, which never lets the UI become idle
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            CarTheme(carThemeConfig = PolestarModernThemeConfig, windowInsets = PaddingValues()) {
                Surface(
                    modifier = Modifier.size(width, height),
                    color = CarTheme.carColors.background,
                    contentColor = CarTheme.carColors.onBackground
                ) {
                    TripDetailsConsumptionSection(
                        viewModel = viewModel,
                        drivingSession = viewModel.tripDetailsState.value.drivingSession,
                        noteEditingEnabled = noteEditingEnabled
                    )
                }
            }
        }
        composeRule.mainClock.advanceTimeBy(1_000)
    }

    private fun assertButtonsFullyVisible() {
        listOf("Save", "Cancel").forEach { label ->
            composeRule.onNodeWithText(label)
                .assertIsDisplayed()
                .assertHeightIsAtLeast(20.dp)
        }
    }

    /** Saves a screenshot of the dialog if SCREENSHOT_DIR is set, for manual inspection. */
    private fun saveScreenshot(name: String) {
        val directory = System.getenv("SCREENSHOT_DIR") ?: return
        val view = ShadowDialog.getLatestDialog().window!!.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        composeRule.runOnUiThread { view.draw(Canvas(bitmap)) }
        File(directory).mkdirs()
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun editor_showsReadableButtonsWhenOpened() {
        renderEditor(width = 800.dp, height = 600.dp)

        assertButtonsFullyVisible()
        composeRule.onNodeWithText("Add note…").assertIsDisplayed()
        saveScreenshot("note_editor_full_height")
    }

    @Test
    fun editor_isFullyVisibleWithoutHeightLeftForSection() {
        renderEditor(width = 800.dp, height = 10.dp)

        assertButtonsFullyVisible()
        composeRule.onNodeWithText("Add note…").assertIsDisplayed()
        composeRule.onNode(hasSetTextAction()).assertHeightIsAtLeast(60.dp)
        saveScreenshot("note_editor_no_section_height")
    }

    @Test
    fun editor_showsHintWhileDriving() {
        renderEditor(width = 800.dp, height = 600.dp, noteEditingEnabled = false)

        assertButtonsFullyVisible()
        composeRule.onNodeWithText("Notes can only be edited while parked").assertIsDisplayed()
        saveScreenshot("note_editor_driving")
    }
}
