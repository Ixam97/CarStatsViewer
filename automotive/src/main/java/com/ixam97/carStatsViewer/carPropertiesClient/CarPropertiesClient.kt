package com.ixam97.carStatsViewer.carPropertiesClient

import android.car.Car
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.car.hardware.property.PropertyNotAvailableErrorCode
import android.car.hardware.property.PropertyNotAvailableException
import android.content.Context
import android.util.Log
import com.ixam97.carStatsViewer.utils.InAppLogger

class CarPropertiesClient(
    context: Context,
    private val propertiesProcessor: (propertyId: Int) -> Unit,
    private val carPropertiesData: CarPropertiesData
) {

    // private var debugTemperatureAttempt = 0
    // private var debugDistanceUnitAttempt = 0

    private val _registeredProperties = mutableListOf<Int>()
    val registeredProperties: List<Int>
        get() = _registeredProperties

    private val carPropertyListener = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(carPropertyValue: CarPropertyValue<*>) {
            if (carPropertyValue.status != CarPropertyValue.STATUS_AVAILABLE) {
                InAppLogger.w("[CarPropertiesClient.carPropertyListener] Property ${CarProperties.getNameById(carPropertyValue.propertyId)} (${carPropertyValue.propertyId}) is currently not available. Status: ${carPropertyValue.status}.")
                return
            }
            carPropertiesData.update(carPropertyValue)
            propertiesProcessor(carPropertyValue.propertyId)
        }
        override fun onErrorEvent(propertyId: Int, zone: Int) {
            InAppLogger.e("[CarPropertiesClient.carPropertyListener] Received error car property event, propId=$propertyId")
        }
    }

    private var car = Car.createCar(context)
    private var carPropertyManager = car.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager

    fun <T>getProperty(propertyId: Int, areaId: Int = 0): T? {

        try {
            val propertyValue = carPropertyManager.getProperty<T>(propertyId, areaId)
            // propertyValue.propertyStatus crashes Polestar 2 emulator
            if (propertyValue.status != CarPropertyValue.STATUS_AVAILABLE /* || (propertyId == CarProperties.DISTANCE_DISPLAY_UNITS && emulatorMode && debugDistanceUnitAttempt < 5) */) {
                InAppLogger.w("[CarPropertiesClient.getProperty] Property ${CarProperties.getNameById(propertyId)} (${propertyId}) is currently not available. Status: ${propertyValue.status}.")
                // if (emulatorMode && propertyId == CarProperties.DISTANCE_DISPLAY_UNITS)
                //     debugDistanceUnitAttempt++
                return null
            }
            return propertyValue.value
        } catch (e: Exception) {
            try {
                if (e is PropertyNotAvailableException) {
                    val errorMsg = "[CarPropertiesClient.getProperty] Property is not available: ${PropertyNotAvailableErrorCode.toString(e.detailedErrorCode)}.\n\r${e.stackTraceToString()}"
                    InAppLogger.logWithFirebase(errorMsg, Log.ERROR)
                } else { throw e }
            } catch (ee: Throwable) {
                InAppLogger.e(ee.stackTraceToString())
                val errorMsg = "[CarPropertiesClient.getProperty] Failed to get Property ${CarProperties.getNameById(propertyId)} ($propertyId).\n\r${e.stackTraceToString()}"
                InAppLogger.logWithFirebase(errorMsg, Log.ERROR)
            }
        }
        return null
    }

    fun updateProperty(propertyId: Int) {
        // if (emulatorMode && propertyId == CarProperties.ENV_OUTSIDE_TEMPERATURE && debugTemperatureAttempt < 2) return
        carPropertyManager.getProperty<Any>(propertyId, 0)?.let {
            carPropertiesData.update(it, allowInvalidTimestamps = true)
        }
        propertiesProcessor(propertyId)
    }

    /**
     * Check availability of a Car Property
     */
    fun checkPropertyAvailability(propertyId: Int, areaId: Int): Boolean {
        return carPropertyManager.isPropertyAvailable(propertyId, areaId)
    }

    fun getIntProperty(propertyId: Int, areaId: Int = 0) = getProperty<Int>(propertyId, areaId)
    fun getFloatProperty(propertyId: Int, areaId: Int = 0) = getProperty<Float>(propertyId, areaId)
    fun getBooleanProperty(propertyId: Int, areaId: Int = 0) = getProperty<Boolean>(propertyId, areaId)
    fun getStringProperty(propertyId: Int, areaId: Int = 0) = getProperty<String>(propertyId, areaId)

    /**
     * Try to register Car Property. Checks if Property is available beforehand.
     */
    fun getCarPropertyUpdates(propertyId: Int): Boolean {

        if (_registeredProperties.contains(propertyId)) return true

        // Debug condition to emulate Polestar 4
        // if (emulatorMode && debugTemperatureAttempt < 2 && propertyId == CarProperties.ENV_OUTSIDE_TEMPERATURE) {
        //     debugTemperatureAttempt++
        //     return false
        // }

        if (!checkPropertyAvailability(propertyId, 0)) { return false }

        // This crashes on the Polestar 2 Emulator
        // return carPropertyManager.subscribePropertyEvents(
        //     propertyId,
        //     (CarProperties.sensorRateMap[propertyId])?:0f,
        //     carPropertyListener
        // )

        if (carPropertyManager.registerCallback(
            carPropertyListener,
            propertyId,
            (CarProperties.sensorRateMap[propertyId])?:0f
        )) {
            _registeredProperties.add(propertyId)
            InAppLogger.d("[CarPropertiesClient] $propertyId registered")
            return true
        }

        return false
    }

    fun disconnect() {
        car.disconnect()
    }
}