package com.ixam97.carStatsViewer.dataProcessor

import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.Defines
import com.ixam97.carStatsViewer.carPropertiesClient.CarProperties
import com.ixam97.carStatsViewer.carPropertiesClient.CarPropertiesData
import com.ixam97.carStatsViewer.dataManager.DrivingState
import com.ixam97.carStatsViewer.database.tripData.DrivingPoint
import com.ixam97.carStatsViewer.emulatorMode
import com.ixam97.carStatsViewer.emulatorPowerSign
import com.ixam97.carStatsViewer.plot.enums.PlotLineMarkerType
import com.ixam97.carStatsViewer.utils.InAppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

class DataProcessor(
    val tripDataManager: TripDataManager
) {
    val carPropertiesData = CarPropertiesData()

    private var pointDrivenDistance: Double = 0.0
    private var pointUsedEnergy: Double = 0.0
    private var valueDrivenDistance: Double = 0.0
    private var valueUsedEnergy: Double = 0.0

    private var usedEnergySum = 0.0
    private var previousDrivingState: Int = DrivingState.UNKNOWN
    private var previousStateOfCharge: Float = -1f

    var staticVehicleData = StaticVehicleData()

    var realTimeData = RealTimeData()
        private set(value) {
            field = value
            _realTimeDataFlow.value = value
        }

    private val _realTimeDataFlow = MutableStateFlow(realTimeData)
    val realTimeDataFlow = _realTimeDataFlow.asStateFlow()

    fun processLocation(lat: Double?, lon: Double?, alt: Double?) {
        realTimeData = realTimeData.copy(
            lat = lat?.toFloat(),
            lon = lon?.toFloat(),
            alt = alt?.toFloat()
        )
    }

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

    // fun resetManualTrip() {
    //     updateDrivingDataPoint(null, true)
    // }

    private fun speedUpdate() {
        if (carPropertiesData.CurrentSpeed.timeDelta > 0 && realTimeData.drivingState == DrivingState.DRIVE) {
            val distanceDelta = (carPropertiesData.CurrentSpeed.value as Float).absoluteValue * (carPropertiesData.CurrentSpeed.timeDelta / 1_000_000_000f)
            pointDrivenDistance += distanceDelta
            valueDrivenDistance += distanceDelta
            if (emulatorMode) {
                val energyDelta = emulatorPowerSign * (carPropertiesData.CurrentPower.value as Float) / 1_000f * (carPropertiesData.CurrentSpeed.timeDelta / 3.6E12)
                pointUsedEnergy += energyDelta
                valueUsedEnergy += energyDelta

                if (valueUsedEnergy > 100) {
                    updateTripDataValues(DrivingState.DRIVE)
                }
            }
            if (pointDrivenDistance >= Defines.PLOT_DISTANCE_INTERVAL) updateDrivingDataPoint()
            if (valueDrivenDistance > 10) {
                updateTripDataValues(DrivingState.DRIVE)
            }
        }
    }

    private fun stateOfChargeUpdate() {
        staticVehicleData.batteryCapacity?.let { batteryCapacity ->
            val currentStateOfCharge = realTimeData.stateOfCharge
            if (previousStateOfCharge < 0) {
                previousStateOfCharge = currentStateOfCharge
                return
            }
            if (currentStateOfCharge != previousStateOfCharge) {
                if (realTimeData.drivingState == DrivingState.DRIVE)
                    tripDataManager.updateUsedStateOfCharge((previousStateOfCharge - currentStateOfCharge).toDouble())
                previousStateOfCharge = currentStateOfCharge
            }
        }
    }

    private fun powerUpdate() {
        if (!emulatorMode) {
            if (carPropertiesData.CurrentPower.timeDelta > 0 && (realTimeData.drivingState == DrivingState.DRIVE || realTimeData.drivingState == DrivingState.CHARGE)) {
                val energyDelta = emulatorPowerSign * (carPropertiesData.CurrentPower.value as Float) / 1_000f * (carPropertiesData.CurrentPower.timeDelta / 3.6E12)
                pointUsedEnergy += energyDelta
                valueUsedEnergy += energyDelta
            }

            if (valueUsedEnergy > 100) {
                updateTripDataValues(DrivingState.DRIVE)
            }
        }
    }

    private fun stateUpdate() {
        val drivingState = realTimeData.drivingState
        if (drivingState != previousDrivingState) {
            InAppLogger.i("[NEO] Drive state changed from ${DrivingState.nameMap[previousDrivingState]} to ${DrivingState.nameMap[drivingState]}")
            // if (drivingState == DrivingState.DRIVE || drivingState == DrivingState.PARKED) updateTripData()
            when (drivingState) {
                DrivingState.DRIVE -> updateDrivingDataPoint(PlotLineMarkerType.BEGIN_SESSION.int)
                DrivingState.PARKED -> updateDrivingDataPoint(PlotLineMarkerType.END_SESSION.int)
            }
            if (drivingState == DrivingState.DRIVE && previousDrivingState == DrivingState.CHARGE)
                previousStateOfCharge = realTimeData.stateOfCharge
            tripDataManager.newDrivingState(drivingState, previousDrivingState)
        }
        previousDrivingState = drivingState
    }

    private fun updateDrivingDataPoint(markerType: Int? = null) {
        usedEnergySum += pointUsedEnergy
        InAppLogger.v("[NEO] Driven distance: $pointDrivenDistance, Used energy: $usedEnergySum")

        val drivingPoint = DrivingPoint(
            driving_point_epoch_time = System.currentTimeMillis(),
            energy_delta = pointUsedEnergy.toFloat(),
            distance_delta = pointDrivenDistance.toFloat(),
            point_marker_type = markerType,
            state_of_charge = realTimeData.stateOfCharge,
            lat = realTimeData.lat,
            lon = realTimeData.lon,
            alt = realTimeData.alt
        )

        pointUsedEnergy = 0.0
        pointDrivenDistance = 0.0

        CoroutineScope(Dispatchers.IO).launch {
            val drivingPoint = drivingPoint.copy()
            CarStatsViewer.tripDataSource.addDrivingPoint(drivingPoint)

            updateTripDataValues(DrivingState.DRIVE)

            // if (doReset) {
            //     val drivingSessionsIdsMap = CarStatsViewer.tripDataSource.getActiveDrivingSessionsIdsMap()
            //     InAppLogger.i("[NEO] drivingSessionsIdsMap: $drivingSessionsIdsMap")
            //     val drivingSessionId = CarStatsViewer.tripDataSource.getActiveDrivingSessionsIdsMap()[TripType.MANUAL]
            //     if (drivingSessionId != null) {
            //         CarStatsViewer.tripDataSource.supersedeDrivingSession(drivingSessionId, System.currentTimeMillis())
            //     } else {
            //         CarStatsViewer.tripDataSource.startDrivingSession(System.currentTimeMillis(), TripType.MANUAL)
            //     }
            // }
        }
    }

    private fun updateChargingDataPoint(markerType: Int? = null) {
        updateTripDataValues(DrivingState.CHARGE)
    }

    private fun updateTripDataValues(drivingState: Int = realTimeData.drivingState) {
        when (drivingState) {
            DrivingState.DRIVE -> tripDataManager.newDrivingDeltas(valueDrivenDistance, valueUsedEnergy)
            DrivingState.CHARGE -> tripDataManager.newChargingDeltas(valueUsedEnergy)
        }
        valueDrivenDistance = 0.0
        valueUsedEnergy = 0.0
    }
}