package com.ixam97.carStatsViewer.carCompose.screens.tripHistory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ixam97.carStatsViewer.CarStatsViewer
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
    val filtersModified: Boolean = false,
    val currentTrips: List<DrivingSession> = listOf(),
    val pastTrips: List<DrivingSession> = listOf(),
    val deleteSelection: List<Long> = listOf()
)

class TripHistoryViewModel: ViewModel() {

    private var _tripHistoryState = MutableStateFlow(TripHistoryState())
    val tripHistoryState = _tripHistoryState.asStateFlow()

    init {

        _tripHistoryState.update { it.copy(filtersModified = filtersModified(it.selectedFilters)) }

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
                _tripHistoryState.update {
                    it.copy(
                        pastTrips = CarStatsViewer.tripDataSource.getPastDrivingSessions(),
                        isLoadingPastTrips = false
                    )
                }
            }
        }
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
                    filtersModified = filtersModified(mutableMap)
                )
            }
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
        }
    }

    private fun filtersModified(filters: Map<Int, Boolean>): Boolean {
        return !(filters[TripType.MANUAL] == true &&
                filters[TripType.MONTH] == true &&
                filters[TripType.AUTO] == true &&
                filters[TripType.SINCE_CHARGE] == true)
    }
}