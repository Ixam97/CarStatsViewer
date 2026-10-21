package com.ixam97.carStatsViewer.utils

import android.car.Car
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat.checkSelfPermission
import kotlin.Int
import kotlin.Pair
import kotlin.String

val PERMISSIONS_BY_SDK = arrayOf<Pair<Int, String>>(
    Build.VERSION_CODES.BASE to Car.PERMISSION_ENERGY,
    Build.VERSION_CODES.BASE to Car.PERMISSION_SPEED,
    Build.VERSION_CODES.BASE to android.Manifest.permission.ACCESS_FINE_LOCATION,
    Build.VERSION_CODES.BASE to android.Manifest.permission.ACCESS_COARSE_LOCATION,
    Build.VERSION_CODES.BASE to android.Manifest.permission.ACCESS_BACKGROUND_LOCATION,
    Build.VERSION_CODES.TIRAMISU to android.Manifest.permission.POST_NOTIFICATIONS
)

fun unGrantedPermissions(context: Context): List<String> {
    return PERMISSIONS_BY_SDK.filter {
        it.first <= Build.VERSION.SDK_INT
                && checkSelfPermission(context, it.second) != PackageManager.PERMISSION_GRANTED
                && it.second != android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
    }.map { it.second }
}

fun Context.hasLocationPermission(): Boolean {
    return checkSelfPermission(
        this,
        android.Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(
        this,
        android.Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}

fun Context.hasVehiclePermissions() : Boolean {
    return checkSelfPermission(
        this,
        Car.PERMISSION_ENERGY
    ) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(
        this,
        Car.PERMISSION_SPEED
    ) == PackageManager.PERMISSION_GRANTED
}

fun Context.hasBackgroundLocationPermission() : Boolean {
    return checkSelfPermission(
        this,
        android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}