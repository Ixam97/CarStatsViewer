package com.ixam97.carStatsViewer.carcompose.screen.tripDetails

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.carcompose.CarComposeViewModel
import com.ixam97.carStatsViewer.carcompose.theme.CarComposeIcon
import com.ixam97.carStatsViewer.map.Mapbox
import com.ixam97.carStatsViewer.utils.StringFormatters
import de.ixam97.carcompose.components.controls.CarButtonDefaults
import de.ixam97.carcompose.components.controls.CarIconButton
import de.ixam97.carcompose.components.controls.CarRow
import de.ixam97.carcompose.components.controls.CarSegmentedButton
import de.ixam97.carcompose.components.layout.CarListDivider
import de.ixam97.carcompose.components.layout.CarTabLayout
import de.ixam97.carcompose.theme.CarTheme

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun TripDetailsPortraitScreen(
    globalViewModel: CarComposeViewModel,
    onBackClick: () -> Unit,
    sessionId: Long
) {
    val factory = remember { TripDetailsViewModelFactory(sessionId) }
    val viewModel: TripDetailsViewModel = viewModel(factory = factory)

    val insets = WindowInsets.safeDrawing.asPaddingValues()

    Row() {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        )
        {
            CarTabLayout(
                isLoading = viewModel.tripDetailsState.isLoading,
                // tabOrientation = globalViewModel.carComposeState.tabLayoutOrientation,
                tabSelectedIndex = 0,
                tabs = listOf(
                    CarTabLayout.Tab(
                        title = "Trip Details",
                        icon = painterResource(id = R.drawable.ic_distance)
                    ),
                    CarTabLayout.Tab(
                        title = "Charging Sessions",
                        icon = painterResource(id = R.drawable.ic_charger)
                    ),
                    CarTabLayout.Tab(
                        title = "Map",
                        icon = rememberVectorPainter(Icons.Outlined.Map),
                        iconActive = rememberVectorPainter(Icons.Filled.Map)
                    ),
                ),
                tabOnIndexChanged = {},
                headerTitle = "Trip Summary",
                headerStartContent = {
                    CarIconButton(
                        painter = painterResource(R.drawable.ic_carcompose_close),
                        active = true,
                        onClick = onBackClick
                    )
                },
                headerIconButtons = listOf({
                    CarIconButton(
                        painter = painterResource(R.drawable.ic_carcompose_export),
                        enabled = false,
                        onClick = {  }
                    )
                })
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
                    CarRow {
                        Row() {
                            CarSegmentedButton(
                                modifier = Modifier
                                    .height(80.dp)
                                    .weight(1f),
                                buttonContents = listOf(
                                    { Text("100 km") },
                                    { Text("40 km") },
                                    { Text("20 km") },
                                    { Text("Trip") },
                                ),
                                selectedIndex = -1,
                                onIndexChanged = {  }
                            )
                            Spacer(Modifier.size(CarTheme.carDimensions.defaultHorizontalPadding))

                            CarSegmentedButton(
                                modifier = Modifier
                                    .height(80.dp)
                                    .width(IntrinsicSize.Min),
                                dimensions = CarButtonDefaults.dimensions.copy(
                                    minWidth = 0.dp,
                                    minHeight = 80.dp,
                                    horizontalPadding = 0.dp,
                                    verticalPadding = 0.dp
                                ),
                                buttonContents = listOf(
                                    { Icon(
                                        painterResource(R.drawable.ic_speed),
                                        null,
                                        modifier = Modifier
                                            .height(CarTheme.carDimensions.iconButtonSize)
                                            .width(80.dp)
                                    ) },
                                    { Icon(
                                        painterResource(R.drawable.ic_altitude),
                                        null,
                                        modifier = Modifier
                                            .height(CarTheme.carDimensions.iconButtonSize)
                                            .width(80.dp)
                                    ) },
                                    { Icon(
                                        painterResource(R.drawable.ic_battery),
                                        null,
                                        modifier = Modifier
                                            .height(CarTheme.carDimensions.iconButtonSize)
                                            .width(80.dp)
                                    ) },
                                ),
                                selectedIndex = 0,
                                onIndexChanged = {  }
                            )
                        }

                    }
                }
            }
        }
        if (globalViewModel.carComposeState.uiTypeIndex == 1) {
            Box(
                modifier = Modifier
                    .weight(0.75f)
                    .padding(
                        top = insets.calculateTopPadding(),
                        bottom = insets.calculateBottomPadding()
                    )
                    .fillMaxHeight()
            ) {
                Mapbox.MapBoxContainer(
                    modifier = Modifier,
                    trip = null,
                    useCarCompose = true,
                    chargingMarkerOnClick = { }
                )
            }
        }
    }
}