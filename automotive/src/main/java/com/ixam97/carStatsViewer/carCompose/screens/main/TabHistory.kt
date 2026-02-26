package com.ixam97.carStatsViewer.carCompose.screens.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.carCompose.CarComposeGlobalViewModel
import com.ixam97.carStatsViewer.carCompose.screens.tripDetails.TripDetailsScreenNavKey
import com.ixam97.carStatsViewer.database.tripData.DrivingSession
import com.ixam97.carStatsViewer.database.tripData.TripType
import com.ixam97.carStatsViewer.utils.StringFormatters
import de.ixam97.carcompose.components.controls.CarButton
import de.ixam97.carcompose.components.controls.CarButtonDefaults
import de.ixam97.carcompose.components.controls.CarRow
import de.ixam97.carcompose.components.controls.CarRowSwitch
import de.ixam97.carcompose.components.layout.CarLazyColumn
import de.ixam97.carcompose.components.layout.CarListDivider
import de.ixam97.carcompose.components.layout.CarListItem
import de.ixam97.carcompose.components.layout.CarListSection
import de.ixam97.carcompose.components.layout.carListDivider
import de.ixam97.carcompose.components.layout.carListSection
import de.ixam97.carcompose.theme.CarTheme
import de.ixam97.carcompose.utils.buildGradientBrush
import java.util.Date
import kotlin.collections.listOf

@Composable
fun TabHistory(
    globalViewModel: CarComposeGlobalViewModel? = null,
    backStack: NavBackStack<NavKey>
) {
    val historyViewModel: TripHistoryViewModel = viewModel()

    globalViewModel?.setLoading(historyViewModel.tripHistoryState.isLoadingPastTrips || historyViewModel.tripHistoryState.isLoadingCurrentTrips)

    val context = LocalContext.current

    val currentTripsList = historyViewModel.currentTripsList
        .sortedBy { it.session_type }
        .toBrowsableCarRow(historyViewModel, backStack)
    val pastTripsList = historyViewModel.pastTripsList
        .sortedByDescending { it.start_epoch_time }
        .toBrowsableCarRow(historyViewModel, backStack)

    Row(
        modifier = Modifier
            .fillMaxSize()
    ) {
        CarLazyColumn(
            modifier = Modifier
                .width(CarTheme.carDimensions.columnDefaultMaxWidth)
        ) {
            carListSection(
                sectionTitle = context.resources.getString(R.string.history_current_trips),
                listItems = if (historyViewModel.tripHistoryState.isLoadingCurrentTrips) {
                    listOf(CarListItem { LoadingIndicatorRow() })
                } else {
                    currentTripsList.ifEmpty {
                        listOf(CarListItem {
                            CarRow(title = "No Trips available")
                        })
                    }
                }
            )
            carListDivider()
            carListSection(
                sectionTitle = context.resources.getString(R.string.history_past_trips),
                listItems = if (historyViewModel.tripHistoryState.isLoadingPastTrips) {
                    listOf(CarListItem { LoadingIndicatorRow() })
                } else {
                    pastTripsList.ifEmpty {
                        listOf(CarListItem {
                            CarRow(title = "No Trips available")
                        })
                    }
                }
            )
        }
        Spacer(Modifier.weight(1f))
        Column (
            Modifier
                .width(600.dp)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.End
        ) {
            // CarRow {
                Column(
                    modifier = Modifier.padding(
                        horizontal = CarTheme.carDimensions.defaultHorizontalPadding,
                        vertical = CarTheme.carDimensions.defaultVerticalPadding
                    ),
                    // horizontalAlignment = Alignment.End
                ) {
                    CarButton(
                        modifier = Modifier.width(400.dp),
                        onClick = {
                            historyViewModel.setDeleteMode(!historyViewModel.tripHistoryState.deleteMode)
                        }
                    ) {
                        Text(if (!historyViewModel.tripHistoryState.deleteMode) "Delete Trips" else "Cancel")
                    }
                    Spacer(Modifier.size(CarTheme.carDimensions.defaultVerticalPadding))
                    if (!historyViewModel.tripHistoryState.deleteMode) {
                        CarButton(
                            modifier = Modifier.width(400.dp),
                            onClick = { }
                        ) { Text(stringResource(R.string.history_dialog_upload_title)) }
                    } else {
                        Text(
                            modifier = Modifier.padding(vertical = CarTheme.carDimensions.defaultVerticalPadding),
                            text = "Selected: 0",
                            fontSize = 30.sp
                        )
                        CarButton(
                            modifier = Modifier.width(400.dp),
                            active = true,
                            onClick = { },
                            colors = CarButtonDefaults.colors.copy(
                                activeBrush = buildGradientBrush(listOf(Color(0xffAD2D2D)))
                            )
                        ) { Text("Delete selected Trips") }
                    }
                }
            // }
            Spacer(Modifier.weight(1f))

            if (!historyViewModel.tripHistoryState.deleteMode)
                HistoryFilters(historyViewModel)
        }
    }
}

private fun List<DrivingSession>.toBrowsableCarRow(
    viewModel: TripHistoryViewModel,
    backStack: NavBackStack<NavKey>
) : List<CarListItem> {
    return this.filter {
        viewModel.selectedTripFilters[it.session_type] == true || (it.end_epoch_time?:0) <= 0
    }.map { drivingSession ->
        val title = "${StringFormatters.getDateString(Date(drivingSession.start_epoch_time))}, ID: ${drivingSession.driving_session_id}"
        CarListItem {
            val context = LocalContext.current
            CarRow(
                title = title,
                browsable = !viewModel.tripHistoryState.deleteMode,
                enabled = !(viewModel.tripHistoryState.deleteMode && (drivingSession.end_epoch_time?:0) <= 0),
                onBrowse = {
                    backStack.add(TripDetailsScreenNavKey(drivingSession.driving_session_id))
//                    context.startActivity(
//                        Intent(
//                            context,
//                            ComposeTripDetailsActivity::class.java
//                        ).putExtra(
//                            "SessionId",
//                            drivingSession.driving_session_id
//                        )
//                    )
                },
                leadingContent = {
                    Icon(
                        modifier = Modifier
                            .size(60.dp),// (CarTheme.carDimensions.iconButtonSize),
                        painter = when (drivingSession.session_type) {
                            1 -> painterResource(R.drawable.ic_hand)
                            2 -> painterResource(R.drawable.ic_charger)
                            3 -> painterResource(R.drawable.ic_day)
                            4 -> painterResource(R.drawable.ic_month)
                            else -> painterResource(R.drawable.ic_help)
                        },
                        tint = LocalContentColor.current,
                        contentDescription = null
                    )

                },
                trailingContent = if (viewModel.tripHistoryState.deleteMode && (drivingSession.end_epoch_time?:0) > 0) {
                    {
                        Checkbox(
                            checked = false,
                            onCheckedChange = {}
                        )
                    }
                } else null,
                descriptionContent = {

                    val descriptionColor = LocalContentColor.current.copy(alpha = LocalContentColor.current.alpha * 0.7f)

                    Row(
                        modifier = Modifier.height(28.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val textSpacing = 5.dp
                        val iconSpacing = 15.dp
                        Icon(
                            modifier = Modifier
                                .padding(end = textSpacing)
                                .size(28.dp),
                            painter = painterResource(R.drawable.ic_distance),
                            tint = descriptionColor,
                            contentDescription = null
                        )
                        Text(
                            modifier = Modifier
                                .padding(end = iconSpacing),
                            style = CarTheme.carTypography.rowContent,
                            color = descriptionColor,
                            text = StringFormatters.getTraveledDistanceString(drivingSession.driven_distance.toFloat())
                        )
                        Icon(
                            modifier = Modifier
                                .padding(end = textSpacing)
                                .size(28.dp),
                            painter = painterResource(R.drawable.ic_power),
                            tint = descriptionColor,
                            contentDescription = null
                        )
                        Text(
                            modifier = Modifier
                                .padding(end = iconSpacing),
                            style = CarTheme.carTypography.rowContent,
                            color = descriptionColor,
                            text = StringFormatters.getEnergyString(drivingSession.used_energy.toFloat())
                        )
                        Icon(
                            modifier = Modifier
                                .padding(end = textSpacing)
                                .size(28.dp),
                            painter = painterResource(R.drawable.ic_avg_consumption),
                            tint = descriptionColor,
                            contentDescription = null
                        )
                        Text(
                            modifier = Modifier
                                .padding(end = iconSpacing),
                            style = CarTheme.carTypography.rowContent,
                            color = descriptionColor,
                            text = StringFormatters.getAvgConsumptionString(drivingSession.used_energy.toFloat(), drivingSession.driven_distance.toFloat())
                        )
                        Icon(
                            modifier = Modifier
                                .padding(end = textSpacing)
                                .size(28.dp),
                            painter = painterResource(R.drawable.ic_time),
                            tint = descriptionColor,
                            contentDescription = null
                        )
                        Text(
                            modifier = Modifier
                                .padding(end = iconSpacing),
                            style = CarTheme.carTypography.rowContent,
                            color = descriptionColor,
                            text = StringFormatters.getElapsedTimeString(drivingSession.drive_time)
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun LoadingIndicatorRow() {
    CarRow(
        content = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(50.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
    )
}

@Composable
private fun ColumnScope.HistoryFilters(
    historyViewModel: TripHistoryViewModel
) {
    CarRow(
        content = {
            Column {
                Text(
                    text = stringResource(R.string.history_dialog_filters_title),
                    style = CarTheme.carTypography.rowTitle,
                    color = CarTheme.carColors.accent
                )
                Spacer(Modifier.size(CarTheme.carDimensions.rowTextSpacing))
                Text(
                    text = stringResource(R.string.history_dialog_filters_note),
                    style = CarTheme.carTypography.rowContent,
                    color = CarTheme.carColors.onBackground.copy(alpha = 0.7f)
                )
            }
        }
    )
    CarListDivider()
    CarListSection(
        listItems = listOf(
            CarListItem {
                CarRowSwitch(
                    title = stringResource(R.string.history_dialog_filters_auto),
                    state = historyViewModel.selectedTripFilters[TripType.AUTO]?:false,
                    onStateChange = {
                        historyViewModel.setTripFilter(TripType.AUTO, it)
                    }
                )
            },
            CarListItem {
                CarRowSwitch(
                    title = stringResource(R.string.history_dialog_filters_month),
                    state = historyViewModel.selectedTripFilters[TripType.MONTH]?:false,
                    onStateChange = {
                        historyViewModel.setTripFilter(TripType.MONTH, it)
                    }
                )
            },
            CarListItem {
                CarRowSwitch(
                    title = stringResource(R.string.history_dialog_filters_charge),
                    state = historyViewModel.selectedTripFilters[TripType.SINCE_CHARGE]?:false,
                    onStateChange = {
                        historyViewModel.setTripFilter(TripType.SINCE_CHARGE, it)
                    }
                )
            },
            CarListItem {
                CarRowSwitch(
                    title = stringResource(R.string.history_dialog_filters_manual),
                    state = historyViewModel.selectedTripFilters[TripType.MANUAL]?:false,
                    onStateChange = {
                        historyViewModel.setTripFilter(TripType.MANUAL, it)
                    }
                )
            },
        )
    )
}