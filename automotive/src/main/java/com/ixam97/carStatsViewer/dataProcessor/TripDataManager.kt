package com.ixam97.carStatsViewer.dataProcessor

import android.util.Log
import com.google.gson.GsonBuilder
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.Defines
import com.ixam97.carStatsViewer.dataManager.DrivingState
import com.ixam97.carStatsViewer.dataManager.TimeTracker
import com.ixam97.carStatsViewer.database.tripData.TripType
import com.ixam97.carStatsViewer.utils.InAppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

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

    val timerMap = mapOf(
        TripType.MANUAL to TimeTracker(),
        TripType.SINCE_CHARGE to TimeTracker(),
        TripType.AUTO to TimeTracker(),
        TripType.MONTH to TimeTracker(),
    )

    private val _drivingTripDataFlow = MutableStateFlow(drivingTripData)
    val drivingTripDataFlow = _drivingTripDataFlow.asStateFlow()

    private val _chargingTripDataFlow = MutableStateFlow(chargingTripData)
    val chargingTripDataFlow = _chargingTripDataFlow.asStateFlow()

    suspend fun newDrivingState(drivingState: Int, oldDrivingState: Int) {
        /** Reset "since charge" after unplugging" */
        if (drivingState != DrivingState.CHARGE && oldDrivingState == DrivingState.CHARGE) {
            resetTrip(TripType.SINCE_CHARGE, drivingState)
        }
        /** Reset "monthly" when last driving point has date in last month */
        /** Reset "Auto" when last driving point is more than 4h old */
        if (drivingState == DrivingState.DRIVE && oldDrivingState != DrivingState.DRIVE) {
            val lastDriveTime = CarStatsViewer.tripDataSource.getLatestDrivingPoint()?.driving_point_epoch_time
            if (lastDriveTime != null) {
                if (Date().month != Date(lastDriveTime).month)
                    resetTrip(TripType.MONTH, drivingState)
                if (lastDriveTime < (System.currentTimeMillis() - Defines.AUTO_RESET_TIME))
                    resetTrip(TripType.AUTO, drivingState)
            } else {
                InAppLogger.w("[NEO] No existing driving points for reset reference!")
            }
        }

        if (drivingState == DrivingState.DRIVE && oldDrivingState != DrivingState.DRIVE) {
            timerMap.forEach {
                it.value.start()
            }
        } else if (drivingState != DrivingState.DRIVE && oldDrivingState == DrivingState.DRIVE) {
            timerMap.forEach {
                it.value.stop()
            }
        }
    }

    suspend fun changeSelectedTrip(tripType: Int) {
        val drivingSessionsIdsMap = CarStatsViewer.tripDataSource.getActiveDrivingSessionsIdsMap()
        val drivingSessionId = drivingSessionsIdsMap[tripType]
        if (drivingSessionId != null) {
            CarStatsViewer.tripDataSource.getDrivingSession(drivingSessionId)?.let { session ->
                InAppLogger.i("[NEO] Selected trip type changed to ${TripType.tripTypesNameMap[tripType]}")
                drivingTripData = drivingTripData.copy(
                    driveTime = session.drive_time,
                    selectedTripType = tripType,
                    drivenDistance = session.driven_distance,
                    usedEnergy = session.used_energy,
                    usedStateOfCharge = session.used_soc,
                    usedStateOfChargeEnergy = session.used_soc_energy
                )
            }
        }
    }

    suspend fun resetTrip(tripType: Int, drivingState: Int) {
        /** Reset the specified trip type. If none exists, create a new one */
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
        if (tripType == drivingTripData.selectedTripType) drivingTripData = DrivingTripData(selectedTripType = drivingTripData.selectedTripType)
        timerMap[tripType]?.reset()
        if (drivingState == DrivingState.DRIVE) timerMap[tripType]?.start()
    }

    suspend fun newDrivingDeltas(distanceDelta: Double, energyDelta: Double) {
        CarStatsViewer.tripDataSource.getActiveDrivingSessionsIds().forEach { sessionIds ->

            CarStatsViewer.tripDataSource.getDrivingSession(sessionIds)?.let { session ->
                CarStatsViewer.tripDataSource.updateDrivingSession(session.copy(
                    drive_time = timerMap[session.session_type]?.getTime()?:0L,
                    driven_distance = session.driven_distance + distanceDelta,
                    used_energy = session.used_energy + energyDelta
                ))
            }

            CarStatsViewer.tripDataSource.getDrivingSession(sessionIds)?.let { session ->
                if (drivingTripData.selectedTripType == session.session_type) {
                    drivingTripData = drivingTripData.copy(
                        driveTime = session.drive_time,
                        drivenDistance = session.driven_distance,
                        usedEnergy = session.used_energy
                    )
                }
            }
        }
    }

    suspend fun newChargingDeltas(energyDelta: Double) {
        val currentChargingEnergy = chargingTripData.chargedEnergy + energyDelta

        chargingTripData = chargingTripData.copy(
            chargedEnergy = currentChargingEnergy
        )
    }

    fun updateTime() {
        CoroutineScope(Dispatchers.IO).launch {
            CarStatsViewer.tripDataSource.getActiveDrivingSessionsIds().forEach { sessionIds ->

                CarStatsViewer.tripDataSource.getDrivingSession(sessionIds)?.let { session ->
                    CarStatsViewer.tripDataSource.updateDrivingSession(session.copy(
                        drive_time = timerMap[session.session_type]?.getTime()?:0L
                    ))
                }

                CarStatsViewer.tripDataSource.getDrivingSession(sessionIds)?.let { session ->
                    if (drivingTripData.selectedTripType == session.session_type) {
                        drivingTripData = drivingTripData.copy(
                            driveTime = session.drive_time
                        )
                    }
                }
            }
        }
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
            changeSelectedTrip(CarStatsViewer.appPreferences.mainViewTrip + 1)

            val drivingSessionsIds = CarStatsViewer.tripDataSource.getActiveDrivingSessionsIds()
            drivingSessionsIds.forEach {
                val session = CarStatsViewer.tripDataSource.getDrivingSession(it)
                timerMap[session?.session_type]?.restore(session?.drive_time?:0)
            }
        }
    }

    fun updateUsedStateOfCharge(usedStateOfChargeDelta: Double) {
        CoroutineScope(Dispatchers.IO).launch {
            CarStatsViewer.tripDataSource.getActiveDrivingSessionsIds().forEach { sessionIds ->

                CarStatsViewer.tripDataSource.getDrivingSession(sessionIds)?.let { session ->
                    CarStatsViewer.tripDataSource.updateDrivingSession(session.copy(
                        used_soc = session.used_soc + usedStateOfChargeDelta,
                        used_soc_energy = session.used_energy
                    ))
                }

                CarStatsViewer.tripDataSource.getDrivingSession(sessionIds)?.let { session ->
                    if (drivingTripData.selectedTripType == session.session_type) {
                        drivingTripData = drivingTripData.copy(
                            usedStateOfCharge = session.used_soc,
                            usedStateOfChargeEnergy = session.used_soc_energy
                        )

                        val usedEnergyPerSoC = drivingTripData.usedStateOfChargeEnergy / drivingTripData.usedStateOfCharge / 100
                        val currentStateOfCharge = (CarStatsViewer.appContext as CarStatsViewer).dataProcessor.realTimeData.stateOfCharge * 100
                        val remainingEnergy = usedEnergyPerSoC * currentStateOfCharge
                        val avgConsumption = drivingTripData.usedEnergy / drivingTripData.drivenDistance * 1000
                        val remainingRange = remainingEnergy / avgConsumption
                        InAppLogger.i("[NEO] $usedEnergyPerSoC Wh/%, $currentStateOfCharge %, $remainingEnergy Wh, ${avgConsumption} Wh/km Remaining range: $remainingRange")
                    }
                }
            }
        }
    }
}