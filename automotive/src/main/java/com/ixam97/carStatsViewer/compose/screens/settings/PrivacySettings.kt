package com.ixam97.carStatsViewer.compose.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.compose.SettingsViewModel
import com.ixam97.carStatsViewer.compose.components.CarRow
import com.ixam97.carStatsViewer.compose.components.CarSwitchRow

@Composable
fun PrivacySettings(
    viewModel: SettingsViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        CarSwitchRow(
            switchState = viewModel.settingsState.locationTracking,
            onClick = { viewModel.setLocationTracking(it) }
        ) {
            Text(text = stringResource(id = R.string.settings_use_location))
        }
        Divider(Modifier.padding(horizontal = 20.dp))
        if (stringResource(R.string.useFirebase) == "true") {
            CarSwitchRow(
                switchState = viewModel.settingsState.analytics,
                onClick = { viewModel.setAnalytics(it) }
            ) {
                Text(text = "Enable crash reports and analytics")
            }
        } else {
            CarRow(title = "Google Firebase is not included in this build.")
        }
    }
}