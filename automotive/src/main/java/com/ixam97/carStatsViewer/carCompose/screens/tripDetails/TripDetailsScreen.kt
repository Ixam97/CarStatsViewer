package com.ixam97.carStatsViewer.carCompose.screens.tripDetails

import android.annotation.SuppressLint
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.carCompose.CarComposeGlobalViewModel
import com.ixam97.carStatsViewer.carCompose.deviceIsWideScreen
import com.ixam97.carStatsViewer.carCompose.theme.CarComposeIcon
import com.ixam97.carStatsViewer.database.tripData.DrivingSession
import com.ixam97.carStatsViewer.map.Mapbox
import com.ixam97.carStatsViewer.utils.StringFormatters
import de.ixam97.carcompose.components.controls.CarRow
import de.ixam97.carcompose.components.controls.CarSegmentedButton
import de.ixam97.carcompose.components.controls.CarSegmentedButtonDefaults
import de.ixam97.carcompose.components.layout.CarListDivider
import de.ixam97.carcompose.components.layout.CarPaneLayout
import de.ixam97.carcompose.components.layout.CarTabLayout
import de.ixam97.carcompose.theme.CarTheme
import kotlinx.serialization.Serializable
import java.util.Date

enum class TripDetailsTabKeys {
    Consumption, Diagram, Charging, Map
}

enum class TripDistanceSegmentKeys {
    Dist100, Dist40, Dist20, Trip
}

enum class SecondaryPlotSegmentKeys {
    Speed, Altitude, StateOfCharge
}

@Serializable
data class TripDetailsScreenNavKey(
    val sessionId: Long
): NavKey

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun TripDetailsScreen(
    globalViewModel: CarComposeGlobalViewModel,
    onBackClick: () -> Unit,
    sessionId: Long,
    viewModel: TripDetailsViewModel = viewModel {
        TripDetailsViewModel(sessionId)
    }
) {
    val tripDetailsState by viewModel.tripDetailsState.collectAsState()

    if (deviceIsWideScreen()) {
        TripDetailsLandscapeScreen(
            globalViewModel = globalViewModel,
            onBackClick = onBackClick,
            viewModel = viewModel,
        )
    } else {
        TripDetailsPortraitScreen(
            globalViewModel = globalViewModel,
            onBackClick = onBackClick,
            viewModel = viewModel
        )
    }
}

@Composable
internal fun TripDetailsPortraitScreen(
    globalViewModel: CarComposeGlobalViewModel,
    onBackClick: () -> Unit,
    viewModel: TripDetailsViewModel
) {
    val tripDetailsState by viewModel.tripDetailsState.collectAsState()
    val maxHeaderFontSize = if (CarTheme.carTypography.title.fontSize > 38.sp) 38.sp else CarTheme.carTypography.title.fontSize

    CarPaneLayout(
        isLoading = tripDetailsState.isLoading,
        headerStartContent = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(CarTheme.carDimensions.headerContentHorizontalPadding * 2)
            ) {
                Text(
                    text = stringResource(R.string.summary_tab_trip_details),
                    color = CarTheme.carColors.accent,
                    style = CarTheme.carTypography.title.copy(fontSize = maxHeaderFontSize)
                )
                Text(
                    text = stringResource(R.string.summary_tab_charging_sessions),
                    style = CarTheme.carTypography.title.copy(fontSize = maxHeaderFontSize)
                )
                Text(
                    text = stringResource(R.string.summary_tab_map),
                    style = CarTheme.carTypography.title.copy(fontSize = maxHeaderFontSize)
                )
            }
        },
        onBackAction = onBackClick,
//                headerIconButtons = listOf({
//                    CarIconButton(
//                        painter = painterResource(R.drawable.ic_carcompose_export),
//                        enabled = false,
//                        onClick = {  }
//                    )
//                })
    ) {
        when(tripDetailsState.selectedTab) {
            TripDetailsTabKeys.Consumption -> {
                tripDetailsState.drivingSession.let { drivingSession ->
                    if (drivingSession != null)
                        TripDetailsConsumptionSection(
                            drivingSession = drivingSession,
                            startLocation = tripDetailsState.startLocation?:stringResource(R.string.summary_loading_location),
                            destinationLocation = tripDetailsState.destinationLocation?:stringResource(R.string.summary_loading_location)
                        )
                    else Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                }
            }
            TripDetailsTabKeys.Diagram -> {}
            TripDetailsTabKeys.Charging -> {}
            TripDetailsTabKeys.Map -> TripDetailsMapSection(tripDetailsState)
        }
    }
}

@Composable
internal fun TripDetailsLandscapeScreen(
    globalViewModel: CarComposeGlobalViewModel,
    onBackClick: () -> Unit,
    viewModel: TripDetailsViewModel
) {
    val tripDetailsState by viewModel.tripDetailsState.collectAsState()

    val tabs = mutableListOf(
        CarTabLayout.Tab(
            title = stringResource(R.string.summary_tab_trip_details),
            icon = painterResource(id = R.drawable.ic_distance),
            key = TripDetailsTabKeys.Consumption
        ),
        CarTabLayout.Tab(
            title = stringResource(R.string.summary_tab_charging_sessions),
            icon = painterResource(id = R.drawable.ic_charger),
            key = TripDetailsTabKeys.Charging
        )
    )

    Row() {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            CarTabLayout(
                isLoading = tripDetailsState.isLoading,
                tabOrientation = CarTabLayout.Orientation.Vertical,
                selectedKey = tripDetailsState.selectedTab,
                tabs = tabs,
                onTabSelected = { viewModel.setSelectedTab(it) },
                headerTitle = stringResource(R.string.summary_title),
                onBackAction = onBackClick
            ) { selectedKey ->
                when(selectedKey) {
                    TripDetailsTabKeys.Consumption -> {
                        tripDetailsState.drivingSession.let { drivingSession ->
                            if (drivingSession != null)
                                TripDetailsConsumptionSection(
                                    drivingSession = drivingSession,
                                    startLocation = tripDetailsState.startLocation?:stringResource(R.string.summary_loading_location),
                                    destinationLocation = tripDetailsState.destinationLocation?:stringResource(R.string.summary_loading_location)
                                )
                            else Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) { CircularProgressIndicator() }
                        }
                    }
                    TripDetailsTabKeys.Charging -> {

                    }
                    else -> { viewModel.setSelectedTab(TripDetailsTabKeys.Consumption) }
                }
            }
        }
        Box(
            modifier = Modifier
                .weight(0.70f)
                .fillMaxHeight()
        ) { TripDetailsMapSection(tripDetailsState) }
    }
}

@Composable
fun TripDetailsConsumptionSection(
    drivingSession: DrivingSession,
    startLocation: String,
    destinationLocation: String
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
    ) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min)
        ) {
            Column(
                modifier = Modifier.weight(1.5f).fillMaxHeight()
            ) {
                val tripTypes = LocalContext.current.resources.getStringArray(R.array.trip_type_names)
                Text(
                    modifier = Modifier.padding(
                        horizontal = CarTheme.carDimensions.defaultHorizontalPadding,
                        vertical = CarTheme.carDimensions.defaultVerticalPadding
                    ),
                    text = "${stringResource(R.string.summary_trip_type)}: ${tripTypes[drivingSession.session_type]}",
                    style = CarTheme.carTypography.rowTitle
                )
                Row(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier
                            .padding(
                                start = CarTheme.carDimensions.defaultHorizontalPadding,
                                top = CarTheme.carDimensions.defaultVerticalPadding
                            )
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val lineColor = CarTheme.carColors.accent
                        val pixelOffset = with(LocalDensity.current){ CarTheme.carDimensions.defaultVerticalPadding.toPx() }
                        CarComposeIcon(R.drawable.ic_location_start)
                        Canvas(modifier = Modifier
                            .weight(1f)
                            .width(4.dp)
                        ) {
                            val actualLength = size.height + pixelOffset
                            drawLine(
                                color = lineColor,
                                strokeWidth = size.width,
                                start = Offset(size.width / 2, 0f),
                                end = Offset(size.width / 2, actualLength),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(actualLength / 7, actualLength / 7), 0f)
                            )
                        }
                    }
                    CarRow(
                        title = StringFormatters.getDateString(Date(drivingSession.start_epoch_time)),
                        description = startLocation
                    )
                }
                drivingSession.end_epoch_time.let { endTime ->
                    if (endTime != null && endTime > 0) {
                        Row() {
                            CarComposeIcon(
                                resID = R.drawable.ic_location_destination,
                                modifier = Modifier
                                    .padding(
                                        start = CarTheme.carDimensions.defaultHorizontalPadding,
                                        top = CarTheme.carDimensions.defaultVerticalPadding
                                    )
                            )
                            CarRow(
                                title = StringFormatters.getDateString(Date(endTime)),
                                description = destinationLocation
                            )
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CarComposeIcon(
                                resID = R.drawable.ic_chevron_up,
                                modifier = Modifier
                                    .padding(start = CarTheme.carDimensions.defaultHorizontalPadding)
                                    .rotate(180f)
                            )
                            CarRow(title = stringResource(R.string.summary_ongoing))
//                            Text(
//                                modifier = Modifier.padding(
//                                    horizontal = CarTheme.carDimensions.defaultHorizontalPadding,
//                                    vertical = CarTheme.carDimensions.defaultVerticalPadding
//                                ),
//                                text = stringResource(R.string.summary_ongoing),
//                                style = CarTheme.carTypography.rowTitle
//                            )
                        }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .padding(vertical = CarTheme.carDimensions.defaultVerticalPadding)
                    .fillMaxHeight()
                    .width(2.dp)
                    .background(brush = CarTheme.carColors.secondaryDivider)
            )
            CompactDataColumn(
                modifier = Modifier.weight(1f),
                drivingSession = drivingSession
            )
        }
        CarListDivider()
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = CarTheme.carDimensions.defaultHorizontalPadding)
                .padding(top = CarTheme.carDimensions.defaultVerticalPadding)
                .background(brush = CarTheme.carColors.secondarySurfaceBrush),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "The Consumption Graph will return Soon™",
                style = CarTheme.carTypography.rowTitle
            )
        }
        CarRow(
            content = {
                Row() {

                    val defaultFontSize = CarTheme.carTypography.rowTitle.fontSize
                    val defaultButtonPadding = CarTheme.carDimensions.segmentedButtonDimensions.buttonHorizontalPadding
                    var adjustedFontSize by remember { mutableStateOf(defaultFontSize) }
                    var adjustedButtonPadding by remember { mutableStateOf(defaultButtonPadding) }
                    val adjustedTextStyle = CarTheme.carTypography.rowTitle.copy(fontSize = adjustedFontSize)

                    val tripDistanceSegments = listOf(
                        CarSegmentedButton.Segment(
                            content = {
                                Text(
                                    "100 km",
                                    style = adjustedTextStyle,
                                    maxLines = 1,
                                    onTextLayout = { if (it.multiParagraph.didExceedMaxLines) {
                                        adjustedFontSize *= 0.95f
                                        adjustedButtonPadding *= 0.9f
                                    } }
                                )
                            },
                            key = TripDistanceSegmentKeys.Dist100
                        ),
                        CarSegmentedButton.Segment(
                            content = { Text("40 km", style = adjustedTextStyle) },
                            key = TripDistanceSegmentKeys.Dist40
                        ),
                        CarSegmentedButton.Segment(
                            content = { Text("20 km", style = adjustedTextStyle) },
                            key = TripDistanceSegmentKeys.Dist20
                        ),
                        CarSegmentedButton.Segment(
                            content = { Text("Trip", style = adjustedTextStyle) },
                            key = TripDistanceSegmentKeys.Trip
                        ),
                    )

                    @Composable
                    fun SegmentIconButton(
                        @DrawableRes id: Int
                    ) {
                        Box(
                            modifier = Modifier.size(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painterResource(id),
                                null,
                                modifier = Modifier.size(CarTheme.carDimensions.iconButtonSize)
                            )
                        }
                    }

                    val secondaryPlotSegments = listOf(
                        CarSegmentedButton.Segment(
                            content = { SegmentIconButton(R.drawable.ic_speed) },
                            key = SecondaryPlotSegmentKeys.Speed
                        ),
                        CarSegmentedButton.Segment(
                            content = { SegmentIconButton(R.drawable.ic_altitude) },
                            key = SecondaryPlotSegmentKeys.Altitude
                        ),
                        CarSegmentedButton.Segment(
                            content = { SegmentIconButton(R.drawable.ic_battery) },
                            key = SecondaryPlotSegmentKeys.StateOfCharge
                        ),
                    )


                    CarSegmentedButton(
                        modifier = Modifier
                            .height(80.dp)
                            .weight(1f),
                        segments = tripDistanceSegments,
                        selectedKey = null,
                        dimensions = CarSegmentedButtonDefaults.dimensions.copy(
                            buttonHorizontalPadding = adjustedButtonPadding,
                        ),
                        onSegmentChanged = {}
                    )
                    Spacer(Modifier.size(CarTheme.carDimensions.defaultHorizontalPadding))

                    CarSegmentedButton(
                        modifier = Modifier
                            .height(80.dp)
                            .width(IntrinsicSize.Min),
                        dimensions = CarSegmentedButtonDefaults.dimensions.copy(
                            buttonMinWidth = 80.dp,
                            buttonMinHeight = 80.dp,
                            buttonHorizontalPadding = 0.dp,
                            buttonVerticalPadding = 0.dp
                        ),
                        segments = secondaryPlotSegments,
                        selectedKey = null,
                        onSegmentChanged = {  }
                    )
                }
            }
        )
    }
}

@Composable
fun TripDetailsMapSection(
    tripDetailsState: TripDetailsState
) {
    Mapbox.MapBoxContainer(
        modifier = Modifier,
        trip = tripDetailsState.drivingSession,
        useCarCompose = true,
        chargingMarkerOnClick = { }
    )
}

@Composable
internal fun CompactDataRow(
    iconPainter: Painter,
    text: String
) {
    Row(
        modifier = Modifier.padding(horizontal = CarTheme.carDimensions.defaultHorizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(CarTheme.carDimensions.defaultHorizontalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = iconPainter,
            contentDescription = null
        )
        Text(
            text = text,
            style = CarTheme.carTypography.rowTitle
        )
    }
}

@Composable
internal fun CompactDataColumn(
    modifier: Modifier = Modifier,
    drivingSession: DrivingSession
) {
    Column(
        modifier = modifier.padding(vertical = CarTheme.carDimensions.defaultVerticalPadding),
        verticalArrangement = Arrangement.spacedBy(CarTheme.carDimensions.defaultVerticalPadding)
    ) {
        CompactDataRow(
            iconPainter = painterResource(R.drawable.ic_distance),
            text = StringFormatters.getTraveledDistanceString(drivingSession.driven_distance.toFloat())
        )
        CompactDataRow(
            iconPainter = painterResource(R.drawable.ic_energy),
            text = StringFormatters.getEnergyString(drivingSession.used_energy.toFloat())
        )
        CompactDataRow(
            iconPainter = painterResource(R.drawable.ic_avg_consumption),
            text = StringFormatters.getAvgConsumptionString(drivingSession.used_energy.toFloat(), drivingSession.driven_distance.toFloat())
        )
        CompactDataRow(
            iconPainter = painterResource(R.drawable.ic_speed),
            text = StringFormatters.getAvgSpeedString(drivingSession.driven_distance.toFloat(), drivingSession.drive_time)
        )
//        CompactDataRow(
//            iconPainter = painterResource(R.drawable.ic_altitude),
//            text = ""
//        )
        CompactDataRow(
            iconPainter = painterResource(R.drawable.ic_time),
            text = StringFormatters.getElapsedTimeString(drivingSession.drive_time)
        )
    }
}