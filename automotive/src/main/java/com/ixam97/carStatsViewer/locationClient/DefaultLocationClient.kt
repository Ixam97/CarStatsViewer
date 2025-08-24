package com.ixam97.carStatsViewer.locationClient

// import com.google.android.gms.location.*
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.emulatorMode
import com.ixam97.carStatsViewer.utils.InAppLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.matthiaszimmermann.location.egm96.Geoid
import kotlin.math.absoluteValue

class DefaultLocationClient(): LocationClient {

    var locationNotAvailable = true

    private var doLocationUpdates: Boolean = false

    init {
        Geoid.init()

    }

    override fun stopLocationUpdates() {
        doLocationUpdates = false
    }

    @SuppressLint("MissingPermission")
    override fun getLocationUpdates(interval: Long, context: Context): Flow<Location?> {
        // val client = LocationServices.getFusedLocationProviderClient(context)

        return flow {
            InAppLogger.i("[LOC] Setting up location client")
            if (!context.hasLocationPermission()) {
                throw LocationClient.LocationException("Missing location permissions")
            }

            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            locationNotAvailable = !isGpsEnabled && !isNetworkEnabled

            if (locationNotAvailable) {
                throw LocationClient.LocationException("GPS is not enabled!")
            }

            doLocationUpdates = true

            while (doLocationUpdates) {
                if (!CarStatsViewer.appPreferences.useLocation) {
                    doLocationUpdates = false
                    break
                }
                var result = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

                if (result != null && result.altitude.absoluteValue > 0) {
                    result.altitude -= Geoid.getOffset(
                        org.matthiaszimmermann.location.Location(result.latitude, result.longitude)
                    )
                } else if (!emulatorMode) {
                    result = null
                    InAppLogger.e("[LOC] GPS Altitude is 0m!")
                }

                if (result == null) InAppLogger.w("[LOC] Location is null!")

                emit(result)
                delay(interval)
            }

            emit(null)
            InAppLogger.i("[LOC] Location tracking stopped")
        }
    }
}