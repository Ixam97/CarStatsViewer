package com.ixam97.carStatsViewer.locationClient

import android.content.Context
import android.location.Location
import kotlinx.coroutines.flow.Flow

interface LocationClient {
    fun getLocationUpdates(interval: Long, context: Context): Flow<Location?>
    fun stopLocationUpdates()
    class LocationException(message: String): Exception(message)
}