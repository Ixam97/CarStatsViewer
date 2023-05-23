package com.ixam97.carStatsViewer.dataProcessor

import android.util.Log
import com.google.gson.GsonBuilder
import com.ixam97.carStatsViewer.CarStatsViewer
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

    fun newDrivingState(drivingState: Int) {
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
                    drivingTripData = drivingTripData.copy(
                        drivenDistance = session.driven_distance,
                        usedEnergy = session.used_energy
                    )
                }

                val fullDrivingSession = CarStatsViewer.tripDataSource.getFullDrivingSession(sessionIds)
                Log.v("Database trip dump", GsonBuilder().setPrettyPrinting().create().toJson(fullDrivingSession))
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
}