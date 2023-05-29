package com.ixam97.carStatsViewer.dataProcessor

import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.dataManager.DrivingState
import com.ixam97.carStatsViewer.database.tripData.TripType
import com.ixam97.carStatsViewer.utils.InAppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TripDataManager {

    private var drivingTripData = DrivingTripData()
        set(value) {
            field = value
            _drivingTripDataFlow.value = value
        }

    private var chargingTripData = ChargingTripData()
        set(value) {
            field = value
            _chargingTripDataFlow.value = field
        }

    private val _drivingTripDataFlow = MutableStateFlow(drivingTripData)
    val drivingTripDataFlow = _drivingTripDataFlow.asStateFlow()

    private val _chargingTripDataFlow = MutableStateFlow(chargingTripData)
    val chargingTripDataFlow = _chargingTripDataFlow.asStateFlow()

    fun newDrivingState(drivingState: Int, oldDrivingState: Int) {
        /** Reset "since charge" after unplugging" */
        if (drivingState != DrivingState.CHARGE && oldDrivingState == DrivingState.CHARGE) {
            CoroutineScope(Dispatchers.IO).launch {
                resetTrip(TripType.SINCE_CHARGE)
            }
        }
        /** Reset "monthly" when last driving point has date in last month */
        /** Reset "Auto" when last driving point is more than 4h old */
    }

    suspend fun changeSelectedTrip(tripType: Int) {
        val drivingSessionsIdsMap = CarStatsViewer.tripDataSource.getActiveDrivingSessionsIdsMap()
        val drivingSessionId = drivingSessionsIdsMap[tripType]
        if (drivingSessionId != null) {
            CarStatsViewer.tripDataSource.getDrivingSession(drivingSessionId)?.let { session ->
                drivingTripData = drivingTripData.copy(
                    selectedTripType = tripType,
                    drivenDistance = session.driven_distance,
                    usedEnergy = session.used_energy
                )
            }
        }
    }

    suspend fun resetTrip(tripType: Int) {
        /** Reset the specified trip type. If none exists, create a new one */
        //CoroutineScope(Dispatchers.IO).launch {
            val drivingSessionsIdsMap = CarStatsViewer.tripDataSource.getActiveDrivingSessionsIdsMap()
            val drivingSessionId = drivingSessionsIdsMap[tripType]
            if (drivingSessionId != null) {
                CarStatsViewer.tripDataSource.supersedeDrivingSession(
                    drivingSessionId,
                    System.currentTimeMillis()
                )
                InAppLogger.i("[NEO] Superseded trip of type ${TripType.tripTypesNameMap[tripType]}")
            } else {
                CarStatsViewer.tripDataSource.startDrivingSession(
                    System.currentTimeMillis(),
                    tripType
                )
                InAppLogger.w("[NEO] No trip of type ${TripType.tripTypesNameMap[tripType]} existing, starting new trip")
            }
        if (tripType == drivingTripData.selectedTripType) drivingTripData = DrivingTripData()
        //}
    }

    fun newDrivingDeltas(distanceDelta: Double, energyDelta: Double) {
        // val currentDrivingDistance = drivingTripData.drivenDistance + distanceDelta
        // val currentDrivingEnergy = drivingTripData.usedEnergy + energyDelta

        // drivingTripData = drivingTripData.copy(
        //     drivenDistance = currentDrivingDistance,
        //     usedEnergy = currentDrivingEnergy
        // )

        CoroutineScope(Dispatchers.IO).launch {
            CarStatsViewer.tripDataSource.getActiveDrivingSessionsIds().forEach { sessionIds ->

                CarStatsViewer.tripDataSource.getDrivingSession(sessionIds)?.let { session ->
                    CarStatsViewer.tripDataSource.updateDrivingSession(session.copy(
                        driven_distance = session.driven_distance + distanceDelta,
                        used_energy = session.used_energy + energyDelta
                    ))
                }

                CarStatsViewer.tripDataSource.getDrivingSession(sessionIds)?.let { session ->
                    if (drivingTripData.selectedTripType == session.session_type) {
                        drivingTripData = drivingTripData.copy(
                            drivenDistance = session.driven_distance,
                            usedEnergy = session.used_energy
                        )
                    }
                }

                val fullDrivingSession = CarStatsViewer.tripDataSource.getFullDrivingSession(sessionIds)
                // Log.v("Database trip dump", GsonBuilder().setPrettyPrinting().create().toJson(fullDrivingSession))
            }
        }

        // Update database
    }

    fun newChargingDeltas(energyDelta: Double) {
        val currentChargingEnergy = chargingTripData.chargedEnergy + energyDelta

        chargingTripData = chargingTripData.copy(
            chargedEnergy = currentChargingEnergy
        )

        // Update database
    }

    fun checkTrips() {
        CoroutineScope(Dispatchers.IO).launch {
            val drivingSessionsIdsMap = CarStatsViewer.tripDataSource.getActiveDrivingSessionsIdsMap()
            if (!drivingSessionsIdsMap.contains(TripType.MANUAL)) {
                CarStatsViewer.tripDataSource.startDrivingSession(System.currentTimeMillis(), TripType.MANUAL)
                InAppLogger.i("[NEO] Created manual trip")
            }
            if (!drivingSessionsIdsMap.contains(TripType.MONTH)) {
                CarStatsViewer.tripDataSource.startDrivingSession(System.currentTimeMillis(), TripType.MONTH)
                InAppLogger.i("[NEO] Created monthly trip")
            }
            if (!drivingSessionsIdsMap.contains(TripType.SINCE_CHARGE)) {
                CarStatsViewer.tripDataSource.startDrivingSession(System.currentTimeMillis(), TripType.SINCE_CHARGE)
                InAppLogger.i("[NEO] Created since charge trip")
            }
            if (!drivingSessionsIdsMap.contains(TripType.AUTO)) {
                CarStatsViewer.tripDataSource.startDrivingSession(System.currentTimeMillis(), TripType.AUTO)
                InAppLogger.i("[NEO] Created auto trip")
            }
        }
    }
}