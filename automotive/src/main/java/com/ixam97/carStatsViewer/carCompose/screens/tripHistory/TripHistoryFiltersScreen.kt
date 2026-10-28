package com.ixam97.carStatsViewer.carCompose.screens.tripHistory

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DateRangePickerState
import androidx.compose.material3.Icon
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.carCompose.components.CarRangeSlider
import com.ixam97.carStatsViewer.carCompose.components.CarStepper
import com.ixam97.carStatsViewer.compose.RowContentText
import com.ixam97.carStatsViewer.database.tripData.TripType
import de.ixam97.carcompose.components.controls.CarButton
import de.ixam97.carcompose.components.controls.CarButtonDefaults
import de.ixam97.carcompose.components.controls.CarIconButton
import de.ixam97.carcompose.components.controls.CarRow
import de.ixam97.carcompose.components.controls.CarTextField
import de.ixam97.carcompose.components.layout.CarColumn
import de.ixam97.carcompose.components.layout.CarListItem
import de.ixam97.carcompose.components.layout.CarListSection
import de.ixam97.carcompose.components.layout.CarPaneLayout
import de.ixam97.carcompose.components.layout.CarScaleContainer
import de.ixam97.carcompose.theme.CarTheme
import kotlinx.serialization.Serializable
import java.util.Date

@Serializable
object TripHistoryFiltersScreenNavKey: NavKey

/**
 * Standalone Screen to configure filters for the trip history.
 */
@Composable
fun TripHistoryFiltersScreen(
    backStack: NavBackStack<NavKey>,
    onBack: () -> Unit,
    viewModel: TripHistoryViewModel
) {
    CarPaneLayout(
        headerTitle = stringResource(R.string.history_dialog_filters_title),
        onBackAction = onBack
    ) {
        CarColumn() {
            TripHistoryFiltersContent(viewModel, showHint = true)
        }
    }
}

@Composable
fun ColumnScope.TripHistoryFiltersContent(
    viewModel: TripHistoryViewModel,
    sectionTitle: String? = null,
    showHint: Boolean = false,
) {
    val tripHistoryState by viewModel.tripHistoryState.collectAsState()
    val mutableItemsList = mutableListOf(
        CarListItem {
            var showDialog by remember { mutableStateOf(false) }
            CarRow(
                title = stringResource(R.string.history_dialog_filters_date_range),
                descriptionContent = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(CarTheme.carDimensions.defaultHorizontalPadding)
                    ) {
                        CarTextField(
                            modifier = Modifier.weight(1f).clickable { showDialog = true },
                            value = dateRangeText(getDateRangeFromValidAndSelectedRanges(
                                tripHistoryState.selectedDateRange,
                                tripHistoryState.validDateRange
                            )),
                            onValueChange = { },
                            leadingIcon = { Icon(
                                painterResource(R.drawable.ic_month),
                                null,
                                modifier = Modifier.size(CarTheme.carDimensions.iconButtonSize)
                            ) }
                        )
                        CarIconButton(
                            painter = painterResource(R.drawable.ic_carcompose_reset),
                            enabled = tripHistoryState.selectedDateRange != null
                        ) { viewModel.setFilterDateRange(null) }
                    }
                    if (showDialog) {
                        DateRangePickerDialog(
                            selectedDateRange = tripHistoryState.selectedDateRange,
                            validDateRange = tripHistoryState.validDateRange,
                            onDateRangeSelected = { dateRange ->
                                showDialog = false
                                viewModel.setFilterDateRange(dateRange)
                            },
                            onCancel = { showDialog = false }
                        )
                    }
                }
            )
        },
        CarListItem {
            var showDialog by remember { mutableStateOf(false) }
            CarRow(
                title = stringResource(R.string.history_dialog_filters_distance),
                descriptionContent = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(CarTheme.carDimensions.defaultHorizontalPadding)
                    ) {
                        CarTextField(
                            modifier = Modifier.weight(1f).clickable { showDialog = true },
                            value = distanceRangeText(tripHistoryState.selectedDistanceRange),
                            onValueChange = { },
                            leadingIcon = { Icon(
                                painterResource(R.drawable.ic_distance),
                                null,
                                modifier = Modifier.size(CarTheme.carDimensions.iconButtonSize)
                            ) }
                        )
                        CarIconButton(
                            painter = painterResource(R.drawable.ic_carcompose_reset),
                            enabled = tripHistoryState.selectedDistanceRange != null
                        ) { viewModel.setFilterDistanceRange(null) }
                    }
                    if (showDialog) {
                        DistanceRangeDialog(
                            selected = tripHistoryState.selectedDistanceRange,
                            onApply = { range ->
                                showDialog = false
                                viewModel.setFilterDistanceRange(range)
                            },
                            onCancel = { showDialog = false }
                        )
                    }
                }
            )
        },
        CarListItem {
            CarRow(
                title = stringResource(R.string.history_dialog_filters_trip_types),
                descriptionContent = {
                    TripTypeFilterGrid(
                        selectedFilters = tripHistoryState.selectedFilters,
                        onToggle = { tripType ->
                            viewModel.setTripFilter(tripType, tripHistoryState.selectedFilters[tripType] != true)
                        }
                    )
                }
            )
        }
    )

    if (showHint) {
        mutableItemsList.add(
            0,
            CarListItem {
                CarRow(
                    content = {
                        RowContentText(text = stringResource(R.string.history_dialog_filters_note),)
                    }
                )
            }
        )
    }

    CarListSection(
        sectionTitle = sectionTitle,
        listItems = mutableItemsList
    )
}

/** Compose dialog containing a date range picker to select a range of trip start dates. */
@Composable
private fun DateRangePickerDialog(
    selectedDateRange: Pair<Long, Long>?,
    validDateRange: Pair<Long, Long>?,
    onDateRangeSelected: (Pair<Long, Long>?) -> Unit,
    onCancel: () -> Unit
) {
    val dateRange = getDateRangeFromValidAndSelectedRanges(selectedDateRange, validDateRange)
    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = dateRange.first,
        initialSelectedEndDateMillis = dateRange.second,
        initialDisplayedMonthMillis = dateRange.second,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                if (validDateRange == null) return false
                return utcTimeMillis in (validDateRange.first)..(validDateRange.second)
            }
        }
    )

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.padding(50.dp)
        ) {
            Column (
                modifier = Modifier
                    .widthIn(max = 1024.dp)
                    .clip(RoundedCornerShape(CarTheme.carShapes.listSectionCornerSize))
                    .background(CarTheme.carColors.listSectionBackground)
                    .padding(CarTheme.carDimensions.defaultHorizontalPadding),
                verticalArrangement = Arrangement.spacedBy(CarTheme.carDimensions.defaultVerticalPadding)
            ) {
                Text(
                    modifier = Modifier.padding(top = CarTheme.carDimensions.defaultVerticalPadding),
                    text = dateRangeText(
                        dateRangePickerState.selectedStartDateMillis to dateRangePickerState.selectedEndDateMillis,
                        stringResource(R.string.history_dialog_date_range_no_range_selected)
                    ),
                    style = CarTheme.carTypography.title
                )
                Text(
                    text = stringResource(R.string.history_dialog_date_range_hint),
                    style = CarTheme.carTypography.defaultBody
                )
                Box(
                    modifier = Modifier
                        .height(2.dp)
                        .fillMaxWidth()
                        .background(CarTheme.carColors.secondaryDivider)
                )
                ScaledDateRangePicker(dateRangePickerState)
                Row() {
                    CarButton(
                        modifier = Modifier.weight(1f),
                        onClick = onCancel
                    ) { Text(stringResource(R.string.dialog_reset_cancel)) }
                    Spacer(modifier = Modifier.size(CarTheme.carDimensions.defaultHorizontalPadding))
                    CarButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (dateRangePickerState.selectedStartDateMillis == null || dateRangePickerState.selectedEndDateMillis == null)
                                onDateRangeSelected(null)
                            else
                                onDateRangeSelected(dateRangePickerState.selectedStartDateMillis!! to dateRangePickerState.selectedEndDateMillis!!)
                        },
                        active = true,
                        enabled = (dateRangePickerState.selectedStartDateMillis != null && dateRangePickerState.selectedEndDateMillis != null)
                    ) { Text(stringResource(R.string.dialog_apply)) }
                }
            }
        }
    }
}

private val tripTypeFilterRows = listOf(
    listOf(TripType.AUTO to R.string.history_dialog_filters_type_auto, TripType.MONTH to R.string.history_dialog_filters_type_month),
    listOf(TripType.SINCE_CHARGE to R.string.history_dialog_filters_type_charge, TripType.MANUAL to R.string.history_dialog_filters_type_manual),
)

/** Toggle buttons for the trip types in a 2x2 grid; a highlighted button means the type is shown. */
@Composable
private fun TripTypeFilterGrid(
    selectedFilters: Map<Int, Boolean>,
    onToggle: (tripType: Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(CarTheme.carDimensions.defaultVerticalPadding)) {
        tripTypeFilterRows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(CarTheme.carDimensions.defaultVerticalPadding)) {
                row.forEach { (tripType, label) ->
                    val selected = selectedFilters[tripType] == true
                    CarButton(
                        modifier = Modifier
                            .weight(1f)
                            .semantics { this.selected = selected },
                        active = selected,
                        dimensions = CarButtonDefaults.dimensions.copy(horizontalPadding = 8.dp),
                        onClick = { onToggle(tripType) }
                    ) {
                        Text(
                            modifier = Modifier.weight(1f),
                            text = stringResource(label),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

private val distanceRangePresets = listOf(
    DistanceRange(0, 10),
    DistanceRange(10, 50),
    DistanceRange(50, 200),
    DistanceRange(200, null),
)

/**
 * Compose dialog to select a range of driven distances in the display unit. Combines a range
 * slider for coarse selection, presets and steppers for fine adjustment. The value pair is the
 * single source of truth, the slider thumbs follow it.
 */
@Composable
internal fun DistanceRangeDialog(
    selected: DistanceRange?,
    onApply: (DistanceRange?) -> Unit,
    onCancel: () -> Unit
) {
    var range by remember { mutableStateOf(selected ?: DistanceRange(0, null)) }
    val unitText = CarStatsViewer.appPreferences.distanceUnit.unit()

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.padding(50.dp)
        ) {
            Column (
                modifier = Modifier
                    .widthIn(max = 1024.dp)
                    .clip(RoundedCornerShape(CarTheme.carShapes.listSectionCornerSize))
                    .background(CarTheme.carColors.listSectionBackground)
                    .padding(CarTheme.carDimensions.defaultHorizontalPadding),
                verticalArrangement = Arrangement.spacedBy(CarTheme.carDimensions.defaultVerticalPadding)
            ) {
                Text(
                    modifier = Modifier.padding(top = CarTheme.carDimensions.defaultVerticalPadding),
                    text = distanceRangeText(range),
                    style = CarTheme.carTypography.title
                )
                Text(
                    text = stringResource(R.string.history_dialog_distance_hint),
                    style = CarTheme.carTypography.defaultBody
                )
                DialogDivider()
                Row(horizontalArrangement = Arrangement.spacedBy(CarTheme.carDimensions.defaultVerticalPadding)) {
                    distanceRangePresets.forEach { preset ->
                        CarButton(
                            modifier = Modifier.weight(1f),
                            active = range == preset,
                            // narrow padding, so the labels fit on four buttons in a narrow dialog
                            dimensions = CarButtonDefaults.dimensions.copy(horizontalPadding = 8.dp),
                            onClick = { range = preset }
                        ) {
                            Text(
                                modifier = Modifier.weight(1f),
                                text = distanceRangeText(preset),
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
                DialogDivider()
                CarRangeSlider(
                    modifier = Modifier.fillMaxWidth(),
                    value = valueToPosition(range.min)..valueToPosition(range.max),
                    onValueChange = { position ->
                        val min = positionToValue(position.start.coerceAtMost(DISTANCE_FILTER_ANCHORS.lastIndex.toFloat())) ?: 0
                        range = DistanceRange(min, positionToValue(position.endInclusive))
                    },
                    valueRange = 0f..DISTANCE_FILTER_UNBOUNDED_POSITION,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CarStepper(
                        valueText = "${range.min} $unitText",
                        onDecrement = { range = range.decrementMin() },
                        onIncrement = { range = range.incrementMin() },
                        decrementEnabled = range.canDecrementMin,
                        incrementEnabled = range.canIncrementMin,
                    )
                    Spacer(Modifier.weight(1f))
                    CarStepper(
                        valueText = range.max?.let { "$it $unitText" } ?: "∞",
                        onDecrement = { range = range.decrementMax() },
                        onIncrement = { range = range.incrementMax() },
                        decrementEnabled = range.canDecrementMax,
                        incrementEnabled = range.canIncrementMax,
                    )
                }
                DialogDivider()
                Row() {
                    CarButton(
                        modifier = Modifier.weight(1f),
                        onClick = onCancel
                    ) { Text(stringResource(R.string.dialog_reset_cancel)) }
                    Spacer(modifier = Modifier.size(CarTheme.carDimensions.defaultHorizontalPadding))
                    CarButton(
                        modifier = Modifier.weight(1f),
                        onClick = { onApply(range.takeUnless { it.isUnbounded }) },
                        active = true
                    ) { Text(stringResource(R.string.dialog_apply)) }
                }
            }
        }
    }
}

@Composable
private fun DialogDivider() {
    Box(
        modifier = Modifier
            .height(2.dp)
            .fillMaxWidth()
            .background(CarTheme.carColors.secondaryDivider)
    )
}

/** Returns a string describing a distance range in the display unit, or "All distances". */
@Composable
private fun distanceRangeText(range: DistanceRange?): String {
    val unitText = CarStatsViewer.appPreferences.distanceUnit.unit()
    return when {
        range == null || range.isUnbounded -> stringResource(R.string.history_dialog_filters_distance_all)
        range.max == null -> stringResource(R.string.history_dialog_distance_at_least, range.min, unitText)
        range.min == 0 -> stringResource(R.string.history_dialog_distance_at_most, range.max, unitText)
        else -> stringResource(R.string.history_dialog_distance_range, range.min, range.max, unitText)
    }
}

/** Returns a string formatted in the current locale containing a start and end date */
@Composable
private fun dateRangeText(
    dateRange: Pair<Long?, Long?>,
    defaultText: String = ""
): String {
    val dateFormat = DateFormat.getDateFormat(LocalContext.current)
    val startDate = dateRange.first.run {
        if (this != null) dateFormat.format(Date(this))
        else defaultText
    }
    val endDate = dateRange.second.run {
        if (this != null) " – " + dateFormat.format(Date(this))
        else ""
    }
    return startDate + endDate
}

/** Material 3 date range picker wrapped in a car compose scale container for better usability in
    a car. Relies on Car Compose scaling factor and embedded Color and Typography. */
@Composable
private fun ColumnScope.ScaledDateRangePicker(
    dateRangePickerState: DateRangePickerState
) {
    CarScaleContainer(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
    ) {
        DateRangePicker(
            showModeToggle = false,
            title = null,
            headline = null,
            colors = DatePickerDefaults.colors(
                containerColor = Color.Transparent,
                selectedDayContainerColor = CarTheme.carColors.accentContainer
            ),
            state = dateRangePickerState
        )
    }
}

/** Returns a date range selected from either the selected range or, if it is null, from the valid range. */
private fun getDateRangeFromValidAndSelectedRanges(
    selectedDateRange: Pair<Long, Long>?,
    validDateRange: Pair<Long, Long>?
) = if (selectedDateRange == null) {
    validDateRange?.first.run {
        if (this == null) null else this + 86400000
    } to validDateRange?.second
} else {
    selectedDateRange.first to selectedDateRange.second
}