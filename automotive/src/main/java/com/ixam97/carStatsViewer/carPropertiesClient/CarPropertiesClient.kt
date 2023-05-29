package com.ixam97.carStatsViewer.carPropertiesClient

import android.car.Car
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context

class CarPropertiesClient(
    context: Context,
    private val propertiesProcessor: (propertyId: Int) -> Unit,
    private val carPropertiesData: CarPropertiesData
) {

    private var car = Car.createCar(context)
    private var carPropertyManager = car.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager

    fun <T>getProperty(propertyId: Int, areaId: Int = 0) = carPropertyManager.getProperty<T>(propertyId, areaId)

    fun updateProperty(propertyId: Int) {
        carPropertiesData.update(carPropertyManager.getProperty<Any>(propertyId, 0), allowInvalidTimestamps = true)
        propertiesProcessor(propertyId)
    }

    fun getIntProperty(propertyId: Int, areaId: Int = 0) = carPropertyManager.getIntProperty(propertyId,areaId)
    fun getFloatProperty(propertyId: Int, areaId: Int = 0) = carPropertyManager.getFloatProperty(propertyId,areaId)
    fun getBooleanProperty(propertyId: Int, areaId: Int = 0) = carPropertyManager.getBooleanProperty(propertyId,areaId)
    fun getStringProperty(propertyId: Int, areaId: Int = 0) = carPropertyManager.getProperty<String>(propertyId,areaId).value as String

    fun getCarPropertiesUpdates(
        carPropertyIdsList: List<Int> = CarProperties.usedProperties
    ) {
        val carPropertyListener = object : CarPropertyManager.CarPropertyEventCallback {
            override fun onChangeEvent(carPropertyValue: CarPropertyValue<*>) {
                carPropertiesData.update(carPropertyValue)
                propertiesProcessor(carPropertyValue.propertyId)
            }
            override fun onErrorEvent(propertyId: Int, zone: Int) {
                throw Exception("Received error car property event, propId=$propertyId")
            }
        }

        for (propertyId in carPropertyIdsList) {
            carPropertyManager.registerCallback(
                carPropertyListener,
                propertyId,
                CarPropertyManager.SENSOR_RATE_ONCHANGE
            )
        }
    }

    fun disconnect() {
        car.disconnect()
    }
}