package com.ixam97.carStatsViewer.carCompose.screens.tripDetails

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.utils.StringFormatters
import de.ixam97.carcompose.components.controls.CarRow
import de.ixam97.carcompose.components.layout.CarLazyColumn
import de.ixam97.carcompose.components.layout.CarListItem
import de.ixam97.carcompose.components.layout.carListSection
import de.ixam97.carcompose.theme.CarTheme
import java.util.Date

@Composable
fun TripDetailsChargingSection(
    viewModel: TripDetailsViewModel,
    chargingSessionsDetails: List<ChargingSessionDetails>?,
) {
    chargingSessionsDetails.let { chargingSessionsDetails ->
        if (chargingSessionsDetails.isNullOrEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.summary_no_charging_sessions),
                    style = CarTheme.carTypography.rowTitle
                )
            }
        } else {
            CarLazyColumn() {
                carListSection(
                    listItems = chargingSessionsDetails.toCarListItems() { viewModel.setSelectedChargingDetails(it)}
                )
            }
        }
    }
}

private fun List<ChargingSessionDetails>.toCarListItems(
    onSelect: (chargingSessionId: Long) -> Unit
) : List<CarListItem> {
    return map { chargingSessionDetails ->
        CarListItem {
            ChargingSessionListRow(chargingSessionDetails) {
                onSelect(chargingSessionDetails.chargingSession.charging_session_id)
            }
        }
    }
}

@Composable
private fun ChargingSessionListRow(
    chargingSessionDetails: ChargingSessionDetails,
    onClick: () -> Unit
) {
    CarRow(
        title = StringFormatters.getDateString(Date(chargingSessionDetails.chargingSession.start_epoch_time)),
        description = chargingSessionDetails.chargingLocation,
        browsable = true,
        onBrowse = onClick
    )
}
