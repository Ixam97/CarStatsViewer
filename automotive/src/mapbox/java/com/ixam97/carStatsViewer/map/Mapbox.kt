package com.ixam97.carStatsViewer.map

import android.content.pm.FeatureInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.compose.components.CarGradientButton
import com.ixam97.carStatsViewer.compose.theme.CarTheme
import com.ixam97.carStatsViewer.database.tripData.DrivingSession
import com.ixam97.carStatsViewer.utils.InAppLogger
import com.mapbox.common.MapboxOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapboxDelicateApi
import com.mapbox.maps.coroutine.awaitCameraForCoordinates
import com.mapbox.maps.dsl.cameraOptions
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.MapboxMapComposable
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.IconImage
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotation
import com.mapbox.maps.extension.compose.annotation.generated.PolylineAnnotationGroup
import com.mapbox.maps.extension.compose.annotation.rememberIconImage
import com.mapbox.maps.extension.compose.style.MapStyle
import com.mapbox.maps.plugin.animation.MapAnimationOptions.Companion.mapAnimationOptions
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.attribution.attribution
import com.mapbox.maps.plugin.gestures.gestures
import de.ixam97.carcompose.components.controls.CarButtonDefaults
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

@Composable
private fun checkGlVersion(): Long {
    LocalContext.current.packageManager.systemAvailableFeatures.let { featureInfos ->
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
    private val ystadLocation: Point = Point.fromLngLat(13.848338959636092, 55.42557254430007)

    data class ChargingLocation(
        val point: Point,
        val chargingSessionId: Long
    )

    override fun isDummy(): Boolean {
        return false
    }

    @Composable
    @MapboxMapComposable
    private fun StartLocationMarker(
        point: Point,
        onClick: () -> Unit = {}
    ){
        val marker = rememberIconImage(key = "start-marker", painterResource(R.drawable.ic_trip_start))
        LocationMarkerImpl(point, onClick, marker)
    }

    @Composable
    @MapboxMapComposable
    private fun DestinationLocationMarker(
        point: Point,
        onClick: () -> Unit = {}
    ){
        val marker = rememberIconImage(key = "destination-marker", painterResource(R.drawable.ic_trip_destination))
        LocationMarkerImpl(point, onClick, marker)
    }

    @Composable
    @MapboxMapComposable
    private fun ChargeLocationMarker(
        point: Point,
        onClick: () -> Unit = {}
    ){
        val marker = rememberIconImage(key = "charge-marker", painterResource(R.drawable.ic_trip_charging_location))
        LocationMarkerImpl(point, onClick, marker)
    }

    @Composable
    @MapboxMapComposable
    private fun LocationMarkerImpl(
        point: Point,
        onClick: () -> Unit = {},
        marker: IconImage
    ){
        PointAnnotation(point = point) {
            iconImage = marker
            iconOffset = listOf(0.0, -21.5)
            interactionsState.onClicked {
                onClick()
                true
            }
        }
    }

    @Composable
    @MapboxMapComposable
    private fun TripPolyline(
        points: List<Point>
    ) {
        PolylineAnnotationGroup(
            annotations = listOf(
                PolylineAnnotationOptions()
                    .withPoints(points)
                    .withLineColor(CarStatsViewer.appContext.getColor(R.color.polestar_orange_outline))
                    .withLineWidth(10.0),
                PolylineAnnotationOptions()
                    .withPoints(points)
                    .withLineColor(CarStatsViewer.appContext.getColor(R.color.polestar_orange))
                    .withLineWidth(6.0),
            )
        )
    }

    @OptIn(MapboxDelicateApi::class)
    @Composable
    override fun MapBoxContainer(
        modifier: Modifier,
        trip: DrivingSession?,
        chargingMarkerOnClick: (id: Long) -> Unit,
        actionFlow: Flow<MapboxInterface.MapboxAction>?,
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

        var tripPoints by remember { mutableStateOf(listOf<Point>()) }
        var chargingLocations by remember { mutableStateOf(listOf<ChargingLocation>()) }

        LaunchedEffect(trip) {
            trip?.let { trip ->
                trip.drivingPoints?.let { drivingPoints ->
                    tripPoints = drivingPoints
                        .filter { it.lat != null && it.lon != null }
                        .map { Point.fromLngLat(it.lon!!.toDouble(), it.lat!!.toDouble()) }
                }
                trip.chargingSessions?.let { chargingSessions ->
                    chargingLocations = chargingSessions
                        .filter { it.lat != null && it.lon != null && (it.end_epoch_time?:0) > 0 }
                        .map { ChargingLocation(
                            point = Point.fromLngLat(it.lon!!.toDouble(), it.lat!!.toDouble()),
                            chargingSessionId =  it.charging_session_id
                        ) }
                }
            }
        }

        var tripCameraPosition by remember { mutableStateOf(
            cameraOptions {
                center(gothenburgLocation)
                zoom(13.0)
            }
        ) }

        val slowEasingAnimationOptions = mapAnimationOptions { duration(2500) }

        val mapViewPortState = rememberMapViewportState { setCameraOptions(tripCameraPosition) }

        fun changeZoom(zoomChange: Double) {
            mapViewPortState.cameraState?.let { cameraState ->
                mapViewPortState.easeTo(cameraOptions {
                    center(cameraState.center)
                    zoom(cameraState.zoom + zoomChange)
                })
            }
        }

        fun resetZoom() {
            mapViewPortState.flyTo(tripCameraPosition, slowEasingAnimationOptions)
        }

        val lifecycleOwner = LocalLifecycleOwner.current
        LaunchedEffect(lifecycleOwner.lifecycle, actionFlow) {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                withContext(Dispatchers.Main.immediate) {
                    actionFlow?.collect { action ->
                        mapViewPortState.flyTo(when (action) {
                            MapboxInterface.MapboxAction.Reset -> tripCameraPosition
                            is MapboxInterface.MapboxAction.ZoomToLocation -> cameraOptions {
                                center(Point.fromLngLat(action.location.lon, action.location.lat))
                                zoom(action.location.zoom)
                            }
                        }, slowEasingAnimationOptions )
                    }
                }
            }
        }

        Box(
            modifier = modifier
                .fillMaxSize(),
            contentAlignment = Alignment.BottomEnd
        ) {
            val glVersion = checkGlVersion()
            InAppLogger.d("GL Version: $glVersion")

            if (glVersion >= 3) {

                val pxEdgeInsets = with(LocalDensity.current) {70.dp.toPx()}.toDouble()
                val pxMapButtonInset = with(LocalDensity.current){95.dp.toPx()}.toDouble()

                MapboxMap(
                    style = { MapStyle(style = "mapbox://styles/ixam97/clfekq5z500hu01mx8s0g54gu") },
                    mapViewportState = mapViewPortState,
                    scaleBar = {},
                    compass = {}
                ) {
                    MapEffect(trip, tripPoints) { mapView ->
                        mapView.apply {
                            gestures.pitchEnabled = false
                            gestures.rotateEnabled = false
                            try {
                                attribution.getMapAttributionDelegate().telemetry().apply {
                                    userTelemetryRequestState = false
                                    disableTelemetrySession()
                                }
                                attribution.getMapAttributionDelegate().geofencingConsent().apply {
                                    setUserConsent(
                                        false,
                                        callback = {}
                                    )
                                }
                            } catch (e: Exception) {
                                InAppLogger.w("Map Attributions not yet available!\n${e.message}")
                            }
                        }

                        if (tripPoints.isNotEmpty()) {
                            tripCameraPosition = mapView.mapboxMap.awaitCameraForCoordinates(
                                coordinates = tripPoints,
                                camera = cameraOptions {  },
                                coordinatesPadding =  EdgeInsets(pxEdgeInsets,pxEdgeInsets,pxEdgeInsets,pxEdgeInsets + pxMapButtonInset),
                                maxZoom = 14.0
                            )
                        }
                        mapViewPortState.flyTo(tripCameraPosition, mapAnimationOptions { duration(0) })
                    }

                    if (tripPoints.isNotEmpty()) {
                        TripPolyline(tripPoints)
                        if (chargingLocations.isNotEmpty()) {
                            chargingLocations.forEach {
                                ChargeLocationMarker(
                                    point = it.point,
                                    onClick = { chargingMarkerOnClick(it.chargingSessionId) }
                                )
                            }
                        }
                        StartLocationMarker(tripPoints.first())
                        DestinationLocationMarker(tripPoints.last())
                    }
                }

                if (trip != null && tripPoints.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            modifier = Modifier
                                .padding(100.dp)
                                .background(Color(0xFF141516))
                                .padding(20.dp),
                            text = "Trip contains no Location Data!",
                            color = Color.White,
                            fontSize = 30.sp
                        )
                    }
                }

            } else {
                Column (
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        modifier = Modifier
                            .background(Color.DarkGray)
                            .padding(50.dp),
                        text = "Open GL 3.0 not supported!",
                        color = Color.White,
                        fontSize = 30.sp
                    )
                    Text(
                        modifier = Modifier.padding(15.dp),
                        text = "${mapViewPortState.cameraState?.center}",
                        color = Color.White,
                        fontSize = 30.sp
                    )
                }
            }

            if (useCarCompose) {
                MapControls(
                    onResetZoom = { resetZoom() },
                    onZoomIn = { changeZoom(1.0) },
                    onZoomOut = { changeZoom(-1.0) }
                )
            } else {
                Column(
                    modifier = Modifier
                        .padding(15.dp)
                ) {
                    CarGradientButton(
                        modifier = Modifier.size(65.dp),
                        contentPadding = PaddingValues(0.dp),
                        onClick = { resetZoom() }
                    ) {
                        Icon(
                            painterResource(id = R.drawable.ic_distance),
                            tint = Color.White,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Spacer(Modifier.size(15.dp))
                    CarGradientButton(
                        modifier = Modifier.size(65.dp),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(
                            topStart = CarTheme.buttonCornerRadius,
                            topEnd = CarTheme.buttonCornerRadius
                        ),
                        onClick = { changeZoom(1.0) }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            tint = Color.White,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Spacer(Modifier.size(4.dp))
                    CarGradientButton(
                        modifier = Modifier.size(65.dp),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(
                            bottomStart = CarTheme.buttonCornerRadius,
                            bottomEnd = CarTheme.buttonCornerRadius
                        ),
                        onClick = { changeZoom(-1.0) }
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

@Composable
private fun MapControls(
    onResetZoom: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit
) {
    val buttonShadow = Shadow(
        radius = 12.dp,
        spread = 0.dp,
        color = Color(0x90000000)
    )

    Column(
        modifier = Modifier.padding(15.dp)
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .dropShadow(
                    shape = CarButtonDefaults.shape,
                    shadow = buttonShadow
                )
                .clip(CarButtonDefaults.shape)
                .background(CarButtonDefaults.colors.backgroundBrush)
                .clickable(onClick = onResetZoom),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(de.ixam97.carcompose.theme.CarTheme.carDimensions.iconButtonSize),
                painter = painterResource(R.drawable.ic_distance),
                contentDescription = null,
                tint = CarButtonDefaults.colors.textColor
            )
        }
        Spacer(Modifier.size(de.ixam97.carcompose.theme.CarTheme.carDimensions.defaultVerticalPadding))

        Column(
            modifier = Modifier
                .width(IntrinsicSize.Min)
                .dropShadow(
                    shape = CarButtonDefaults.shape,
                    shadow = buttonShadow
                )
                .clip(CarButtonDefaults.shape)
                .background(CarButtonDefaults.colors.backgroundBrush)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clickable(onClick = onZoomIn),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.size(de.ixam97.carcompose.theme.CarTheme.carDimensions.iconButtonSize),
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = CarButtonDefaults.colors.textColor
                )
            }
            Spacer(Modifier
                .height(2.dp)
                .fillMaxWidth()
                .padding(horizontal = 15.dp)
                .background(de.ixam97.carcompose.theme.CarTheme.carColors.onSurface.copy(alpha = 0.3f))
            )
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clickable(onClick = onZoomOut),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.size(de.ixam97.carcompose.theme.CarTheme.carDimensions.iconButtonSize),
                    imageVector = Icons.Default.Remove,
                    contentDescription = null,
                    tint = CarButtonDefaults.colors.textColor
                )
            }
        }
    }
}