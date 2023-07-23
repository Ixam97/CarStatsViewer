package com.ixam97.carStatsViewer.carPropertiesClient

import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import com.ixam97.carStatsViewer.utils.InAppLogger

class CarPropertiesData {
    /** Current speed in m/s */
    val CurrentSpeed = CarProperty(VehiclePropertyIds.PERF_VEHICLE_SPEED)
    /** Current power in mW */
    val CurrentPower = CarProperty(VehiclePropertyIds.EV_BATTERY_INSTANTANEOUS_CHARGE_RATE)
    /** Current gear selection */
    val CurrentGear = CarProperty(VehiclePropertyIds.GEAR_SELECTION)
    /** Connection status of the charge port */
    val ChargePortConnected = CarProperty(VehiclePropertyIds.EV_CHARGE_PORT_CONNECTED)
    /** Battery level in Wh, only usable for calculating the SoC! */
    val BatteryLevel = CarProperty(VehiclePropertyIds.EV_BATTERY_LEVEL)
    /** Ignition state of the vehicle */
    val CurrentIgnitionState = CarProperty( VehiclePropertyIds.IGNITION_STATE)
    /** Current ambientTemperature */
    val CurrentAmbientTemperature = CarProperty( VehiclePropertyIds.ENV_OUTSIDE_TEMPERATURE)

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

    var traveledDistance: Double = 0.0
    var usedEnergy: Double = 0.0

    /** Update data manager using a VehiclePropertyValue. Returns VALID when value was changed.
     * @param value The CarPropertyValue received by the CarPropertyManager.
     * @param doLog Info about the updated value is printed to the console.
     * @param valueMustChange If set true only values different from the current values will be accepted.
     * @param allowInvalidTimestamps If set true the timestamp will be set to the startup timestamp should it be smaller than this.
     * @return Int representing the success of the update. 0 means a valid update.
     */
    fun update(value: CarPropertyValue<*>, doLog: Boolean = false, valueMustChange: Boolean = false, allowInvalidTimestamps: Boolean = false): Int {
        if (value.status != CarPropertyValue.STATUS_AVAILABLE) InAppLogger.d("PropertyStatus ${CarProperties.getNameById(value.propertyId)}: ${value.status}")
        return update(value.value, value.timestamp, value.propertyId, doLog, valueMustChange, allowInvalidTimestamps)
    }

    /** Update data manager using a VehiclePropertyValue. Returns VALID when value was changed.
     * @param value The actual value of the property (Int, Float, Boolean or String).
     * @param pTimestamp The Timestamp of the new property value in nanoseconds.
     * @param propertyId: The PropertyID of the property to update.
     * @param doLog Info about the updated value is printed to the console.
     * @param valueMustChange If set true only values different from the current values will be accepted.
     * @param allowInvalidTimestamps If set true the timestamp will be set to the startup timestamp should it be smaller than this.
     * @return Int representing the success of the update. 0 means a valid update.
     */
    fun update(value: Any?, pTimestamp: Long, propertyId: Int, doLog: Boolean = false, valueMustChange: Boolean = false, allowInvalidTimestamps: Boolean = false): Int {
        var timestamp = pTimestamp
        val failedPropertyString = "Failed to update car property ${CarProperties.getNameById(propertyId)}:"
        if (!CarProperties.usedProperties.contains(propertyId)) {
            InAppLogger.w("$failedPropertyString Invalid property ID")
            return INVALID_PROPERTY_ID
        }
        val property: CarProperty = propertiesMap[propertyId]!!
        if (value !is Boolean? && value !is Float? && value !is Int? && value !is String?){
            InAppLogger.w("$failedPropertyString Invalid data type")
            return INVALID_TYPE
        }
        if (!allowInvalidTimestamps && timestamp < property.timestamp) {
            InAppLogger.w("$failedPropertyString Invalid timestamp")
            return INVALID_TIMESTAMP
        }
        if (property.value == value && valueMustChange) {
            return SKIP_SAME_VALUE
        }
        property.value = value
        property.timestamp = timestamp
        if (doLog) InAppLogger.v("Updated ${CarProperties.getNameById(propertyId)}, value=${property.value}, valueDelta=${property.valueDelta}, timeDelta=${property.timeDelta}")
        return VALID
    }

    private val propertiesMap: Map<Int, CarProperty> = mapOf(
        CurrentSpeed.propertyId to CurrentSpeed,
        CurrentPower.propertyId to CurrentPower,
        CurrentGear.propertyId to CurrentGear,
        ChargePortConnected.propertyId to ChargePortConnected,
        BatteryLevel.propertyId to BatteryLevel,
        CurrentIgnitionState.propertyId to CurrentIgnitionState,
        CurrentAmbientTemperature.propertyId to CurrentAmbientTemperature
    )

}