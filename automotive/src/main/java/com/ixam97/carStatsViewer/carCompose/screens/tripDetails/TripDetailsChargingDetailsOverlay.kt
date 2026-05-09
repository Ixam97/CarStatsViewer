package com.ixam97.carStatsViewer.carCompose.screens.tripDetails

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import de.ixam97.carcompose.components.controls.CarButton
import de.ixam97.carcompose.components.layout.CarPaneLayout
import de.ixam97.carcompose.theme.CarTheme

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
            CarButton(
                modifier = Modifier.padding(
                    horizontal = CarTheme.carDimensions.defaultHorizontalPadding,
                    vertical = CarTheme.carDimensions.defaultVerticalPadding
                ),
                onClick = { viewModel.setLocation() }
            ) { Text("Debug Location Trigger") }
        }
    }
}