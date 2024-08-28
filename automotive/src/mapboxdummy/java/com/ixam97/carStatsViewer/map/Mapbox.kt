package com.ixam97.carStatsViewer.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ixam97.carStatsViewer.database.tripData.DrivingSession

// This is a dummy class
object Mapbox: MapboxInterface {
    override fun isDummy() = true

    @Composable
    override fun MapBoxContainer(modifier: Modifier, trip: DrivingSession?) {
        Box (
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text("No Mapbox API configured!")
        }

    }
}