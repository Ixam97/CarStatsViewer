package com.ixam97.carStatsViewer

import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import com.ixam97.carStatsViewer.objects.TripData

sealed class VehicleData(val printableName: String) {
    private val startupTimestamp = System.nanoTime()

    var value: Any? = null
        internal set(value) {
            lastValue = field
            field = value
        }

    private var lastValue: Any? = null

    var timestamp: Long = startupTimestamp
        internal set(value) {
            lastTimestamp = field
            field = value
        }

    private var lastTimestamp: Long = startupTimestamp

    /** returns the time difference between value updates in nanoseconds */
    val timeDelta: Long get() {
        if (lastTimestamp == 0L || lastTimestamp < startupTimestamp) return 0L
        return timestamp - lastTimestamp
    }

    val valueDelta: Any?
        get() {
            val returnValue = value
            if (returnValue == null || lastValue == null) return null
            if (returnValue is Float) return (returnValue - lastValue as Float)
            if (returnValue is Int) return (returnValue - lastValue as Int)
            return false
        }
}

sealed class VehicleProperty(printableName: String, val propertyId: Int): VehicleData(printableName)

sealed class DataManager {
    object CurrentSpeed: VehicleProperty("CurrentSpeed", VehiclePropertyIds.PERF_VEHICLE_SPEED)
    object CurrentPower: VehicleProperty("CurrentPower", VehiclePropertyIds.EV_BATTERY_INSTANTANEOUS_CHARGE_RATE)

    object UsedEnergy: VehicleData("UsedEnergy")
    object TraveledDistance: VehicleData("TraveledDistance")
    object TravelTime: VehicleData("TravelTime")

    companion object {
        val currentSpeed get() = CurrentSpeed.value as Float
        val currentPower get() = CurrentPower.value as Float
        val usedEnergy get() = UsedEnergy.value as Float
        val traveledDistance get() = TraveledDistance.value as Float
        val travelTime get() = TravelTime.value as Long

        /** Value was updated */
        const val VALID = 0
        /** Timestamp of new value is invalid (smaller than current timestamp) */
        const val INVALID_TIMESTAMP = 1
        /** The PropertyID is not implemented in the DataManager */
        const val INVALID_PROPERTY_ID = 2
        /** The new Value is of an invalid Type */
        const val INVALID_TYPE = 3

        /** Update data manager using a VehiclePropertyValue. Returns VALID when value was changed. */
        fun update(value: CarPropertyValue<*>): Int {
            return update(value.value, value.timestamp, value.propertyId)
        }

        /** Update data manager using a value of type Int, Boolean, Float or String, timestamp and propertyID. Returns VALID when value was changed. */
        fun update(value: Any?, timestamp: Long, propertyId: Int): Int {
            if (!propertiesMap.containsKey(propertyId)) return INVALID_PROPERTY_ID
            if (value !is Boolean? && value !is Float? && value !is Int? && value !is String?) return INVALID_TYPE
            val property: VehicleProperty = propertiesMap[propertyId]!!
            if (timestamp < property.timestamp) return INVALID_TIMESTAMP
            property.value = value
            property.timestamp = timestamp
            return VALID
        }
        /** Get tripData for summary or saving. Set tripData to load current trip into DataManager */
        /*
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

        private val propertiesMap: Map<Int, VehicleProperty> = mapOf(
            CurrentSpeed.propertyId to CurrentSpeed,
            CurrentPower.propertyId to CurrentPower
        )
    }
}