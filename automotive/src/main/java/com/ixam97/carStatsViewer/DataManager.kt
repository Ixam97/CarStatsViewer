package com.ixam97.carStatsViewer

import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.util.Log
import com.ixam97.carStatsViewer.objects.TripData
import kotlin.math.absoluteValue

sealed class VehicleData(val printableName: String) {
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

sealed class VehicleProperty(printableName: String, val propertyId: Int): VehicleData(printableName)

sealed class DataManager {
    /** Current speed in m/s */
    object CurrentSpeed: VehicleProperty("CurrentSpeed", VehiclePropertyIds.PERF_VEHICLE_SPEED)
    /** Current power in mW */
    object CurrentPower: VehicleProperty("CurrentPower", VehiclePropertyIds.EV_BATTERY_INSTANTANEOUS_CHARGE_RATE)
    /** Current gear selection */
    object CurrentGear: VehicleProperty("CurrentGear", VehiclePropertyIds.GEAR_SELECTION)
    /** Connection status of the charge port */
    object ChargePortConnected: VehicleProperty("ChargePortConnected", VehiclePropertyIds.EV_CHARGE_PORT_CONNECTED)
    /** Battery level in Wh, only usable for calculating the SoC! */
    object BatteryLevel: VehicleProperty("BatteryLevel", VehiclePropertyIds.EV_BATTERY_LEVEL)

    /** Used energy in Wh **/
    object UsedEnergy: VehicleData("UsedEnergy")
    /** Traveled distance in m */
    object TraveledDistance: VehicleData("TraveledDistance")
    /** Travel time in nanoseconds */
    object TravelTime: VehicleData("TravelTime")

    companion object {
        const val TAG = "DataManager"
        /** Current speed in m/s */
        val currentSpeed get() = ((CurrentSpeed.value?: 0F) as Float).absoluteValue
        /** Current power in mW */
        val currentPower get() = (CurrentPower.value?: 0F) as Float

        // Vehicle Data may be set directly using system time as timestamp since they are not
        // relying on accurate timestamps for precise calculations.
        /** Used energy in Wh **/
        val usedEnergy get() = (UsedEnergy.value?: 0F) as Float
        /** Traveled distance in m */
        var traveledDistance get() = (TraveledDistance.value?: 0F) as Float; set(value) = setData(TraveledDistance, value)
        /** Travel time in nanoseconds */
        val travelTime get() = (TravelTime.value?: 0L) as Long

        /** Value was updated */
        const val VALID = 0
        /** Timestamp of new value is invalid (smaller than current timestamp) */
        const val INVALID_TIMESTAMP = 1
        /** The PropertyID is not implemented in the DataManager */
        const val INVALID_PROPERTY_ID = 2
        /** The new Value is of an invalid Type */
        const val INVALID_TYPE = 3
        /** The DataID is not implemented in the DataManager */
        const val INVALID_DATA_ID = 4
        /** The new value is equal to the last value */
        const val SKIP_SAME_VALUE = 5

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

        /** Update data manager using a value of type Int, Boolean, Float or String, timestamp and dataID. Returns VALID when value was changed. */
        fun update(value: Any?, pTimestamp: Long, dataId: VehicleDataIds, doLog: Boolean = false, valueMustChange: Boolean = false, allowInvalidTimestamps: Boolean = false): Int {
            var timestamp = pTimestamp
            if (!dataMap.containsKey(dataId)) {
                if (doLog) Log.w(TAG, "${timestamp}: Failed to update data ID ${dataId}: Invalid data ID")
                return INVALID_DATA_ID
            }
            val data: VehicleData = dataMap[dataId]!!
            if (value !is Boolean? && value !is Float? && value !is Int? && value !is String?){
                if (doLog) Log.w(TAG, "${timestamp}: Failed to update ${data.printableName}: Invalid data type")
                return INVALID_TYPE
            }
            if (allowInvalidTimestamps && timestamp < data.timestamp){
                timestamp = data.startupTimestamp
            }
            if (timestamp < data.timestamp) {
                if (doLog) Log.w(TAG, "${timestamp}: Failed to update ${data.printableName}: Invalid timestamp")
                return INVALID_TIMESTAMP
            }
            if (data.value == value && valueMustChange) {
                if (doLog) Log.i(TAG, "${timestamp}: Skipped update for ${data.printableName}: Value not changed")
                return SKIP_SAME_VALUE
            }
            data.value = value
            data.timestamp = timestamp
            if (doLog) Log.i(TAG, "${data.timestamp}: Updated ${data.printableName}, value=${data.value}, valueDelta=${data.valueDelta}, timeDelta=${data.timeDelta}")
            return VALID
        }

        /*
        /** Get tripData for summary or saving. Set tripData to load current trip into DataManager */
        var tripData
            get() = TripData(
                traveledDistance = traveledDistance,
                usedEnergy = usedEnergy,
                travelTimeMillis = travelTime / 1_000_000
            )
            set(value) {
                TraveledDistance.value = value.traveledDistance
                UsedEnergy.value = value.usedEnergy
                TravelTime.value = value.travelTimeMillis * 1_000_000
            }
        */

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

        fun getVehicleDataById(dataId: VehicleDataIds): VehicleData? {
            if(!dataMap.containsKey(dataId)) return null
            return dataMap[dataId]
        }

        private fun setData(data: VehicleData, value: Any?) {
            data.lastTimestamp = data.timestamp
            data.timestamp = System.nanoTime()
            data.lastValue = data.value
            data.value = value
        }

        private val propertiesMap: Map<Int, VehicleProperty> = mapOf(
            CurrentSpeed.propertyId to CurrentSpeed,
            CurrentPower.propertyId to CurrentPower,
            CurrentGear.propertyId to CurrentGear,
            ChargePortConnected.propertyId to ChargePortConnected,
            BatteryLevel.propertyId to BatteryLevel
        )

        private val dataMap: Map<VehicleDataIds, VehicleData> = mapOf(
            VehicleDataIds.USED_ENERGY to UsedEnergy,
            VehicleDataIds.TRAVEL_TIME to TravelTime,
            VehicleDataIds.TRAVELED_DISTANCE to TraveledDistance
        )
    }
}

enum class VehicleDataIds {
    USED_ENERGY,
    TRAVEL_TIME,
    TRAVELED_DISTANCE
}