package com.ixam97.carStatsViewer.services

import com.ixam97.carStatsViewer.*
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import com.google.android.gms.location.*


class LocCollector : Service() {
    companion object {
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate() {
        super.onCreate()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        getLocationUpdates()
        startLocationUpdates()
        InAppLogger.log("GPS started")
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
    }

    private lateinit var locationRequest: LocationRequest

    private lateinit var locationCallback: LocationCallback

    override fun onBind(intent: Intent): IBinder? {
        return null
    }


    private fun getLocationUpdates() {
        InAppLogger.log("getLocationUpdate")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest()
        locationRequest.interval = 10_000
        locationRequest.fastestInterval = 10_000
        locationRequest.smallestDisplacement = 170f // 170 m = 0.1 mile
        locationRequest.priority =
            LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return

                if (locationResult.locations.isNotEmpty()) {
                    // get latest location
                    val location = locationResult.lastLocation
                    val altitude = location.altitude
                    InAppLogger.log("LOCATION-altitude: " + altitude)
                }
            }
        }
    }

    //start location updates
    private fun startLocationUpdates() {
        if (checkLocPermission()) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                null /* Looper */
            )
        }
    }

    // stop location updates
    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    //TO-DO: build solid permission check and request for location
    private fun checkLocPermission(): Boolean {
        if (checkSelfPermission(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED){
        InAppLogger.log("Location Permission missing!")
            return false
        }

        return true
    }
}