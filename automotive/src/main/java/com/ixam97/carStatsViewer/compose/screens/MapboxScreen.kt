package com.ixam97.carStatsViewer.compose.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ixam97.carStatsViewer.mapbox.Mapbox

@Composable
fun MapboxScreen() {
    Mapbox.MapBoxContainer(modifier = Modifier)
}