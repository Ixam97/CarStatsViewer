package com.ixam97.carStatsViewer.carCompose.screens.tripDetails

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.carCompose.CarComposeGlobalViewModel
import com.ixam97.carStatsViewer.carCompose.components.ChargeCurveGraph
import com.ixam97.carStatsViewer.carCompose.deviceIsWideScreen
import com.ixam97.carStatsViewer.carCompose.theme.polestar4ContentPadding
import com.ixam97.carStatsViewer.compose.theme.clubHint
import com.ixam97.carStatsViewer.compose.theme.clubHintDark
import com.ixam97.carStatsViewer.map.MapboxInterface
import com.ixam97.carStatsViewer.utils.StringFormatters
import de.ixam97.carcompose.components.controls.CarButton
import de.ixam97.carcompose.components.controls.CarRow
import de.ixam97.carcompose.components.layout.CarListDivider
import de.ixam97.carcompose.components.layout.CarPaneLayout
import de.ixam97.carcompose.components.layout.LocalCarSnackBarState
import de.ixam97.carcompose.theme.CarTheme
import java.util.Date
import kotlin.math.roundToInt

@Composable
fun TripDetailsChargingDetailsOverlay(
    visible: Boolean,
    viewModel: TripDetailsViewModel,
    globalViewModel: CarComposeGlobalViewModel
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(initialOffsetX = { it }),
        exit = slideOutHorizontally(targetOffsetX = { it })
    ) {

        val tripDetailsState by viewModel.tripDetailsState.collectAsState()

        CarPaneLayout(
            headerTitle = stringResource(R.string.summary_charging_session_title),
            onBackAction = { viewModel.closeChargingDetails() }
        ) {
            tripDetailsState.chargingSessionsDetails.firstOrNull { it.chargingSession.charging_session_id == tripDetailsState.selectedChargingSessionDetailsId }.let { chargingSessionDetails ->

                if (chargingSessionDetails != null) {
                    TripDetailsChargingDetailsOverlayContent(
                        modifier = Modifier.padding(start = if (deviceIsWideScreen(2000.dp)) polestar4ContentPadding else 0.dp),
                        viewModel = viewModel,
                        globalViewModel = globalViewModel,
                        chargingSessionDetails =  chargingSessionDetails
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = if (deviceIsWideScreen()) polestar4ContentPadding else 0.dp)
                            .padding(CarTheme.carDimensions.defaultHorizontalPadding)
                            .background(CarTheme.carColors.primarySurface)
                            .padding(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Charging session with ID ${tripDetailsState.selectedChargingSessionDetailsId} could not be found in this trip!",
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
    modifier: Modifier = Modifier,
    viewModel: TripDetailsViewModel,
    globalViewModel: CarComposeGlobalViewModel,
    chargingSessionDetails: ChargingSessionDetails
) {
    val tripDetailsState by viewModel.tripDetailsState.collectAsState()

    if (chargingSessionDetails.chargingSession.end_epoch_time == null) {
        Text("This charging session contains no end time!")
        return
    }

    val endSoc = if (tripDetailsState.drivingSession?.drivingPoints?.isNotEmpty() == true
        && chargingSessionDetails.chargingSession.chargingPoints?.isNotEmpty() == true) {
        tripDetailsState.drivingSession?.drivingPoints?.find {
            it.driving_point_epoch_time >= chargingSessionDetails.chargingSession.end_epoch_time
        }?.state_of_charge?: chargingSessionDetails.chargingSession.chargingPoints?.last()?.state_of_charge
    } else { 0f }
    val startSoc = if (tripDetailsState.drivingSession?.drivingPoints?.isNotEmpty() == true
        && chargingSessionDetails.chargingSession.chargingPoints?.isNotEmpty() == true) {
        tripDetailsState.drivingSession?.drivingPoints?.findLast {
            it.driving_point_epoch_time <= chargingSessionDetails.chargingSession.start_epoch_time
        }?.state_of_charge?: chargingSessionDetails.chargingSession.chargingPoints?.first()?.state_of_charge
    } else { 0f }

    val socString = if (endSoc != null && startSoc != null) {
        "${(startSoc * 100f).roundToInt()}% → ${(endSoc * 100f).roundToInt()}%"
    } else stringResource(R.string.summary_soc_unavailable)

    val curveInterrupted: Boolean =
        (chargingSessionDetails.chargingSession.chargingPoints?.filter { it.point_marker_type == 2}?.size?:0) > 1



    Column(
        modifier = modifier
            .fillMaxHeight()
    ) {
//        Row(
//            modifier = Modifier.height(IntrinsicSize.Min)
//        ) {
            Column(
                modifier = Modifier
                    // .weight(1.5f)
                    .height(IntrinsicSize.Min)
            ) {
                CarRow(
                    title = StringFormatters.getDateString(Date(chargingSessionDetails.chargingSession.start_epoch_time)),
                    description = chargingSessionDetails.chargingLocation,
                    leadingContent = {
                        Image(
                            modifier = Modifier.size(CarTheme.carDimensions.iconButtonSize),
                            painter = painterResource(R.drawable.ic_trip_charging_location),
                            contentDescription =  null)
                    },
                    browsable = true,
                    onBrowse = {
                        if (chargingSessionDetails.chargingSession.lon != null && chargingSessionDetails.chargingSession.lat != null) {
                            viewModel.setMapLocation(
                                MapboxInterface.MapboxLocation(
                                    lon = chargingSessionDetails.chargingSession.lon.toDouble(),
                                    lat = chargingSessionDetails.chargingSession.lat.toDouble(),
                                )
                            )
                        }
                    }
                )
                CarListDivider()
                Row(
                    modifier = Modifier.height(IntrinsicSize.Min)
                ) {
                    CarRow(
                        modifier = Modifier.weight(1f),
                        title = StringFormatters.getEnergyString(chargingSessionDetails.chargingSession.charged_energy.toFloat()) + " ($socString)",
                        description = stringResource(R.string.summary_charged_energy),
                        leadingContent = {
                            Icon(
                                modifier = Modifier.size(CarTheme.carDimensions.iconButtonSize),
                                painter = painterResource(R.drawable.ic_battery),
                                contentDescription = null,
                            )
                        }
//                        descriptionContent = {
//                            Text(
//                                text = stringResource(R.string.summary_charged_energy),
//                                style = CarTheme.carTypography.rowContent,
//                                color = LocalContentColor.current.copy(alpha =  0.7f)
//                            )
//                            if (curveInterrupted) {
//                                Spacer(Modifier.size(CarTheme.carDimensions.rowTextSpacing))
//                                Text(
//                                    text = stringResource(R.string.summary_interruption_warning),
//                                    style = CarTheme.carTypography.rowContent,
//                                    color = clubHint
//                                )
//                            }
//                        }
                    )
                    Box(
                        modifier = Modifier
                            .padding(vertical = CarTheme.carDimensions.defaultVerticalPadding)
                            .fillMaxHeight()
                            .width(2.dp)
                            .background(brush = CarTheme.carColors.secondaryDivider)
                    )
                    CarRow(
                        modifier = Modifier.weight(1f),
                        title = StringFormatters.getElapsedTimeString(chargingSessionDetails.chargingSession.end_epoch_time - chargingSessionDetails.chargingSession.start_epoch_time),
                        description = stringResource(R.string.summary_charge_time),
                        leadingContent = {
                            Icon(
                                modifier = Modifier.size(CarTheme.carDimensions.iconButtonSize),
                                painter = painterResource(R.drawable.ic_time),
                                contentDescription = null,
                            )
                        }
                    )
                }
//                if (curveInterrupted) {
//                    CarListDivider()
//                    CarRow(
//                        title = stringResource(R.string.summary_interruption_warning),
//                        foregroundColor = clubHint
//                    )
//                }
                CarListDivider()
                if (curveInterrupted) {
                    Row (
                        modifier = Modifier
                            .padding(
                                horizontal = CarTheme.carDimensions.defaultHorizontalPadding,
                            )
                            .padding(top = CarTheme.carDimensions.defaultVerticalPadding)
                            .clip(CarTheme.carShapes.buttonShape)
                            .background(if (CarTheme.useDarkTheme) clubHintDark else clubHint)
                            .padding(
                                horizontal = CarTheme.carDimensions.defaultHorizontalPadding,
                                vertical = CarTheme.carDimensions.defaultVerticalPadding
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(CarTheme.carDimensions.defaultHorizontalPadding)
                    ) {
                        Icon(
                            modifier = Modifier.size(CarTheme.carDimensions.iconButtonSize),
                            painter = painterResource(R.drawable.ic_error),
                            contentDescription = null,
                            tint = if (CarTheme.useDarkTheme) clubHint else clubHintDark
                        )
                        Text(
                            text = stringResource(R.string.summary_interruption_warning_0_29),
                            style = CarTheme.carTypography.rowContent,
                            maxLines = 2,
                            autoSize = TextAutoSize.StepBased(
                                maxFontSize = CarTheme.carTypography.rowContent.fontSize
                            ),
                            color = if (CarTheme.useDarkTheme) clubHint else clubHintDark
                        )
                    }
                }
                CarRow(
                    content = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(CarTheme.carDimensions.defaultHorizontalPadding),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CarButton(
                                modifier = Modifier
                                    .heightIn(max = 70.dp),
                                onClick = {
                                    globalViewModel.exportChargingSession(
                                        chargingSession = chargingSessionDetails.chargingSession
                                    )
                                },
                                enabled = CarStatsViewer.appPreferences.dataExportEnabled
                            ) {
                                Icon(
                                    modifier = Modifier.size(40.dp),
                                    painter = painterResource(R.drawable.ic_mail),
                                    contentDescription = null
                                )
                                Text(stringResource(R.string.summary_charging_session_export_button))
                            }
                            CarButton(
                                modifier = Modifier
                                    .heightIn(max = 70.dp),
                                onClick = {},
                                enabled = false
                            ) {
                                Icon(
                                    modifier = Modifier.size(40.dp),
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null
                                )
                                Text(stringResource(R.string.summary_charging_session_edit_button))
                            }
                        }
                    }
                )
            }
            /*
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
                val snackBarHostState = LocalCarSnackBarState.current
                CarButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 70.dp),
                    onClick = {
                        viewModel.exportChargingSession(
                            sessionID = chargingSessionDetails.chargingSession.charging_session_id,
                            snackBarHostState = snackBarHostState
                        )
                    },
                    enabled = CarStatsViewer.appPreferences.dataExportEnabled
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
            */
        // }
        CarListDivider()
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            ChargeCurveGraph(chargingSessionDetails.chargingSession)
        }
    }
}