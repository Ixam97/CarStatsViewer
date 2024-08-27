package com.ixam97.carStatsViewer.compose.screens

import android.app.Activity
import android.view.MotionEvent
import android.view.View
import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.compose.components.CarHeaderWithContent
import com.ixam97.carStatsViewer.database.tripData.DrivingSession
import com.ixam97.carStatsViewer.map.Mapbox
import com.ixam97.carStatsViewer.ui.activities.MainActivity
import com.ixam97.carStatsViewer.ui.plot.enums.PlotDimensionSmoothingType
import com.ixam97.carStatsViewer.ui.plot.enums.PlotDimensionX
import com.ixam97.carStatsViewer.ui.plot.enums.PlotDimensionY
import com.ixam97.carStatsViewer.ui.plot.enums.PlotHighlightMethod
import com.ixam97.carStatsViewer.ui.plot.enums.PlotLineLabelFormat
import com.ixam97.carStatsViewer.ui.plot.enums.PlotMarkerType
import com.ixam97.carStatsViewer.ui.plot.enums.PlotSessionGapRendering
import com.ixam97.carStatsViewer.ui.plot.graphics.PlotLinePaint
import com.ixam97.carStatsViewer.ui.plot.graphics.PlotPaint
import com.ixam97.carStatsViewer.ui.plot.objects.PlotLine
import com.ixam97.carStatsViewer.ui.plot.objects.PlotLineConfiguration
import com.ixam97.carStatsViewer.ui.plot.objects.PlotRange
import com.ixam97.carStatsViewer.ui.views.PlotView
import com.ixam97.carStatsViewer.utils.DataConverters
import com.ixam97.carStatsViewer.utils.StringFormatters
import com.ixam97.carStatsViewer.utils.getColorFromAttribute
import java.util.Date

@Composable
fun TripDetailsPortraitScreen(trip: DrivingSession?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {

        val context = LocalContext.current

        val consumptionPlotLinePaint = PlotLinePaint(
            PlotPaint.byColor(getColorFromAttribute(context, R.attr.primary_plot_color), CarStatsViewer.appContext.resources.getDimension(R.dimen.reduced_font_size)),
            PlotPaint.byColor(getColorFromAttribute(context, R.attr.secondary_plot_color), CarStatsViewer.appContext.resources.getDimension(R.dimen.reduced_font_size)),
            PlotPaint.byColor(getColorFromAttribute(context, R.attr.tertiary_plot_color), CarStatsViewer.appContext.resources.getDimension(R.dimen.reduced_font_size))
        ) { CarStatsViewer.appPreferences.consumptionPlotSecondaryColor }

        val consumptionPlotLine = PlotLine(
            PlotLineConfiguration(
                PlotRange(-200f, 600f, -200f, 600f, 100f, 0f),
                PlotLineLabelFormat.NUMBER,
                PlotHighlightMethod.AVG_BY_DISTANCE,
                "Wh/km"
            ),
        )

        trip?.drivingPoints?.let { drivingPoints ->
            val plotPoints = DataConverters.consumptionPlotLineFromDrivingPoints(drivingPoints)
            val plotMarkers = DataConverters.plotMarkersFromSession(trip)

            consumptionPlotLine.addDataPoints(plotPoints)
        }

        var selectedSection by remember { mutableIntStateOf(0) }

        CarHeaderWithContent(
            onBackClick = { (context as Activity).finish() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(40.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .clickable { selectedSection = 0 },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        textAlign = TextAlign.Center,
                        text = "Trip Details",
                        style = MaterialTheme.typography.h1,
                        color = if (selectedSection == 0) MaterialTheme.colors.primary else MaterialTheme.colors.onBackground
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .clickable { selectedSection = 1 },
                    contentAlignment = Alignment.Center
                    ) {
                    Text(
                        textAlign = TextAlign.Center,
                        text = "Charging Sessions",
                        style = MaterialTheme.typography.h1,
                        color = if (selectedSection == 1) MaterialTheme.colors.primary else MaterialTheme.colors.onBackground
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .clickable { selectedSection = 2 },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        textAlign = TextAlign.Center,
                        text = "Map",
                        style = MaterialTheme.typography.h1,
                        color = if (selectedSection == 2) MaterialTheme.colors.primary else MaterialTheme.colors.onBackground
                    )
                }
            }
        }
        if (trip == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No data available")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                when (selectedSection) {
                    0 -> {
                        TripDetails(trip)
                        Divider(Modifier.padding(horizontal = 24.dp))
                        ConsumptionPlot(
                            plotLine = consumptionPlotLine,
                            plotLinePaint = consumptionPlotLinePaint,
                            distance = trip.driven_distance.toFloat()
                        )
                    }
                    1 -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No data available")
                        }
                    }
                    2 -> {
                        Mapbox.MapBoxContainer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            trip = trip
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TripDataRow(
    title: String,
    text: String,
    @DrawableRes iconResId: Int? = null
) {
    Row(
        modifier = Modifier
            .padding(vertical = 15.dp, horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (iconResId != null) {
            Icon(
                modifier = Modifier
                    .height(60.dp)
                    .width(60.dp),
                painter = painterResource(id = iconResId),
                contentDescription = null,
                tint = Color.White
            )
            Spacer(modifier = Modifier.size(24.dp))
        }
        Column {
            Text(
                text = title
            )
            Spacer(modifier = Modifier.size(10.dp))
            Text(
                text = text,
                fontSize = 25.sp,
                color = colorResource(id = R.color.secondary_text_color)
            )
        }
    }
}

@Composable
fun TripDetails(trip: DrivingSession) {
    Column {
        Row(
            modifier = Modifier
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TripDataRow(
                title = StringFormatters.getDateString(Date(trip.start_epoch_time)),
                text = "Location"
            )
            Icon(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .size(60.dp),
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = Color.White
            )
            TripDataRow(
                title = StringFormatters.getDateString(Date(trip.start_epoch_time)),
                text = "Location"
            )
        }
        Divider(Modifier.padding(horizontal = 24.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
            ) {
                TripDataRow(
                    title = StringFormatters.getTraveledDistanceString(trip.driven_distance.toFloat()),
                    text = stringResource(R.string.summary_traveled_distance),
                    iconResId = R.drawable.ic_distance
                )
                Divider(Modifier.padding(start = 24.dp))
                TripDataRow(
                    title = StringFormatters.getEnergyString(trip.used_energy.toFloat()),
                    text = stringResource(R.string.summary_used_energy),
                    iconResId = R.drawable.ic_energy_large
                )
                Divider(Modifier.padding(start = 24.dp))
                TripDataRow(
                    title = StringFormatters.getAvgConsumptionString(trip.used_energy.toFloat(), trip.driven_distance.toFloat()),
                    text = stringResource(R.string.summary_average_consumption),
                    iconResId = R.drawable.ic_avg_consumption
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
            ) {
                TripDataRow(
                    title = StringFormatters.getAvgSpeedString(trip.driven_distance.toFloat(), trip.drive_time),
                    text = stringResource(R.string.summary_speed),
                    iconResId = R.drawable.ic_speed_large
                )
                Divider(Modifier.padding(horizontal = 24.dp))
                TripDataRow(
                    title = "Placeholder",
                    text = stringResource(R.string.summary_altitude),
                    iconResId = R.drawable.ic_altitude
                )
                Divider(Modifier.padding(horizontal = 24.dp))
                TripDataRow(
                    title = StringFormatters.getElapsedTimeString(trip.drive_time, true),
                    text = stringResource(R.string.summary_travel_time),
                    iconResId = R.drawable.ic_time_large
                )
            }
        }
    }
}

@Composable
fun ConsumptionPlot(
    plotLine: PlotLine,
    plotLinePaint: PlotLinePaint,
    distance: Float
) {
    val appPreferences = CarStatsViewer.appPreferences

    val density = LocalDensity.current.density

    AndroidView(
        modifier = Modifier
            .padding(horizontal = 24.dp),
        factory = { context ->
            PlotView(
                context,
                xMargin = (70 * density).toInt(),
                yMargin = (60 * density).toInt()
            ).apply {
                if (appPreferences.consumptionUnit) {
                    plotLine.Configuration.Unit = "Wh/%s".format(appPreferences.distanceUnit.unit())
                    plotLine.Configuration.LabelFormat = PlotLineLabelFormat.NUMBER
                    plotLine.Configuration.Divider = appPreferences.distanceUnit.toFactor() * 1f
                } else {
                    plotLine.Configuration.Unit = "kWh/100%s".format(appPreferences.distanceUnit.unit())
                    plotLine.Configuration.LabelFormat = PlotLineLabelFormat.FLOAT
                    plotLine.Configuration.Divider = appPreferences.distanceUnit.toFactor() * 10f
                }

                dimension = PlotDimensionX.DISTANCE
                dimensionYSecondary = PlotDimensionY.STATE_OF_CHARGE
                dimensionRestrictionMin = appPreferences.distanceUnit.asUnit(
                    MainActivity.DISTANCE_TRIP_DIVIDER)
                dimensionSmoothing = 0.02f
                dimensionSmoothingType = PlotDimensionSmoothingType.PERCENTAGE
                visibleMarkerTypes.add(PlotMarkerType.CHARGE)
                visibleMarkerTypes.add(PlotMarkerType.PARK)

                addPlotLine(plotLine, plotLinePaint)
                dimensionRestriction = appPreferences.distanceUnit.asUnit(((appPreferences.distanceUnit.toUnit(distance) / MainActivity.DISTANCE_TRIP_DIVIDER).toInt() + 1) * MainActivity.DISTANCE_TRIP_DIVIDER) + 1
                sessionGapRendering = PlotSessionGapRendering.JOIN
                setOnTouchListener { v, event ->
                    disallowIntercept(v, event)
                    false
                }
            }
        },
        update = { plotView ->
        }
    )
}

private fun disallowIntercept(v: View, event: MotionEvent) {
    when (event.action) {
        MotionEvent.ACTION_DOWN -> {
            v.parent.requestDisallowInterceptTouchEvent(true)
        }
        MotionEvent.ACTION_UP -> {
            v.parent.requestDisallowInterceptTouchEvent(false)
        }
    }
}

