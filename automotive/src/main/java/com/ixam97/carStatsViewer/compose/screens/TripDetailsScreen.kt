package com.ixam97.carStatsViewer.compose.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.view.MotionEvent
import android.view.View
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowRightAlt
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.ixam97.carStatsViewer.compose.TripDetailsViewModel
import com.ixam97.carStatsViewer.compose.components.CarHeaderWithContent
import com.ixam97.carStatsViewer.compose.components.CarSegmentedButton
import com.ixam97.carStatsViewer.compose.theme.clubHint
import com.ixam97.carStatsViewer.compose.theme.slideUpBackground
import com.ixam97.carStatsViewer.database.tripData.ChargingSession
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun TripDetailsPortraitScreen(
    viewModel: TripDetailsViewModel,
    sessionId: Long
) {

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
    ) {
        val width = maxWidth
        val height = maxHeight

        val splitScreenCondition = (width > 1500.dp) && (width > height)

        val tripDetailsState = viewModel.tripDetailsState
        val trip = tripDetailsState.drivingSession

        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {

            val context = LocalContext.current

            CarHeaderWithContent(
                onBackClick = {
                    if (viewModel.tripDetailsState.showChargingSessionDetails && tripDetailsState.selectedSection == TripDetailsViewModel.CHARGING_SECTION) {
                        viewModel.closeChargingSessionDetails()
                    } else {
                        (context as Activity).finish()
                    }
                }
            ) {
                if (viewModel.tripDetailsState.drivingSession == null) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        // horizontalArrangement = Arrangement.spacedBy(40.dp)
                    ) {
                        Text(
                            textAlign = TextAlign.Center,
                            text = stringResource(R.string.summary_loading_trip),
                            style = MaterialTheme.typography.h1
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    // horizontalArrangement = Arrangement.spacedBy(40.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .clickable { viewModel.setSelectedSection(TripDetailsViewModel.DETAILS_SECTION) }
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            textAlign = TextAlign.Center,
                            text = stringResource(R.string.summary_tab_trip_details),
                            style = MaterialTheme.typography.h1,
                            color = if (tripDetailsState.selectedSection == TripDetailsViewModel.DETAILS_SECTION) MaterialTheme.colors.primary else MaterialTheme.colors.onBackground
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .clickable {
                                viewModel.setSelectedSection(TripDetailsViewModel.CHARGING_SECTION)
                                viewModel.closeChargingSessionDetails()
                            }
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            textAlign = TextAlign.Center,
                            text = stringResource(R.string.summary_tab_charging_sessions),
                            style = MaterialTheme.typography.h1,
                            color = if (tripDetailsState.selectedSection == TripDetailsViewModel.CHARGING_SECTION) MaterialTheme.colors.primary else MaterialTheme.colors.onBackground
                        )
                    }
                    if (!splitScreenCondition && !Mapbox.isDummy()) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .clickable { viewModel.setSelectedSection(TripDetailsViewModel.MAP_SECTION) }
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                textAlign = TextAlign.Center,
                                text = stringResource(R.string.summary_tab_map),
                                style = MaterialTheme.typography.h1,
                                color = if (tripDetailsState.selectedSection == TripDetailsViewModel.MAP_SECTION) MaterialTheme.colors.primary else MaterialTheme.colors.onBackground
                            )
                        }
                    }
                }
            }

            if (viewModel.tripDetailsState.drivingSession == null) {
                viewModel.loadDrivingSession(sessionId)
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(160.dp),
                        strokeWidth = 10.dp
                    )
                }
            } else {
                val consumptionPlotLinePaint = PlotLinePaint(
                    PlotPaint.byColor(
                        getColorFromAttribute(context, R.attr.primary_plot_color),
                        CarStatsViewer.appContext.resources.getDimension(R.dimen.reduced_font_size)
                    ),
                    PlotPaint.byColor(
                        getColorFromAttribute(context, R.attr.secondary_plot_color),
                        CarStatsViewer.appContext.resources.getDimension(R.dimen.reduced_font_size)
                    ),
                    PlotPaint.byColor(
                        getColorFromAttribute(context, R.attr.tertiary_plot_color),
                        CarStatsViewer.appContext.resources.getDimension(R.dimen.reduced_font_size)
                    )
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

                if (trip == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(R.string.summary_no_data))
                    }
                } else {
                    Row {
                        Column(
                            modifier = Modifier
                                // .then(
                                //     if (height < 1000.dp) {
                                //         Modifier.verticalScroll(rememberScrollState())
                                //     } else {
                                //         Modifier
                                //     }
                                // )
                                .weight(1f)
                                .fillMaxHeight(),
                        ) {
                            when (tripDetailsState.selectedSection) {
                                0 -> {
                                    TripDetails(
                                        trip = trip,
                                        startLocation = tripDetailsState.startLocation?: stringResource(R.string.summary_loading_location),
                                        endLocation = tripDetailsState.endLocation?: stringResource(R.string.summary_loading_location)
                                    )
                                    Divider(Modifier.padding(horizontal = 24.dp))
                                    Row(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box (
                                            Modifier.weight(1f)
                                        ) {
                                            var distance by remember {mutableStateOf(trip.driven_distance.toFloat())}

                                            ConsumptionPlot(
                                                plotLine = consumptionPlotLine,
                                                plotLinePaint = consumptionPlotLinePaint,
                                                distance = distance,
                                                limitedHeight = height < 1000.dp,
                                                secondaryDimension = viewModel.tripDetailsState.selectedSecondaryDimension
                                            )

                                            LaunchedEffect(Unit) {
                                                viewModel.changeDistanceFlow.collect { newValue ->
                                                    distance = if (newValue == -1f) {
                                                        trip.driven_distance.toFloat()
                                                    } else {
                                                        newValue
                                                    }
                                                }
                                            }
                                        }
                                        if (height < 1000.dp) {
                                            Box(Modifier
                                                .background(MaterialTheme.colors.surface)
                                                .height(500.dp)
                                                .width(100.dp)
                                                .padding(10.dp)
                                            )
                                        }
                                    }
                                    Row(
                                        modifier = Modifier
                                            .height(IntrinsicSize.Min)
                                            .padding(horizontal = 24.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                                    ) {
                                        val distanceUnit = CarStatsViewer.appPreferences.distanceUnit.unit()
                                        CarSegmentedButton(
                                            modifier = Modifier.fillMaxHeight().weight(1f),
                                            options = listOf("100 $distanceUnit", "40 $distanceUnit", "20 $distanceUnit", "Trip"),
                                            selectedIndex = null,
                                            onSelectedIndexChanged = { index ->
                                                viewModel.setTripDistance(index)
                                            },
                                            contentPadding = PaddingValues(15.dp)
                                        )
                                        CarSegmentedButton(
                                            modifier = Modifier.fillMaxHeight().width(IntrinsicSize.Min),
                                            // options = listOf("Speed", "SoC", "Alt"),
                                            optionsContent = listOf(
                                                { SegmentedButtonIcon(R.drawable.ic_speed_large) },
                                                { SegmentedButtonIcon(R.drawable.ic_battery) },
                                                { SegmentedButtonIcon(R.drawable.ic_altitude) }
                                            ),
                                            selectedIndex = viewModel.tripDetailsState.selectedSecondaryDimension - 1,
                                            onSelectedIndexChanged = { index ->
                                                val newValue = if (index == viewModel.tripDetailsState.selectedSecondaryDimension - 1) {
                                                    0
                                                } else {
                                                    index + 1
                                                }
                                                viewModel.setSecondaryPlotDimension(newValue)
                                            },
                                            contentPadding = PaddingValues(vertical = 10.dp, horizontal = 15.dp)
                                        )
                                    }
                                }

                                TripDetailsViewModel.CHARGING_SECTION -> {
                                    if (trip.chargingSessions?.any { chargingSession ->
                                            chargingSession.end_epoch_time != null && chargingSession.end_epoch_time > 0
                                        } == true) {
                                        trip.chargingSessions?.let {
                                            ChargingSessions(
                                                viewModel = viewModel,
                                                chargingSessions = it
                                            )
                                        }
                                    } else Box(
                                        modifier = Modifier
                                            .fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(stringResource(R.string.summary_no_charging_sessions))
                                    }
                                }

                                2 -> {
                                    Mapbox.MapBoxContainer(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        trip = trip,
                                        chargingMarkerOnClick = {id -> viewModel.selectChargingSession(id)}
                                    )
                                }
                            }
                        }
                        if (splitScreenCondition) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {

                                Mapbox.MapBoxContainer(
                                    modifier = Modifier,
                                    trip = trip,
                                    chargingMarkerOnClick = {id -> viewModel.selectChargingSession(id)}
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TripDataRow(
    modifier: Modifier = Modifier,
    title: String,
    text: String? = null,
    @DrawableRes iconResId: Int? = null
) {
    Row(
        modifier = modifier
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
            if (text != null) {
                Spacer(modifier = Modifier.size(10.dp))
                Text(
                    modifier = Modifier
                        .basicMarquee(
                            iterations = Int.MAX_VALUE,
                            repeatDelayMillis = 5000,
                            spacing = MarqueeSpacing(50.dp)
                        ),
                    text = text,
                    fontSize = 25.sp,
                    color = colorResource(id = R.color.secondary_text_color),
                    softWrap = false
                )
            }
        }
    }
}

@Composable
fun TripDetails(
    trip: DrivingSession,
    startLocation: String,
    endLocation: String
) {
    var altUp = 0f
    var altDown = 0f
    var prevAlt = 0f
    val altList = trip.drivingPoints?.mapNotNull { it.alt }?: listOf()
    altList.forEachIndexed { index, alt ->
        if (index == 0) prevAlt = alt
        else {
            when {
                alt > prevAlt -> altUp += alt - prevAlt
                alt < prevAlt -> altDown += prevAlt - alt
            }
        }
        prevAlt = alt
    }

    val tripTypes = LocalContext.current.resources.getStringArray(R.array.trip_type_names)

    Column {
        var visibleDetails by remember { mutableStateOf(true) }
        Text(modifier = Modifier
            .padding(vertical = 15.dp, horizontal = 24.dp),
            text = "${stringResource(R.string.summary_trip_type)}: ${tripTypes[trip.session_type]}"
        )
        Row(
            modifier = Modifier
                .clickable {
                    visibleDetails = !visibleDetails
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            TripDataRow(
                modifier = Modifier.weight(1f),
                title = StringFormatters.getDateString(Date(trip.start_epoch_time)),
                text = startLocation
            )
            Icon(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .size(60.dp),
                imageVector = Icons.Default.ArrowRightAlt,
                contentDescription = null,
                tint = Color.White
            )
            if (trip.end_epoch_time == null || trip.end_epoch_time <= 0) {
                TripDataRow(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.summary_ongoing)
                )
            } else {
                TripDataRow(
                    modifier = Modifier.weight(1f),
                    title = StringFormatters.getDateString(Date(trip.end_epoch_time)),
                    text = endLocation
                )
            }

            Icon(
                modifier = Modifier
                    .padding(end = 24.dp)
                    .size(60.dp),
                imageVector = if (visibleDetails) {
                    Icons.Default.UnfoldLess
                } else {
                    Icons.Default.UnfoldMore
                },
                contentDescription = null,
                tint = Color.White
            )
        }
        androidx.compose.animation.AnimatedVisibility(
            visible =  visibleDetails,
            enter = expandVertically(expandFrom = Alignment.Top),
            exit = shrinkVertically(shrinkTowards = Alignment.Top)
        ) {
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
                        title = StringFormatters.getAltitudeString(altUp, altDown),
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
}

@Composable
fun ConsumptionPlot(
    plotLine: PlotLine,
    plotLinePaint: PlotLinePaint,
    distance: Float,
    limitedHeight: Boolean,
    secondaryDimension: Int
) {
    val appPreferences = CarStatsViewer.appPreferences

    val density = LocalDensity.current.density

    AndroidView(
        modifier = Modifier
            .then(
                if (limitedHeight) {
                    Modifier.height(500.dp)
                } else {
                    Modifier
                }
            )
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
                dimensionYSecondary = when (secondaryDimension) {
                    1 -> PlotDimensionY.SPEED
                    2 -> PlotDimensionY.STATE_OF_CHARGE
                    4 -> PlotDimensionY.ALTITUDE
                    else -> null
                }
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
            if (distance != 0f) {
                plotView.dimensionRestriction = appPreferences.distanceUnit.asUnit(distance).toLong() + 1
                plotView.dimensionShift = 0
            }
            plotView.dimensionYSecondary = when (secondaryDimension) {
                1 -> PlotDimensionY.SPEED
                2 -> PlotDimensionY.STATE_OF_CHARGE
                3 -> PlotDimensionY.ALTITUDE
                else -> null
            }
        }
    )
}

@Composable
fun ChargingPlot(
    plotLine: PlotLine,
    plotLinePaint: PlotLinePaint,
    limitedHeight: Boolean,
    time: Long
) {
    val appPreferences = CarStatsViewer.appPreferences

    val density = LocalDensity.current.density

    AndroidView(
        modifier = Modifier
            .then(
                if (limitedHeight) {
                    Modifier.height(500.dp)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 24.dp),
        factory = { context ->
            PlotView(
                context,
                xMargin = (70 * density).toInt(),
                yMargin = (60 * density).toInt()
            ).apply {
                dimension = PlotDimensionX.TIME
                dimensionRestrictionMin = TimeUnit.MINUTES.toMillis(5)
                dimensionSmoothing = 0.01f
                dimensionSmoothingType = PlotDimensionSmoothingType.PERCENTAGE
                dimensionYSecondary = PlotDimensionY.STATE_OF_CHARGE

                addPlotLine(plotLine, plotLinePaint)
                dimensionRestriction = TimeUnit.MINUTES.toMillis((TimeUnit.MILLISECONDS.toMinutes(time / 5)) + 1) * 5 + 1
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

@Composable
fun ChargingSessions(
    viewModel: TripDetailsViewModel,
    chargingSessions: List<ChargingSession>
) {

    // viewModel.tripDetailsState.drivingSession?.drivingPoints.

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
        ) {
            items(chargingSessions.filter { it.end_epoch_time != null }, itemContent = { session: ChargingSession ->

                val endSoc = viewModel.tripDetailsState.drivingSession?.drivingPoints?.find {
                    it.driving_point_epoch_time >= session.end_epoch_time!!
                }?.state_of_charge?: session.chargingPoints?.last()?.state_of_charge
                val startSoc = viewModel.tripDetailsState.drivingSession?.drivingPoints?.findLast {
                    it.driving_point_epoch_time <= session.start_epoch_time
                }?.state_of_charge?: session.chargingPoints?.first()?.state_of_charge

                val socString = if (endSoc != null && startSoc != null) {
                    String.format(
                        "%d%%  →  %d%%",
                        (startSoc * 100f).roundToInt(),
                        (endSoc * 100f).roundToInt(),
                    )
                } else stringResource(R.string.summary_soc_unavailable)

                Column (
                    modifier = Modifier.fillMaxWidth()
                        .clickable {
                            viewModel.selectChargingSession(session.charging_session_id)
                        }
                ) {
                    val defaultLocationText = stringResource(R.string.summary_loading_location)
                    var location by remember { mutableStateOf<String?>(defaultLocationText) }
                    TripDataRow(
                        title = "${StringFormatters.getDateString(Date(session.start_epoch_time))}, $socString",
                        text = location?: stringResource(R.string.summary_location_unavailable)
                    )
                    Divider(Modifier.padding(horizontal = 24.dp))

                    LaunchedEffect(Unit) {
                        withContext(Dispatchers.IO) {
                            location = if (session.lat != null && session.lon != null)
                                Mapbox.getAddress(session.lon.toDouble(), session.lat.toDouble())
                            else null
                        }
                    }
                }
            })
        }

        AnimatedVisibility(
            visible = viewModel.tripDetailsState.showChargingSessionDetails,
            enter = expandVertically(expandFrom = Alignment.Top),
            exit = shrinkVertically(shrinkTowards = Alignment.Top)
        ) {
            viewModel.tripDetailsState.chargingSession?.let {
                ChargingSessionDetails(
                    session = it,
                    viewModel = viewModel,
                    onCollapseClick = { viewModel.closeChargingSessionDetails() }
                )
            }?: Text("Error")
        }
    }
}

@Composable
fun ChargingSessionDetails(
    session: ChargingSession,
    viewModel: TripDetailsViewModel,
    onCollapseClick: () -> Unit
) {

    val endSoc = viewModel.tripDetailsState.drivingSession?.drivingPoints?.find {
        it.driving_point_epoch_time >= session.end_epoch_time!!
    }?.state_of_charge?: session.chargingPoints?.last()?.state_of_charge
    val startSoc = viewModel.tripDetailsState.drivingSession?.drivingPoints?.findLast {
        it.driving_point_epoch_time <= session.start_epoch_time
    }?.state_of_charge?: session.chargingPoints?.first()?.state_of_charge

    val socString = if (endSoc != null && startSoc != null) {
        String.format(
            "%d%%  →  %d%%",
            (startSoc * 100f).roundToInt(),
            (endSoc * 100f).roundToInt(),
        )
    } else stringResource(R.string.summary_soc_unavailable)

    val context = LocalContext.current

    // TODO: Just for visualization for now...
    val filteredChargingPoints = (session.chargingPoints?: listOf()).filter { it.power < -500_000 }
    val plotPoints = DataConverters.chargePlotLineFromChargingPoints(filteredChargingPoints)
    val defaultLocationText = stringResource(R.string.summary_loading_location)
    var location by remember { mutableStateOf<String?>(defaultLocationText) }

    val chargePlotPaint =PlotLinePaint(
        PlotPaint.byColor(
            context.getColor(R.color.charge_plot_color),
            CarStatsViewer.appContext.resources.getDimension(R.dimen.reduced_font_size)
        ),
        PlotPaint.byColor(
            context.getColor(R.color.secondary_plot_color),
            CarStatsViewer.appContext.resources.getDimension(R.dimen.reduced_font_size)
        ),
        PlotPaint.byColor(
            context.getColor(R.color.secondary_plot_color_alt),
            CarStatsViewer.appContext.resources.getDimension(R.dimen.reduced_font_size)
        )
    ) { CarStatsViewer.appPreferences.chargePlotSecondaryColor }

    val chargePlotLine = PlotLine(
        PlotLineConfiguration(
            PlotRange(0f, 20f, 0f, 400f, 20f),
            PlotLineLabelFormat.FLOAT,
            PlotHighlightMethod.AVG_BY_TIME,
            "kW"
        )
    )

    chargePlotLine.addDataPoints(plotPoints)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(slideUpBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onCollapseClick()
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            TripDataRow(
                modifier = Modifier.weight(1f),
                title = "${StringFormatters.getDateString(Date(session.start_epoch_time))}, $socString",
                text = location ?: stringResource(R.string.summary_location_unavailable)
            )
            Icon(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .size(60.dp),
                imageVector = Icons.Default.UnfoldLess,
                contentDescription = null,
                tint = Color.White
            )
        }
        Divider(Modifier.padding(horizontal = 24.dp))
        if (session.end_epoch_time != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                TripDataRow(
                    title = StringFormatters.getEnergyString(session.charged_energy.toFloat()),
                    text = stringResource(R.string.summary_charged_energy),
                    iconResId = R.drawable.ic_energy_large
                )
                if ((session.chargingPoints?.filter { it.point_marker_type == 2}?.size?:0) > 1) {
                    Icon(
                        modifier = Modifier.padding(horizontal = 24.dp).size(60.dp),
                        imageVector = Icons.Outlined.Warning,
                        tint = clubHint,
                        contentDescription = ""
                    )
                    Text(
                        modifier = Modifier.padding(end = 24.dp),
                        text = stringResource(R.string.summary_interruption_warning),
                        color = clubHint,
                        fontSize = 25.sp
                    )
                }
            }
            Divider(Modifier.padding(horizontal = 24.dp))
            TripDataRow(
                title = StringFormatters.getElapsedTimeString(session.end_epoch_time - session.start_epoch_time),
                text = stringResource(R.string.summary_charge_time),
                iconResId = R.drawable.ic_time_large
            )
            Divider(Modifier.padding(horizontal = 24.dp))
            ChargingPlot(
                plotLine = chargePlotLine,
                plotLinePaint = chargePlotPaint,
                limitedHeight = false,
                time = session.end_epoch_time - session.start_epoch_time
            )
        } else {
            Text(
                modifier = Modifier.padding(24.dp),
                text = stringResource(R.string.summary_ongoing_charging_session)
            )
        }

        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                location = if (session.lat != null && session.lon != null)
                    Mapbox.getAddress(session.lon.toDouble(), session.lat.toDouble())
                else null
            }
        }
    }
}

@Composable
fun SegmentedButtonIcon(@DrawableRes resId: Int) {
    Icon(
        modifier = Modifier.size(48.dp),
        painter = painterResource(resId),
        contentDescription = null,
        tint = Color.White
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

