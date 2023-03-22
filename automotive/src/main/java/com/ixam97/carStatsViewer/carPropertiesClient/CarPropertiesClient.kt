package com.ixam97.carStatsViewer.carPropertiesClient

import android.car.Car
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import com.ixam97.carStatsViewer.utils.InAppLogger
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class CarPropertiesClient(context: Context) {

    private var car = Car.createCar(context)
    private var carPropertyManager = car.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager

    fun <T>getProperty(propertyId: Int, areaId: Int = 0) = carPropertyManager.getProperty<T>(propertyId, areaId)
    fun getIntProperty(propertyId: Int, areaId: Int = 0) = carPropertyManager.getIntProperty(propertyId,areaId)
    fun getFloatProperty(propertyId: Int, areaId: Int = 0) = carPropertyManager.getFloatProperty(propertyId,areaId)
    fun getBooleanProperty(propertyId: Int, areaId: Int = 0) = carPropertyManager.getBooleanProperty(propertyId,areaId)
    fun getStringProperty(propertyId: Int, areaId: Int = 0) = carPropertyManager.getProperty<String>(propertyId,areaId).value as String

    fun getCarPropertiesUpdates(
        carPropertyIdsList: List<Int> = CarProperties.usedProperties,
        carPropertiesData: CarPropertiesData? = null
    ): Flow<CarProperty> {
        return callbackFlow {

            val carPropertyListener = object : CarPropertyManager.CarPropertyEventCallback {
                override fun onChangeEvent(carPropertyValue: CarPropertyValue<*>) {
                    carPropertiesData?.update(carPropertyValue)
                    val returnProperty = CarProperty(carPropertyValue.propertyId)
                    returnProperty.value = carPropertyValue.value
                    returnProperty.timestamp = carPropertyValue.timestamp
                    launch { send(returnProperty) }
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

            awaitClose {
                car.disconnect()
            }
        }
    }
}