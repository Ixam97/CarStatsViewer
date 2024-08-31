package com.ixam97.carStatsViewer.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.database.tripData.DrivingSession
import com.ixam97.carStatsViewer.map.Mapbox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TripDetailsViewModel: ViewModel() {

    data class TripDetailsState(
        val drivingSession: DrivingSession? = null,
        val selectedSection: Int = 0,
        val startLocation: String? = null,
        val endLocation: String? = null,
    )

    // private var _tripDetailsState = MutableStateFlow<TripDetailsState>(TripDetailsState())
    // val tripDetailsState = _tripDetailsState.asStateFlow()

    var tripDetailsState by mutableStateOf(TripDetailsState())
        private set

    init {
    }

    fun setSelectedSection(section: Int) {
        // _tripDetailsState.update { _tripDetailsState.value.copy(selectedSection = section) }
        tripDetailsState = tripDetailsState.copy(
            selectedSection = section
        )
    }

    fun loadLocationStrings(): Job {
        return viewModelScope.launch(Dispatchers.IO) {
            delay(2000)
            tripDetailsState.drivingSession?.let { trip ->
                if (!trip.drivingPoints.isNullOrEmpty()) {
                    val coordinates = trip.drivingPoints!!.filter { it.lat != null }
                    val startAddr = Mapbox.getAddress(
                        coordinates.first().lon!!.toDouble(),
                        coordinates.first().lat!!.toDouble()
                    )
                    val destAddr = Mapbox.getAddress(
                        coordinates.last().lon!!.toDouble(),
                        coordinates.last().lat!!.toDouble()
                    )
                    // _tripDetailsState.update {
                    //     _tripDetailsState.value.copy(
                    //         startLocation = startAddr,
                    //         endLocation = destAddr
                    //     )
                    // }
                    tripDetailsState = tripDetailsState.copy(
                        startLocation = startAddr,
                        endLocation = destAddr
                    )
                } else {
                    // _tripDetailsState.update {
                    //     _tripDetailsState.value.copy(
                    //         startLocation = "Location not available",
                    //         endLocation = "Location not available"
                    //     )
                    // }
                    tripDetailsState = tripDetailsState.copy(
                        startLocation = "Location not available",
                        endLocation = "Location not available"
                    )
                }
            }
        }
    }

    fun loadDrivingSession(sessionId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            delay(2000)
            tripDetailsState = tripDetailsState.copy(
                drivingSession = CarStatsViewer.tripDataSource.getFullDrivingSession(sessionId)
            )
            loadLocationStrings().join()
        }
    }
}