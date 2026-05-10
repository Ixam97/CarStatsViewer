package com.ixam97.carStatsViewer.carCompose.screens.tripDetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.database.tripData.ChargingSession
import com.ixam97.carStatsViewer.database.tripData.DrivingSession
import com.ixam97.carStatsViewer.map.Mapbox
import com.ixam97.carStatsViewer.map.MapboxInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChargingSessionDetails(
    val chargingSession: ChargingSession,
    val chargingLocation: String? = null,
)

data class TripDetailsState(
    val isLoading: Boolean,
    val isSideBySideLayout: Boolean = false,
    val selectedTab: TripDetailsTabKeys = TripDetailsTabKeys.Consumption,
    val prevSelectedTab: TripDetailsTabKeys? = null,
    val drivingSession: DrivingSession? = null,
    val startLocation: String? = null,
    val destinationLocation: String? = null,
    val chargingSessionsDetails: List<ChargingSessionDetails> = listOf(),
    val selectedChargingSessionDetailsId: Long? = null,
    val showChargingDetails: Boolean = false,
    val debugLandscapeOverride: Boolean = false
)

class TripDetailsViewModel(sessionId: Long): ViewModel() {

    private val _tripDetailsState = MutableStateFlow(TripDetailsState(isLoading = true))
    val tripDetailsState = _tripDetailsState.asStateFlow()

    private val _mapAction = Channel<MapboxInterface.MapboxAction>()
    val mapAction = _mapAction.receiveAsFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            if (CarStatsViewer.appPreferences.debugDelays) delay(5000)
            delay(500)
            val session = CarStatsViewer.tripDataSource.getFullDrivingSession(sessionId)
            _tripDetailsState.update {
                it.copy(
                    isLoading = false,
                    drivingSession = session,
                    chargingSessionsDetails = session.chargingSessions?.map { chargingSession ->
                        ChargingSessionDetails(
                            chargingSession = chargingSession,
                            chargingLocation = if (chargingSession.lat != null)
                                CarStatsViewer.appContext.getString(R.string.summary_loading_location)
                            else CarStatsViewer.appContext.getString(R.string.summary_location_unavailable)
                        )
                    }?: listOf()
                )
            }
            loadLocationStrings().join()
        }
    }

    fun setSideBySideLayout(value: Boolean) {
        _tripDetailsState.update { it.copy(
            isSideBySideLayout = value
        ) }
    }

    fun setMapLocation(location: MapboxInterface.MapboxLocation) {
        if (!_tripDetailsState.value.isSideBySideLayout) {
            _tripDetailsState.update { it.copy(
                showChargingDetails = false,
                selectedTab = TripDetailsTabKeys.Map
            ) }
        }
        viewModelScope.launch {
            _mapAction.send(MapboxInterface.MapboxAction.ZoomToLocation(location))
        }
    }

    fun resetMapLocation() {
        viewModelScope.launch {
            _mapAction.send(MapboxInterface.MapboxAction.Reset)
        }
    }

    fun setDebugOverride() {
        _tripDetailsState.update { it.copy(
            debugLandscapeOverride = true
        ) }
    }

    fun setSelectedTab(tab: TripDetailsTabKeys) {
        val prevSelectedTab = _tripDetailsState.value.selectedTab
        _tripDetailsState.update {
            it.copy(
                selectedTab = tab,
                prevSelectedTab = prevSelectedTab
            )
        }
    }

    fun closeChargingDetails() {
        resetMapLocation()
        _tripDetailsState.update {
            it.copy(showChargingDetails = false)
        }
    }

    fun setSelectedChargingDetails(chargingSessionId: Long) {
        _tripDetailsState.update { it.copy(
            selectedChargingSessionDetailsId = chargingSessionId,
            showChargingDetails = true
        ) }
        if (_tripDetailsState.value.isSideBySideLayout) {
            viewModelScope.launch {
                _tripDetailsState.value.chargingSessionsDetails.firstOrNull { it.chargingSession.charging_session_id == chargingSessionId }.let {
                    _mapAction.send(MapboxInterface.MapboxAction.ZoomToLocation(
                        if (it != null && it.chargingSession.lat != null && it.chargingSession.lon != null) {
                            MapboxInterface.MapboxLocation(it.chargingSession.lon.toDouble(), it.chargingSession.lat.toDouble(), 14.5)
                        } else {
                            MapboxInterface.MapboxLocation(13.848338959636092, 55.42557254430007, 14.5)
                        }
                    ))
                }
            }
        }
    }

    private fun loadLocationStrings(): Job {
        return viewModelScope.launch(Dispatchers.IO) {
            var startLocation = CarStatsViewer.appContext.getString(R.string.summary_location_unavailable)
            var destinationLocation = CarStatsViewer.appContext.getString(R.string.summary_location_unavailable)

            if (CarStatsViewer.appPreferences.debugDelays) delay(2000)

            _tripDetailsState.value.drivingSession?.let { drivingSession ->
                if (!drivingSession.drivingPoints.isNullOrEmpty()) {
                    val coordinates = drivingSession.drivingPoints!!.filter { it.lat != null }
                    if (coordinates.isNotEmpty()) {
                        startLocation = Mapbox.getAddress(
                            coordinates.first().lon!!.toDouble(),
                            coordinates.first().lat!!.toDouble()
                        )
                        destinationLocation = Mapbox.getAddress(
                            coordinates.last().lon!!.toDouble(),
                            coordinates.last().lat!!.toDouble()
                        )
                    }
                }
            }

            _tripDetailsState.update {
                it.copy(
                    startLocation = startLocation,
                    destinationLocation = destinationLocation
                )
            }

            data class ChargingLocation(
                val sessionId: Long,
                val lat: Double?,
                val lon: Double?
            )

            val chargingLocations = _tripDetailsState.value.chargingSessionsDetails
                .map { ChargingLocation(
                    sessionId = it.chargingSession.charging_session_id,
                    lat = it.chargingSession.lat?.toDouble(),
                    lon = it.chargingSession.lon?.toDouble()
                ) }

            chargingLocations.forEach { chargingLocation ->

                if (CarStatsViewer.appPreferences.debugDelays) delay(2000)

                var location = CarStatsViewer.appContext.getString(R.string.summary_location_unavailable)
                if (chargingLocation.lat != null && chargingLocation.lon != null) {
                    location = Mapbox.getAddress(chargingLocation.lon, chargingLocation.lat)
                }

                _tripDetailsState.value.chargingSessionsDetails.toMutableList().let { mutableChargingSessionsDetails ->
                    val index = mutableChargingSessionsDetails.indexOfFirst { it.chargingSession.charging_session_id == chargingLocation.sessionId }
                    mutableChargingSessionsDetails[index] = mutableChargingSessionsDetails[index].copy(chargingLocation = location)

                    _tripDetailsState.update { it.copy(
                        chargingSessionsDetails = mutableChargingSessionsDetails
                    ) }
                }
            }
        }
    }
}