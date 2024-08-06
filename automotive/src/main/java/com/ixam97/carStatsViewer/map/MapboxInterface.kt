package com.ixam97.carStatsViewer.map

import android.location.Geocoder
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.database.tripData.DrivingSession
import java.util.Locale

interface MapboxInterface {
    fun isDummy(): Boolean

    @Composable
    fun MapBoxContainer(
        modifier: Modifier,
        trip: DrivingSession?
    )

    // Using the built in Android Geocoder
    suspend fun getAddress(lon: Double, lat: Double): String {
        if (!Geocoder.isPresent()) return ("$lat, $lon")

        val geocoder = Geocoder(CarStatsViewer.appContext, Locale.getDefault())
        // Deprecated with API level 33, but not available in lower API levels. Therefore suspend fun.
        val result = geocoder.getFromLocation(lat, lon, 1)

        if (!result.isNullOrEmpty()) {
            return result[0].getAddressLine(0)
        } else {
            return ("$lat, $lon")
        }
    }
}