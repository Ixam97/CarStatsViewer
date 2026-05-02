package com.ixam97.carStatsViewer.carCompose.screens.tripDetails

import android.annotation.SuppressLint
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.carCompose.CarComposeGlobalViewModel
import com.ixam97.carStatsViewer.carCompose.deviceIsWideScreen
import com.ixam97.carStatsViewer.carCompose.theme.CarComposeIcon
import com.ixam97.carStatsViewer.map.Mapbox
import com.ixam97.carStatsViewer.utils.StringFormatters
import de.ixam97.carcompose.components.controls.CarIconButton
import de.ixam97.carcompose.components.controls.CarRow
import de.ixam97.carcompose.components.controls.CarSegmentedButton
import de.ixam97.carcompose.components.controls.CarSegmentedButtonDefaults
import de.ixam97.carcompose.components.layout.CarListDivider
import de.ixam97.carcompose.components.layout.CarTabLayout
import de.ixam97.carcompose.theme.CarTheme
import kotlinx.serialization.Serializable

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
fun TripDetailsPortraitScreen(
    globalViewModel: CarComposeGlobalViewModel,
    onBackClick: () -> Unit,
    sessionId: Long,
    viewModel: TripDetailsViewModel = viewModel {
        TripDetailsViewModel(sessionId)
    }
) {
    val tripDetailsState by viewModel.tripDetailsState.collectAsState()

    val tabs = mutableListOf(
        CarTabLayout.Tab(
            title = "Trip Details",
            icon = painterResource(id = R.drawable.ic_distance),
            key = TripDetailsTabKeys.Consumption
        ),
//        CarTabLayout.Tab(
//            title = "Diagram",
//            icon = painterResource(id = R.drawable.ic_diagram),
//            key = TripDetailsTabKeys.Diagram
//        ),
        CarTabLayout.Tab(
            title = "Charging Sessions",
            icon = painterResource(id = R.drawable.ic_charger),
            key = TripDetailsTabKeys.Charging
        )
    )

    if (!deviceIsWideScreen()) {
        tabs.add(CarTabLayout.Tab(
            title = "Map",
            icon = rememberVectorPainter(Icons.Outlined.Map),
            key = TripDetailsTabKeys.Map
        ))
    }

    Row() {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        )
        {
            CarTabLayout(
                isLoading = tripDetailsState.isLoading,
                tabOrientation = if (deviceIsWideScreen()) CarTabLayout.Orientation.Vertical else CarTabLayout.Orientation.HorizontalCompact,
                selectedKey = tripDetailsState.selectedTab,
                tabs = tabs,
                onTabSelected = { viewModel.setSelectedTab(it) },
                headerTitle = "Trip Summary",
                onBackAction = onBackClick,
                headerIconButtons = listOf({
                    CarIconButton(
                        painter = painterResource(R.drawable.ic_carcompose_export),
                        enabled = false,
                        onClick = {  }
                    )
                })
            ) { selectedKey ->
                when(selectedKey) {
                    TripDetailsTabKeys.Consumption -> TripDetailsConsumptionSection(tripDetailsState)
                    TripDetailsTabKeys.Diagram -> {}
                    TripDetailsTabKeys.Charging -> {}
                    TripDetailsTabKeys.Map -> TripDetailsMapSection(tripDetailsState)
                }
            }
        }
        if (deviceIsWideScreen()) {
            Box(
                modifier = Modifier
                    .weight(0.70f)
                    .fillMaxHeight()
            ) {
                TripDetailsMapSection(tripDetailsState)
            }
        }
    }
}

@Composable
fun TripDetailsConsumptionSection(
    tripDetailsState: TripDetailsState
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
    ) {
        CarRow(
            title = "Placeholder"
        )
        CarListDivider()
        Row {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                CarRow(
                    title = StringFormatters.getTraveledDistanceString(0f),
                    description = stringResource(R.string.summary_traveled_distance),
                    leadingContent = { CarComposeIcon(R.drawable.ic_distance) }
                )
                CarListDivider()
                CarRow(
                    title = StringFormatters.getEnergyString(0f),
                    description = stringResource(R.string.summary_used_energy),
                    leadingContent = { CarComposeIcon(R.drawable.ic_energy_large) }
                )
                CarListDivider()
                CarRow(
                    title = StringFormatters.getAvgConsumptionString(0f, 0f),
                    description = stringResource(R.string.summary_average_consumption),
                    leadingContent = { CarComposeIcon(R.drawable.ic_avg_consumption) }
                )
            }
            Column(
                modifier = Modifier.weight(1f)
            ) {
                CarRow(
                    title = StringFormatters.getAvgSpeedString(0f, 0),
                    description = stringResource(R.string.summary_speed),
                    leadingContent = { CarComposeIcon(R.drawable.ic_speed) }
                )
                CarListDivider()
                CarRow(
                    title = "#elevationString",
                    description = stringResource(R.string.summary_altitude),
                    leadingContent = { CarComposeIcon(R.drawable.ic_altitude)}
                )
                CarListDivider()
                CarRow(
                    title = StringFormatters.getElapsedTimeString(0, true),
                    description = stringResource(R.string.summary_travel_time),
                    leadingContent = { CarComposeIcon(R.drawable.ic_time) }
                )
            }
        }
        CarListDivider()
        Spacer(Modifier.weight(1f))
        CarRow(
            content = {
                Row() {

                    val tripDistanceSegments = listOf(
                        CarSegmentedButton.Segment(
                            content = { Text("100 km") },
                            key = TripDistanceSegmentKeys.Dist100
                        ),
                        CarSegmentedButton.Segment(
                            content = { Text("40 km") },
                            key = TripDistanceSegmentKeys.Dist40
                        ),
                        CarSegmentedButton.Segment(
                            content = { Text("20 km") },
                            key = TripDistanceSegmentKeys.Dist20
                        ),
                        CarSegmentedButton.Segment(
                            content = { Text("Trip") },
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