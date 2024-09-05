package com.ixam97.carStatsViewer.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.database.tripData.ChargingSession
import com.ixam97.carStatsViewer.database.tripData.DrivingSession
import com.ixam97.carStatsViewer.map.Mapbox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class TripDetailsViewModel: ViewModel() {

    companion object {
        const val DETAILS_SECTION = 0
        const val CHARGING_SECTION = 1
        const val MAP_SECTION = 2
    }

    data class TripDetailsState(
        val drivingSession: DrivingSession? = null,
        val selectedSection: Int = 0,
        val startLocation: String? = null,
        val endLocation: String? = null,
        val selectedSecondaryDimension: Int = 0,
        val showChargingSessionDetails: Boolean = false,
        val chargingSession: ChargingSession? = null
    )

    private val _changeDistanceFlow = MutableSharedFlow<Float>()
    val changeDistanceFlow = _changeDistanceFlow.asSharedFlow()

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
            delay(1000)
            tripDetailsState = tripDetailsState.copy(
                drivingSession = CarStatsViewer.tripDataSource.getFullDrivingSession(sessionId),
                selectedSecondaryDimension = CarStatsViewer.appPreferences.secondaryConsumptionDimension
            )
            loadLocationStrings().join()
        }
    }

    fun setTripDistance(index: Int) {
        viewModelScope.launch {
            _changeDistanceFlow.emit(0f)
            // delay(50)
            _changeDistanceFlow.emit(
                when (index) {
                    0 -> 100_000f
                    1 -> 40_000f
                    2 -> 20_000f
                    else -> -1f
                }
            )
        }
    }

    fun setSecondaryPlotDimension(index: Int) {
        CarStatsViewer.appPreferences.secondaryConsumptionDimension = index
        tripDetailsState = tripDetailsState.copy(
            selectedSecondaryDimension = index
        )
    }

    fun selectChargingSession(id:Long) {
        tripDetailsState.drivingSession?.chargingSessions?.let { chargingSessions ->
            val session = chargingSessions.find { it.charging_session_id == id }
            session?.let { session ->
                tripDetailsState = tripDetailsState.copy(
                    selectedSection = CHARGING_SECTION,
                    showChargingSessionDetails = true,
                    chargingSession = session
                )
            }
        }
    }

    fun closeChargingSessionDetails() {
        tripDetailsState = tripDetailsState.copy(
            showChargingSessionDetails = false
        )
    }
}