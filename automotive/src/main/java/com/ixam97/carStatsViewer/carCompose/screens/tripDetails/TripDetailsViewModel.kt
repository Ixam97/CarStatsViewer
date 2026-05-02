package com.ixam97.carStatsViewer.carCompose.screens.tripDetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.database.tripData.DrivingSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TripDetailsState(
    val isLoading: Boolean,
    val selectedTab: TripDetailsTabKeys = TripDetailsTabKeys.Consumption,
    val drivingSession: DrivingSession? = null
)

data class TripDataState(
    val distance: Float? = null
)

class TripDetailsViewModel(sessionId: Long): ViewModel() {

    private val _tripDetailsState = MutableStateFlow(TripDetailsState(isLoading = true))
    val tripDetailsState = _tripDetailsState.asStateFlow()

    init {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                if (CarStatsViewer.appPreferences.debugDelays) delay(5000)
                val session = CarStatsViewer.tripDataSource.getFullDrivingSession(sessionId)
                _tripDetailsState.update {
                    it.copy(
                        isLoading = false,
                        drivingSession = session
                    )
                }
            }
        }
    }

    fun setSelectedTab(tab: TripDetailsTabKeys) {
        _tripDetailsState.update {
            it.copy(selectedTab = tab)
        }
    }
}