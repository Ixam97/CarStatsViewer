package com.ixam97.carStatsViewer.map

import android.location.Geocoder
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.database.tripData.DrivingSession
import com.ixam97.carStatsViewer.utils.InAppLogger
import de.ixam97.carcompose.theme.CarTheme
import kotlinx.coroutines.flow.Flow
import java.util.Locale

interface MapboxInterface {

    sealed class MapboxAction {
        data class ZoomToLocation(val location: MapboxLocation): MapboxAction()
        data object Reset: MapboxAction()
    }

    data class MapPoint(
        val lon: Double,
        val lat: Double
    )

    data class MapboxLocation(
        val lon: Double,
        val lat: Double,
        val zoom: Double = 15.5,
        val pitch: Double = 30.0,
        val bearing: Double = 0.0
    )

    data class ChargingLocation(
        val point: MapPoint,
        val chargingSessionId: Long
    )

    data class MapTripDataState(
        val tripPoints: List<MapPoint> = listOf(),
        val stopPoints: List<MapPoint> = listOf(),
        val chargingLocations: List<ChargingLocation> = listOf()
    ) {
        companion object {

            fun createStateFromTrip(trip: DrivingSession): MapTripDataState {
                val tripPoints = trip.drivingPoints?.let { drivingPoints ->
                    drivingPoints
                        .filter { it.lat != null && it.lon != null }
                        .map { MapPoint(it.lon!!.toDouble(), it.lat!!.toDouble()) }
                } ?: listOf<MapPoint>()

                val stopPoints = trip.drivingPoints?.let { drivingPoints ->
                    val mappedPoints = drivingPoints
                        .filter { it.point_marker_type == 2 && it.lat != null && it.lon != null }
                        .map { MapPoint(it.lon!!.toDouble(), it.lat!!.toDouble()) }
                    if (mappedPoints.size > 1) mappedPoints.subList(0, mappedPoints.size - 2) else listOf()
                } ?: listOf<MapPoint>()

                val chargingLocations = trip.chargingSessions?.let { chargingSessions ->
                    chargingSessions
                        .filter { it.lat != null && it.lon != null && (it.end_epoch_time ?: 0) > 0 }
                        .map {
                            ChargingLocation(
                                point = MapPoint(it.lon!!.toDouble(), it.lat!!.toDouble()),
                                chargingSessionId = it.charging_session_id
                            )
                        }
                } ?: listOf<ChargingLocation>()

                return MapTripDataState(
                    tripPoints = tripPoints,
                    stopPoints = stopPoints,
                    chargingLocations = chargingLocations
                )
            }
        }
    }

    fun isDummy(): Boolean

    @Composable
    fun MapboxContainer(
        modifier: Modifier,
        mapTripDataState: MapTripDataState,
        chargingMarkerOnClick: ((id: Long) -> Unit),
        useDarkMap: Boolean = CarTheme.useDarkTheme,
        actionFlow: Flow<MapboxAction>? = null
    ) {
        Box (
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No Mapbox API configured!",
                style = CarTheme.carTypography.rowTitle
            )
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

        if (!result.isNullOrEmpty()) {
            val thoroughfare: String? = result[0].thoroughfare
            val subThoroughfare: String? = result[0].subThoroughfare
            val postalCode: String? = result[0].postalCode
            val locality: String? = result[0].locality

            if (subThoroughfare == null || thoroughfare == null || postalCode == null)
                return result[0].getAddressLine(0)

            val road = result[0].getAddressLine(0).split(',').find {
                it.contains(thoroughfare) || it.contains(subThoroughfare)
            }?.trim(',')?.trim()
            val city = locality ?: result[0].getAddressLine(0).substringAfterLast(postalCode).substringBefore(',').trim()
//            InAppLogger.d("${result} \n ${result[0].locality}")
//            result[0].getAddressLine(0).substringAfterLast(result[0].postalCode).substringBefore(',').trim()
            return if (road != null) "$road, $city"
            else result[0].getAddressLine(0)
        } else {
            return ("%.6f, %.6f".format(lat, lon))
        }
    }
}