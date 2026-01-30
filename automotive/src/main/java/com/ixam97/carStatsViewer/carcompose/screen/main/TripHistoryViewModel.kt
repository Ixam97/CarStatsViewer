package com.ixam97.carStatsViewer.carcompose.screen.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.database.tripData.DrivingSession
import com.ixam97.carStatsViewer.database.tripData.TripType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TripHistoryViewModel: ViewModel() {

    data class TripHistoryState(
        val isLoadingCurrentTrips: Boolean = false,
        val isLoadingPastTrips: Boolean = false,
        val deleteMode: Boolean = false,
    )

    var tripHistoryState by mutableStateOf(TripHistoryState())
        private set

    var selectedTripFilters by mutableStateOf(mapOf(
        TripType.MANUAL to true,
        TripType.SINCE_CHARGE to true,
        TripType.AUTO to true,
        TripType.MONTH to true,
    ))
        private set

    var currentTripsList by mutableStateOf<List<DrivingSession>>(listOf())
        private set
    var pastTripsList by mutableStateOf<List<DrivingSession>>(listOf())
        private set


    init {

        tripHistoryState = tripHistoryState.copy(
            isLoadingPastTrips = true,
            isLoadingCurrentTrips = true,
        )

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                if (CarStatsViewer.appPreferences.debugDelays) delay(2000L)
                delay(500L)
                currentTripsList = CarStatsViewer.tripDataSource.getActiveDrivingSessions()
                tripHistoryState = tripHistoryState.copy(
                    isLoadingCurrentTrips = false
                )
            }
        }

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                if (CarStatsViewer.appPreferences.debugDelays) delay(5000L)
                delay(500L)
                pastTripsList = CarStatsViewer.tripDataSource.getPastDrivingSessions()
                tripHistoryState = tripHistoryState.copy(
                    isLoadingPastTrips = false
                )
            }
        }
    }

    fun setDeleteMode(enabled: Boolean) {
        tripHistoryState = tripHistoryState.copy(
            deleteMode = enabled
        )
    }

    fun setTripFilter(tripType: Int, filter: Boolean) {
        if (selectedTripFilters.contains(tripType)) {
            selectedTripFilters = selectedTripFilters + (tripType to filter)
        }
    }
}