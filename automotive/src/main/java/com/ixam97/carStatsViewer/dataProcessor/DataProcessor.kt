package com.ixam97.carStatsViewer.dataProcessor

import android.app.Notification
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.Defines
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.carPropertiesClient.CarProperties
import com.ixam97.carStatsViewer.carPropertiesClient.CarPropertiesData
import com.ixam97.carStatsViewer.database.tripData.ChargingPoint
import com.ixam97.carStatsViewer.database.tripData.ChargingSession
import com.ixam97.carStatsViewer.database.tripData.DrivingPoint
import com.ixam97.carStatsViewer.database.tripData.DrivingSession
import com.ixam97.carStatsViewer.database.tripData.TripType
import com.ixam97.carStatsViewer.emulatorMode
import com.ixam97.carStatsViewer.emulatorPowerSign
import com.ixam97.carStatsViewer.liveDataApi.http.HttpLiveData
import com.ixam97.carStatsViewer.ui.plot.enums.PlotLineMarkerType
import com.ixam97.carStatsViewer.utils.InAppLogger
import com.ixam97.carStatsViewer.utils.Ticker
import com.ixam97.carStatsViewer.utils.TimeTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

class DataProcessor {
    val carPropertiesData = CarPropertiesData()

    val chargingPointInterval = 5_000
    val chargingInterruptionThreshold = 5*60*1000

    // private var usedEnergySum = 0.0
    private var previousDrivingState: Int = DrivingState.UNKNOWN
    private var previousIgnitionState: Int = IgnitionState.UNDEFINED
    private var previousStateOfCharge: Float = -1f

    private var pointDrivenDistance: Double = 0.0
    private var pointUsedEnergy: Double = 0.0
    private var valueDrivenDistance: Double = 0.0
    private var valueUsedEnergy: Double = 0.0

    // private val timestampSynchronizer = TimestampSynchronizer()

    private var lastChargingPointTime: Long = -1L
    private var chargingTickerActive: Boolean = false

    private var localSessionsAccess: Boolean = true

    var dataInitialized: Boolean = false

    /**
     * List of local copies of the current trips. Used for storing sum values and saving them to
     * disk less frequently. This should prevent hiccups when adding sums of distance and energy.
     */
    // private var localSessions: MutableList<DrivingSession> = mutableListOf()

    private val _localSessionsState = MutableStateFlow<MutableList<DrivingSession>>(mutableListOf())
    val localSessionsState: StateFlow<MutableList<DrivingSession>> = _localSessionsState.asStateFlow()

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
            // aaosExec?.execute(aaosRunnable)
        }

    // private var chargingTripData = ChargingTripData()
    //     set(value) {
    //         field = value
    //         _chargingTripDataFlow.value = field
    //     }

    var selectedSessionData : DrivingSession? = null
        private set(value) {
            field = value
            _selectedSessionDataFlow.value = value
            // aaosExec?.execute(aaosRunnable)
        }

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

    fun loadSessionsToMemory(): Job {
        /**
         * This job is returned by the function to ensure the database read is completed. Otherwise
         * a ConcurrentModificationException can occur when resetting a trip. This is caused by
         * writing and reading sessions for the local session copies at the same time. When run in a
         * Coroutine this job should always be joined to ensure data integrity. See
         * updateDrivingDataPoint as well!
         */
        return CoroutineScope(Dispatchers.IO).launch {
            _localSessionsState.update { localSessions ->
                localSessions.clear()
                CarStatsViewer.tripDataSource.getActiveDrivingSessionsIds().forEach { sessionId ->
                    // CarStatsViewer.tripDataSource.getFullDrivingSession(sessionId).let { session ->
                    CarStatsViewer.tripDataSource.getDrivingSession(sessionId)?.let { loadedSession ->
                        val session = loadedSession.copy()
                        session.drivingPoints = CarStatsViewer.tripDataSource.getDrivingPointsSince(
                            startTime = loadedSession.start_epoch_time,
                            limit = Defines.MAIN_VIEW_DRIVING_POINTS
                        )
                        localSessions.add(session)
                        if (session.session_type == CarStatsViewer.appPreferences.mainViewTrip + 1) {
                            selectedSessionData = session
                        }
                    }
                }
                localSessions
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

        if (!staticVehicleData.isEssentialInitialized()) {
            InAppLogger.w("[NEO] Static vehicle Data not yet available!")
            return
        }

        try {
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
        } catch (e: Throwable) {
            InAppLogger.e("[NEO] Failed to update real time data!\n\r${e.stackTraceToString()}")
            return
        }


        if (!realTimeData.isEssentialInitialized() || !staticVehicleData.isEssentialInitialized()) {
            if (dataInitialized != false) {
                dataInitialized = false
                InAppLogger.i("[NEO] Waiting for car properties to be initialized...")
            }
            return
        }

        if (dataInitialized == false) {
            dataInitialized = true
            InAppLogger.i("[NEO] Essential Car Properties initialization complete.")
        }

        when (carProperty) {
            CarProperties.PERF_VEHICLE_SPEED -> speedUpdate()
            CarProperties.EV_BATTERY_INSTANTANEOUS_CHARGE_RATE -> powerUpdate()
            CarProperties.IGNITION_STATE, CarProperties.EV_CHARGE_PORT_CONNECTED, CarProperties.GEAR_SELECTION -> stateUpdate()
            CarProperties.EV_BATTERY_LEVEL -> stateOfChargeUpdate()
        }
    }

    /** Actions related to changes in speed */
    private fun speedUpdate() {
        if (carPropertiesData.CurrentSpeed.isInitialValue) {
            InAppLogger.w("[NEO] Dropped speed value, flagged as initial")
            return
        }
        //if (timestampSynchronizer.isSynced()){
        //if (timestampSynchronizer.getSystemTimeFromNanosTimestamp(carPropertiesData.CurrentSpeed.timestamp) < System.currentTimeMillis() - 500) {
        if (carPropertiesData.CurrentSpeed.timestamp < System.nanoTime() - 500_000_000) {
            InAppLogger.w("[NEO] Dropped speed value, timestamp too old. Time delta: ${(System.nanoTime() - carPropertiesData.CurrentSpeed.timestamp)/1_000_000}")
            return
        //}
        }
        if (carPropertiesData.CurrentSpeed.timeDelta > 0 && (realTimeData.drivingState == DrivingState.DRIVE || (realTimeData.drivingState == DrivingState.CHARGE && emulatorMode))) {
            // if (!timestampSynchronizer.isSynced()) timestampSynchronizer.sync(System.currentTimeMillis(), carPropertiesData.CurrentSpeed.timestamp)
            val distanceDelta = (carPropertiesData.CurrentSpeed.value as Float).absoluteValue * (carPropertiesData.CurrentSpeed.timeDelta / 1_000_000_000f)
            pointDrivenDistance += distanceDelta
            valueDrivenDistance += distanceDelta

            if (pointDrivenDistance >= Defines.PLOT_DISTANCE_INTERVAL)
                //updateDrivingDataPoint(timestamp = timestampSynchronizer.getSystemTimeFromNanosTimestamp(carPropertiesData.CurrentSpeed.timestamp))
                updateDrivingDataPoint()
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
        // if (timestampSynchronizer.isSynced()){
            //if (timestampSynchronizer.getSystemTimeFromNanosTimestamp(carPropertiesData.CurrentPower.timestamp) < System.currentTimeMillis() - 500) {
        if (carPropertiesData.CurrentPower.timestamp < System.nanoTime() - 500_000_000) {
            InAppLogger.w("[NEO] Dropped power value, timestamp too old. Time delta: ${(System.nanoTime() - carPropertiesData.CurrentPower.timestamp)/1_000_000}")
            return
        }
        // }

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
            InAppLogger.i("[NEO] Ignition switched from ${IgnitionState.nameMap[prevIgnition]} to ${IgnitionState.nameMap[ignitionState]}")
            if (prevIgnition == IgnitionState.START && ignitionState <= IgnitionState.ON && CarStatsViewer.appPreferences.phoneNotification) {

                val phoneNotification = Notification.Builder(
                    CarStatsViewer.appContext,
                    CarStatsViewer.RESTART_CHANNEL_ID
                )
                    .setContentTitle(CarStatsViewer.appContext.getString(R.string.notification_phone))
                    .setContentText(CarStatsViewer.appContext.getString(R.string.notification_valuables))
                    .setSmallIcon(R.drawable.ic_notification_phone)
                    .setOngoing(false)
                    .setCategory(Notification.CATEGORY_CALL)
                    .build()

                CarStatsViewer.notificationManager.notify(99, phoneNotification)
            }
            if (prevIgnition < IgnitionState.ON && ignitionState >= IgnitionState.ON) {
                CarStatsViewer.notificationManager.cancel(99)
            }

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
                    resetTrip(TripType.MONTH, oldDrivingState)
                if (lastDriveTime < (System.currentTimeMillis() - Defines.AUTO_RESET_TIME))
                    resetTrip(TripType.AUTO, oldDrivingState)
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
            // val sessionIterator = localSessions.iterator()
            while (!localSessionsAccess) {
                InAppLogger.w("WAITING for local session access")
            }
            _localSessionsState.update { localSessions ->
                val localSessionsCopy = localSessions.toMutableList() // Copying the list before read and write at the same time
                localSessionsCopy.forEachIndexed { index, session ->
                    val drivingPoints = session.drivingPoints?.toMutableList()?: mutableListOf()
                    drivingPoints.add(drivingPoint)
                    localSessions[index] = session.copy(last_edited_epoch_time = System.currentTimeMillis())
                    localSessions[index].drivingPoints = drivingPoints.drop((drivingPoints.size - Defines.MAIN_VIEW_DRIVING_POINTS).coerceAtLeast(0))
                    if (session.session_type == CarStatsViewer.appPreferences.mainViewTrip + 1) {
                        selectedSessionData = localSessions[index]
                    }
                }
                localSessions
            }


            writeTripsToDatabase()
            InAppLogger.d("[NEO] Driving point written: ${mDrivenDistance.toFloat()} m, ${mUsedEnergy.toFloat()} Wh")

            CoroutineScope(Dispatchers.IO).launch {
                (CarStatsViewer.liveDataApis[1] as HttpLiveData).sendWithDrivingPoint(realTimeData, listOf(drivingPoint))
            }
        }
    }

    /** Update sums of a trip or charging session */
    private fun updateTripDataValues(drivingState: Int = realTimeData.drivingState) {
        localSessionsAccess = false
        val mDrivenDistance = valueDrivenDistance
        valueDrivenDistance = 0.0
        val mUsedEnergy = valueUsedEnergy
        valueUsedEnergy = 0.0

        when (drivingState) {
            DrivingState.DRIVE -> newDrivingDeltas(mDrivenDistance, mUsedEnergy)
            DrivingState.CHARGE -> newChargingDeltas(mUsedEnergy)
        }

        localSessionsAccess = true
    }

    fun updateTripDataValuesByTick() {
        updateTripDataValues()
    }

    /**
     * Update all active trips in the database with new sums for energy and distance.
     * Update driving trip data flow for UI.
     */
    private fun newDrivingDeltas(distanceDelta: Double, energyDelta: Double) {
        _localSessionsState.update { localSessions ->
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
                    selectedSessionData = localSessions[index]
                }
            }
            localSessions
        }

    }

    private suspend fun writeTripsToDatabase() {
        try {
            while (!localSessionsAccess) {
                InAppLogger.w("WAITING for local session access")
            }
            val localSessions = localSessionsState.value.toMutableList()
            localSessions.forEach {localSession ->
                CarStatsViewer.tripDataSource.updateDrivingSession(localSession)
            }
        } catch (e: Exception) {
            InAppLogger.logThrowableWithFirebase("FATAL ERROR! Writing trips was not successful", e)
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

            if ((realTimeData.drivingState != DrivingState.CHARGE && markerType != PlotLineMarkerType.END_SESSION.int) || localChargingSession?.charging_session_id == null) {
                InAppLogger.w("[NEO] No charging session loaded yet!")
            // } else if (!timestampSynchronizer.isSynced()) {
            //     InAppLogger.w("[NEO] Time stamps not yet synchronized!")
            // } else if ((!emulatorMode && timestampSynchronizer.getSystemTimeFromNanosTimestamp(carPropertiesData.CurrentPower.timestamp) < System.currentTimeMillis() - 500) || (emulatorMode && timestampSynchronizer.getSystemTimeFromNanosTimestamp(carPropertiesData.CurrentSpeed.timestamp) < System.currentTimeMillis() - 500)) {
            //     InAppLogger.w("[NEO] Power value is too old!")
            } else {
                // InAppLogger.d("[CHARGING CURVE] Before time check: ")
                // InAppLogger.d("[CHARGING CURVE] SoC timestamp: ${carPropertiesData.BatteryLevel.timestamp}")
                // InAppLogger.d("[CHARGING CURVE] Power timestamp: ${carPropertiesData.CurrentPower.timestamp}")
                // while ((!emulatorMode && timestampSynchronizer.getSystemTimeFromNanosTimestamp(carPropertiesData.CurrentPower.timestamp) < System.currentTimeMillis() - 500) || (emulatorMode && timestampSynchronizer.getSystemTimeFromNanosTimestamp(carPropertiesData.CurrentSpeed.timestamp) < System.currentTimeMillis() - 500)) {
                while ((!emulatorMode && carPropertiesData.CurrentPower.timestamp < System.nanoTime() - 500_000_000) || (emulatorMode && carPropertiesData.CurrentSpeed.timestamp < System.nanoTime() - 500_000_000)) {
                    InAppLogger.w("[NEO] Power value is too old!")
                    delay(250)
                }

                // InAppLogger.d("[CHARGING CURVE] After time check: ")
                // InAppLogger.d("[CHARGING CURVE] SoC timestamp: ${carPropertiesData.BatteryLevel.timestamp}")
                // InAppLogger.d("[CHARGING CURVE] Power timestamp: ${carPropertiesData.CurrentPower.timestamp}")

                val currentTime = System.currentTimeMillis()
                // InAppLogger.d("Time delta: ${currentTime - lastChargingPointTime}")

                var marker = markerType

                if (currentTime > lastChargingPointTime + (chargingPointInterval * 1.1)) {
                    InAppLogger.i("[NEO] Resuming charging curve after long time delta")
                    if (markerType == null) {
                        marker = PlotLineMarkerType.BEGIN_SESSION.int
                    } else if (markerType == PlotLineMarkerType.END_SESSION.int) {
                        marker = PlotLineMarkerType.SINGLE_SESSION.int
                    }
                }

                val chargingPoint = ChargingPoint(
                    currentTime,
                    localChargingSession?.charging_session_id!!,
                    mUsedEnergy.toFloat(),
                    realTimeData.power?:0f,
                    realTimeData.stateOfCharge?:0f,
                    point_marker_type = marker
                )

                CarStatsViewer.tripDataSource.addChargingPoint(chargingPoint)
                lastChargingPointTime = currentTime
                localChargingSession?.let {
                    val chargingPoints = it.chargingPoints?.toMutableList()?: mutableListOf()
                    if ((chargingPoint.point_marker_type == 1 || chargingPoint.point_marker_type == 3) && chargingPoints.isNotEmpty()) {
                        val prevChargingPoint = chargingPoints.last()
                        if (prevChargingPoint.point_marker_type != 2 && prevChargingPoint.point_marker_type != 3) {
                            chargingPoints[chargingPoints.size - 1] = prevChargingPoint.copy(point_marker_type = 2)
                        }
                    }
                    chargingPoints.add(chargingPoint)
                    localChargingSession?.chargeTime = chargeTimer.getTime()
                    localChargingSession?.chargingPoints = chargingPoints
                    _currentChargingSessionDataFlow.value = localChargingSession
                    CarStatsViewer.tripDataSource.updateChargingSession(it)
                    InAppLogger.d("[NEO] Charging point written: ${chargingPoint.power / 1_000_000f} kW, ${(chargingPoint.state_of_charge * 100).roundToInt()} %, ${chargingPoint.charging_point_epoch_time}")
                }
            }
        }
    }

    /** Change the selected trip type to update the trip data flow with */
    fun changeSelectedTrip(tripType: Int) {
        if (localSessionsState.value.isNotEmpty()) {
            try {
                selectedSessionData = localSessionsState.value.first { it.session_type == tripType }
                updateTripDataValues()
            } catch (e: Exception) {
                val type = when (tripType) {
                    TripType.MANUAL, TripType.MONTH, TripType.AUTO, TripType.SINCE_CHARGE -> TripType.tripTypesNameMap[tripType]
                    else -> "Unknown Trip Type!"
                }
                InAppLogger.logThrowableWithFirebase("Error switching trip type! It appears there is no \"$type\".\n${e.message}", e)
            }
        }
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
        // aaosExec?.execute(aaosRunnable)
    }

    private fun startChargingSession(): Job {
        chargeTimer.reset()
        return CoroutineScope(Dispatchers.IO).launch {
            val latestChargingSession = CarStatsViewer.tripDataSource.getLatestChargingSession()

            latestChargingSession?.let {
                if (System.currentTimeMillis() - chargingInterruptionThreshold < (it.end_epoch_time?: 0)) {
                    InAppLogger.i("[NEO] Found charging session ${it.charging_session_id} to be considered interrupted")
                    CarStatsViewer.tripDataSource.updateChargingSession(it.copy(end_epoch_time = null))
                }
            }

            val activeChargingSessionIds = CarStatsViewer.tripDataSource.getActiveChargingSessionIds()
            val chargingSessionId = if (activeChargingSessionIds.isNotEmpty()) {
                activeChargingSessionIds.forEachIndexed { index, sessionId ->
                    // Make sure only one charging session is active at all times
                    if (index < activeChargingSessionIds.size - 1) {
                        val chargingSession = CarStatsViewer.tripDataSource.getChargingSessionById(sessionId)
                        CarStatsViewer.tripDataSource.updateChargingSession(
                            chargingSession!!.copy(end_epoch_time = System.currentTimeMillis())
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
            val endTime = System.currentTimeMillis()
            CarStatsViewer.tripDataSource.endChargingSession(
                endTime,
                localChargingSession?.charging_session_id
            )
            localChargingSession?.let {
                val chargingPoints = it.chargingPoints
                localChargingSession = it.copy(end_epoch_time = endTime)
                localChargingSession?.chargeTime = chargeTimer.getTime()
                localChargingSession?.chargingPoints = chargingPoints
            }
            _currentChargingSessionDataFlow.value = localChargingSession
            CoroutineScope(Dispatchers.IO).launch {
                var chargingSession = localChargingSession

                localChargingSession?.let {
                    var chargedSoc = 0f

                    chargingSession!!.chargingPoints?.let { cp ->
                        if (cp.size > 1) {
                            val startSoc = (cp.first().state_of_charge * 100f).roundToInt()
                            val endSoc = (cp.last().state_of_charge * 100f).roundToInt()
                            chargedSoc = (endSoc - startSoc).toFloat() / 100f
                        }
                    }

                    chargingSession = chargingSession.copy(charged_soc = chargedSoc)
                    chargingSession.chargingPoints = it.chargingPoints
                    chargingSession.chargeTime = it.chargeTime
                }

                (CarStatsViewer.liveDataApis[1] as HttpLiveData).sendWithDrivingPoint(realTimeData, chargingSessions = if (chargingSession == null) null else listOf(chargingSession))
            }
            InAppLogger.i("[NEO] Charging session with ID ${localChargingSession?.charging_session_id} ended")
        }
    }

    private fun stopChargeTicker() {
        chargingTickerActive = false
        chargeTicker?.cancel()
    }

    private fun startChargeTicker() {
        chargingTickerActive = true
        chargeTicker = CoroutineScope(Dispatchers.Default).launch {
            Ticker.tickerFlow(5000).collectLatest {
                if (chargingTickerActive)
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