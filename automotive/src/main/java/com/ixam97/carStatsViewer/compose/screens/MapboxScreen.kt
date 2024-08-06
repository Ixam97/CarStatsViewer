package com.ixam97.carStatsViewer.compose.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.map.Mapbox

@Composable
fun MapboxScreen() {

    val trip = CarStatsViewer.dataProcessor.selectedSessionData

    var startAddr by remember { mutableStateOf("Unknown") }
    var destAddr by remember { mutableStateOf("Unknown") }

    Column {
        Mapbox.MapBoxContainer(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            trip = trip
        )
        Text(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 25.dp),
            text = "Start: $startAddr"
        )
        Text(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 25.dp),
            text = "Destination: $destAddr"
        )
    }

    LaunchedEffect(Unit) {
        if (trip != null) {
            if (!trip.drivingPoints.isNullOrEmpty()) {
                val coordinates = trip.drivingPoints!!.filter { it.lat != null }
                startAddr = Mapbox.getAddress(
                    coordinates.first().lon!!.toDouble(),
                    coordinates.first().lat!!.toDouble()
                )
                destAddr = Mapbox.getAddress(
                    coordinates.last().lon!!.toDouble(),
                    coordinates.last().lat!!.toDouble()
                )
            }
        }
    }
}