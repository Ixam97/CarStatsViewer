package com.ixam97.carStatsViewer.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.compose.theme.polestarOrange
import com.mapbox.geojson.Feature
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxDelicateApi
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.compose.MapboxMapComposable
import com.mapbox.maps.extension.compose.style.BooleanValue
import com.mapbox.maps.extension.compose.style.ColorValue
import com.mapbox.maps.extension.compose.style.DoubleValue
import com.mapbox.maps.extension.compose.style.layers.ImageValue
import com.mapbox.maps.extension.compose.style.layers.generated.IconAnchorValue
import com.mapbox.maps.extension.compose.style.layers.generated.LineJoinValue
import com.mapbox.maps.extension.compose.style.layers.generated.LineLayer
import com.mapbox.maps.extension.compose.style.layers.generated.SymbolLayer
import com.mapbox.maps.extension.compose.style.layers.generated.VisibilityValue
import com.mapbox.maps.extension.compose.style.rememberStyleImage
import com.mapbox.maps.extension.compose.style.sources.GeoJSONData
import com.mapbox.maps.extension.compose.style.sources.generated.GeoJsonSourceState
import com.mapbox.maps.extension.compose.style.sources.generated.rememberGeoJsonSourceState
import com.mapbox.maps.extension.style.expressions.dsl.generated.switchCase
import com.mapbox.maps.extension.style.expressions.generated.Expression
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class MapTripAnnotationsState(
    val startLocationSourceState: GeoJsonSourceState,
    val destinationLocationSourceState: GeoJsonSourceState,
    val chargingLocationsSourceState: GeoJsonSourceState,
    val stopLocationsSourceState: GeoJsonSourceState,
    val routeSourceState: GeoJsonSourceState,
    val routeSpeedSourceState: GeoJsonSourceState,
    val routeConsumptionSourceState: GeoJsonSourceState,
    val onLocationClick: (point: Point) -> Unit,
    val onChargingLocationClick: (chargingSessionId: Long) -> Unit
) {
    suspend fun updateState(mapTripDataState: MapboxInterface.MapTripDataState) {
        if (mapTripDataState.tripPoints.isNotEmpty()) {
            startLocationSourceState.data = GeoJSONData(createMarkerLocationFeatures(listOf(mapTripDataState.tripPoints.first().toPoint())))
            destinationLocationSourceState.data = GeoJSONData(createMarkerLocationFeatures(listOf(mapTripDataState.tripPoints.last().toPoint())))
        }
        chargingLocationsSourceState.data = GeoJSONData(createChargingLocationFeatures(mapTripDataState.chargingLocations))
        stopLocationsSourceState.data =  GeoJSONData(createMarkerLocationFeatures(mapTripDataState.stopPoints.map { it.toPoint() }))
        routeSourceState.data = GeoJSONData(createRouteFeature(mapTripDataState.tripPoints.map { it.toPoint() }))

        // Linear in the number of points, but kept off the main thread for very long trips.
        val (speedFeatures, consumptionFeatures) = withContext(Dispatchers.Default) {
            createColoredRouteFeatures(mapTripDataState.tripPoints, { it.speed }, RouteSegments.SPEED_BUCKET_SIZE) to
                createColoredRouteFeatures(mapTripDataState.tripPoints, { it.consumption }, RouteSegments.CONSUMPTION_BUCKET_SIZE)
        }
        routeSpeedSourceState.data = GeoJSONData(speedFeatures)
        routeConsumptionSourceState.data = GeoJSONData(consumptionFeatures)
    }
}

@OptIn(MapboxDelicateApi::class)
@Composable
fun rememberMapTripAnnotationsState(
    onLocationClick: (point: Point) -> Unit,
    onChargingLocationClick: (chargingSessionId: Long) -> Unit
): MapTripAnnotationsState {
    return MapTripAnnotationsState(
        startLocationSourceState = rememberGeoJsonSourceState { generateId = BooleanValue(true) },
        destinationLocationSourceState = rememberGeoJsonSourceState { generateId = BooleanValue(true) },
        chargingLocationsSourceState = rememberGeoJsonSourceState { generateId = BooleanValue(true) },
        stopLocationsSourceState = rememberGeoJsonSourceState { generateId = BooleanValue(true) },
        routeSourceState = rememberGeoJsonSourceState { generateId = BooleanValue(true) },
        routeSpeedSourceState = rememberGeoJsonSourceState { generateId = BooleanValue(true) },
        routeConsumptionSourceState = rememberGeoJsonSourceState { generateId = BooleanValue(true) },
        onLocationClick = onLocationClick,
        onChargingLocationClick = onChargingLocationClick
    )
}

@OptIn(MapboxExperimental::class)
@Composable
@MapboxMapComposable
fun MapTripAnnotations(
    mapTripAnnotationsState: MapTripAnnotationsState,
    routeColorMode: MapboxInterface.RouteColorMode = MapboxInterface.RouteColorMode.SOLID
) {
    val startLocationMarkerImage = rememberStyleImage(
        imageId = "startLocationMarkerImage",
        resourceId = R.drawable.ic_trip_start
    )
    val destinationLocationMarkerImage = rememberStyleImage(
        imageId = "destinationLocationMarkerImage",
        resourceId = R.drawable.ic_trip_destination
    )
    val stopLocationMarkerImage = rememberStyleImage(
        imageId = "stopLocationMarkerImage",
        resourceId = R.drawable.ic_trip_stop
    )
    val chargingLocationMarkerImage = rememberStyleImage(
        imageId = "chargingLocationMarkerImage",
        resourceId = R.drawable.ic_trip_charging_location
    )

    LineLayer(sourceState = mapTripAnnotationsState.routeSourceState) {
        lineColor = ColorValue(Color(0xFF542600))
        lineWidth = DoubleValue(10.0)
        lineEmissiveStrength = DoubleValue(1.0)
        lineJoin = LineJoinValue.ROUND
    }
    // Only the layer of the active mode is visible. Switching only changes the visibility, the data stays.
    LineLayer(sourceState = mapTripAnnotationsState.routeSourceState) {
        lineColor = ColorValue(polestarOrange)
        lineWidth = DoubleValue(6.0)
        lineEmissiveStrength = DoubleValue(1.0)
        lineJoin = LineJoinValue.ROUND
        visibility = visibilityFor(routeColorMode == MapboxInterface.RouteColorMode.SOLID)
    }
    LineLayer(sourceState = mapTripAnnotationsState.routeSpeedSourceState) {
        lineColor = ColorValue(RouteColorScales.speed(CarStatsViewer.appPreferences.distanceUnit).stops.toColorExpression())
        lineWidth = DoubleValue(6.0)
        lineEmissiveStrength = DoubleValue(1.0)
        lineJoin = LineJoinValue.ROUND
        visibility = visibilityFor(routeColorMode == MapboxInterface.RouteColorMode.SPEED)
    }
    LineLayer(sourceState = mapTripAnnotationsState.routeConsumptionSourceState) {
        lineColor = ColorValue(RouteColorScales.consumption(CarStatsViewer.appPreferences.distanceUnit).stops.toColorExpression())
        lineWidth = DoubleValue(6.0)
        lineEmissiveStrength = DoubleValue(1.0)
        lineJoin = LineJoinValue.ROUND
        visibility = visibilityFor(routeColorMode == MapboxInterface.RouteColorMode.CONSUMPTION)
    }
    SymbolLayer(sourceState = mapTripAnnotationsState.stopLocationsSourceState) {
        iconImage = ImageValue(stopLocationMarkerImage)
        iconAnchor = IconAnchorValue.BOTTOM
        interactionsState.onClicked { feature, _ ->
            mapTripAnnotationsState.onLocationClick(feature.geometry as Point)
            true
        }
    }
    SymbolLayer(sourceState = mapTripAnnotationsState.chargingLocationsSourceState) {
        iconImage = ImageValue(chargingLocationMarkerImage)
        iconAnchor = IconAnchorValue.BOTTOM
        interactionsState.onClicked { feature, _ ->
            feature.properties.optString("charging_session_id")
                .takeIf { it.isNotEmpty() && it.toLongOrNull() != null }
                ?.let { mapTripAnnotationsState.onChargingLocationClick(it.toLong()) }
            true
        }
    }
    SymbolLayer(sourceState = mapTripAnnotationsState.startLocationSourceState) {
        iconImage = ImageValue(startLocationMarkerImage)
        iconAnchor = IconAnchorValue.BOTTOM
        iconAllowOverlap = BooleanValue(true)
        interactionsState.onClicked { feature, _ ->
            mapTripAnnotationsState.onLocationClick(feature.geometry as Point)
            true
        }
    }
    SymbolLayer(sourceState = mapTripAnnotationsState.destinationLocationSourceState) {
        iconImage = ImageValue(destinationLocationMarkerImage)
        iconAnchor = IconAnchorValue.BOTTOM
        iconAllowOverlap = BooleanValue(true)
        interactionsState.onClicked { feature, _ ->
            mapTripAnnotationsState.onLocationClick(feature.geometry as Point)
            true
        }
    }
}

private fun createMarkerLocationFeatures(points: List<Point>): List<Feature> {
    return points.map { Feature.fromGeometry(it) }
}

private fun createChargingLocationFeatures(chargingLocations: List<MapboxInterface.ChargingLocation>): List<Feature> {
    return chargingLocations.map {
        val feature = Feature.fromGeometry(it.point.toPoint())
        feature.addStringProperty("charging_session_id", it.chargingSessionId.toString())
        feature
    }
}

private fun createRouteFeature(points: List<Point>): Feature {
    return Feature.fromGeometry(LineString.fromLngLats(points))
}

private fun createColoredRouteFeatures(
    points: List<MapboxInterface.MapPoint>,
    value: (MapboxInterface.MapPoint) -> Float?,
    bucketSize: Float
): List<Feature> {
    return RouteSegments.build(points, value, bucketSize).map { segment ->
        val feature = Feature.fromGeometry(LineString.fromLngLats(segment.points.map { it.toPoint() }))
        segment.value?.let { feature.addNumberProperty(ROUTE_VALUE_PROPERTY, it) }
        feature
    }
}

private const val ROUTE_VALUE_PROPERTY = "value"

/** Interpolates the segment color from its "value" property, segments without a value are drawn grey. */
private fun List<ColorStop>.toColorExpression(): Expression = switchCase {
    has(ROUTE_VALUE_PROPERTY)
    interpolate {
        linear()
        get(ROUTE_VALUE_PROPERTY)
        this@toColorExpression.forEach { colorStop ->
            stop(colorStop.value.toDouble()) { color(colorStop.color.toArgb()) }
        }
    }
    color(RouteColorScales.unknown.toArgb())
}

private fun visibilityFor(visible: Boolean) = if (visible) VisibilityValue.VISIBLE else VisibilityValue.NONE
