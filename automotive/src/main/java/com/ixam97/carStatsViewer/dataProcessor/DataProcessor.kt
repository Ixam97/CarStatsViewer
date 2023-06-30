package com.ixam97.carStatsViewer.dataProcessor

import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.Defines
import com.ixam97.carStatsViewer.carPropertiesClient.CarProperties
import com.ixam97.carStatsViewer.carPropertiesClient.CarPropertiesData
import com.ixam97.carStatsViewer.dataCollector.DrivingState
import com.ixam97.carStatsViewer.dataCollector.IgnitionState
import com.ixam97.carStatsViewer.utils.TimeTracker
import com.ixam97.carStatsViewer.database.tripData.*
import com.ixam97.carStatsViewer.emulatorMode
import com.ixam97.carStatsViewer.emulatorPowerSign
import com.ixam97.carStatsViewer.ui.plot.enums.PlotLineMarkerType
import com.ixam97.carStatsViewer.utils.InAppLogger
import com.ixam97.carStatsViewer.utils.Ticker
import com.ixam97.carStatsViewer.utils.TimestampSynchronizer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import java.util.*
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

class DataProcessor {
    val carPropertiesData = CarPropertiesData()

    val chargingPointInterval = 5_000

    // private var usedEnergySum = 0.0
    private var previousDrivingState: Int = DrivingState.UNKNOWN
    private var previousIgnitionState: Int = IgnitionState.UNDEFINED
    private var previousStateOfCharge: Float = -1f

    private var pointDrivenDistance: Double = 0.0
    private var pointUsedEnergy: Double = 0.0
    private var valueDrivenDistance: Double = 0.0
    private var valueUsedEnergy: Double = 0.0

    private val timestampSynchronizer = TimestampSynchronizer()

    private var lastChargingPointTime: Long = -1L

    /**
     * List of local copies of the current trips. Used for storing sum values and saving them to
     * disk less frequently. This should prevent hiccups when adding sums of distance and energy.
     */
    private var localSessions: MutableList<DrivingSession> = mutableListOf()

    private var localChargingSession: ChargingSession? = null

    /**
     * Due to the usage of coroutines for data bank access, these booleans make sure no duplicates
     * of deltas and points are created when new values come in at a fast rate and the deltas have
     * not been reset yet. This is just for safety since it should be sufficient if the point and
     * value vars above are copied and reset at the beginning of the corresponding functions. This
     * also ensures no changing data while it is used fot data base writes.
     *
     * These have been superseded by other means of ensuring correct execution order, preventing
     * database blocks and reducing writes.
     */
    // private var drivingPointUpdating: Boolean = false
    // private var chargingPointUpdating: Boolean = false
    // private var tripDataUpdating: Boolean = false

    var staticVehicleData = StaticVehicleData()

    var realTimeData = RealTimeData()
        private set(value) {
            field = value
            _realTimeDataFlow.value = value
        }

    // private var chargingTripData = ChargingTripData()
    //     set(value) {
    //         field = value
    //         _chargingTripDataFlow.value = field
    //     }


    private val _realTimeDataFlow = MutableStateFlow(realTimeData)
    val realTimeDataFlow = _realTimeDataFlow.asStateFlow()

    private val _selectedSessionDataFlow = MutableStateFlow<DrivingSession?>(null)
    val selectedSessionDataFlow = _selectedSessionDataFlow.asStateFlow()

    // private val _chargingTripDataFlow = MutableStateFlow(chargingTripData)
    // val chargingTripDataFlow = _chargingTripDataFlow.asStateFlow()

    private val _currentChargingSessionDataFlow = MutableStateFlow<ChargingSession?>(null)
    val currentChargingSessionDataFlow = _currentChargingSessionDataFlow.asStateFlow()

    var chargeTicker: Job? = null

    private val timerMap = mapOf(
        TripType.MANUAL to TimeTracker(),
        TripType.SINCE_CHARGE to TimeTracker(),
        TripType.AUTO to TimeTracker(),
        TripType.MONTH to TimeTracker(),
    )

    private val chargeTimer = TimeTracker()

    init {
        loadSessionsToMemory()
    }

    fun loadSessionsToMemory(): Job {
        /**
         * This job is returned by the function to ensure the database read is completed. Otherwise
         * a ConcurrentModificationException can occur when resetting a trip. This is caused by
         * writing and reading sessions for the local session copies at the same time. When run in a
         * Coroutine this job should always be joined to ensure data integrity. See
         * updateDrivingDataPoint as well!
         */
        return CoroutineScope(Dispatchers.IO).launch {
            localSessions.clear()
            CarStatsViewer.tripDataSource.getActiveDrivingSessionsIds().forEach { sessionId ->
                CarStatsViewer.tripDataSource.getFullDrivingSession(sessionId).let { session ->
                    localSessions.add(session)
                    if (session.session_type == CarStatsViewer.appPreferences.mainViewTrip + 1) {
                        _selectedSessionDataFlow.value = session
                    }
                }
            }
        }
    }

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

        //realTimeData = realTimeData.copy(
        //    speed = ((carPropertiesData.CurrentSpeed.value as Float?)?: 0f).absoluteValue,
        //    power = emulatorPowerSign * ((carPropertiesData.CurrentPower.value as Float?)?: 0f),
        //    batteryLevel = (carPropertiesData.BatteryLevel.value as Float?)?: 0f,
        //    stateOfCharge = ((carPropertiesData.BatteryLevel.value as Float?)?: 0f) / staticVehicleData.batteryCapacity!!,
        //    ambientTemperature = (carPropertiesData.CurrentAmbientTemperature.value as Float?)?: 0f,
        //    selectedGear = (carPropertiesData.CurrentGear.value as Int?)?: 0,
        //    ignitionState = (carPropertiesData.CurrentIgnitionState.value as Int?)?: 0,
        //    chargePortConnected = (carPropertiesData.ChargePortConnected.value as Boolean?)?: false
        //)

        realTimeData = realTimeData.copy(
            speed = if (carPropertiesData.CurrentSpeed.value == null) null else ((carPropertiesData.CurrentSpeed.value as Float?)?: 0f).absoluteValue,
            power = if (carPropertiesData.CurrentPower.value == null) null else emulatorPowerSign * ((carPropertiesData.CurrentPower.value as Float?)?: 0f),
            batteryLevel = if (carPropertiesData.BatteryLevel.value == null) null else (carPropertiesData.BatteryLevel.value as Float?)?: 0f,
            stateOfCharge = if (carPropertiesData.BatteryLevel.value == null) null else ((carPropertiesData.BatteryLevel.value as Float?)?: 0f) / staticVehicleData.batteryCapacity!!,
            ambientTemperature = if (carPropertiesData.CurrentAmbientTemperature.value == null) null else (carPropertiesData.CurrentAmbientTemperature.value as Float?)?: 0f,
            selectedGear = if (carPropertiesData.CurrentGear.value == null) null else (carPropertiesData.CurrentGear.value as Int?)?: 0,
            ignitionState = if (carPropertiesData.CurrentIgnitionState.value == null) null else (carPropertiesData.CurrentIgnitionState.value as Int?)?: 0,
            chargePortConnected = if (carPropertiesData.ChargePortConnected.value == null) null else (carPropertiesData.ChargePortConnected.value as Boolean?)?: false
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
        if (carPropertiesData.CurrentSpeed.isInitialValue) {
            InAppLogger.w("[NEO] Dropped speed value, flagged as initial")
            return
        }
        if (timestampSynchronizer.isSynced()){
            if (timestampSynchronizer.getSystemTimeFromNanosTimestamp(carPropertiesData.CurrentSpeed.timestamp) < System.currentTimeMillis() - 500) {
                InAppLogger.w("[NEO] Dropped power value, timestamp too old. Time delta: ${timestampSynchronizer.getSystemTimeFromNanosTimestamp(carPropertiesData.CurrentSpeed.timestamp) - System.currentTimeMillis()}")
                return
            }
        }
        if (carPropertiesData.CurrentSpeed.timeDelta > 0 && (realTimeData.drivingState == DrivingState.DRIVE || (realTimeData.drivingState == DrivingState.CHARGE && emulatorMode))) {
            if (!timestampSynchronizer.isSynced()) timestampSynchronizer.sync(System.currentTimeMillis(), carPropertiesData.CurrentSpeed.timestamp)
            val distanceDelta = (carPropertiesData.CurrentSpeed.value as Float).absoluteValue * (carPropertiesData.CurrentSpeed.timeDelta / 1_000_000_000f)
            pointDrivenDistance += distanceDelta
            valueDrivenDistance += distanceDelta

            if (pointDrivenDistance >= Defines.PLOT_DISTANCE_INTERVAL)
                updateDrivingDataPoint(timestamp = timestampSynchronizer.getSystemTimeFromNanosTimestamp(carPropertiesData.CurrentSpeed.timestamp))
            // Put this else here to make sure only one of these functions is executed
            // else if (valueDrivenDistance >= Defines.PLOT_DISTANCE_INTERVAL / 2)
            //    updateTripDataValues(DrivingState.DRIVE)

            /** only relevant in emulator since power is not updated periodically */
            if (emulatorMode) {
                val energyDelta = emulatorPowerSign * (carPropertiesData.CurrentPower.value as Float) / 1_000f * (carPropertiesData.CurrentSpeed.timeDelta / 3.6E12)
                pointUsedEnergy += energyDelta
                valueUsedEnergy += energyDelta

                if (valueUsedEnergy >= 100)
                    updateTripDataValues(DrivingState.DRIVE)
            }
        }
    }

    /** Actions related to changes in power draw */
    private fun powerUpdate() {
        if (emulatorMode) return /** skip if run in emulator, see speedUpdate() */
        if (carPropertiesData.CurrentPower.isInitialValue) {
            InAppLogger.w("[NEO] Dropped power value, flagged as initial")
            return
        }
        if (timestampSynchronizer.isSynced()){
            if (timestampSynchronizer.getSystemTimeFromNanosTimestamp(carPropertiesData.CurrentPower.timestamp) < System.currentTimeMillis() - 500) {
                InAppLogger.w("[NEO] Dropped power value, timestamp too old")
                return
            }
        }

        if (carPropertiesData.CurrentPower.timeDelta > 0 && (realTimeData.drivingState == DrivingState.DRIVE || realTimeData.drivingState == DrivingState.CHARGE)) {
            val energyDelta = emulatorPowerSign * (carPropertiesData.CurrentPower.value as Float) / 1_000f * (carPropertiesData.CurrentPower.timeDelta / 3.6E12)
            pointUsedEnergy += energyDelta
            valueUsedEnergy += energyDelta

            // if (realTimeData.drivingState == DrivingState.CHARGE) {
            //     if (pointUsedEnergy.absoluteValue > Defines.PLOT_ENERGY_INTERVAL)
            //         updateChargingDataPoint()
            //     if (valueUsedEnergy.absoluteValue >= 100)
            //         updateTripDataValues(DrivingState.CHARGE)
            // }

            if (valueUsedEnergy.absoluteValue >= 100 && realTimeData.drivingState == DrivingState.DRIVE)
                updateTripDataValues(DrivingState.DRIVE)
        }
    }

    /** Actions related to changes in the state of charge/battery level */
    private fun stateOfChargeUpdate() {
        staticVehicleData.batteryCapacity?.let {
            realTimeData.stateOfCharge?.let {
                val currentStateOfCharge = it
                if (previousStateOfCharge < 0) {
                    previousStateOfCharge = currentStateOfCharge
                    return
                }
                if (currentStateOfCharge != previousStateOfCharge) {
                    // if (realTimeData.drivingState == DrivingState.DRIVE)
                    //    updateUsedStateOfCharge((previousStateOfCharge - currentStateOfCharge).toDouble())
                    previousStateOfCharge = currentStateOfCharge
                }
            }
        }
    }

    /** Actions related to changes in the driving state */
    private fun stateUpdate() {
        val drivingState = realTimeData.drivingState
        val prevState = previousDrivingState
        val ignitionState = realTimeData.ignitionState?:IgnitionState.UNDEFINED
        val prevIgnition = previousIgnitionState

        previousDrivingState = drivingState
        previousIgnitionState = ignitionState

        if (ignitionState != prevIgnition) {
            InAppLogger.i("[NEO] Ignition switched to ${IgnitionState.nameMap[ignitionState]}")
        }

        if (drivingState != prevState) {
            CoroutineScope(Dispatchers.IO).launch {
                InAppLogger.i("[NEO] Drive state changed from ${DrivingState.nameMap[prevState]} to ${DrivingState.nameMap[drivingState]}")

                /** Reset Trips before inserting new data points */
                newDrivingState(drivingState, prevState)
                /**
                 * Begin or end sessions depending on driving state. Ensures exact values saved
                 * in data points and trip sums.
                 */
                if (drivingState == DrivingState.DRIVE) {
                    updateDrivingDataPoint(PlotLineMarkerType.BEGIN_SESSION.int).join()
                }
                if (drivingState != DrivingState.DRIVE && prevState == DrivingState.DRIVE)
                    updateDrivingDataPoint(PlotLineMarkerType.END_SESSION.int).join()
                if (drivingState == DrivingState.CHARGE) {
                    startChargingSession().join()
                }
                if (drivingState != DrivingState.CHARGE && prevState == DrivingState.CHARGE) {
                    stopChargingSession().join()
                }
                if (prevState == DrivingState.UNKNOWN && drivingState != DrivingState.CHARGE) {
                    // Check for stray charging session(s)
                    checkChargingSessions().join()
                }
                previousStateOfCharge = realTimeData.stateOfCharge?:0f
            }
        }
    }

    /** Get the current running time for each trip type and charging session from the timers */
    /*
    fun updateTime() {
        // Update trip data regularly to keep data up to date. Does not write to database.
        updateTripDataValues(DrivingState.DRIVE)

        localSessions.forEachIndexed { index, session ->
            val drivingPoints = session.drivingPoints
            localSessions[index] = session.copy(
                drive_time = timerMap[session.session_type]?.getTime()?:0L,
                last_edited_epoch_time = System.currentTimeMillis()
            )
            localSessions[index].drivingPoints = drivingPoints
            if (session.session_type == CarStatsViewer.appPreferences.mainViewTrip + 1) {
                _selectedSessionDataFlow.value = localSessions[index]
            }
        }
    }
     */

    /** Make sure every type of trip has an active trip */
    suspend fun checkTrips() {
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

        loadSessionsToMemory().join()

        changeSelectedTrip(CarStatsViewer.appPreferences.mainViewTrip + 1)

        val drivingSessionsIds = CarStatsViewer.tripDataSource.getActiveDrivingSessionsIds()
        drivingSessionsIds.forEach {
            val session = CarStatsViewer.tripDataSource.getDrivingSession(it)
            timerMap[session?.session_type]?.restore(session?.drive_time?:0)
        }
    }

    fun checkChargingSessions(): Job {
        return CoroutineScope(Dispatchers.IO).launch {
            val activeChargingSessions = CarStatsViewer.tripDataSource.getActiveChargingSessionIds()
            if (activeChargingSessions.isNotEmpty()) {
                InAppLogger.w("[NEO] Found ${activeChargingSessions.size} stray charging sessions")
                activeChargingSessions.forEach { sessionId ->
                    val timestamp = CarStatsViewer.tripDataSource.getLatestChargingPoint()?.charging_point_epoch_time?: System.currentTimeMillis()
                    CarStatsViewer.tripDataSource.endChargingSession(timestamp, sessionId)
                    InAppLogger.w("[NEO] Charging session ID $sessionId was ended")
                }
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
        /** Reset "since charge" after plugging in" */
        // Moved to charging session Start
        // if (drivingState == DrivingState.CHARGE && oldDrivingState != DrivingState.CHARGE) {
        //     resetTrip(TripType.SINCE_CHARGE, drivingState)
        // }
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
    private fun updateDrivingDataPoint(markerType: Int? = null, timestamp: Long? = null): Job {
        val mUsedEnergy = pointUsedEnergy
        pointUsedEnergy = 0.0
        val mDrivenDistance = pointDrivenDistance
        pointDrivenDistance = 0.0

        updateTripDataValues(DrivingState.DRIVE) // let's put this outside of the coroutine in the hope of fixing this stupid difference between point and trip values...

        /**
         * This job is returned by the function to ensure the database write is completed. Otherwise
         * a ConcurrentModificationException can occur when resetting a trip. This is caused by
         * writing and reading sessions for the local session copies at the same time. When run in a
         * Coroutine this job should always be joined to ensure data integrity. See
         * loadSessionsToMemory as well!
         */
        return CoroutineScope(Dispatchers.IO).launch { // Run database write in own coroutine to not Block fast Updates

            // if (mUsedEnergy.absoluteValue < .1 && mDrivenDistance.absoluteValue > 1) {
            //     InAppLogger.w("[NEO] Driving point not written, implausible values: ${mDrivenDistance.toFloat()} m, ${mUsedEnergy.toFloat()} Wh")
            //     return@launch
            // }
            val drivingPoint = DrivingPoint(
                driving_point_epoch_time = timestamp?: System.currentTimeMillis(),
                energy_delta = mUsedEnergy.toFloat(),
                distance_delta = mDrivenDistance.toFloat(),
                point_marker_type = markerType,
                state_of_charge = realTimeData.stateOfCharge?:0f,
                lat = realTimeData.lat,
                lon = realTimeData.lon,
                alt = realTimeData.alt
            )

            CarStatsViewer.tripDataSource.addDrivingPoint(drivingPoint)
            localSessions.forEachIndexed { index, session ->
                val drivingPoints = session.drivingPoints?.toMutableList()
                drivingPoints?.add(drivingPoint)
                localSessions[index] = session.copy(last_edited_epoch_time = System.currentTimeMillis())
                localSessions[index].drivingPoints = drivingPoints
                if (session.session_type == CarStatsViewer.appPreferences.mainViewTrip + 1) {
                    _selectedSessionDataFlow.value = localSessions[index]
                }
            }

            writeTripsToDatabase()
            InAppLogger.d("[NEO] Driving point written: ${mDrivenDistance.toFloat()} m, ${mUsedEnergy.toFloat()} Wh")

        }
    }

    /** Update sums of a trip or charging session */
    private fun updateTripDataValues(drivingState: Int = realTimeData.drivingState) {
        val mDrivenDistance = valueDrivenDistance
        valueDrivenDistance = 0.0
        val mUsedEnergy = valueUsedEnergy
        valueUsedEnergy = 0.0

        when (drivingState) {
            DrivingState.DRIVE -> newDrivingDeltas(mDrivenDistance, mUsedEnergy)
            DrivingState.CHARGE -> newChargingDeltas(mUsedEnergy)
        }
    }

    fun updateTripDataValuesByTick() {
        updateTripDataValues()
    }

    /**
     * Update all active trips in the database with new sums for energy and distance.
     * Update driving trip data flow for UI.
     */
    private fun newDrivingDeltas(distanceDelta: Double, energyDelta: Double) {
        localSessions.forEachIndexed {index, localSession ->
            val drivingPoints = localSession.drivingPoints
            localSessions[index] = localSession.copy(
                drive_time = timerMap[localSession.session_type]?.getTime()?:0L,
                driven_distance = localSession.driven_distance + distanceDelta,
                used_energy = localSession.used_energy + energyDelta,
                last_edited_epoch_time = System.currentTimeMillis()
            )
            localSessions[index].drivingPoints = drivingPoints
            if (localSession.session_type == CarStatsViewer.appPreferences.mainViewTrip + 1) {
                _selectedSessionDataFlow.value = localSessions[index]
            }
        }
    }

    private suspend fun writeTripsToDatabase() {
        localSessions.forEach { localSession ->
            CarStatsViewer.tripDataSource.updateDrivingSession(localSession)
        }
    }

    /**
     * Update the active charging session in the database with new sums for energy and SoC.
     * Update charging trip data flow for UI.
     */
    private fun newChargingDeltas(energyDelta: Double) {
        /*
        val currentChargingEnergy = chargingTripData.chargedEnergy - energyDelta

        chargingTripData = chargingTripData.copy(
            chargedEnergy = currentChargingEnergy,
            chargeTime = chargeTimer.getTime()
        )

         */

        localChargingSession?.let {
            val chargingPoints = it.chargingPoints
            localChargingSession = it.copy(
                charged_energy = it.charged_energy - energyDelta
            )
            localChargingSession?.chargeTime = chargeTimer.getTime()
            localChargingSession?.chargingPoints = chargingPoints
        }

        _currentChargingSessionDataFlow.value = localChargingSession


    }

    /** Update data points when charging */
    private fun updateChargingDataPoint(markerType: Int? = null): Job {
        val mUsedEnergy = pointUsedEnergy
        pointUsedEnergy = 0.0

        updateTripDataValues(DrivingState.CHARGE)

        return CoroutineScope(Dispatchers.IO).launch {
            if (realTimeData.drivingState != DrivingState.CHARGE || localChargingSession?.charging_session_id == null) {
                InAppLogger.w("[NEO] No charging session loaded yet!")
            } else {
                val currentTime = System.currentTimeMillis()
                val chargingPoint = ChargingPoint(
                    currentTime,
                    localChargingSession?.charging_session_id!!,
                    mUsedEnergy.toFloat(),
                    realTimeData.power?:0f,
                    realTimeData.stateOfCharge?:0f,
                    point_marker_type = if (currentTime > lastChargingPointTime + (chargingPointInterval * 1.1) && markerType == null) {
                        PlotLineMarkerType.BEGIN_SESSION.int
                    } else {
                        markerType
                    }
                )

                CarStatsViewer.tripDataSource.addChargingPoint(chargingPoint)
                lastChargingPointTime = currentTime
                localChargingSession?.let {
                    val chargingPoints = it.chargingPoints?.toMutableList()?: mutableListOf()
                    chargingPoints.add(chargingPoint)
                    localChargingSession?.chargeTime = chargeTimer.getTime()
                    localChargingSession?.chargingPoints = chargingPoints
                    _currentChargingSessionDataFlow.value = localChargingSession
                    CarStatsViewer.tripDataSource.updateChargingSession(it)
                    InAppLogger.d("[NEO] Charging point written: ${chargingPoint.power.toFloat()} kW, ${(chargingPoint.state_of_charge * 100).roundToInt()} %, ${chargingPoint.charging_point_epoch_time}")
                }
            }
        }
    }

    /** Change the selected trip type to update the trip data flow with */
    fun changeSelectedTrip(tripType: Int) {
        if (localSessions.isNotEmpty())
            _selectedSessionDataFlow.value = localSessions[localSessions.indexOfFirst { it.session_type == tripType }]
    }

    suspend fun resetTrip(tripType: Int, drivingState: Int) {
        /** Create data point right before reset, but only while driving */
        if (drivingState == DrivingState.DRIVE) updateDrivingDataPoint().join()

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
        timerMap[tripType]?.reset()
        loadSessionsToMemory().join()
        if (drivingState == DrivingState.DRIVE) timerMap[tripType]?.start()
    }

    private fun startChargingSession(): Job {
        chargeTimer.reset()
        return CoroutineScope(Dispatchers.IO).launch {
            val activeChargingSessionIds = CarStatsViewer.tripDataSource.getActiveChargingSessionIds()
            val chargingSessionId = if (activeChargingSessionIds.isNotEmpty()) {
                activeChargingSessionIds.forEachIndexed { index, sessionId ->
                    // Make sure only one charging session is active at all times
                    if (index < activeChargingSessionIds.size - 1) {
                        val chargingSession = CarStatsViewer.tripDataSource.getChargingSessionById(sessionId)
                        CarStatsViewer.tripDataSource.updateChargingSession(
                            chargingSession.copy(end_epoch_time = System.currentTimeMillis())
                        )
                    }
                }
                val id = activeChargingSessionIds.last()
                InAppLogger.i("[NEO] Resuming charging session with ID $id")
                id
            } else {
                // reset since charge before starting charging session to prevent unintended deletion
                resetTrip(TripType.SINCE_CHARGE, DrivingState.CHARGE)
                val id = CarStatsViewer.tripDataSource.startChargingSession(
                    System.currentTimeMillis(),
                    realTimeData.ambientTemperature?: 0f,
                    realTimeData.lat,
                    realTimeData.lon
                )
                InAppLogger.i("[NEO] Charging session started with ID $id")
                id
            }

            localChargingSession = CarStatsViewer.tripDataSource.getCompleteChargingSessionById(chargingSessionId)
            InAppLogger.i("[NEO] Loaded charging session ID ${localChargingSession?.charging_session_id}")

            // chargingTripData = chargingTripData.copy(
            //     chargedEnergy = localChargingSession?.charged_energy?: 0.0,
            //     chargeTime = 0 ,
            //     chargingSessionId = chargingSessionId
            // )

            localChargingSession?.let {
                chargeTimer.restore(System.currentTimeMillis() - it.start_epoch_time)
                _currentChargingSessionDataFlow.value = localChargingSession
            }
            updateChargingDataPoint(PlotLineMarkerType.BEGIN_SESSION.int).join()
            chargeTimer.start()
            startChargeTicker()
        }
    }

    private fun stopChargingSession(): Job {
        chargeTimer.stop()
        stopChargeTicker()
        return CoroutineScope(Dispatchers.IO).launch {
            updateChargingDataPoint(PlotLineMarkerType.END_SESSION.int).join()
            CarStatsViewer.tripDataSource.endChargingSession(
                System.currentTimeMillis(),
                localChargingSession?.charging_session_id
            )
            InAppLogger.i("[NEO] Charging session with ID ${localChargingSession?.charging_session_id} ended")
        }
    }

    private fun stopChargeTicker() {
        chargeTicker?.cancel()
    }

    private fun startChargeTicker() {
        chargeTicker = CoroutineScope(Dispatchers.Default).launch {
            Ticker.tickerFlow(5000).collectLatest {
                updateChargingDataPoint()
            }
        }
    }

    /** Weird stuff for range estimate, not quite working as intended yet. */
    /*
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
                    if (session.session_type == CarStatsViewer.appPreferences.mainViewTrip + 1) {
                        _selectedSessionDataFlow.value = session


                        val usedEnergyPerSoC = session.used_soc_energy / session.used_soc / 100
                        val currentStateOfCharge = CarStatsViewer.dataProcessor.realTimeData.stateOfCharge * 100
                        val remainingEnergy = usedEnergyPerSoC * currentStateOfCharge
                        val avgConsumption = session.used_energy / session.driven_distance * 1000
                        val remainingRange = remainingEnergy / avgConsumption
                        InAppLogger.i("[NEO] $usedEnergyPerSoC Wh/%, $currentStateOfCharge %, $remainingEnergy Wh, ${avgConsumption} Wh/km Remaining range: $remainingRange")
                    }
                }
            }
        }
    }
    */
}