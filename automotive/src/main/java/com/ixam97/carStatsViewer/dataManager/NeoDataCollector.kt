package com.ixam97.carStatsViewer.dataManager

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.widget.Toast
import com.google.android.gms.location.LocationServices
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.activities.MainActivity
import com.ixam97.carStatsViewer.carPropertiesClient.CarProperties
import com.ixam97.carStatsViewer.carPropertiesClient.CarPropertiesClient
import com.ixam97.carStatsViewer.dataProcessor.DataProcessor
import com.ixam97.carStatsViewer.locationTracking.DefaultLocationClient
import com.ixam97.carStatsViewer.locationTracking.LocationClient
import com.ixam97.carStatsViewer.utils.InAppLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class NeoDataCollector: Service() {

    companion object {
        const val LIVE_DATA_TASK_INTERVAL = 5_000L
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var foregroundServiceNotification: Notification.Builder
    private lateinit var locationClient: LocationClient

    private lateinit var carPropertiesClient: CarPropertiesClient
    private lateinit var dataProcessor: DataProcessor

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        intent?.let {
            if (it.hasExtra("reason")) {
                Toast.makeText(applicationContext, getString(R.string.restart_toast_background), Toast.LENGTH_LONG).show()
            }
        }

        startForeground(CarStatsViewer.FOREGROUND_NOTIFICATION_ID + 10, foregroundServiceNotification.build())
        return START_NOT_STICKY
    }

    override fun onCreate() {
        super.onCreate()

        dataProcessor  = (applicationContext as CarStatsViewer).dataProcessor
        carPropertiesClient = CarPropertiesClient(
            context = applicationContext,
            propertiesProcessor = dataProcessor::processProperty,
            carPropertiesData = dataProcessor.carPropertiesData
        )

        foregroundServiceNotification = Notification.Builder(applicationContext, CarStatsViewer.FOREGROUND_CHANNEL_ID)
            // .setContentTitle(getString(R.string.app_name))
            .setContentTitle(getString(R.string.foreground_service_info) + " (Neo)")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)

        foregroundServiceNotification.setContentIntent(
            PendingIntent.getActivity(
                applicationContext,
                0,
                Intent(applicationContext, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )
        )

        locationClient = DefaultLocationClient(
            CarStatsViewer.appContext,
            LocationServices.getFusedLocationProviderClient(this)
        )

        locationClient
            .getLocationUpdates(5_000L)
            .catch { e ->
                InAppLogger.e("LocationClient: ${e.message}")
            }
            .onEach { location ->
                location?.let {
                    InAppLogger.d("Location: lat: %.5f, lon: %.5f, alt: %.2fm, time: %d".format(location.latitude, location.longitude, location.altitude, location.time))
                    dataProcessor.realTimeData = dataProcessor.realTimeData.copy(
                        lat = it.latitude.toFloat(),
                        lon = it.longitude.toFloat(),
                        alt = it.altitude.toFloat()
                    )
                }
            }
            .launchIn(serviceScope)

        CarStatsViewer.liveDataApis[0]
            .requestFlow(
                serviceScope,
                dataManager = DataManagers.CURRENT_TRIP.dataManager,
                LIVE_DATA_TASK_INTERVAL
            ).catch { e -> InAppLogger.e("requestFlow: ${e.message}") }
            .launchIn(serviceScope)

        CarStatsViewer.liveDataApis[1]
            .requestFlow(
                serviceScope,
                dataManager = DataManagers.CURRENT_TRIP.dataManager,
                LIVE_DATA_TASK_INTERVAL
            ).catch { e -> InAppLogger.e("requestFlow: ${e.message}") }
            .launchIn(serviceScope)

        carPropertiesClient.getCarPropertiesUpdates()

        dataProcessor.staticVehicleData = dataProcessor.staticVehicleData.copy(
            batteryCapacity = carPropertiesClient.getFloatProperty(CarProperties.INFO_EV_BATTERY_CAPACITY)
        )

        carPropertiesClient.updateProperty(CarProperties.GEAR_SELECTION)
        carPropertiesClient.updateProperty(CarProperties.IGNITION_STATE)
        carPropertiesClient.updateProperty(CarProperties.EV_BATTERY_LEVEL)
        carPropertiesClient.updateProperty(CarProperties.EV_CHARGE_PORT_CONNECTED)
        carPropertiesClient.updateProperty(CarProperties.ENV_OUTSIDE_TEMPERATURE)


    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        carPropertiesClient.disconnect()
    }

}