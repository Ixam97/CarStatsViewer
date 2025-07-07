package com.ixam97.carStatsViewer.carPropertiesClient

import android.car.Car
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.car.hardware.property.PropertyNotAvailableErrorCode
import android.car.hardware.property.PropertyNotAvailableException
import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import com.ixam97.carStatsViewer.utils.InAppLogger

class CarPropertiesClient(
    context: Context,
    private val propertiesProcessor: (propertyId: Int) -> Unit,
    private val carPropertiesData: CarPropertiesData
) {

    private val carPropertyListener = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(carPropertyValue: CarPropertyValue<*>) {
            carPropertiesData.update(carPropertyValue)
            propertiesProcessor(carPropertyValue.propertyId)
        }
        override fun onErrorEvent(propertyId: Int, zone: Int) {
            throw Exception("Received error car property event, propId=$propertyId")
        }
    }

    private var car = Car.createCar(context)
    private var carPropertyManager = car.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager

    fun <T>getProperty(propertyId: Int, areaId: Int = 0): T? {
        try {
            val propertyValue = carPropertyManager.getProperty<T>(propertyId, areaId)
            // propertyValue.propertyStatus crashes Polestar 2 emulator
            if (propertyValue.status != CarPropertyValue.STATUS_AVAILABLE) {
                InAppLogger.w("[CarPropertiesClient] Property $propertyId is currently unavailable!")
                return null
            }
            return propertyValue.value
        } catch (e: Exception) {
            if (e is PropertyNotAvailableException) {
                val errorMsg = "[CarPropertiesClient] Property is not available: ${PropertyNotAvailableErrorCode.toString(e.detailedErrorCode)}"
                InAppLogger.e(errorMsg)
                Firebase.crashlytics.log(errorMsg)
            } else {
                throw e
            }
        }
        return null
    }

    fun updateProperty(propertyId: Int) {
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
    fun getCaPropertyUpdates(propertyId: Int): Boolean {

        if (!checkPropertyAvailability(propertyId, 0)) {
            return false
        }

        // This crashes on the Polestar 2 Emulator
        // return carPropertyManager.subscribePropertyEvents(
        //     propertyId,
        //     (CarProperties.sensorRateMap[propertyId])?:0f,
        //     carPropertyListener
        // )

        return carPropertyManager.registerCallback(
            carPropertyListener,
            propertyId,
            (CarProperties.sensorRateMap[propertyId])?:0f
        )
    }
/*
    fun getCarPropertiesUpdates(
        carPropertyIdsList: List<Int> = CarProperties.usedProperties
    ) {

        for (propertyId in carPropertyIdsList) {
            carPropertyManager.registerCallback(
                carPropertyListener,
                propertyId,
                (CarProperties.sensorRateMap[propertyId])?:0f
            )
        }
    }
*/
    fun disconnect() {
        car.disconnect()
    }
}