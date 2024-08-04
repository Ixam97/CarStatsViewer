package com.ixam97.carStatsViewer.mapbox

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

// This is a dummy class
object Mapbox: MapboxInterface {
    override fun isDummy() = true

    @Composable
    override fun MapBoxContainer(modifier: Modifier) {
        Box (
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No Mapbox API configured!")
        }

    }
}