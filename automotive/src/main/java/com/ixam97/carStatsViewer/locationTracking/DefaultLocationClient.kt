package com.ixam97.carStatsViewer.locationTracking

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import com.google.android.gms.location.*
import com.ixam97.carStatsViewer.utils.InAppLogger
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import org.matthiaszimmermann.location.egm96.Geoid

class DefaultLocationClient(
    private val context: Context,
    private val client: FusedLocationProviderClient
): LocationClient {

    var locationNotAvailable = true
    var lastTimeStamp = 0L

    init {
        Geoid.init()

    }

    @SuppressLint("MissingPermission")
    override fun getLocationUpdates(interval: Long): Flow<Location> {
        return callbackFlow {
            InAppLogger.log("Setting up location client")
            if (!context.hasLocationPermission()) {
                throw LocationClient.LocationException("Missing location permissions")
            }

            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            locationNotAvailable = !isGpsEnabled && !isNetworkEnabled

            val request = LocationRequest.create()
                .setInterval(interval)
                .setFastestInterval(interval / 2)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setMaxWaitTime(interval)

            InAppLogger.log("MaxWaitTime: ${request.maxWaitTime}")

            if (locationNotAvailable) {
                throw LocationClient.LocationException("GPS is not enabled!")
            }

            val locationCallback = object: LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    super.onLocationResult(locationResult)
                    locationResult.locations.lastOrNull()?.let { location ->
                        if (location.altitude > 0 || location.altitude < 0) {
                            location.altitude -= Geoid.getOffset(
                                org.matthiaszimmermann.location.Location(location.latitude, location.longitude)
                            )
                            if (location.time > lastTimeStamp + 5_000) {
                                InAppLogger.log("LocationClient: Interval exceeded!")
                            }
                            lastTimeStamp = location.time
                            launch { send(location) }
                        } else {
                            InAppLogger.log("LocationClient has returned altitude of 0m")
                        }

                    }
                }
            }

            client.requestLocationUpdates(
                request,
                locationCallback,
                Looper.getMainLooper()
            )

            InAppLogger.log("Location tracking started. isGpsEnabled: $isGpsEnabled, isNetworkEnabled: $isNetworkEnabled")

            awaitClose {
                client.removeLocationUpdates(locationCallback)
            }
        }
    }
}