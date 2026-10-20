package com.ixam97.carStatsViewer.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.compose.theme.polestarOrange
import com.ixam97.carStatsViewer.utils.InAppLogger
import com.mapbox.common.MapboxOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapboxDelicateApi
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.coroutine.awaitCameraForCoordinates
import com.mapbox.maps.dsl.cameraOptions
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.style.MapStyle
import com.mapbox.maps.plugin.animation.MapAnimationOptions.Companion.mapAnimationOptions
import com.mapbox.maps.plugin.attribution.attribution
import com.mapbox.maps.plugin.gestures.gestures
import de.ixam97.carcompose.components.controls.CarButtonDefaults
import de.ixam97.carcompose.theme.CarTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

// this is the real Mapbox class
object Mapbox: MapboxInterface {

    private val gothenburgLocation: Point = Point.fromLngLat(11.964375837172494,57.71544764178327)
    private val ystadLocation: Point = Point.fromLngLat(13.848338959636092, 55.42557254430007)

    private val slowEasingAnimationOptions = mapAnimationOptions { duration(2500) }
    private val fastEasingAnimationOptions = mapAnimationOptions { duration(750) }

    private val mapStyleDarkBasic = "mapbox://styles/ixam97/clfekq5z500hu01mx8s0g54gu"
    private val mapStyleDusk3d = "mapbox://styles/ixam97/cmvd1v9yq003001s5df0d9e8l"
    private val mapStyleNight3d = "mapbox://styles/ixam97/cmvd2alrf000e01s21tenfyuw"
    private val mapStyleDay3d = "mapbox://styles/ixam97/cmvd0bucz008101s61n3qb9ks"
    private val mapStyleDawn3d = "mapbox://styles/ixam97/cmvfx9ygp008l01s21o5l782i"
    private val mapStyleSatellite = "mapbox://styles/ixam97/cmvgjbqmk00a601s216cifzmu"

    override fun isDummy(): Boolean {
        return false
    }

    @OptIn(MapboxDelicateApi::class, MapboxExperimental::class)
    @Composable
    override fun MapboxContainer(
        modifier: Modifier,
        mapTripDataState: MapboxInterface.MapTripDataState,
        chargingMarkerOnClick: (id: Long) -> Unit,
        useDarkMap: Boolean,
        actionFlow: Flow<MapboxInterface.MapboxAction>?
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
                    text = "No Mapbox Access Token provided!\n\nPlease contact the maintainer of the App to fix this.",
                    style = CarTheme.carTypography.rowTitle
                )
            }
            return
        }

        var satelliteTheme by remember { mutableStateOf(false) }

        var tripCameraPosition by rememberSaveable { mutableStateOf(
            cameraOptions {
                center(gothenburgLocation)
                zoom(13.0)
            }
        ) }

        val mapViewportState = rememberMapViewportState { setCameraOptions(tripCameraPosition) }
        val mapTripAnnotationsState = rememberMapTripAnnotationsState(
            onLocationClick = {flyToPoint(mapViewportState, it)},
            onChargingLocationClick = chargingMarkerOnClick
        )

        val lifecycleOwner = LocalLifecycleOwner.current
        LaunchedEffect(lifecycleOwner.lifecycle, actionFlow) {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                withContext(Dispatchers.Main.immediate) {
                    actionFlow?.collect { action ->
                        mapViewportState.flyTo(when (action) {
                            MapboxInterface.MapboxAction.Reset -> tripCameraPosition
                            is MapboxInterface.MapboxAction.ZoomToLocation -> cameraOptions {
                                center(Point.fromLngLat(action.location.lon, action.location.lat))
                                zoom(action.location.zoom)
                                pitch(action.location.pitch)
                                bearing(action.location.bearing)
                            }
                        }, slowEasingAnimationOptions )
                    }
                }
            }
        }

        LaunchedEffect(tripCameraPosition) {
            mapViewportState.flyTo(
                cameraOptions = tripCameraPosition,
                animationOptions = fastEasingAnimationOptions
            )
        }

        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomEnd
        ) {
            if (CarStatsViewer.glVersion >= 3) {

                val pxEdgeInsets = with(LocalDensity.current) {70.dp.toPx()}.toDouble()
                val pxMapButtonInset = with(LocalDensity.current){95.dp.toPx()}.toDouble()

                MapboxMap(
                    style = { MapStyle(style = if (satelliteTheme) mapStyleSatellite else if (useDarkMap) mapStyleDusk3d else mapStyleDawn3d) },
                    mapViewportState = mapViewportState,
                    attribution = { Attribution(iconColor = polestarOrange) },
                    scaleBar = {},
                    compass = {}
                ) {
                    MapEffect( mapTripDataState) { mapView ->
                        mapView.apply {
                            gestures.pitchEnabled = true
                            gestures.rotateEnabled = true
                            try {
                                attribution.getMapAttributionDelegate().telemetry().apply {
                                    userTelemetryRequestState = false
                                    disableTelemetrySession()
                                }
                                attribution.getMapAttributionDelegate().geofencingConsent()
                                    .apply {
                                        setUserConsent(
                                            false,
                                            callback = {}
                                        )
                                    }
                            } catch (e: Exception) {
                                InAppLogger.w("Map Attributions not yet available!\n${e.message}")
                            }
                        }

                        mapTripAnnotationsState.updateState(mapTripDataState)

                        if (mapTripDataState.tripPoints.isNotEmpty()) {
                            tripCameraPosition = mapView.mapboxMap.awaitCameraForCoordinates(
                                coordinates = mapTripDataState.tripPoints.map { it.toPoint() },
                                camera = cameraOptions {
                                    pitch(0.0)
                                    bearing(0.0)
                                                       },
                                coordinatesPadding = EdgeInsets(
                                    pxEdgeInsets,
                                    pxEdgeInsets,
                                    pxEdgeInsets,
                                    pxEdgeInsets + pxMapButtonInset
                                ),
                                maxZoom = 14.0,
                            )
                        }
                    }
                    MapTripAnnotations(mapTripAnnotationsState)
                }

                if (mapTripDataState.tripPoints.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            modifier = Modifier
                                .padding(100.dp)
                                .background(Color(0xFF141516))
                                .padding(20.dp),
                            text = stringResource(R.string.summary_no_route),
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
                        text = "${mapViewportState.cameraState?.center}",
                        color = Color.White,
                        fontSize = 30.sp
                    )
                }
            }

            MapControls(
                onResetZoom = { resetZoom(mapViewportState, tripCameraPosition) },
                onResetBearing = { resetBearing(mapViewportState) },
                onZoomIn = { changeZoom(mapViewportState, 1.0) },
                onZoomOut = { changeZoom(mapViewportState, -1.0) },
                // onSatellite = { satelliteTheme = !satelliteTheme },
                showBearingButton = mapViewportState.cameraState?.let { it.bearing != 0.0 || it.pitch != 0.0 }?:false,
                bearingAngle = mapViewportState.cameraState?.bearing?.toFloat()?:0f,
                // satelliteSelected = satelliteTheme
            )
        }
    }

    private fun changeZoom(
        mapViewportState: MapViewportState,
        zoomChange: Double
    ) {
        mapViewportState.cameraState?.let { cameraState ->
            mapViewportState.easeTo(cameraOptions {
                center(cameraState.center)
                zoom(cameraState.zoom + zoomChange)
            })
        }
    }

    private fun resetZoom(
        mapViewportState: MapViewportState,
        tripCameraPosition: CameraOptions
    ) {
        mapViewportState.flyTo(tripCameraPosition, slowEasingAnimationOptions)
    }

    private fun resetBearing(
        mapViewportState: MapViewportState,
    ) {
        mapViewportState.easeTo(cameraOptions {
            pitch(0.0)
            bearing(0.0)
        })
    }

    private fun flyToPoint(
        mapViewportState: MapViewportState,
        point: Point
    ) {
        val location = MapboxInterface.MapboxLocation(
            lon = point.longitude(),
            lat = point.latitude()
        )
        mapViewportState.flyTo(
            cameraOptions {
                center(Point.fromLngLat(
                    location.lon,
                    location.lat
                ))
                zoom(location.zoom)
                pitch(location.pitch)
                bearing(location.bearing)
            },
            slowEasingAnimationOptions
        )
    }

    @Composable
    private fun MapControls(
        onResetZoom: () -> Unit,
        onResetBearing: () -> Unit,
        onZoomIn: () -> Unit,
        onZoomOut: () -> Unit,
        onSatellite: (() -> Unit)? = null,
        satelliteSelected: Boolean = false,
        showBearingButton: Boolean = false,
        bearingAngle: Float = 0f
    ) {
        val buttonShadow = Shadow(
            radius = 12.dp,
            spread = 0.dp,
            color = Color(0x90000000)
        )

        Column(
            modifier = Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(CarTheme.carDimensions.defaultVerticalPadding)
        ) {
            if (showBearingButton) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .dropShadow(
                            shape = CarButtonDefaults.shape,
                            shadow = buttonShadow
                        )
                        .clip(CarButtonDefaults.shape)
                        .background(CarButtonDefaults.colors.backgroundBrush)
                        .clickable(onClick = onResetBearing),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier
                            .size(CarTheme.carDimensions.iconButtonSize)
                            .rotate((bearingAngle * -1f) - 45f),
                        painter = painterResource(R.drawable.ic_bearing),
                        contentDescription = null,
                        tint = CarButtonDefaults.colors.textColor
                    )
                }
            }

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
                    modifier = Modifier.size(CarTheme.carDimensions.iconButtonSize),
                    painter = painterResource(R.drawable.ic_distance),
                    contentDescription = null,
                    tint = CarButtonDefaults.colors.textColor
                )
            }

            if (onSatellite != null) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .dropShadow(
                            shape = CarButtonDefaults.shape,
                            shadow = buttonShadow
                        )
                        .clip(CarButtonDefaults.shape)
                        .background(CarButtonDefaults.colors.backgroundBrush)
                        .clickable(onClick = onSatellite),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier.size(CarTheme.carDimensions.iconButtonSize),
                        painter = painterResource(R.drawable.ic_satellite),
                        contentDescription = null,
                        tint = if (satelliteSelected) CarTheme.carColors.accent else CarButtonDefaults.colors.textColor
                    )
                }
            }

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
                        modifier = Modifier.size(CarTheme.carDimensions.iconButtonSize),
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = CarButtonDefaults.colors.textColor
                    )
                }
                Spacer(Modifier
                    .height(2.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp)
                    .background(CarTheme.carColors.onSurface.copy(alpha = 0.3f))
                )
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clickable(onClick = onZoomOut),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier.size(CarTheme.carDimensions.iconButtonSize),
                        imageVector = Icons.Default.Remove,
                        contentDescription = null,
                        tint = CarButtonDefaults.colors.textColor
                    )
                }
            }
        }
    }
}

internal fun MapboxInterface.MapPoint.toPoint() = Point.fromLngLat(this.lon, this.lat)