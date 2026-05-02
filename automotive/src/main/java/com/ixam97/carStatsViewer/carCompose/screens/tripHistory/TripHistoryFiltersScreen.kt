package com.ixam97.carStatsViewer.carCompose.screens.tripHistory

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.database.tripData.TripType
import de.ixam97.carcompose.components.controls.CarRow
import de.ixam97.carcompose.components.controls.CarRowCheckbox
import de.ixam97.carcompose.components.layout.CarColumn
import de.ixam97.carcompose.components.layout.CarListItem
import de.ixam97.carcompose.components.layout.CarListSection
import de.ixam97.carcompose.components.layout.CarPaneLayout
import kotlinx.serialization.Serializable

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
            CarRow(
                title = stringResource(R.string.history_dialog_filters_note)
            )
            TripHistoryFiltersContent(viewModel)
        }
    }
}

@Composable
fun ColumnScope.TripHistoryFiltersContent(
    viewModel: TripHistoryViewModel
) {

    val tripHistoryState by viewModel.tripHistoryState.collectAsState()

    CarListSection(
        listItems = listOf(
            CarListItem {
                CarRowCheckbox(
                    title = stringResource(R.string.history_dialog_filters_auto),
                    isSelected = tripHistoryState.selectedFilters[TripType.AUTO]?:false,
                    onSelect = {
                        viewModel.setTripFilter(TripType.AUTO, !(tripHistoryState.selectedFilters[TripType.AUTO]?:false))
                    }
                )
            },
            CarListItem {
                CarRowCheckbox(
                    title = stringResource(R.string.history_dialog_filters_month),
                    isSelected = tripHistoryState.selectedFilters[TripType.MONTH]?:false,
                    onSelect = {
                        viewModel.setTripFilter(TripType.MONTH, !(tripHistoryState.selectedFilters[TripType.MONTH]?:false))
                    }
                )
            },
            CarListItem {
                CarRowCheckbox(
                    title = stringResource(R.string.history_dialog_filters_charge),
                    isSelected = tripHistoryState.selectedFilters[TripType.SINCE_CHARGE]?:false,
                    onSelect = {
                        viewModel.setTripFilter(TripType.SINCE_CHARGE, !(tripHistoryState.selectedFilters[TripType.SINCE_CHARGE]?:false))
                    }
                )
            },
            CarListItem {
                CarRowCheckbox(
                    title = stringResource(R.string.history_dialog_filters_manual),
                    isSelected = tripHistoryState.selectedFilters[TripType.MANUAL]?:false,
                    onSelect = {
                        viewModel.setTripFilter(TripType.MANUAL, !(tripHistoryState.selectedFilters[TripType.MANUAL]?:false))
                    }
                )
            },
        )
    )
}