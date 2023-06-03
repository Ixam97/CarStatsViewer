package com.ixam97.carStatsViewer.dataProcessor

import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.Defines
import com.ixam97.carStatsViewer.carPropertiesClient.CarProperties
import com.ixam97.carStatsViewer.carPropertiesClient.CarPropertiesData
import com.ixam97.carStatsViewer.dataManager.DrivingState
import com.ixam97.carStatsViewer.dataManager.TimeTracker
import com.ixam97.carStatsViewer.database.tripData.DrivingPoint
import com.ixam97.carStatsViewer.database.tripData.TripType
import com.ixam97.carStatsViewer.emulatorMode
import com.ixam97.carStatsViewer.emulatorPowerSign
import com.ixam97.carStatsViewer.plot.enums.PlotLineMarkerType
import com.ixam97.carStatsViewer.utils.InAppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.absoluteValue

class DataProcessor {
    val carPropertiesData = CarPropertiesData()

    private var usedEnergySum = 0.0
    private var previousDrivingState: Int = DrivingState.UNKNOWN
    private var previousStateOfCharge: Float = -1f

    private var pointDrivenDistance: Double = 0.0
    private var pointUsedEnergy: Double = 0.0
    private var valueDrivenDistance: Double = 0.0
    private var valueUsedEnergy: Double = 0.0

    /**
     * Due to the usage of coroutines for data bank access, these booleans make sure no duplicates
     * of deltas and points are created when new values come in at a fast rate and the deltas have
     * not been reset yet. This is just for safety since it should be sufficient if the point and
     * value vars above are copied and reset at the beginning of the corresponding functions. This
     * also ensures no changing data while it is used fot data base writes.
     */
    private var drivingPointUpdating: Boolean = false
    private var chargingPointUpdating: Boolean = false
    private var tripDataUpdating: Boolean = false

    var staticVehicleData = StaticVehicleData()

    var realTimeData = RealTimeData()
        private set(value) {
            field = value
            _realTimeDataFlow.value = value
        }

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


    private val _realTimeDataFlow = MutableStateFlow(realTimeData)
    val realTimeDataFlow = _realTimeDataFlow.asStateFlow()

    private val _drivingTripDataFlow = MutableStateFlow(drivingTripData)
    val drivingTripDataFlow = _drivingTripDataFlow.asStateFlow()

    private val _chargingTripDataFlow = MutableStateFlow(chargingTripData)
    val chargingTripDataFlow = _chargingTripDataFlow.asStateFlow()

    val timerMap = mapOf(
        TripType.MANUAL to TimeTracker(),
        TripType.SINCE_CHARGE to TimeTracker(),
        TripType.AUTO to TimeTracker(),
        TripType.MONTH to TimeTracker(),
    )

    fun processLocation(lat: Double?, lon: Double?, alt: Double?) {
        realTimeData = realTimeData.copy(
            lat = lat?.toFloat(),
            lon = lon?.toFloat(),
            alt = alt?.toFloat()
        )
    }

    /** To be passed to the CarPropertiesClient. Processes data the CarPropertiesClient writes to
     *  carPropertiesData.
     */
    fun processProperty(carProperty: Int) {

        realTimeData = realTimeData.copy(
            speed = ((carPropertiesData.CurrentSpeed.value as Float?)?: 0f).absoluteValue,
            power = emulatorPowerSign * ((carPropertiesData.CurrentPower.value as Float?)?: 0f),
            batteryLevel = (carPropertiesData.BatteryLevel.value as Float?)?: 0f,
            stateOfCharge = ((carPropertiesData.BatteryLevel.value as Float?)?: 0f) / staticVehicleData.batteryCapacity!!,
            ambientTemperature = (carPropertiesData.CurrentAmbientTemperature.value as Float?)?: 0f,
            selectedGear = (carPropertiesData.CurrentGear.value as Int?)?: 0,
            ignitionState = (carPropertiesData.CurrentIgnitionState.value as Int?)?: 0,
            chargePortConnected = (carPropertiesData.ChargePortConnected.value as Boolean?)?: false
        )

        when (carProperty) {
            CarProperties.PERF_VEHICLE_SPEED -> speedUpdate()
            CarProperties.EV_BATTERY_INSTANTANEOUS_CHARGE_RATE -> powerUpdate()
            CarProperties.IGNITION_STATE, CarProperties.EV_CHARGE_PORT_CONNECTED -> stateUpdate()
            CarProperties.EV_BATTERY_LEVEL -> stateOfChargeUpdate()
        }
    }

    /** Actions related to changes in speed */
    private fun speedUpdate() {
        if (carPropertiesData.CurrentSpeed.timeDelta > 0 && realTimeData.drivingState == DrivingState.DRIVE) {
            val distanceDelta = (carPropertiesData.CurrentSpeed.value as Float).absoluteValue * (carPropertiesData.CurrentSpeed.timeDelta / 1_000_000_000f)
            pointDrivenDistance += distanceDelta
            valueDrivenDistance += distanceDelta

            CoroutineScope(Dispatchers.IO).launch {
                if (pointDrivenDistance >= Defines.PLOT_DISTANCE_INTERVAL)
                    updateDrivingDataPoint()

                if (valueDrivenDistance > 10)
                    updateTripDataValues(DrivingState.DRIVE)

                /** only relevant in emulator since power is not updated periodically */
                if (emulatorMode) {
                    val energyDelta = emulatorPowerSign * (carPropertiesData.CurrentPower.value as Float) / 1_000f * (carPropertiesData.CurrentSpeed.timeDelta / 3.6E12)
                    pointUsedEnergy += energyDelta
                    valueUsedEnergy += energyDelta

                    if (valueUsedEnergy > 100)
                        updateTripDataValues(DrivingState.DRIVE)
                }
            }
        }
    }

    /** Actions related to changes in power draw */
    private fun powerUpdate() {
        if (emulatorMode) return /** skip if run in emulator, see speedUpdate() */

        if (carPropertiesData.CurrentPower.timeDelta > 0 && (realTimeData.drivingState == DrivingState.DRIVE || realTimeData.drivingState == DrivingState.CHARGE)) {
            val energyDelta = emulatorPowerSign * (carPropertiesData.CurrentPower.value as Float) / 1_000f * (carPropertiesData.CurrentPower.timeDelta / 3.6E12)
            pointUsedEnergy += energyDelta
            valueUsedEnergy += energyDelta

            CoroutineScope(Dispatchers.IO).launch {
                if (realTimeData.drivingState == DrivingState.CHARGE) {
                    if (pointUsedEnergy.absoluteValue > Defines.PLOT_ENERGY_INTERVAL)
                        updateChargingDataPoint()
                    if (valueUsedEnergy.absoluteValue > 100)
                        updateTripDataValues(DrivingState.CHARGE)
                }

                if (valueUsedEnergy.absoluteValue > 100 && realTimeData.drivingState == DrivingState.DRIVE)
                    updateTripDataValues(DrivingState.DRIVE)
            }
        }
    }

    /** Actions related to changes in the state of charge/battery level */
    private fun stateOfChargeUpdate() {
        staticVehicleData.batteryCapacity?.let { batteryCapacity ->
            val currentStateOfCharge = realTimeData.stateOfCharge
            if (previousStateOfCharge < 0) {
                previousStateOfCharge = currentStateOfCharge
                return
            }
            if (currentStateOfCharge != previousStateOfCharge) {
                if (realTimeData.drivingState == DrivingState.DRIVE)
                    updateUsedStateOfCharge((previousStateOfCharge - currentStateOfCharge).toDouble())
                previousStateOfCharge = currentStateOfCharge
            }
        }
    }

    /** Actions related to changes in the driving state */
    private fun stateUpdate() {
        val drivingState = realTimeData.drivingState
        val prevState = previousDrivingState

        previousDrivingState = drivingState

        if (drivingState != prevState) {
            CoroutineScope(Dispatchers.IO).launch {
                InAppLogger.i("[NEO] Drive state changed from ${DrivingState.nameMap[prevState]} to ${DrivingState.nameMap[drivingState]}")

                // Reset Trips before inserting new data points
                newDrivingState(drivingState, prevState)

                // Begin or end plot sessions depending on driving state. Ensures exact values
                // saved in data points and trip sums.
                if (drivingState == DrivingState.DRIVE)
                    updateDrivingDataPoint(PlotLineMarkerType.BEGIN_SESSION.int)
                if (drivingState != DrivingState.DRIVE && prevState == DrivingState.DRIVE)
                    updateDrivingDataPoint(PlotLineMarkerType.END_SESSION.int)
                if (drivingState == DrivingState.CHARGE)
                    updateChargingDataPoint(PlotLineMarkerType.BEGIN_SESSION.int)
                if (drivingState != DrivingState.CHARGE && prevState == DrivingState.CHARGE)
                    updateChargingDataPoint(PlotLineMarkerType.END_SESSION.int)
                previousStateOfCharge = realTimeData.stateOfCharge
            }
        }
    }

    /** Get the current running time for each trip type and charging session from the timers */
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

    /** Make sure every type of trip has an active trip */
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

    /**
     * The following functions have functionally that depends of a correct execution order and
     * interfaces with the database. Therefore they are suspend functions. If the execution order is
     * important they shall be executed within the same coroutine. See for example the reset of time
     * based trips and die insertion of a starting data point when the driving state changes to DRIVE.
     */

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

    /** Update data points when driving */
    private suspend fun updateDrivingDataPoint(markerType: Int? = null) {
        if (drivingPointUpdating) return
        drivingPointUpdating = true
        val mUsedEnergy = pointUsedEnergy
        pointUsedEnergy = 0.0
        val mDrivenDistance = pointDrivenDistance
        pointDrivenDistance = 0.0

        usedEnergySum += mUsedEnergy
        InAppLogger.v("[NEO] Driven distance: $mDrivenDistance, Used energy: $mUsedEnergy")

        val drivingPoint = DrivingPoint(
            driving_point_epoch_time = System.currentTimeMillis(),
            energy_delta = mUsedEnergy.toFloat(),
            distance_delta = mDrivenDistance.toFloat(),
            point_marker_type = markerType,
            state_of_charge = realTimeData.stateOfCharge,
            lat = realTimeData.lat,
            lon = realTimeData.lon,
            alt = realTimeData.alt
        )

        CarStatsViewer.tripDataSource.addDrivingPoint(drivingPoint)
        updateTripDataValues(DrivingState.DRIVE)

        drivingPointUpdating = false
    }

    /**
     * Update all active trips in the database with new sums for energy and distance.
     * Update driving trip data flow for UI.
     */
    private suspend fun newDrivingDeltas(distanceDelta: Double, energyDelta: Double) {
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

    /**
     * Update the active charging session in the database with new sums for energy and SoC.
     * Update charging trip data flow for UI.
     */
    private suspend fun newChargingDeltas(energyDelta: Double) {
        val currentChargingEnergy = chargingTripData.chargedEnergy + energyDelta

        chargingTripData = chargingTripData.copy(
            chargedEnergy = currentChargingEnergy
        )
    }

    /** Update data points when charging */
    private suspend fun updateChargingDataPoint(markerType: Int? = null) {
        if (chargingPointUpdating) return
        chargingPointUpdating = true
        val mUsedEnergy = pointUsedEnergy
        pointUsedEnergy = 0.0

        updateTripDataValues(DrivingState.CHARGE)

        chargingPointUpdating = false
    }

    /** Update sums of a trip or charging session */
    private suspend fun updateTripDataValues(drivingState: Int = realTimeData.drivingState) {
        if (tripDataUpdating) return
        tripDataUpdating = true
        val mDrivenDistance = valueDrivenDistance
        valueDrivenDistance = 0.0
        val mUsedEnergy = valueUsedEnergy
        valueUsedEnergy = 0.0

        when (drivingState) {
            DrivingState.DRIVE -> newDrivingDeltas(mDrivenDistance, mUsedEnergy)
            DrivingState.CHARGE -> newChargingDeltas(mUsedEnergy)
        }

        tripDataUpdating = false
    }

    /** Change the selected trip type to update the trip data flow with */
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

    /** Weird stuff for range estimate, not quite working as intended yet. */
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