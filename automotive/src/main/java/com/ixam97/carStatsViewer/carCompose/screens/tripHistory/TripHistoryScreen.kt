package com.ixam97.carStatsViewer.carCompose.screens.tripHistory

import android.app.AlertDialog
import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onVisibilityChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.carCompose.CarComposeGlobalViewModel
import com.ixam97.carStatsViewer.carCompose.deviceIsWideScreen
import com.ixam97.carStatsViewer.carCompose.screens.tripDetails.TripDetailsScreenNavKey
import com.ixam97.carStatsViewer.carCompose.theme.badRed
import com.ixam97.carStatsViewer.carCompose.theme.polestar4ContentPadding
import com.ixam97.carStatsViewer.database.tripData.DrivingSession
import com.ixam97.carStatsViewer.database.tripData.TripType
import com.ixam97.carStatsViewer.utils.StringFormatters
import de.ixam97.carcompose.components.controls.CarButton
import de.ixam97.carcompose.components.controls.CarButtonDefaults
import de.ixam97.carcompose.components.controls.CarIconButton
import de.ixam97.carcompose.components.controls.CarRow
import de.ixam97.carcompose.components.controls.CarRowCheckbox
import de.ixam97.carcompose.components.layout.CarLazyColumn
import de.ixam97.carcompose.components.layout.CarListItem
import de.ixam97.carcompose.components.layout.CarPaneLayout
import de.ixam97.carcompose.components.layout.carListSection
import de.ixam97.carcompose.theme.CarTheme
import kotlinx.serialization.Serializable
import java.util.Date

@Serializable
object TripHistoryScreenNavKey: NavKey

/**
 * Standalone Screen to show the trip history.
 */
@Composable
fun TripHistoryScreen(
    backStack: NavBackStack<NavKey>,
    onBack: () -> Unit,
    globalViewModel: CarComposeGlobalViewModel
) {

    val viewModel: TripHistoryViewModel = viewModel()
    val tripHistoryState by viewModel.tripHistoryState.collectAsState()
    val globalState by globalViewModel.globalState.collectAsState()
    val context = LocalContext.current

    val headerIconsList = listOf(
            @Composable { CarIconButton(
                painterResource(R.drawable.ic_carcompose_filter),
                active = tripHistoryState.filtersModified
            ) { backStack.add(TripHistoryFiltersScreenNavKey) } },
            { CarIconButton(
                painterResource(R.drawable.ic_carcompose_delete)
            ) { viewModel.setDeleteMode(!tripHistoryState.deleteMode) } }
        )
    val headerDeleteModeIconsList = listOf(
        @Composable
        {
            Row() {
                CarButton(
                    onClick = { viewModel.setDeleteMode(false) }
                ) { Text(stringResource(R.string.dialog_reset_cancel)) }
                Spacer(Modifier.size(CarTheme.carDimensions.defaultHorizontalPadding))
                CarButton(
                    enabled = tripHistoryState.deleteSelection.isNotEmpty(),
                    colors = CarButtonDefaults.colors.copy(
                        backgroundBrush = SolidColor(badRed),
                        textColor = Color.White
                    ),
                    onClick = { showDeleteDialog(context, tripHistoryState.deleteSelection.size) { viewModel.deleteSelectedTrips() } }
                ) { Text(deleteButtonText(tripHistoryState.deleteSelection.size)) }
            }
        },
    )

    CarPaneLayout(
        headerTitle = if (tripHistoryState.deleteMode) "Delete Trips" else stringResource(R.string.history_title),
        onBackAction = if (tripHistoryState.deleteMode) null else onBack,
        isLoading = globalState.isLoading,
//        headerStartContent = if (!tripHistoryState.deleteMode) null else {
//            {
//                CarButton(
//                    onClick = { viewModel.setDeleteMode(false) }
//                ) { Text(stringResource(R.string.dialog_reset_cancel)) }
//            }
//        },
        headerIconButtons = when {
            deviceIsWideScreen() -> listOf()
            tripHistoryState.deleteMode -> headerDeleteModeIconsList
            else -> headerIconsList
        }
    ) {
        TripHistoryContent(
            modifier = Modifier.padding(start = if (deviceIsWideScreen()) polestar4ContentPadding else 0.dp),
            backStack = backStack,
            globalViewModel = globalViewModel,
            viewModel = viewModel
        )

    }
}

@Composable
fun TripHistoryContent(
    modifier: Modifier = Modifier,
    backStack: NavBackStack<NavKey>,
    globalViewModel: CarComposeGlobalViewModel,
    viewModel: TripHistoryViewModel
) {

    val tripHistoryState by viewModel.tripHistoryState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(tripHistoryState.isLoadingPastTrips, tripHistoryState.isLoadingCurrentTrips) {
        globalViewModel.setLoading(tripHistoryState.isLoadingPastTrips || tripHistoryState.isLoadingCurrentTrips)
    }

    Row(
        modifier = modifier
    ) {
        TripHistoryList(
            modifier = Modifier
                .width(CarTheme.carDimensions.columnDefaultMaxWidth - 100.dp),
            backStack =backStack,
            viewModel = viewModel
        )

        if (deviceIsWideScreen()) {
            Spacer(Modifier.weight(1f))
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.End
            ) {
                if (!tripHistoryState.deleteMode) {
                    CarButton(
                        modifier = Modifier
                            .padding(
                                horizontal = CarTheme.carDimensions.defaultHorizontalPadding,
                                vertical = CarTheme.carDimensions.defaultVerticalPadding
                            )
                            .width(400.dp),
                        onClick = { viewModel.setDeleteMode(true) }
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_carcompose_delete),
                            null,
                            Modifier.size(CarTheme.carDimensions.iconButtonSize)
                        )
                        Text("Delete Trips")
                    }
                } else {
                    CarButton(
                        modifier = Modifier
                            .padding(
                                horizontal = CarTheme.carDimensions.defaultHorizontalPadding,
                                vertical = CarTheme.carDimensions.defaultVerticalPadding
                            )
                            .width(400.dp),
                        onClick = { showDeleteDialog(context, tripHistoryState.deleteSelection.size) { viewModel.deleteSelectedTrips() } },
                        enabled = tripHistoryState.deleteSelection.isNotEmpty(),
                        colors = CarButtonDefaults.colors.copy(
                            backgroundBrush = SolidColor(badRed),
                            textColor = Color.White
                        )
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_carcompose_delete),
                            null,
                            Modifier.size(CarTheme.carDimensions.iconButtonSize)
                        )
                        Text(deleteButtonText(tripHistoryState.deleteSelection.size))
                    }
                    CarButton(
                        modifier = Modifier
                            .padding(
                                horizontal = CarTheme.carDimensions.defaultHorizontalPadding,
                            )
                            .width(400.dp),
                        onClick = { viewModel.setDeleteMode(false) }
                    ) { Text(stringResource(R.string.dialog_reset_cancel)) }
                }
                Spacer(Modifier.weight(1f))
                TripHistoryFiltersContent(
                    viewModel = viewModel
                )
            }
        }
    }
}

/**
 * CarColumn containing the actual list of trips.
 */
@Composable
internal fun TripHistoryList(
    modifier: Modifier = Modifier,
    backStack: NavBackStack<NavKey>,
    viewModel: TripHistoryViewModel,
) {
    val tripHistoryState by viewModel.tripHistoryState.collectAsState()
    val context = LocalContext.current

    val currentTripsListItems = when {
        tripHistoryState.isLoadingCurrentTrips && tripHistoryState.currentTrips.isEmpty() -> listOf(CarListItem { LoadingRow() })
        tripHistoryState.currentTrips.isNotEmpty() -> tripHistoryState.currentTrips
            .sortedBy { it.session_type }
            .map { drivingSession ->
                CarListItem {
                    DrivingSessionRow(
                        drivingSession = drivingSession,
                        deleteMode = tripHistoryState.deleteMode,
                        onReset = {
                            showResetDialog(context) { viewModel.resetTrip(
                                tripType = drivingSession.session_type,
                                sessionId = drivingSession.driving_session_id
                            ) }
                        }
                    ) { backStack.add(TripDetailsScreenNavKey(it)) }
                }
            }
        else -> listOf(CarListItem { NoDataRow() })
    }

    val pastTripsListItems = when {
        tripHistoryState.isLoadingPastTrips && tripHistoryState.pastTrips.isEmpty() -> listOf(CarListItem { LoadingRow() })
        tripHistoryState.pastTrips.any { tripHistoryState.selectedFilters[it.session_type] == true } -> tripHistoryState.pastTrips
            .sortedBy { it.start_epoch_time }
            .reversed()
            .filter { tripHistoryState.selectedFilters[it.session_type] == true }
            .map { drivingSession ->
                CarListItem {
                    DrivingSessionRow(
                        drivingSession = drivingSession,
                        deleteMode = tripHistoryState.deleteMode,
                        onSelect = { viewModel.addOrRemoveDeleteSelection(drivingSession.driving_session_id) },
                        selected = tripHistoryState.deleteSelection.contains(drivingSession.driving_session_id)
                    ) { backStack.add(TripDetailsScreenNavKey(it)) }
                }
            }
        else -> listOf(CarListItem { NoDataRow() })
    }

    val currentTripsTitle = stringResource(R.string.history_current_trips)
    val pastTripsTitle = stringResource(R.string.history_past_trips)

    CarLazyColumn(
        modifier = modifier
            .onVisibilityChanged { if (it) viewModel.reloadTrips() }
        // TODO: Make sure the list updates if a trip reset appears while on this screen
    ) {
        carListSection(
            sectionTitle = currentTripsTitle,
            listItems = currentTripsListItems
        )
        carListSection(
            sectionTitle = pastTripsTitle,
            listItems = pastTripsListItems
        )
    }
}

@Composable
internal fun LoadingRow() {
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
internal fun NoDataRow() {
    CarRow(
        title = "No trips available"
    )
}

@Composable
internal fun DrivingSessionRow(
    drivingSession: DrivingSession,
    deleteMode: Boolean,
    onReset: (() -> Unit)? = null,
    onSelect: (() -> Unit)? = null,
    selected: Boolean = false,
    onClick: (sessionId: Long) -> Unit,
) {

    val rowTitle = "${StringFormatters.getDateString(Date(drivingSession.start_epoch_time))}, ID: ${drivingSession.driving_session_id}"
    @Composable
    fun LeadingContent() {
        Icon(
            modifier = Modifier
                .size(60.dp),// (CarTheme.carDimensions.iconButtonSize),
            painter = when (drivingSession.session_type) {
                TripType.MANUAL -> painterResource(R.drawable.ic_hand)
                TripType.SINCE_CHARGE -> painterResource(R.drawable.ic_charger)
                TripType.AUTO -> painterResource(R.drawable.ic_day)
                TripType.MONTH -> painterResource(R.drawable.ic_month)
                else -> painterResource(R.drawable.ic_help)
            },
            tint = LocalContentColor.current,
            contentDescription = null
        )
    }
    @Composable
    fun DescriptionContent() {
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
                text = StringFormatters.getElapsedTimeString(drivingSession.drive_time, true)
            )
        }
    }

    if (deleteMode && onSelect != null) {
        CarRowCheckbox(
            title = rowTitle,
            isSelected = selected,
            leadingContent = { LeadingContent() },
            descriptionContent = { DescriptionContent() },
            onSelect = onSelect
        )
    } else {
        CarRow(
            title = rowTitle,
            browsable = true,
            enabled = !deleteMode,
            onBrowse = {
                onClick(drivingSession.driving_session_id)
            },
            leadingContent = { LeadingContent() },
            trailingContent = if (onReset != null) {
                {
                    CarIconButton(
                        painterResource(R.drawable.ic_carcompose_reset),
                        enabled = !deleteMode
                    ) { onReset() }
                }
            } else null,
            descriptionContent = { DescriptionContent() }
        )
    }
}

internal fun showResetDialog(
    context: Context,
    onConfirm: () -> Unit
) {
    val resetDialog = AlertDialog.Builder(context).apply {
        setTitle(R.string.dialog_reset_title)
        setMessage(R.string.dialog_reset_message)
        setPositiveButton(R.string.dialog_reset_confirm) { _,_ ->
            onConfirm()
        }
        setNegativeButton(R.string.dialog_reset_cancel) { _,_ -> }
    }
    resetDialog.show()
}

@Composable
internal fun deleteButtonText(tripsNumber: Int): String {
    return when (tripsNumber) {
        0 -> "No trips selected"
        1 -> stringResource(R.string.history_dialog_delete_confirm)
        else -> stringResource(R.string.history_dialog_multi_delete_delete, tripsNumber)
    }
}

internal fun showDeleteDialog(
    context: Context,
    tripsNumber: Int,
    onConfirm: () -> Unit
) {
    val deleteDialog = AlertDialog.Builder(context).apply {
        when (tripsNumber) {
            1 -> {
                setTitle(R.string.history_dialog_delete_title)
                setMessage(R.string.history_dialog_delete_message)
                setPositiveButton(R.string.history_dialog_delete_confirm) { _, _ ->
                    onConfirm()
                }
                setNegativeButton(R.string.dialog_reset_cancel) { _,_ -> }
            }
            else -> {
                setTitle(R.string.history_dialog_multi_delete_title)
                setMessage(context.getString(
                    R.string.history_dialog_multi_delete_message,
                    tripsNumber.toString()
                ))
                setPositiveButton(context.getString(
                    R.string.history_dialog_multi_delete_delete,
                    tripsNumber.toString()
                )) { _, _ ->
                    onConfirm()
                }
                setNegativeButton(R.string.dialog_reset_cancel) { _,_ -> }
            }
        }
    }
    deleteDialog.show()
}