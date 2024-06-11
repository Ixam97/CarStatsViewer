package com.ixam97.carStatsViewer.compose.screens.settingsScreens

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
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.compose.SettingsViewModel
import com.ixam97.carStatsViewer.compose.components.CarSwitchRow
import com.ixam97.carStatsViewer.compose.components.SideTab
import com.ixam97.carStatsViewer.compose.screens.SettingsScreens

@Composable
fun GeneralSettingsScreen(
    settingsState: SettingsViewModel.SettingsState,
    viewModel: SettingsViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        CarSwitchRow(
            switchState = settingsState.locationTracking,
            onClick = { viewModel.setLocationTracking(!settingsState.locationTracking)}
        ) {
            Text(text = stringResource(id = R.string.settings_use_location))
        }
        Divider(Modifier.padding(horizontal = 20.dp))
        CarSwitchRow(
            switchState = settingsState.altConsumptionUnit,
            onClick = { viewModel.setAltConsumptionUnit(!settingsState.altConsumptionUnit)}
        ) {
            Text(text = stringResource(id = R.string.settings_consumption_unit, CarStatsViewer.appPreferences.distanceUnit.unit()))
        }
        Divider(Modifier.padding(horizontal = 20.dp))
        CarSwitchRow(
            switchState = settingsState.autoAppStart,
            onClick = { viewModel.setAutoAppStart(!settingsState.autoAppStart)}
        ) {
            Text(text = stringResource(id = R.string.settings_autostart))
        }
    }
}