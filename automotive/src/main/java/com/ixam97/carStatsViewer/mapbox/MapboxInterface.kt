package com.ixam97.carStatsViewer.mapbox

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface MapboxInterface {
    fun isDummy(): Boolean

    @Composable
    fun MapBoxContainer(modifier: Modifier)
}