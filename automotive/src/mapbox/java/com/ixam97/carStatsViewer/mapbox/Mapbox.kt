package com.ixam97.carStatsViewer.mapbox

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mapbox.geojson.Point
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.style.MapStyle

// this is the real Mapbox class
object Mapbox: MapboxInterface {
    override fun isDummy() = false

    @Composable
    override fun MapBoxContainer(modifier: Modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            MapboxMap(
                modifier = Modifier.fillMaxSize(),
                mapViewportState = rememberMapViewportState {
                    setCameraOptions {
                        center(Point.fromLngLat(11.957314, 57.710032))
                        zoom(12.0)
                    }
                },
                // style = { MapStyle("mapbox://styles/ixam97/clfekq5z500hu01mx8s0g54gu") },
                scaleBar = {}
            )
        }
    }
}