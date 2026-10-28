package com.ixam97.carStatsViewer.carCompose.screens.tripHistory

import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.TestAppEnvironment
import com.ixam97.carStatsViewer.database.tripData.TripType
import com.ixam97.carStatsViewer.utils.DistanceUnitEnum
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class TripHistoryViewModelFilterTest {

    private lateinit var environment: TestAppEnvironment

    /** Session ids by driven distance in km. */
    private val sessions = mutableMapOf<Int, Long>()

    @Before
    fun setUp() {
        environment = TestAppEnvironment()
        CarStatsViewer.appPreferences.apply {
            tripFilterManual = true
            tripFilterAuto = true
            tripFilterCharge = true
            tripFilterMonth = true
            distanceUnit = DistanceUnitEnum.KM
        }
        // one trip per day, starting on day 1
        createSession(day = 1, distanceKm = 5, type = TripType.AUTO)
        createSession(day = 2, distanceKm = 42, type = TripType.AUTO)
        createSession(day = 3, distanceKm = 120, type = TripType.MANUAL)
        createSession(day = 4, distanceKm = 240, type = TripType.MANUAL)
        createSession(day = 5, distanceKm = 900, type = TripType.MONTH)
    }

    @After
    fun tearDown() {
        environment.close()
    }

    private fun createSession(day: Int, distanceKm: Int, type: Int) = runBlocking {
        val start = day * DAY
        val sessionId = environment.tripDataSource.startDrivingSession(start, type)
        val session = environment.tripDataSource.getDrivingSession(sessionId)!!
        environment.tripDataSource.updateDrivingSession(session.copy(driven_distance = distanceKm * 1000.0))
        environment.tripDataSource.endDrivingSession(start + 3_600_000L, sessionId)
        sessions[distanceKm] = sessionId
    }

    /** Runs pending main looper tasks (viewModelScope) until [condition] holds. */
    private fun waitUntil(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 10_000
        while (true) {
            shadowOf(Looper.getMainLooper()).idle()
            if (condition()) return
            check(System.currentTimeMillis() < deadline) { "Timed out" }
            Thread.sleep(10)
        }
    }

    private fun createLoadedViewModel(): TripHistoryViewModel {
        val viewModel = TripHistoryViewModel()
        waitUntil { !viewModel.tripHistoryState.value.isLoadingPastTrips && viewModel.filteredPastTrips.value.isNotEmpty() }
        return viewModel
    }

    private fun TripHistoryViewModel.filteredIds(): List<Long> {
        shadowOf(Looper.getMainLooper()).idle()
        return filteredPastTrips.value.map { it.driving_session_id }
    }

    private fun ids(vararg distancesKm: Int) = distancesKm.map { sessions[it]!! }

    @Test
    fun withoutFilters_showsAllPastTripsNewestFirst() {
        val viewModel = createLoadedViewModel()

        assertEquals(ids(900, 240, 120, 42, 5), viewModel.filteredIds())
        assertFalse(viewModel.tripHistoryState.value.filtersModified)
    }

    @Test
    fun distanceRange_minOnly() {
        val viewModel = createLoadedViewModel()

        viewModel.setFilterDistanceRange(DistanceRange(200, null))

        assertEquals(ids(900, 240), viewModel.filteredIds())
        assertTrue(viewModel.tripHistoryState.value.filtersModified)
    }

    @Test
    fun distanceRange_maxOnly() {
        val viewModel = createLoadedViewModel()

        viewModel.setFilterDistanceRange(DistanceRange(0, 42))

        assertEquals(ids(42, 5), viewModel.filteredIds())
    }

    @Test
    fun distanceRange_bothBoundsInclusive() {
        val viewModel = createLoadedViewModel()

        viewModel.setFilterDistanceRange(DistanceRange(42, 240))

        assertEquals(ids(240, 120, 42), viewModel.filteredIds())
    }

    @Test
    fun distanceRange_combinedWithTypeAndDateFilters() {
        val viewModel = createLoadedViewModel()

        viewModel.setFilterDistanceRange(DistanceRange(10, 1000))
        viewModel.setTripFilter(TripType.MONTH, false)
        assertEquals(ids(240, 120, 42), viewModel.filteredIds())

        // date range covers the start days 2..3
        viewModel.setFilterDateRange(2 * DAY to 2 * DAY)
        assertEquals(ids(120, 42), viewModel.filteredIds())
    }

    @Test
    fun distanceRange_withoutMatchingTrips_resultsInEmptyList() {
        val viewModel = createLoadedViewModel()

        viewModel.setFilterDistanceRange(DistanceRange(2000, null))

        assertEquals(emptyList<Long>(), viewModel.filteredIds())
    }

    @Test
    fun distanceRange_fullRangeIsNormalizedToNoFilter() {
        val viewModel = createLoadedViewModel()
        viewModel.setFilterDistanceRange(DistanceRange(10, 50))
        assertTrue(viewModel.tripHistoryState.value.filtersModified)

        viewModel.setFilterDistanceRange(DistanceRange(0, null))

        assertNull(viewModel.tripHistoryState.value.selectedDistanceRange)
        assertFalse(viewModel.tripHistoryState.value.filtersModified)
        assertEquals(5, viewModel.filteredIds().size)
    }

    @Test
    fun distanceRange_isInterpretedInMiles() {
        CarStatsViewer.appPreferences.distanceUnit = DistanceUnitEnum.MILES
        val viewModel = createLoadedViewModel()

        // 120 km = 74.6 mi, 240 km = 149.1 mi
        viewModel.setFilterDistanceRange(DistanceRange(100, 200))

        assertEquals(ids(240), viewModel.filteredIds())
    }

    private companion object {
        const val DAY = 86_400_000L
    }
}
