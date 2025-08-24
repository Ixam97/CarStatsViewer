package com.ixam97.carStatsViewer.map

import android.location.Geocoder
import androidx.compose.foundation.layout.Box
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.database.tripData.DrivingSession
import com.ixam97.carStatsViewer.utils.InAppLogger
import java.util.Locale

interface MapboxInterface {
    fun isDummy(): Boolean

    @Composable
    fun MapBoxContainer(
        modifier: Modifier,
        trip: DrivingSession?,
        chargingMarkerOnClick: ((id: Long) -> Unit)
    ) {
        Box (
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text("No Mapbox API configured!")
        }

    }

    // Using the built in Android Geocoder
    suspend fun getAddress(lon: Double, lat: Double): String {
        if (!Geocoder.isPresent()) return ("%.6f, %.6f".format(lat, lon))

        val geocoder = Geocoder(CarStatsViewer.appContext, Locale.getDefault())
        val result =
            try {
                // Deprecated with API level 33, but not available in lower API levels.
                // Therefore suspend fun. TODO: Implement new function for API 33+
                geocoder.getFromLocation(lat, lon, 1)
            } catch (e: Throwable) {
                InAppLogger.e("[Geo] Unable to get address from geocoder:")
                InAppLogger.e(e.stackTraceToString())
                null
            }

        return if (!result.isNullOrEmpty()) {
            result[0].getAddressLine(0)
        } else {
            ("%.6f, %.6f".format(lat, lon))
        }
    }
}