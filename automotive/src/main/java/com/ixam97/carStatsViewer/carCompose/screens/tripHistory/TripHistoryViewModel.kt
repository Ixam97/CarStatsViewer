package com.ixam97.carStatsViewer.carCompose.screens.tripHistory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.database.tripData.DrivingPoint
import com.ixam97.carStatsViewer.database.tripData.DrivingSession
import com.ixam97.carStatsViewer.database.tripData.TripType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TripHistoryState(
    val isLoadingCurrentTrips: Boolean = true,
    val isLoadingPastTrips: Boolean = true,
    val deleteMode: Boolean = false,
    val selectedFilters: Map<Int, Boolean> = mapOf(
        TripType.MANUAL to CarStatsViewer.appPreferences.tripFilterManual,
        TripType.SINCE_CHARGE to CarStatsViewer.appPreferences.tripFilterCharge,
        TripType.AUTO to CarStatsViewer.appPreferences.tripFilterAuto,
        TripType.MONTH to CarStatsViewer.appPreferences.tripFilterMonth,
    ),
    val selectedDateRange: Pair<Long, Long>? = null,
    val validDateRange: Pair<Long, Long>? = null,
    val filtersModified: Boolean = false,
    val currentTrips: List<DrivingSession> = listOf(),
    val pastTrips: List<DrivingSession> = listOf(),
    val deleteSelection: List<Long> = listOf()
)

class TripHistoryViewModel: ViewModel() {

    private var _tripHistoryState = MutableStateFlow(TripHistoryState())
    val tripHistoryState = _tripHistoryState.asStateFlow()

    init {

        _tripHistoryState.update { it.copy(filtersModified = filtersModified(it.selectedFilters, it.selectedDateRange)) }

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                if (CarStatsViewer.appPreferences.debugDelays) delay(2000L)
                delay(500L)
                _tripHistoryState.update {
                    it.copy(
                        currentTrips = CarStatsViewer.tripDataSource.getActiveDrivingSessions(),
                        isLoadingCurrentTrips = false
                    )
                }
            }
        }

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                if (CarStatsViewer.appPreferences.debugDelays) delay(5000L)
                delay(500L)
                val pastTrips = getPastDrivingSessionsWithLastDrivingPoint() // CarStatsViewer.tripDataSource.getPastDrivingSessions().sortedByDescending { it.start_epoch_time }
                _tripHistoryState.update {
                    it.copy(
                        pastTrips = pastTrips,
                        isLoadingPastTrips = false,
                        validDateRange = calculateValidDateRange(pastTrips),
                    )
                }
            }
        }
    }

    /**
     * Loading past driving sessions and populate the drive points with the last point before the
     * end time to get more accurate end time to display. If there is no suitable drive point, the
     * start time will be used as a dummy. Probably not relevant for real devices and only relevant
     * for trips that have been reset immediately again.
     */
    private suspend fun getPastDrivingSessionsWithLastDrivingPoint(): List<DrivingSession> {
        return CarStatsViewer.tripDataSource.getPastDrivingSessions()
            .sortedByDescending { it.start_epoch_time }
            .apply {
                this.forEach {
                    var lastDrivingPoint = if (it.end_epoch_time != null)
                        CarStatsViewer.tripDataSource.getLatestDrivingPointBefore(it.end_epoch_time)
                    else null
                    if (lastDrivingPoint != null) {
                        if (lastDrivingPoint.driving_point_epoch_time <= it.start_epoch_time)
                            lastDrivingPoint = lastDrivingPoint.copy(driving_point_epoch_time = it.start_epoch_time)
                        it.drivingPoints = listOf(lastDrivingPoint)
                    } else {
                        it.drivingPoints = listOf(DrivingPoint(
                            driving_point_epoch_time = it.start_epoch_time,
                            energy_delta = 0f,
                            distance_delta = 0f,
                            point_marker_type = null,
                            state_of_charge = 0f,
                            lat = null,
                            lon = null,
                            alt = null
                        ))
                    }
                }
            }
    }

    private fun calculateValidDateRange(pastTrips: List<DrivingSession>): Pair<Long, Long>? {
        if (pastTrips.isEmpty()) return null
        val earliestDateMillis = pastTrips.last().start_epoch_time - 86400000
        val latestDateMillis = pastTrips.first().start_epoch_time
        return earliestDateMillis to latestDateMillis
    }

    fun updateValidDateRange() {
        _tripHistoryState.value.pastTrips.let { pastTrips ->
            _tripHistoryState.update {
                it.copy(
                    validDateRange = calculateValidDateRange(pastTrips)
                )
            }
        }
    }

    fun getValidDateRange(): Pair<Long, Long> {
        return _tripHistoryState.value.validDateRange?:(0L to 0L)
    }

    fun reloadTrips() {
        _tripHistoryState.update { it.copy(
            isLoadingCurrentTrips = true,
            isLoadingPastTrips = true
        ) }

        viewModelScope.launch(Dispatchers.IO) {
            if (CarStatsViewer.appPreferences.debugDelays) delay(2000L)
            delay(500L)
            _tripHistoryState.update {
                it.copy(
                    pastTrips = getPastDrivingSessionsWithLastDrivingPoint(), // CarStatsViewer.tripDataSource.getPastDrivingSessions().sortedByDescending { it.start_epoch_time },
                    isLoadingPastTrips = false
                )
            }
            updateValidDateRange()
        }

        viewModelScope.launch(Dispatchers.IO) {
            if (CarStatsViewer.appPreferences.debugDelays) delay(5000L)
            delay(500L)
            _tripHistoryState.update {
                it.copy(
                    currentTrips = CarStatsViewer.tripDataSource.getActiveDrivingSessions(),
                    isLoadingCurrentTrips = false
                )
            }
        }
    }

    fun setFilterDateRange(dateRange: Pair<Long, Long>?) {
        _tripHistoryState.update { it.copy(
            selectedDateRange = dateRange,
            filtersModified = filtersModified(it.selectedFilters, dateRange)
        ) }
        //TODO: Implement Preferences for Date range
    }

    fun setTripFilter(tripType: Int, filter: Boolean) {
        if (_tripHistoryState.value.selectedFilters.contains(tripType)) {
            when(tripType) {
                TripType.MANUAL -> CarStatsViewer.appPreferences.tripFilterManual = filter
                TripType.AUTO -> CarStatsViewer.appPreferences.tripFilterAuto = filter
                TripType.SINCE_CHARGE -> CarStatsViewer.appPreferences.tripFilterCharge = filter
                TripType.MONTH -> CarStatsViewer.appPreferences.tripFilterMonth = filter
            }
            _tripHistoryState.update {
                val mutableMap = it.selectedFilters.toMutableMap()
                mutableMap[tripType] = filter
                it.copy(
                    selectedFilters = mutableMap,
                    filtersModified = filtersModified(mutableMap, it.selectedDateRange)
                )
            }
            updateValidDateRange()
        }
    }

    fun setDeleteMode(enabled: Boolean) {
        _tripHistoryState.update { it.copy(
            deleteMode = enabled,
            deleteSelection = listOf()
        ) }
    }

    fun resetTrip(tripType: Int, sessionId: Long) {
        _tripHistoryState.update { it.copy(isLoadingCurrentTrips = true) }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                CarStatsViewer.dataProcessor.resetTrip(tripType, CarStatsViewer.dataProcessor.realTimeData.drivingState)
                _tripHistoryState.update { state ->
                    val deletedSession = _tripHistoryState.value.currentTrips.first { it.driving_session_id == sessionId }
                    val pastTrips = _tripHistoryState.value.pastTrips.toMutableList()
                    pastTrips.add(index = 0, deletedSession)
                    state.copy(
                        isLoadingCurrentTrips = false,
                        currentTrips = CarStatsViewer.tripDataSource.getActiveDrivingSessions(),
                        pastTrips = pastTrips
                    )
                }
                updateValidDateRange()
            }
        }
    }

    fun addOrRemoveDeleteSelection(sessionId: Long) {
        val mutableDeleteSelection = _tripHistoryState.value.deleteSelection.toMutableList()

        if (mutableDeleteSelection.contains(sessionId)) {
            mutableDeleteSelection.removeAll {it == sessionId}
        } else {
            mutableDeleteSelection.add(sessionId)
        }

        _tripHistoryState.update { it.copy(deleteSelection = mutableDeleteSelection) }
    }

    fun deleteSelectedTrips() {
        val mutablePastTrips = _tripHistoryState.value.pastTrips.toMutableList()
        val deleteSelection = _tripHistoryState.value.deleteSelection

        mutablePastTrips.removeAll { deleteSelection.contains(it.driving_session_id) }

        viewModelScope.launch(Dispatchers.IO) {
            deleteSelection.forEach { sessionId ->
                CarStatsViewer.tripDataSource.deleteDrivingSessionById(sessionId)
            }
            _tripHistoryState.update {
                it.copy(
                    pastTrips = mutablePastTrips,
                    deleteSelection = listOf(),
                    deleteMode = false
                )
            }
            updateValidDateRange()
        }
    }

    private fun filtersModified(filters: Map<Int, Boolean>, dateRange: Pair<Long, Long>?): Boolean {
        return !(filters[TripType.MANUAL] == true &&
                filters[TripType.MONTH] == true &&
                filters[TripType.AUTO] == true &&
                filters[TripType.SINCE_CHARGE] == true &&
                dateRange == null)
    }
}