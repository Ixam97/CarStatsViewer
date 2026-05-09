package com.ixam97.carStatsViewer.carCompose.screens.tripDetails

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.ixam97.carStatsViewer.BuildConfig
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.carCompose.CarComposeGlobalViewModel
import com.ixam97.carStatsViewer.carCompose.deviceIsWideScreen
import com.ixam97.carStatsViewer.map.Mapbox
import de.ixam97.carcompose.components.controls.CarIconButton
import de.ixam97.carcompose.components.layout.CarPaneLayout
import de.ixam97.carcompose.components.layout.CarTabLayout
import de.ixam97.carcompose.theme.CarTheme
import kotlinx.serialization.Serializable

enum class TripDetailsTabKeys {
    Consumption, Charging, Map
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

    LaunchedEffect(tripDetailsState.isLoading) {
        globalViewModel.setLoading(tripDetailsState.isLoading)
    }

    if (deviceIsWideScreen() || tripDetailsState.debugLandscapeOverride) {
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
private fun TripDetailsPortraitScreen(
    globalViewModel: CarComposeGlobalViewModel,
    onBackClick: () -> Unit,
    viewModel: TripDetailsViewModel
) {
    val tripDetailsState by viewModel.tripDetailsState.collectAsState()
    val globalState by globalViewModel.globalState.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd
    ) {
        AnimatedVisibility(
            visible = !tripDetailsState.showChargingDetails,
            enter = slideInHorizontally(initialOffsetX = { -it }),
            exit = slideOutHorizontally(targetOffsetX = { -it })
        ) {
            val iconButtons = if (BuildConfig.FLAVOR_version == "dev") listOf(@Composable{ CarIconButton(painterResource(R.drawable.ic_debug)) { viewModel.setDebugOverride() } }) else listOf()
            CarPaneLayout(
                isLoading = globalState.isLoading,
                headerIconButtons = iconButtons,
                headerStartContent = {
                    Row(
                        // modifier = Modifier.offset(x = CarTheme.carDimensions.headerContentHorizontalPadding * -1)
                    ) {
                        HeaderTabButton(
                            title = stringResource(R.string.summary_tab_trip_details),
                            active = tripDetailsState.selectedTab == TripDetailsTabKeys.Consumption
                        ) { viewModel.setSelectedTab(TripDetailsTabKeys.Consumption) }

                        HeaderTabButton(
                            title = stringResource(R.string.summary_tab_charging_sessions),
                            active = tripDetailsState.selectedTab == TripDetailsTabKeys.Charging
                        ) { viewModel.setSelectedTab(TripDetailsTabKeys.Charging) }

                        HeaderTabButton(
                            title = stringResource(R.string.summary_tab_map),
                            active = tripDetailsState.selectedTab == TripDetailsTabKeys.Map
                        ) { viewModel.setSelectedTab(TripDetailsTabKeys.Map) }
                    }
                },
                onBackAction = onBackClick
            ) {
                Box() {

                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset()
                    ) {
                        val offset by animateIntOffsetAsState(
                            targetValue = IntOffset(x = if (tripDetailsState.selectedTab == TripDetailsTabKeys.Map) 0 else constraints.maxWidth, y = 0),
                            animationSpec = spring(
                                stiffness = Spring.StiffnessMediumLow,
                                visibilityThreshold = IntOffset.VisibilityThreshold
                            )
                        )
                        Box(
                            modifier = Modifier
                                .width(maxWidth)
                                .height(maxHeight)
                                .offset { offset }
                        ) {
                            TripDetailsMapSection(tripDetailsState, viewModel)
                        }
                    }

                    AnimatedVisibility(
                        visible = tripDetailsState.selectedTab == TripDetailsTabKeys.Consumption,
                        enter = slideInHorizontally(initialOffsetX = { -it }),
                        exit = slideOutHorizontally(targetOffsetX = { -it })
                    ) {
                        TripDetailsConsumptionSection(
                            viewModel = viewModel,
                            drivingSession = tripDetailsState.drivingSession
                        )
                    }

                    AnimatedVisibility(
                        visible = tripDetailsState.selectedTab == TripDetailsTabKeys.Charging,
                        enter = slideInHorizontally(initialOffsetX = { if (tripDetailsState.prevSelectedTab == TripDetailsTabKeys.Map) -it else it }),
                        exit = slideOutHorizontally(targetOffsetX = { if (tripDetailsState.selectedTab == TripDetailsTabKeys.Map) -it else it })
                    ) {
                        TripDetailsChargingSection(
                            viewModel,
                            tripDetailsState.chargingSessionsDetails
                        )
                    }
                }
            }
        }

        TripDetailsChargingDetailsOverlay(
            visible = tripDetailsState.showChargingDetails,
            viewModel = viewModel
        )
    }
}

@Composable
private fun TripDetailsLandscapeScreen(
    globalViewModel: CarComposeGlobalViewModel,
    onBackClick: () -> Unit,
    viewModel: TripDetailsViewModel
) {
    val tripDetailsState by viewModel.tripDetailsState.collectAsState()
    val globalState by globalViewModel.globalState.collectAsState()

    if (!tripDetailsState.isSideBySideLayout) viewModel.setSideBySideLayout(true)

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
                .fillMaxHeight(),
            contentAlignment = Alignment.BottomEnd
        ) {
            this@Row.AnimatedVisibility(
                visible = !tripDetailsState.showChargingDetails,
                enter = slideInHorizontally(initialOffsetX = { -it }),
                exit = slideOutHorizontally(targetOffsetX = { -it })
            ) {
                CarTabLayout(
                    isLoading = globalState.isLoading,
                    tabOrientation = CarTabLayout.Orientation.Vertical,
                    selectedKey = tripDetailsState.selectedTab,
                    tabs = tabs,
                    onTabSelected = { viewModel.setSelectedTab(it) },
                    headerTitle = stringResource(R.string.summary_title),
                    onBackAction = onBackClick
                ) { selectedKey ->
                    this@Row.AnimatedVisibility(
                        visible = selectedKey == TripDetailsTabKeys.Consumption,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) { TripDetailsConsumptionSection(viewModel, tripDetailsState.drivingSession) }

                    this@Row.AnimatedVisibility(
                        visible = selectedKey == TripDetailsTabKeys.Charging,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        TripDetailsChargingSection(
                            viewModel,
                            tripDetailsState.chargingSessionsDetails
                        )
                    }
                }
            }
            TripDetailsChargingDetailsOverlay(
                visible = tripDetailsState.showChargingDetails,
                viewModel = viewModel
            )
        }
        Box(
            modifier = Modifier
                .weight(0.80f)
                .fillMaxHeight()
        ) { TripDetailsMapSection(tripDetailsState, viewModel) }
    }
}

@Composable
private fun TripDetailsMapSection(
    tripDetailsState: TripDetailsState,
    viewModel: TripDetailsViewModel
) {
    Mapbox.MapBoxContainer(
        modifier = Modifier,
        trip = tripDetailsState.drivingSession,
        useCarCompose = true,
        chargingMarkerOnClick = { viewModel.setSelectedChargingDetails(it) },
        actionFlow = viewModel.mapAction
    )
}

@Composable
private fun HeaderTabButton(
    title: String,
    active: Boolean,
    onClick: () -> Unit
) {
    val textColor = if (active) CarTheme.carColors.accent else LocalContentColor.current
    val maxHeaderFontSize = if (CarTheme.carTypography.title.fontSize > 38.sp) 38.sp else CarTheme.carTypography.title.fontSize

    Box(
        modifier = Modifier
            .width(IntrinsicSize.Max)
            .height(CarTheme.carDimensions.iconButtonSize),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .requiredHeight(CarTheme.carDimensions.iconButtonSize * 1.6f)
                .clickable(onClick = onClick)
                .padding(horizontal = CarTheme.carDimensions.headerContentHorizontalPadding)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                color = textColor,
                style = CarTheme.carTypography.title.copy(fontSize = maxHeaderFontSize)
            )
        }
    }
}