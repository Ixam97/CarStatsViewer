package com.ixam97.carStatsViewer.carCompose.screens.tripHistory

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.TestAppEnvironment
import com.ixam97.carStatsViewer.database.tripData.TripType
import de.ixam97.carcompose.components.layout.CarColumn
import de.ixam97.carcompose.theme.CarTheme
import de.ixam97.carcompose.theme.themes.PolestarModernThemeConfig
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders the trip history filters as in the side column of wide screens (scrollable CarColumn
 * with limited height). All filters must stay reachable, the trip types are shown in a 2x2 grid.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w2000dp-h800dp-land-mdpi")
class TripHistoryFiltersLayoutTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var environment: TestAppEnvironment
    private lateinit var viewModel: TripHistoryViewModel

    @Before
    fun setUp() {
        environment = TestAppEnvironment()
        CarStatsViewer.appPreferences.apply {
            tripFilterManual = true
            tripFilterAuto = true
            tripFilterCharge = true
            tripFilterMonth = true
        }
        viewModel = TripHistoryViewModel()
        // let the initial loading finish before the in-memory database is closed in tearDown
        val deadline = System.currentTimeMillis() + 10_000
        while (viewModel.tripHistoryState.value.run { isLoadingPastTrips || isLoadingCurrentTrips }) {
            check(System.currentTimeMillis() < deadline) { "Timed out" }
            Thread.sleep(10)
        }
    }

    @After
    fun tearDown() {
        environment.close()
    }

    private fun renderSideColumn() {
        composeRule.setContent {
            CarTheme(carThemeConfig = PolestarModernThemeConfig, windowInsets = PaddingValues()) {
                Surface(
                    modifier = Modifier.size(700.dp, 600.dp),
                    color = CarTheme.carColors.background,
                    contentColor = CarTheme.carColors.onBackground
                ) {
                    CarColumn {
                        TripHistoryFiltersContent(viewModel = viewModel, sectionTitle = "Trip filters:", showHint = true)
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    /** Saves a screenshot of the activity if SCREENSHOT_DIR is set, for manual inspection. */
    private fun saveScreenshot(name: String) {
        val directory = System.getenv("SCREENSHOT_DIR") ?: return
        val view = composeRule.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        composeRule.runOnUiThread { view.draw(Canvas(bitmap)) }
        File(directory).mkdirs()
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun tripTypes_areShownInTwoByTwoGrid() {
        renderSideColumn()
        saveScreenshot("filters_side_column_top")

        // scroll once, so all buttons are measured at the same scroll position
        composeRule.onNodeWithText("Manual Trips").performScrollTo()
        saveScreenshot("filters_side_column_scrolled")
        val bounds = listOf("Automatic Trips", "Monthly Trips", "Since Charge Trips", "Manual Trips").map {
            composeRule.onNodeWithText(it).getUnclippedBoundsInRoot()
        }
        val (auto, month, charge, manual) = bounds
        assertEquals(auto.top, month.top)
        assertEquals(charge.top, manual.top)
        assertTrue(charge.top > auto.top)
        assertEquals(auto.left, charge.left)
        assertEquals(month.left, manual.left)
        assertTrue(month.left > auto.left)
    }

    @Test
    fun lastTripType_isReachableAndToggles() {
        renderSideColumn()

        composeRule.onNodeWithText("Manual Trips").performScrollTo().assertIsDisplayed().assertIsSelected()
        composeRule.onNodeWithText("Manual Trips").performClick()

        composeRule.onNodeWithText("Manual Trips").assertIsNotSelected()
        assertFalse(viewModel.tripHistoryState.value.selectedFilters[TripType.MANUAL]!!)
        assertTrue(viewModel.tripHistoryState.value.filtersModified)
    }
}
