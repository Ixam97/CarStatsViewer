package com.ixam97.carStatsViewer.carCompose.screens.tripHistory

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Surface
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.TestAppEnvironment
import com.ixam97.carStatsViewer.utils.DistanceUnitEnum
import de.ixam97.carcompose.theme.CarTheme
import de.ixam97.carcompose.theme.themes.PolestarModernThemeConfig
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog
import java.io.File

/**
 * Renders the distance range dialog of the trip history filters on a narrow and a wide screen.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DistanceRangeDialogLayoutTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var environment: TestAppEnvironment
    private var appliedRange: DistanceRange? = null
    private var applied = false

    @Before
    fun setUp() {
        environment = TestAppEnvironment()
        CarStatsViewer.appPreferences.distanceUnit = DistanceUnitEnum.KM
    }

    @After
    fun tearDown() {
        environment.close()
    }

    private fun renderDialog(selected: DistanceRange?) {
        composeRule.setContent {
            CarTheme(carThemeConfig = PolestarModernThemeConfig, windowInsets = PaddingValues()) {
                // provides the content color the dialog inherits in the app
                Surface(color = CarTheme.carColors.background, contentColor = CarTheme.carColors.onBackground) {
                    DistanceRangeDialog(
                        selected = selected,
                        onApply = {
                            applied = true
                            appliedRange = it
                        },
                        onCancel = { }
                    )
                }
            }
        }
        composeRule.waitForIdle()
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

    private fun assertControlsDisplayed() {
        listOf("Cancel", "Apply", "≤ 10 km", "10 – 50 km", "50 – 200 km", "≥ 200 km").forEach { label ->
            composeRule.onNodeWithText(label).assertIsDisplayed()
        }
        val stepperButtons = stepperButtons()
        assertEquals(4, stepperButtons.size)
        stepperButtons.forEach { button ->
            composeRule.onNode(button)
                .assertHeightIsAtLeast(76.dp)
                .assertWidthIsAtLeast(76.dp)
        }
    }

    @Test
    @Config(qualifiers = "w1000dp-h700dp-land-mdpi")
    fun dialog_showsAllControls_narrow() {
        renderDialog(DistanceRange(10, 200))

        assertControlsDisplayed()
        composeRule.onAllNodesWithText("10 – 200 km")[0].assertIsDisplayed()
        saveScreenshot("distance_dialog_narrow")
    }

    @Test
    @Config(qualifiers = "w1920dp-h1080dp-land-mdpi")
    fun dialog_showsAllControls_wide() {
        renderDialog(null)

        assertControlsDisplayed()
        composeRule.onNodeWithText("All distances").assertIsDisplayed()
        saveScreenshot("distance_dialog_wide")
    }

    @Test
    @Config(qualifiers = "w1000dp-h700dp-land-mdpi")
    fun upperStepper_fineAdjustsAndApplies() {
        renderDialog(DistanceRange(200, 300))
        val upperDecrement = stepperButtons()[2]

        repeat(6) { composeRule.onNode(upperDecrement).performClick() }

        composeRule.onNodeWithText("200 – 240 km").assertIsDisplayed()
        composeRule.onNodeWithText("Apply").performClick()
        assertEquals(true, applied)
        assertEquals(DistanceRange(200, 240), appliedRange)
    }

    @Test
    @Config(qualifiers = "w1000dp-h700dp-land-mdpi")
    fun slider_movesValue() {
        renderDialog(DistanceRange(0, null))

        // the slider exposes its two thumbs as separate SetProgress nodes; move the lower one to the 200 anchor
        composeRule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))[0]
            .performSemanticsAction(SemanticsActions.SetProgress) { it(valueToPosition(200)) }

        // title and the matching preset
        composeRule.onAllNodesWithText("≥ 200 km").assertCountEquals(2)
    }

    /** Matchers for the stepper buttons in layout order: lower −, lower +, upper −, upper +. */
    private fun stepperButtons(): List<SemanticsMatcher> = composeRule
        .onAllNodes(hasClickAction() and !SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))
        .fetchSemanticsNodes()
        .filter { node -> node.config.none { it.key.name == "Text" } }
        .sortedWith(compareBy({ it.boundsInRoot.top }, { it.boundsInRoot.left }))
        .map { node -> SemanticsMatcher("id ${node.id}") { it.id == node.id } }
}
