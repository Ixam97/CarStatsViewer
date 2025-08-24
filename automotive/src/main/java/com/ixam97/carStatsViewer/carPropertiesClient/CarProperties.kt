package com.ixam97.carStatsViewer.carPropertiesClient

import android.car.VehiclePropertyIds
import android.car.hardware.property.CarPropertyManager

object CarProperties {

    // Dynamic Properties
    const val PERF_VEHICLE_SPEED = VehiclePropertyIds.PERF_VEHICLE_SPEED
    const val EV_BATTERY_INSTANTANEOUS_CHARGE_RATE = VehiclePropertyIds.EV_BATTERY_INSTANTANEOUS_CHARGE_RATE
    const val GEAR_SELECTION = VehiclePropertyIds.GEAR_SELECTION
    const val EV_CHARGE_PORT_CONNECTED = VehiclePropertyIds.EV_CHARGE_PORT_CONNECTED
    const val EV_BATTERY_LEVEL = VehiclePropertyIds.EV_BATTERY_LEVEL
    const val IGNITION_STATE = VehiclePropertyIds.IGNITION_STATE
    const val ENV_OUTSIDE_TEMPERATURE = VehiclePropertyIds.ENV_OUTSIDE_TEMPERATURE
    const val INFO_EV_BATTERY_CAPACITY = VehiclePropertyIds.INFO_EV_BATTERY_CAPACITY

    // Static Properties
    const val INFO_MODEL = VehiclePropertyIds.INFO_MODEL
    const val INFO_MAKE = VehiclePropertyIds.INFO_MAKE
    const val INFO_MODEL_YEAR = VehiclePropertyIds.INFO_MODEL_YEAR
    const val DISTANCE_DISPLAY_UNITS = VehiclePropertyIds.DISTANCE_DISPLAY_UNITS

    @Deprecated("Use essential and optional properties lists instead.")
    val usedProperties = listOf(
        PERF_VEHICLE_SPEED,
        EV_BATTERY_INSTANTANEOUS_CHARGE_RATE,
        GEAR_SELECTION,
        EV_CHARGE_PORT_CONNECTED,
        EV_BATTERY_LEVEL,
        IGNITION_STATE,
        ENV_OUTSIDE_TEMPERATURE,
    )

    val essentialDynamicProperties = listOf(
        PERF_VEHICLE_SPEED,
        EV_BATTERY_INSTANTANEOUS_CHARGE_RATE,
        GEAR_SELECTION,
        IGNITION_STATE,
        EV_CHARGE_PORT_CONNECTED,
        EV_BATTERY_LEVEL
    )
    val optionalDynamicProperties = listOf(
        ENV_OUTSIDE_TEMPERATURE
    )

    val usedStaticProperties = listOf(
        INFO_MODEL,
        INFO_MAKE,
        INFO_MODEL_YEAR,
        DISTANCE_DISPLAY_UNITS
    )

    val sensorRateMap = mapOf(
        PERF_VEHICLE_SPEED to 0f,
        EV_BATTERY_INSTANTANEOUS_CHARGE_RATE to CarPropertyManager.SENSOR_RATE_FAST,
        GEAR_SELECTION to 0f,
        EV_CHARGE_PORT_CONNECTED to 0f,
        EV_BATTERY_LEVEL to 0f,
        IGNITION_STATE to 0f,
        ENV_OUTSIDE_TEMPERATURE to 0f,
    )
    fun getNameById(propertyId: Int) = when (propertyId) {
        PERF_VEHICLE_SPEED -> "Speed"
        EV_BATTERY_INSTANTANEOUS_CHARGE_RATE -> "Power"
        GEAR_SELECTION -> "Gear selection"
        EV_CHARGE_PORT_CONNECTED -> "Charge port connected"
        EV_BATTERY_LEVEL -> "Battery level"
        IGNITION_STATE -> "Ignition state"
        ENV_OUTSIDE_TEMPERATURE -> "Outside temperature"
        INFO_MODEL -> "Model"
        INFO_MAKE -> "Make"
        INFO_MODEL_YEAR -> "Model year"
        DISTANCE_DISPLAY_UNITS -> "Distance unit"
        else -> "unused car property $propertyId"
    }
}