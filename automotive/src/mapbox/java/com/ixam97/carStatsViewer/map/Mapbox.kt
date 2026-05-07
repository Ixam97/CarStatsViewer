package com.ixam97.carStatsViewer.map

import android.content.Context
import android.content.pm.FeatureInfo
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.google.gson.GsonBuilder
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.compose.components.CarGradientButton
import com.ixam97.carStatsViewer.compose.theme.CarTheme
import com.ixam97.carStatsViewer.database.tripData.DrivingSession
import com.ixam97.carStatsViewer.utils.InAppLogger
import com.ixam97.carStatsViewer.utils.getBitmapFromVectorDrawable
import com.mapbox.common.MapboxOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxDelicateApi
import com.mapbox.maps.dsl.cameraOptions
import com.mapbox.maps.extension.style.expressions.dsl.generated.zoom
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.easeTo
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.OnPointAnnotationClickListener
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import com.mapbox.maps.plugin.attribution.attribution
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.scalebar.scalebar
import de.ixam97.carcompose.components.controls.CarButtonDefaults
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

private fun checkGlVersion(context: Context): Long {
    context.packageManager.systemAvailableFeatures.let { featureInfos ->
        featureInfos.forEach { featureInfo ->
            if (featureInfo.name == null) {
                return if (featureInfo.reqGlEsVersion != FeatureInfo.GL_ES_VERSION_UNDEFINED) {
                    ((featureInfo.reqGlEsVersion.toLong() and 0xffff0000) shr 16)
                } else 1
            }
        }
    }
    return 1
}

// this is the real Mapbox class
object Mapbox: MapboxInterface {

    private val gothenburgLocation: Point = Point.fromLngLat(11.964375837172494,57.71544764178327)
    // private val ystadLocation: Point = Point.fromLngLat(13.848338959636092, 55.42557254430007)

    override fun isDummy(): Boolean {
        return false
    }

    private lateinit var initialCameraListener: View.OnLayoutChangeListener
    private lateinit var polylineAnnotationManager: PolylineAnnotationManager
    private lateinit var pointAnnotationManager: PointAnnotationManager

    @OptIn(MapboxDelicateApi::class)
    @Composable
    override fun MapBoxContainer(
        modifier: Modifier,
        trip: DrivingSession?,
        chargingMarkerOnClick: (id: Long) -> Unit,
        zoomCoordinatesFlow: Flow<MapboxInterface.ZoomCoordinates?>?,
        useCarCompose: Boolean
    ) {

        if (MapboxOptions.accessToken.isBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(100.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    textAlign = TextAlign.Center,
                    text = "No Mapbox Access Token provided!\n\nPlease contact the maintainer of the App to fix this."
                )
            }
            return
        }

        val context = LocalContext.current

        var coordinates = listOf<Point>()
        var firstLoad = true
        var updateViewport by remember { mutableStateOf(false) }
        var zoomIn by remember { mutableStateOf(false) }
        var zoomOut by remember { mutableStateOf(false) }

        var dynamicCameraOptions by remember { mutableStateOf<CameraOptions?>(null) }

        val lifecycleOwner = LocalLifecycleOwner.current
        LaunchedEffect(lifecycleOwner.lifecycle, zoomCoordinatesFlow) {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                withContext(Dispatchers.Main.immediate) {
                    zoomCoordinatesFlow?.collect { zoomCoordinates ->
                        InAppLogger.d("Collecting Map Action!")
                        InAppLogger.d("ZoomCoordinates: ${zoomCoordinates?.lat}, ${zoomCoordinates?.lon}")
                        dynamicCameraOptions = if (zoomCoordinates != null) {
                            CameraOptions.Builder()
                                .center(Point.fromLngLat(zoomCoordinates.lon, zoomCoordinates.lat))
                                .zoom(zoomCoordinates.zoom)
                                .build()
                        } else null
                        updateViewport = true
                    }
                }
            }
        }

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
            .withLineColor(context.getColor(R.color.polestar_orange))
            .withLineWidth(6.0)

        val polylineAnnotationOptionsBackground: PolylineAnnotationOptions = PolylineAnnotationOptions()
            .withPoints(coordinates)
            .withLineColor(context.getColor(R.color.polestar_orange_outline))
            .withLineWidth(10.0)

        initialCameraListener = View.OnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
            updateViewport = true
            // view.removeOnLayoutChangeListener(initialCameraListener)
        }

        Box(
            modifier = modifier
                .fillMaxSize()
        ) {
            val defaultCameraOptions = CameraOptions.Builder()
                .center(gothenburgLocation)
                .zoom(13.0)
                .build()

            val context = LocalContext.current
            val glVersion = checkGlVersion(context)

            InAppLogger.d("GL Version: $glVersion")

            if (glVersion >= 3) {
                AndroidView(
                    factory = { context ->
                        val chargeMarkerBitmap = getBitmapFromVectorDrawable(
                            context,
                            R.drawable.ic_trip_charging_location
                        )
                        val destinationMarkerBitmap =
                            getBitmapFromVectorDrawable(context, R.drawable.ic_trip_destination)
                        val startMarkerBitmap =
                            getBitmapFromVectorDrawable(context, R.drawable.ic_trip_start)

                        MapView(context).apply {
                            gestures.rotateEnabled = false
                            gestures.pitchEnabled = false
                            compass.enabled = false
                            attribution.getMapAttributionDelegate().telemetry().apply {
                                userTelemetryRequestState = false
                                disableTelemetrySession()
                            }
                            scalebar.enabled = false
                            mapboxMap.loadStyle("mapbox://styles/ixam97/clfekq5z500hu01mx8s0g54gu")
                            // mapboxMap.loadStyle(style = Style.DARK)


                            mapboxMap.setCamera(defaultCameraOptions)

                            polylineAnnotationManager =
                                annotations.createPolylineAnnotationManager()
                            polylineAnnotationManager.create(polylineAnnotationOptionsBackground)
                            polylineAnnotationManager.create(polylineAnnotationOptions)

                            pointAnnotationManager = annotations.createPointAnnotationManager()
                            trip?.let {
                                val gson = GsonBuilder().create()
                                it.chargingSessions?.let { chargingSessions ->
                                    val completedChargingSessions =
                                        chargingSessions.filter { chargingSession ->
                                            chargingSession.end_epoch_time != null && chargingSession.end_epoch_time > 0
                                        }
                                    if (completedChargingSessions.isNotEmpty()) {
                                        completedChargingSessions.forEach { chargingSession ->
                                            if (chargingSession.lon != null && chargingSession.lat != null) {
                                                pointAnnotationManager.create(
                                                    PointAnnotationOptions()
                                                        .withPoint(
                                                            Point.fromLngLat(
                                                                chargingSession.lon.toDouble(),
                                                                chargingSession.lat.toDouble()
                                                            )
                                                        )
                                                        .withIconImage(chargeMarkerBitmap)
                                                        .withIconOffset(listOf(0.0, -27.0))
                                                        .withIconSize(0.7)
                                                        .withData(gson.toJsonTree(chargingSession.charging_session_id))
                                                )
                                            }
                                            pointAnnotationManager.addClickListener(
                                                OnPointAnnotationClickListener { annotation ->
                                                    if (annotation.getData()?.isJsonNull == false) {
                                                        println("Charging Session ID: ${annotation.getData()?.asLong}")
                                                        annotation.getData()?.asLong?.let { id ->
                                                            chargingMarkerOnClick(id)
                                                        }
                                                    }
                                                    true
                                                }
                                            )
                                        }
                                    }
                                }
                                if (coordinates.isNotEmpty()) {
                                    pointAnnotationManager.create(
                                        PointAnnotationOptions()
                                            .withPoint(coordinates.last())
                                            .withIconImage(destinationMarkerBitmap)
                                            .withIconOffset(listOf(0.0, -27.0))
                                            .withIconSize(0.7)
                                    )
                                    pointAnnotationManager.create(
                                        PointAnnotationOptions()
                                            .withPoint(coordinates.first())
                                            .withIconImage(startMarkerBitmap)
                                            .withIconOffset(listOf(0.0, -27.0))
                                            .withIconSize(0.7)
                                    )
                                }
                            }

                            addOnLayoutChangeListener(initialCameraListener)
                        }
                    },
                    update = { mapView ->

                        fun changeZoom(delta: Double) {
                            val currentZoom = mapView.mapboxMap.cameraState.zoom
                            val newCameraOptions = CameraOptions.Builder()
                                .zoom(currentZoom + delta)
                                .build()
                            mapView.mapboxMap.easeTo(newCameraOptions)
                        }

//                    InAppLogger.d("Map View Update")
                        if (updateViewport) {

                            val newCameraOptions = when {
                                coordinates.isNotEmpty() && dynamicCameraOptions == null -> {
                                    mapView.mapboxMap.cameraForCoordinates(
                                        coordinates = coordinates,
                                        camera = cameraOptions { },
                                        coordinatesPadding = EdgeInsets(50.0, 50.0, 50.0, 50.0),
                                        maxZoom = 14.0,
                                        offset = null
                                    )
                                }
                                // dynamicCameraOptions != null -> dynamicCameraOptions!!
                                else -> dynamicCameraOptions ?: defaultCameraOptions
                            }
                            if (firstLoad) {
                                mapView.mapboxMap.setCamera(newCameraOptions)
                                firstLoad = false
                            } else {
                                mapView.mapboxMap.easeTo(
                                    cameraOptions = newCameraOptions,
                                    animationOptions = MapAnimationOptions.mapAnimationOptions {
                                        duration(1000)
                                    }
                                )
                            }
                            updateViewport = false
                        }
                        if (zoomIn) {
                            changeZoom(+1.0)
                            zoomIn = false
                        }
                        if (zoomOut) {

                            changeZoom(-1.0)
                            zoomOut = false
                        }
                    }
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        modifier = Modifier
                            .background(Color.DarkGray)
                            .padding(50.dp),
                        text = "Open GL 3.0 not supported!",
                        color = Color.White,
                        fontSize = 30.sp
                    )
                }
            }

            Column (
                modifier = Modifier
                    .padding(15.dp)
            ) {

                if (useCarCompose){

                    Box(
                        modifier = Modifier
                            .clip(CarButtonDefaults.shape)
                            .background(CarButtonDefaults.colors.backgroundBrush)
                            .clickable {
                                dynamicCameraOptions = null
                                updateViewport = true
                            }
                            .padding(10.dp)
                    ) {
                        Icon(
                            modifier = Modifier
                                .size(50.dp),
                            painter = painterResource(R.drawable.ic_distance),
                            contentDescription = null,
                            tint = CarButtonDefaults.colors.textColor
                        )
                    }
                    Spacer(Modifier.size(15.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(
                                corner = de.ixam97.carcompose.theme.CarTheme.carShapes.defaultOuterCornerSize
                            ))
                            .background(CarButtonDefaults.colors.backgroundBrush)
                            .clickable { zoomIn = true }
                            .padding(10.dp)
                    ) {
                        Icon(
                            modifier = Modifier
                                .size(50.dp),
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = CarButtonDefaults.colors.textColor
                        )
                    }
                    Spacer(Modifier.size(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(
                                corner = de.ixam97.carcompose.theme.CarTheme.carShapes.defaultOuterCornerSize
                            ))
                            .background(CarButtonDefaults.colors.backgroundBrush)
                            .clickable { zoomOut = true }
                            .padding(10.dp)
                    ) {
                        Icon(
                            modifier = Modifier
                                .size(50.dp),
                            imageVector = Icons.Default.Remove,
                            contentDescription = null,
                            tint = CarButtonDefaults.colors.textColor
                        )
                    }
                } else {
                    CarGradientButton (
                        modifier = Modifier.size(65.dp),
                        contentPadding = PaddingValues(0.dp),
                        onClick = {
                            dynamicCameraOptions = null
                            updateViewport = true
                        }
                    ) {
                        Icon(
                            painterResource(id = R.drawable.ic_distance),
                            tint = Color.White,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Spacer(Modifier.size(15.dp))
                    CarGradientButton (
                        modifier = Modifier.size(65.dp),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(
                            topStart = CarTheme.buttonCornerRadius,
                            topEnd = CarTheme.buttonCornerRadius
                        ),
                        onClick = {
                            zoomIn = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            tint = Color.White,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Spacer(Modifier.size(4.dp))
                    CarGradientButton (
                        modifier = Modifier.size(65.dp),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(
                            bottomStart = CarTheme.buttonCornerRadius,
                            bottomEnd = CarTheme.buttonCornerRadius
                        ),
                        onClick = {
                            zoomOut = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Remove,
                            tint = Color.White,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }
        }
    }
}