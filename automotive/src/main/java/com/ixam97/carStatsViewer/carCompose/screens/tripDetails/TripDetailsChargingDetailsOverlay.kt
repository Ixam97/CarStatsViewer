package com.ixam97.carStatsViewer.carCompose.screens.tripDetails

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.ixam97.carStatsViewer.map.MapboxInterface
import de.ixam97.carcompose.components.controls.CarRow
import de.ixam97.carcompose.components.layout.CarColumn
import de.ixam97.carcompose.components.layout.CarListItem
import de.ixam97.carcompose.components.layout.CarListSection
import de.ixam97.carcompose.components.layout.CarPaneLayout

@Composable
fun TripDetailsChargingDetailsOverlay(
    visible: Boolean,
    viewModel: TripDetailsViewModel
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(initialOffsetX = { it }),
        exit = slideOutHorizontally(targetOffsetX = { it })
    ) {

        val tripDetailsState by viewModel.tripDetailsState.collectAsState()

        CarPaneLayout(
            headerTitle = "Charging Details",
            onBackAction = { viewModel.closeChargingDetails() }
        ) {
            tripDetailsState.chargingSessionsDetails.firstOrNull { it.chargingSession.charging_session_id == tripDetailsState.selectedChargingSessionDetailsId }?.let { chargingSessionDetails ->
                CarColumn() {
                    CarListSection(
                        listItems = listOf(
                            CarListItem {
                                CarRow(
                                    title = chargingSessionDetails.chargingLocation,
                                    description = "Location",
                                    browsable = true,
                                    onBrowse = {
                                        if (chargingSessionDetails.chargingSession.lon != null && chargingSessionDetails.chargingSession.lat != null) {
                                            viewModel.setMapLocation(
                                                MapboxInterface.MapboxLocation(
                                                    lon = chargingSessionDetails.chargingSession.lon.toDouble(),
                                                    lat = chargingSessionDetails.chargingSession.lat.toDouble(),
                                                    zoom = 14.5
                                                )
                                            )
                                        }
                                    }
                                )
                            }
                        )
                    )
                }
            }
        }
    }
}