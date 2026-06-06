package com.ixam97.carStatsViewer.carCompose.screens.tripDetails

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.carCompose.theme.CarComposeIcon
import com.ixam97.carStatsViewer.compose.theme.polestarOrange
import com.ixam97.carStatsViewer.database.tripData.DrivingSession
import com.ixam97.carStatsViewer.utils.StringFormatters
import de.ixam97.carcompose.components.controls.CarButton
import de.ixam97.carcompose.components.controls.CarRow
import de.ixam97.carcompose.components.controls.CarSegmentedButton
import de.ixam97.carcompose.components.controls.CarSegmentedButtonDefaults
import de.ixam97.carcompose.components.layout.CarColumn
import de.ixam97.carcompose.components.layout.CarListDivider
import de.ixam97.carcompose.components.layout.CarListItem
import de.ixam97.carcompose.components.layout.CarListSection
import de.ixam97.carcompose.components.layout.LocalCarSnackBarState
import de.ixam97.carcompose.theme.CarTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Date

/**
 * ViewModel to store the currently configured layout across visibility changes of the details
 * section.
 */
class TripDetailsConsumptionSectionLayoutStateViewModel(): ViewModel() {

    data class LayoutState(
        val collapsedGraph: Boolean = false,
        val compactDataSectionHeight: Int? = null
    )

    private val _layoutState = MutableStateFlow(LayoutState())
    val layoutState = _layoutState.asStateFlow()

    fun setCollapsedGraph(collapsed: Boolean) {
        _layoutState.update { it.copy(collapsedGraph = collapsed) }
    }

    fun setCompactDataSectionHeight(height: Int) {
        _layoutState.update { it.copy(compactDataSectionHeight = height) }
    }
}

@Composable
internal fun TripDetailsConsumptionSection(
    viewModel: TripDetailsViewModel,
    drivingSession: DrivingSession?
) {
    drivingSession.let { drivingSession ->
        if (drivingSession != null)
            TripDetailsConsumptionSectionContent(
                viewModel = viewModel,
                drivingSession = drivingSession
            )
        else Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) { CircularProgressIndicator() }
    }
}

@Composable
private fun TripDetailsConsumptionSectionContent(
    viewModel: TripDetailsViewModel,
    drivingSession: DrivingSession
) {
    val layoutStateViewModel: TripDetailsConsumptionSectionLayoutStateViewModel = viewModel()
    val tripDetailsState by viewModel.tripDetailsState.collectAsState()
    val layoutState by layoutStateViewModel.layoutState.collectAsState()
    val density = LocalDensity.current

    Column(
        modifier = Modifier
            .fillMaxHeight()
    ) {
        Row(
            modifier = layoutState.compactDataSectionHeight.run {
                if (this != null) {
                    Modifier
                        .height(with(density) { this@run.toDp() } )
                } else {
                    Modifier
                        .height(IntrinsicSize.Min)
                        .onGloballyPositioned { layoutStateViewModel.setCompactDataSectionHeight(it.size.height) }
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .weight(1.5f)
                    .fillMaxHeight()
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
                        val lineColor = polestarOrange // CarTheme.carColors.accent
                        val pixelOffset = with(LocalDensity.current){ CarTheme.carDimensions.defaultVerticalPadding.toPx() }
                        Image(
                            painter = painterResource(R.drawable.ic_trip_start),
                            contentDescription = null
                        )
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
                        description = tripDetailsState.startLocation?:stringResource(R.string.summary_loading_location)
                    )
                }
                drivingSession.end_epoch_time.let { endTime ->
                    if (endTime != null && endTime > 0) {
                        Row() {
                            Image(
                                painter = painterResource(R.drawable.ic_trip_destination),
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(
                                        start = CarTheme.carDimensions.defaultHorizontalPadding,
                                        top = CarTheme.carDimensions.defaultVerticalPadding
                                    )
                            )
                            CarRow(
                                title = StringFormatters.getDateString(Date(endTime)),
                                description = tripDetailsState.destinationLocation?:stringResource(R.string.summary_loading_location)
                            )
                        }
                    } else {
                        Row(verticalAlignment = Alignment.Top) {
                            CarComposeIcon(
                                resID = R.drawable.ic_car,
                                modifier = Modifier
                                    .padding(
                                        start = CarTheme.carDimensions.defaultHorizontalPadding,
                                        top = CarTheme.carDimensions.defaultVerticalPadding / 2
                                    )
                                    // .rotate(180f)
                            )
                            CarRow(
                                title = stringResource(R.string.summary_ongoing),
                                description = tripDetailsState.destinationLocation?:stringResource(R.string.summary_loading_location)
                            )
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
            if (!layoutState.collapsedGraph)
                CompactDataColumn(
                    modifier = Modifier.weight(1f),
                    drivingSession = drivingSession,
                    onClick = { layoutStateViewModel.setCollapsedGraph(true) }
                )
            else
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(
                            horizontal = CarTheme.carDimensions.defaultHorizontalPadding,
                            vertical = CarTheme.carDimensions.defaultVerticalPadding
                        ),
                    verticalArrangement = Arrangement.spacedBy(CarTheme.carDimensions.defaultVerticalPadding)
                ) {
                    val snackBarHostState = LocalCarSnackBarState.current
                    CarButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 70.dp),
                        onClick = { viewModel.exportTrip(
                            sessionID = drivingSession.driving_session_id,
                            snackBarHostState = snackBarHostState
                        ) },
                        enabled = CarStatsViewer.appPreferences.dataExportEnabled
                    ) {
                        Icon(
                            modifier = Modifier.size(40.dp),
                            painter = painterResource(R.drawable.ic_mail),
                            contentDescription = null
                        )
                        Text("Export trip")
                    }
                    CarButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 70.dp),
                        onClick = {  },
                        enabled = false
                    ) {
                        Icon(
                            modifier = Modifier.size(40.dp),
                            painter = rememberVectorPainter(Icons.Outlined.Share),
                            contentDescription = null
                        )
                        Text("Share via API")
                    }
                }
        }
        AnimatedVisibility(
            modifier = Modifier.weight(1f),
            visible = layoutState.collapsedGraph,
            enter = expandVertically(expandFrom = Alignment.Top),
            exit = shrinkVertically(shrinkTowards = Alignment.Top)
        ) {
            Column() {
                CarListDivider()
                CarColumn(
                    modifier = Modifier.fillMaxHeight()
                ) {
                    CarListSection(
                        listItems = listOf(
                            CarListItem {
                                StandardDataRow(
                                    iconPainter = painterResource(R.drawable.ic_distance),
                                    valueText = StringFormatters.getTraveledDistanceString(drivingSession.driven_distance.toFloat()),
                                    descriptionText = stringResource(R.string.summary_traveled_distance)
                                )
                            },
                            CarListItem {
                                StandardDataRow(
                                    iconPainter = painterResource(R.drawable.ic_energy),
                                    valueText = StringFormatters.getEnergyString(drivingSession.used_energy.toFloat()),
                                    descriptionText = stringResource(R.string.summary_used_energy)
                                )
                            },
                            CarListItem {
                                StandardDataRow(
                                    iconPainter = painterResource(R.drawable.ic_avg_consumption),
                                    valueText = StringFormatters.getAvgConsumptionString(drivingSession.used_energy.toFloat(), drivingSession.driven_distance.toFloat()),
                                    descriptionText = stringResource(R.string.summary_average_consumption)
                                )
                            },
                            CarListItem {
                                StandardDataRow(
                                    iconPainter = painterResource(R.drawable.ic_speed),
                                    valueText = StringFormatters.getAvgSpeedString(drivingSession.driven_distance.toFloat(), drivingSession.drive_time),
                                    descriptionText = stringResource(R.string.summary_speed)
                                )
                            },
                            CarListItem {
                                StandardDataRow(
                                    iconPainter = painterResource(R.drawable.ic_altitude),
                                    valueText = "Placeholder",
                                    descriptionText = stringResource(R.string.summary_altitude)
                                )
                            },
                            CarListItem {
                                StandardDataRow(
                                    iconPainter = painterResource(R.drawable.ic_time),
                                    valueText = StringFormatters.getElapsedTimeString(drivingSession.drive_time),
                                    descriptionText = stringResource(R.string.summary_travel_time)
                                )
                            },
                        )
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .padding(bottom = if (layoutState.collapsedGraph) 24.dp else 15.dp)
                .fillMaxWidth()
                .height(2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .requiredHeight(48.dp)
                    .clickable { layoutStateViewModel.setCollapsedGraph(!layoutState.collapsedGraph) },
                verticalAlignment = Alignment.CenterVertically
            ){
                Box(Modifier.weight(1f)) {
                    CarListDivider()
                }
                Box(
                    modifier = Modifier.width(IntrinsicSize.Min),
                    contentAlignment = Alignment.Center
                ) {
                    Box(Modifier
                        .height(if (layoutState.collapsedGraph) 48.dp else 20.dp)
                        .fillMaxWidth()
                        .clip(CarTheme.carShapes.buttonShape)
                        .background(CarTheme.carColors.secondarySurfaceBrush)
                    )
                    Icon(
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .rotate(if (layoutState.collapsedGraph) 0f else 180f),
                        painter = painterResource(if (layoutState.collapsedGraph) R.drawable.ic_diagram else R.drawable.ic_chevron_up),
                        contentDescription = null
                    )
                }
                Box(Modifier.weight(1f)) {
                    CarListDivider()
                }
            }
        }

        AnimatedVisibility(
            visible = !layoutState.collapsedGraph,
            enter = expandVertically(expandFrom = Alignment.Bottom),
            exit = shrinkVertically(shrinkTowards = Alignment.Bottom)
        ) {
            Column(
                modifier = Modifier.fillMaxHeight()
            ) {
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
                            val defaultButtonPadding =
                                CarTheme.carDimensions.segmentedButtonDimensions.buttonHorizontalPadding
                            var adjustedFontSize by remember { mutableStateOf(defaultFontSize) }
                            var adjustedButtonPadding by remember {
                                mutableStateOf(
                                    defaultButtonPadding
                                )
                            }
                            val adjustedTextStyle =
                                CarTheme.carTypography.rowTitle.copy(fontSize = adjustedFontSize)

                            val tripDistanceSegments = listOf(
                                CarSegmentedButton.Segment(
                                    content = {
                                        Text(
                                            "100 km",
                                            style = adjustedTextStyle,
                                            maxLines = 1,
                                            onTextLayout = {
                                                if (it.multiParagraph.didExceedMaxLines) {
                                                    adjustedFontSize *= 0.95f
                                                    adjustedButtonPadding *= 0.9f
                                                }
                                            }
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
                                onSegmentChanged = { }
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun CompactDataRow(
    iconPainter: Painter,
    valueText: String
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
            text = valueText,
            style = CarTheme.carTypography.rowTitle
        )
    }
}

@Composable
private fun StandardDataRow(
    iconPainter: Painter,
    valueText: String,
    descriptionText: String
) {
    CarRow(
        title = valueText,
        description = descriptionText,
        leadingContent = {
            Icon(
                modifier = Modifier.size(CarTheme.carDimensions.iconButtonSize),
                painter = iconPainter,
                contentDescription = null,
            )
        }
    )
}

@Composable
private fun CompactDataColumn(
    modifier: Modifier = Modifier,
    drivingSession: DrivingSession,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = CarTheme.carDimensions.defaultVerticalPadding),
        verticalArrangement = Arrangement.spacedBy(CarTheme.carDimensions.defaultVerticalPadding)
    ) {
        CompactDataRow(
            iconPainter = painterResource(R.drawable.ic_distance),
            valueText = StringFormatters.getTraveledDistanceString(drivingSession.driven_distance.toFloat())
        )
        CompactDataRow(
            iconPainter = painterResource(R.drawable.ic_energy),
            valueText = StringFormatters.getEnergyString(drivingSession.used_energy.toFloat())
        )
        CompactDataRow(
            iconPainter = painterResource(R.drawable.ic_avg_consumption),
            valueText = StringFormatters.getAvgConsumptionString(drivingSession.used_energy.toFloat(), drivingSession.driven_distance.toFloat())
        )
        CompactDataRow(
            iconPainter = painterResource(R.drawable.ic_speed),
            valueText = StringFormatters.getAvgSpeedString(drivingSession.driven_distance.toFloat(), drivingSession.drive_time)
        )
//        CompactDataRow(
//            iconPainter = painterResource(R.drawable.ic_altitude),
//            text = ""
//        )
        CompactDataRow(
            iconPainter = painterResource(R.drawable.ic_time),
            valueText = StringFormatters.getElapsedTimeString(drivingSession.drive_time)
        )
    }
}