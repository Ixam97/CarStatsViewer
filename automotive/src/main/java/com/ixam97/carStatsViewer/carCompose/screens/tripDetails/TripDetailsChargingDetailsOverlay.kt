package com.ixam97.carStatsViewer.carCompose.screens.tripDetails

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.carCompose.theme.CarComposeIcon
import com.ixam97.carStatsViewer.map.MapboxInterface
import com.ixam97.carStatsViewer.utils.StringFormatters
import de.ixam97.carcompose.components.controls.CarButton
import de.ixam97.carcompose.components.controls.CarRow
import de.ixam97.carcompose.components.layout.CarListDivider
import de.ixam97.carcompose.components.layout.CarPaneLayout
import de.ixam97.carcompose.theme.CarTheme
import java.util.Date
import kotlin.math.roundToInt

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
            tripDetailsState.chargingSessionsDetails.firstOrNull { it.chargingSession.charging_session_id == tripDetailsState.selectedChargingSessionDetailsId }.let { chargingSessionDetails ->

                if (chargingSessionDetails != null) {
                    TripDetailsChargingDetailsOverlayContent(viewModel, chargingSessionDetails)
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Charging session with ID ${tripDetailsState.selectedChargingSessionDetailsId} could not be found int this trip!",
                            style = CarTheme.carTypography.rowTitle
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TripDetailsChargingDetailsOverlayContent(
    viewModel: TripDetailsViewModel,
    chargingSessionDetails: ChargingSessionDetails
) {
    val tripDetailsState by viewModel.tripDetailsState.collectAsState()

    if (chargingSessionDetails.chargingSession.end_epoch_time == null) return

    val endSoc = tripDetailsState.drivingSession?.drivingPoints?.find {
        it.driving_point_epoch_time >= chargingSessionDetails.chargingSession.end_epoch_time
    }?.state_of_charge?: chargingSessionDetails.chargingSession.chargingPoints?.last()?.state_of_charge
    val startSoc = tripDetailsState.drivingSession?.drivingPoints?.findLast {
        it.driving_point_epoch_time <= chargingSessionDetails.chargingSession.start_epoch_time
    }?.state_of_charge?: chargingSessionDetails.chargingSession.chargingPoints?.first()?.state_of_charge

    val socString = if (endSoc != null && startSoc != null) {
        String.format(
            "%d%%  →  %d%%",
            (startSoc * 100f).roundToInt(),
            (endSoc * 100f).roundToInt(),
        )
    } else stringResource(R.string.summary_soc_unavailable)

    Column(
        modifier = Modifier
            .fillMaxHeight()
    ) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min)
        ) {
            Column(
                modifier = Modifier
                    .weight(1.5f)
                    .fillMaxHeight()
            ) {
                CarRow(
                    title = "${StringFormatters.getDateString(Date(chargingSessionDetails.chargingSession.start_epoch_time))}, $socString",
                    description = chargingSessionDetails.chargingLocation,
                    leadingContent = { CarComposeIcon(resID = R.drawable.ic_location_charge) },
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
                CarListDivider()
                CarRow(
                    title = StringFormatters.getEnergyString(chargingSessionDetails.chargingSession.charged_energy.toFloat()),
                    description = stringResource(R.string.summary_charged_energy)
                )
                CarListDivider()
                CarRow(
                    title = StringFormatters.getElapsedTimeString(chargingSessionDetails.chargingSession.chargeTime),
                    description = stringResource(R.string.summary_charge_time)
                )
            }
            Box(
                modifier = Modifier
                    .padding(vertical = CarTheme.carDimensions.defaultVerticalPadding)
                    .fillMaxHeight()
                    .width(2.dp)
                    .background(brush = CarTheme.carColors.secondaryDivider)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        horizontal = CarTheme.carDimensions.defaultHorizontalPadding,
                        vertical = CarTheme.carDimensions.defaultVerticalPadding
                    ),
                verticalArrangement = Arrangement.spacedBy(CarTheme.carDimensions.defaultVerticalPadding)
            ) {
                CarButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 70.dp),
                    onClick = {  },
                    enabled = false
                ) {
                    Icon(
                        modifier = Modifier.size(40.dp),
                        painter = painterResource(R.drawable.ic_mail),
                        contentDescription = null
                    )
                    Text("Export")
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
                    Text("Share")
                }
            }
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
                text = "The Charging Curve will return Soon™",
                style = CarTheme.carTypography.rowTitle
            )
        }
    }
}