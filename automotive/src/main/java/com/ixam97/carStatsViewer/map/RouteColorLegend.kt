package com.ixam97.carStatsViewer.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.utils.StringFormatters
import de.ixam97.carcompose.theme.CarTheme

/**
 * Legend for the colored trip route. Built from the same [ColorStop]s as the map layers,
 * labels are converted to the user's units. Nothing is shown for [MapboxInterface.RouteColorMode.SOLID].
 */
@Composable
fun RouteColorLegend(
    modifier: Modifier = Modifier,
    routeColorMode: MapboxInterface.RouteColorMode
) {
    val scale = RouteColorScales.forMode(routeColorMode, CarStatsViewer.appPreferences.distanceUnit) ?: return
    val stops = scale.stops
    val min = stops.first().value
    val max = stops.last().value
    val fraction: (Float) -> Float = { (it - min) / (max - min) }

    val title = when (routeColorMode) {
        MapboxInterface.RouteColorMode.SPEED ->
            "${stringResource(R.string.map_route_color_speed)} (${StringFormatters.getSpeedUnitString()})"
        else ->
            "${stringResource(R.string.map_route_color_consumption)} (${StringFormatters.getConsumptionUnitString()})"
    }
    val labels = scale.labels.mapIndexed { index, value ->
        val label = when (routeColorMode) {
            MapboxInterface.RouteColorMode.SPEED -> StringFormatters.getSpeedValueString(value, withUnit = false)
            else -> StringFormatters.getConsumptionValueString(value, withUnit = false)
        }
        if (scale.openEnd && index == scale.labels.lastIndex) "$label+" else label
    }

    Column(modifier = modifier.widthIn(max = 500.dp).fillMaxWidth()) {
        Text(
            text = title,
            style = CarTheme.carTypography.rowTitle
        )
        Spacer(Modifier.size(10.dp))
        // Scales crossing 0 (consumption) get a white zero line that sticks out of the gradient bar,
        // separating regeneration from consumption.
        val zeroFraction = if (min < 0f && max > 0f) fraction(0f) else null
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .drawWithContent {
                    drawContent()
                    zeroFraction?.let {
                        val x = size.width * it
                        drawLine(
                            color = Color.White,
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 3.dp.toPx()
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.horizontalGradient(
                        colorStops = stops.map { fraction(it.value) to it.color }.toTypedArray()
                    ))
            )
        }
        Spacer(Modifier.size(2.dp))
        PositionedLabels(
            labels = labels,
            fractions = scale.labels.map { fraction(it) }
        )
    }
}

/** Places each label horizontally centered on its fraction of the width. */
@Composable
private fun PositionedLabels(
    labels: List<String>,
    fractions: List<Float>
) {
    Layout(
        modifier = Modifier.fillMaxWidth(),
        content = {
            labels.forEach {
                Text(
                    text = it,
                    style = CarTheme.carTypography.rowContent
                )
            }
        }
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0)) }
        val width = constraints.maxWidth
        val height = placeables.maxOfOrNull { it.height } ?: 0
        layout(width, height) {
            placeables.forEachIndexed { index, placeable ->
                val anchor = (fractions[index] * width).toInt()
                val x = anchor - placeable.width / 2
                placeable.place(x.coerceIn(0, maxOf(0, width - placeable.width)), 0)
            }
        }
    }
}
