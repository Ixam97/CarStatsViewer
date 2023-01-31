package com.ixam97.carStatsViewer

import android.car.VehicleIgnitionState
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.util.Log
import com.ixam97.carStatsViewer.objects.ChargeCurve
import com.ixam97.carStatsViewer.objects.TripData
import com.ixam97.carStatsViewer.plot.enums.PlotDimension
import com.ixam97.carStatsViewer.plot.enums.PlotHighlightMethod
import com.ixam97.carStatsViewer.plot.enums.PlotLabelPosition
import com.ixam97.carStatsViewer.plot.enums.PlotLineLabelFormat
import com.ixam97.carStatsViewer.plot.objects.*
import java.util.Date
import kotlin.math.absoluteValue

class TimeTracker(val printableName: String = "", val doLog: Boolean = false) {
    private var startDate: Date? = null
    private var timeSpan: Long = 0

    /** Start or resume time tracking */
    fun start() {
        if (startDate == null) startDate = Date()
    }

    /** Stop time tracking */
    fun stop() {
        startDate.let {
            if (it != null) timeSpan += (Date().time - it.time)
        }
        startDate = null
    }

    /** Reset tracked time to zero */
    fun reset() {
        startDate = null
        timeSpan = 0L
    }

    /** Restore tracked time to defined base value */
    fun restore(pTimeSpan: Long) {
        timeSpan = pTimeSpan
    }

    /** returns the current tracked time in milliseconds */
    fun getTime(): Long {
        return timeSpan + (Date().time - (startDate?.time?: Date().time))
    }
}

class DrivingState(val ChargePortConnected: VehicleProperty, val CurrentIgnitionState: VehicleProperty) {
    // States
    companion object {
        val UNKNOWN = -1
        val PARKED = 0
        val DRIVE = 1
        val CHARGE = 2
    }

    var lastDriveState: Int = UNKNOWN

    fun hasChanged(): Boolean {
        if (getDriveState() != lastDriveState) {
            lastDriveState = getDriveState()
            return true
        }
        return false
    }

    fun getDriveState(): Int {
        var newDriveState = UNKNOWN
        val chargePortConnected = (ChargePortConnected.value?: false) as Boolean
        val currentIgnitionState = (CurrentIgnitionState.value?: 0) as Int
        if (chargePortConnected) newDriveState = CHARGE
        else if (currentIgnitionState == VehicleIgnitionState.START) newDriveState = DRIVE
        else if (currentIgnitionState == VehicleIgnitionState.OFF || currentIgnitionState == VehicleIgnitionState.ON) newDriveState = PARKED
        return newDriveState
    }
}

class VehicleProperty(val printableName: String, val propertyId: Int) {
    internal val startupTimestamp = System.nanoTime()

    /** Value of the Vehicle */
    var value: Any? = null
        internal set(value) {
            lastValue = field
            field = value
        }

    var lastValue: Any? = null
        internal set

    /** Timestamp of the last time the value was changed in nanoseconds */
    var timestamp: Long = startupTimestamp
        internal set(value) {
            lastTimestamp = field
            field = value
        }

    internal var lastTimestamp: Long = startupTimestamp

    /** Time difference between value changes in nanoseconds */
    val timeDelta: Long get() {
        if (lastTimestamp == 0L || lastTimestamp < startupTimestamp) return 0L
        return timestamp - lastTimestamp
    }

    /** Returns true if the value difference is null, therefore the value is the initial value */
    val isInitialValue: Boolean get() = (valueDelta == null)

    /** Value difference since last value change */
    val valueDelta: Any?
        get() {
            value.let {
                if (it == null || lastValue == null) return null
                if (it is Float) return (it - lastValue as Float)
                if (it is Int) return (it - lastValue as Int)
                if (it is Boolean) return !(lastValue as Boolean)
            }
            return null
        }
}

/**
 * The DataManager is responsible of holding and managing all data regarding driving and charging
 * statistics of the vehicle.
 *
 * The start of a drive should be triggered by putting the car into drive (therefore putting the
 * ignition state into "START". When the car is left and locked the ignition state changes to "OFF",
 * marking the end of a drive. In between those events used energy and time may be tracked.
 *
 * A charging session is started by plugging in the charge cable, stopped by unplugging it. Like a
 * drive, charging time and charged energy are tracked in between these events.
 */
class DataManager {
    /** Current speed in m/s */
    val CurrentSpeed = VehicleProperty("CurrentSpeed", VehiclePropertyIds.PERF_VEHICLE_SPEED)
    /** Current power in mW */
    val CurrentPower = VehicleProperty("CurrentPower", VehiclePropertyIds.EV_BATTERY_INSTANTANEOUS_CHARGE_RATE)
    /** Current gear selection */
    val CurrentGear = VehicleProperty("CurrentGear", VehiclePropertyIds.GEAR_SELECTION)
    /** Connection status of the charge port */
    val ChargePortConnected = VehicleProperty("ChargePortConnected", VehiclePropertyIds.EV_CHARGE_PORT_CONNECTED)
    /** Battery level in Wh, only usable for calculating the SoC! */
    val BatteryLevel = VehicleProperty("BatteryLevel", VehiclePropertyIds.EV_BATTERY_LEVEL)
    /** Ignition state of the vehicle */
    val CurrentIgnitionState = VehicleProperty("CurrentIgnitionState",  VehiclePropertyIds.IGNITION_STATE)

    /** Travel time in milliseconds */
    val TravelTime = TimeTracker()
    /** Charge time in milliseconds */
    val ChargeTime = TimeTracker()

    val DriveState = DrivingState(ChargePortConnected, CurrentIgnitionState)

    // companion object {
        val TAG = "DataManager"
        // Easier vehicle property access and implicit values
        /** Current speed in m/s */
        val currentSpeed get() = ((CurrentSpeed.value?: 0F) as Float).absoluteValue
        /** Current power in mW */
        val currentPower get() = com.ixam97.carStatsViewer.activities.emulatorPowerSign * (CurrentPower.value ?: 0F) as Float
        /** Current gear selection */
        val currentGear get() = (CurrentGear.value?: 0) as Int
        /** Connection status of the charge port */
        val chargePortConnected get() = (ChargePortConnected.value?: false) as Boolean
        /** Battery level in Wh, only usable for calculating the SoC! */
        val batteryLevel get() = (BatteryLevel.value?: 0F) as Float
        /** Current state of charge */
        val stateOfCharge: Int get() = ((batteryLevel / maxBatteryLevel) * 100F).toInt()
        /** Ignition state of the vehicle */
        val currentIgnitionState get() = (CurrentIgnitionState.value?: 0) as Int
        /** instantaneous consumption in Wh/m */
        val instConsumption: Float? get() = (currentPower / currentSpeed).run { if (this.isFinite()) return (this / 3_600_000) else return null }
        /** Average consumption in Wh/m, null if distance is zero */
        val avgConsumption: Float? get() = (usedEnergy / traveledDistance).run { if (this.isFinite()) return this else return null }
        /** Average speed in m/s */
        val avgSpeed: Float get() = (traveledDistance / (travelTime / 1_000)).run { if (this.isFinite()) return this else return 0F}
        /** Travel time in milliseconds */
        val travelTime: Long get() = TravelTime.getTime()
        /** Charge time in milliseconds */
        val chargeTime: Long get() = ChargeTime.getTime()

        val driveState: Int get() = DriveState.getDriveState()


        // Vehicle statistics
        /** Max battery level in Wh, only usable for calculating the SoC! */
        var maxBatteryLevel: Float = 0F
        /** Date on reset */
        var tripStartDate: Date = Date()
        /** Used energy in Wh **/
        var usedEnergy: Float = 0F
        /** Traveled distance in m */
        var traveledDistance: Float = 0F
        /** Used energy in Wh **/
        var chargedEnergy: Float = 0F

        var plotMarkers = PlotMarkers()

        var chargeCurves: ArrayList<ChargeCurve> = ArrayList()

        var consumptionPlotLine = PlotLine(
            PlotLineConfiguration(
                PlotRange(-300f, 900f, -300f, 900f, 100f, 0f),
                PlotLineLabelFormat.NUMBER,
                PlotLabelPosition.LEFT,
                PlotHighlightMethod.AVG_BY_DISTANCE,
                "Wh/km"
            ),
        )

        var chargePlotLine = PlotLine(
            PlotLineConfiguration(
                PlotRange(0f, 20f, 0f, 160f, 20f),
                PlotLineLabelFormat.NUMBER,
                PlotLabelPosition.LEFT,
                PlotHighlightMethod.AVG_BY_TIME,
                "kW"
            ),
        )

        // Updater return values
        /** Value was updated */
        val VALID = 0
        /** Timestamp of new value is invalid (smaller than current timestamp) */
        val INVALID_TIMESTAMP = 1
        /** The PropertyID is not implemented in the DataManager */
        val INVALID_PROPERTY_ID = 2
        /** The new Value is of an invalid Type */
        val INVALID_TYPE = 3
        /** The new value is equal to the last value */
        val SKIP_SAME_VALUE = 4

        /** Update data manager using a VehiclePropertyValue. Returns VALID when value was changed.
         * @param value The CarPropertyValue received by the CarPropertyManager.
         * @param doLog Info about the updated value is printed to the console.
         * @param valueMustChange If set true only values different from the current values will be accepted.
         * @param allowInvalidTimestamps If set true the timestamp will be set to the startup timestamp should it be smaller than this.
         * @return Int representing the success of the update. 0 means a valid update.
         */
        fun update(value: CarPropertyValue<*>, doLog: Boolean = false, valueMustChange: Boolean = false, allowInvalidTimestamps: Boolean = false): Int {
            return update(value.value, value.timestamp, value.propertyId, doLog, valueMustChange, allowInvalidTimestamps)
        }

        /** Update data manager using a value of type Int, Boolean, Float or String, timestamp and propertyID. Returns VALID when value was changed. */
        fun update(value: Any?, pTimestamp: Long, propertyId: Int, doLog: Boolean = false, valueMustChange: Boolean = false, allowInvalidTimestamps: Boolean = false): Int {
            var timestamp = pTimestamp
            if (!propertiesMap.containsKey(propertyId)) {
                if (doLog) Log.w(TAG, "${timestamp}: Failed to update property ID ${propertyId}: Invalid property ID")
                return INVALID_PROPERTY_ID
            }
            val property: VehicleProperty = propertiesMap[propertyId]!!
            if (value !is Boolean? && value !is Float? && value !is Int? && value !is String?){
                if (doLog) Log.w(TAG, "${timestamp}: Failed to update ${property.printableName}: Invalid data type")
                return INVALID_TYPE
            }
            if (allowInvalidTimestamps && timestamp < property.timestamp){
                timestamp = property.startupTimestamp
            }
            if (timestamp < property.timestamp) {
                if (doLog) Log.w(TAG, "${timestamp}: Failed to update ${property.printableName}: Invalid timestamp")
                return INVALID_TIMESTAMP
            }
            if (property.value == value && valueMustChange) {
                // if (doLog) Log.i(TAG, "Skipped update for ${property.printableName}: Value not changed")
                return SKIP_SAME_VALUE
            }
            property.value = value
            property.timestamp = timestamp
            if (doLog) Log.i(TAG, "${property.timestamp}: Updated ${property.printableName}, value=${property.value}, valueDelta=${property.valueDelta}, timeDelta=${property.timeDelta}")
            return VALID
        }


        /** Get tripData for summary or saving. Set tripData to load current trip into DataManager */
        var tripData: TripData?
            get() = TripData(
                appVersion = BuildConfig.VERSION_NAME,
                tripStartDate = tripStartDate,
                usedEnergy = usedEnergy,
                traveledDistance = traveledDistance,
                travelTime = travelTime,
                chargedEnergy = chargedEnergy,
                chargeTime = chargeTime,
                markers = plotMarkers.markers.toList(),
                chargeCurves = chargeCurves.toList(),
                consumptionPlotLine = consumptionPlotLine.getDataPoints(PlotDimension.DISTANCE).toList(),
                chargePlotLine = chargePlotLine.getDataPoints(PlotDimension.TIME).toList()
            )
            set(value) {
                tripStartDate = value?.tripStartDate?: Date()
                traveledDistance = value?.traveledDistance?: 0F
                usedEnergy = value?.usedEnergy?: 0F
                TravelTime.restore(value?.travelTime?: 0L)
                chargedEnergy = value?.chargedEnergy?: 0F
                ChargeTime.restore(value?.chargeTime?: 0L)
                plotMarkers.addMarkers(value?.markers?: listOf())
                chargeCurves = ArrayList()
                consumptionPlotLine.reset()
                chargePlotLine.reset()

                if (value != null) {
                    if(value.chargeCurves.isNotEmpty()) {
                        for (curve in value.chargeCurves) {
                            chargeCurves.add(curve)
                        }
                    }
                    consumptionPlotLine.addDataPoints(value.consumptionPlotLine?: listOf())
                    chargePlotLine.addDataPoints(value.chargePlotLine?: listOf())
                } else {
                    TravelTime.reset()
                    ChargeTime.reset()
                    when (driveState) {
                        DrivingState.DRIVE -> TravelTime.start()
                        DrivingState.CHARGE -> ChargeTime.start()
                    }
                }
            }


        fun getVehiclePropertyIds(): List<Int> {
            var idArrayList: ArrayList<Int> = arrayListOf()
            for (property in propertiesMap) {
                idArrayList.add(property.key)
            }
            return idArrayList.toList()
        }

        fun getVehiclePropertyById(propertyId: Int): VehicleProperty? {
            if (!propertiesMap.containsKey(propertyId)) return null
            return propertiesMap[propertyId]
        }

        fun reset() {
            tripData = null
        }

        private val propertiesMap: Map<Int, VehicleProperty> = mapOf(
            CurrentSpeed.propertyId to CurrentSpeed,
            CurrentPower.propertyId to CurrentPower,
            CurrentGear.propertyId to CurrentGear,
            ChargePortConnected.propertyId to ChargePortConnected,
            BatteryLevel.propertyId to BatteryLevel,
            CurrentIgnitionState.propertyId to CurrentIgnitionState
        )

}