package com.ixam97.carStatsViewer.map

import android.location.Geocoder
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.database.tripData.DrivingSession
import com.ixam97.carStatsViewer.utils.InAppLogger
import kotlinx.coroutines.flow.Flow
import java.util.Locale

interface MapboxInterface {

    data class MapboxLocation(
        val lon: Double,
        val lat: Double,
        val zoom: Double = 15.5,
        val pitch: Double = 30.0,
        val bearing: Double = 0.0
    )

    sealed class MapboxAction {
        data class ZoomToLocation(val location: MapboxLocation): MapboxAction()
        data object Reset: MapboxAction()
    }

    fun isDummy(): Boolean

    @Composable
    fun MapBoxContainer(
        modifier: Modifier,
        trip: DrivingSession?,
        chargingMarkerOnClick: ((id: Long) -> Unit),
        useBrightMap: Boolean = false,
        actionFlow: Flow<MapboxAction>? = null,
        useCarCompose: Boolean = false
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
            val thoroughfare = result[0].thoroughfare
            val subThoroughfare = result[0].subThoroughfare
            val postalCode = result[0].postalCode

            if (subThoroughfare == null || thoroughfare == null || postalCode == null)
                result[0].getAddressLine(0)

            val road = result[0].getAddressLine(0).split(',').find {
                it.contains(thoroughfare) || it.contains(subThoroughfare)
            }?.trim(',')?.trim()
            val city = result[0].locality ?: result[0].getAddressLine(0).substringAfterLast(postalCode).substringBefore(',').trim()
//            InAppLogger.d("${result} \n ${result[0].locality}")
//            result[0].getAddressLine(0).substringAfterLast(result[0].postalCode).substringBefore(',').trim()
            if (road != null) "$road, $city"
            else result[0].getAddressLine(0)
        } else {
            ("%.6f, %.6f".format(lat, lon))
        }
    }
}