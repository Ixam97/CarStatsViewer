package com.ixam97.carStatsViewer.map

import android.util.Log
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.compose.components.CarGradientButton
import com.ixam97.carStatsViewer.database.tripData.DrivingSession
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxDelicateApi
import com.mapbox.maps.dsl.cameraOptions
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import com.mapbox.maps.plugin.attribution.attribution
import com.mapbox.maps.plugin.scalebar.scalebar

// this is the real Mapbox class
object Mapbox: MapboxInterface {

    private val gothenburgLocation: Point = Point.fromLngLat(11.964375837172494,57.71544764178327)
    // private val ystadLocation: Point = Point.fromLngLat(13.848338959636092, 55.42557254430007)

    override fun isDummy(): Boolean {
        return false
    }

    private lateinit var initialCameraListener: View.OnLayoutChangeListener
    private lateinit var polylineAnnotationManager: PolylineAnnotationManager

    @OptIn(MapboxDelicateApi::class)
    @Composable
    override fun MapBoxContainer(modifier: Modifier, trip: DrivingSession?) {

        var coordinates = listOf<Point>()
        var updateViewport by remember { mutableStateOf(false) }

        trip?.let {
            if (!it.drivingPoints.isNullOrEmpty()) {
                val coordinatesList = mutableListOf<Point>()
                it.drivingPoints!!.forEach { point ->
                    if (point.lon != null && point.lat != null) {
                        coordinatesList.add(Point.fromLngLat(point.lon.toDouble(), point.lat.toDouble()))
                    }
                }
                if (coordinatesList.isNotEmpty()) {
                    coordinates = coordinatesList
                }
            }
        }

        val polylineAnnotationOptions: PolylineAnnotationOptions = PolylineAnnotationOptions()
            .withPoints(coordinates)
            .withLineColor(MaterialTheme.colors.primary.toArgb())
            .withLineWidth(6.0)

        val polylineAnnotationOptionsBackground: PolylineAnnotationOptions = PolylineAnnotationOptions()
            .withPoints(coordinates)
            .withLineColor(MaterialTheme.colors.surface.toArgb())
            .withLineWidth(10.0)

        initialCameraListener = View.OnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
            updateViewport = true
            // view.removeOnLayoutChangeListener(initialCameraListener)
        }

        Box(
            modifier = modifier
                .fillMaxSize()
        ) {
            AndroidView(
                factory = { context ->
                    MapView(context).apply {
                        attribution.getMapAttributionDelegate().telemetry().apply {
                            userTelemetryRequestState = false
                            disableTelemetrySession()
                        }
                        scalebar.enabled = false
                        mapboxMap.loadStyle("mapbox://styles/ixam97/clfekq5z500hu01mx8s0g54gu")
                        // mapboxMap.loadStyle(style = Style.DARK)

                        val defaultCameraOptions = CameraOptions.Builder()
                            .center(gothenburgLocation)
                            .zoom(13.0)
                            .build()
                        mapboxMap.setCamera(defaultCameraOptions)

                        polylineAnnotationManager = annotations.createPolylineAnnotationManager()
                        polylineAnnotationManager.create(polylineAnnotationOptionsBackground)
                        polylineAnnotationManager.create(polylineAnnotationOptions)

                        addOnLayoutChangeListener(initialCameraListener)
                    }
                },
                update = { mapView ->
                    Log.d("MAP VIEW", "update")
                    if (updateViewport) {
                        val newCameraOptions = mapView.mapboxMap.cameraForCoordinates(
                            coordinates = coordinates,
                            camera = cameraOptions {  },
                            coordinatesPadding = EdgeInsets(170.0,50.0,50.0,50.0),
                            maxZoom = null,
                            offset = null
                        )
                        mapView.mapboxMap.setCamera(newCameraOptions)
                        updateViewport = false
                    }
                }
            )

            CarGradientButton (
                modifier = Modifier
                    .padding(20.dp),
                onClick = {
                    updateViewport = true
                }
            ) {
                Icon(
                    painterResource(id = R.drawable.ic_distance),
                    tint = Color.White,
                    contentDescription = null
                )
            }
        }
    }
}