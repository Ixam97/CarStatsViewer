package com.ixam97.carStatsViewer.compose.screens.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.compose.DefaultColumnScrollbar
import com.ixam97.carStatsViewer.compose.SettingsViewModel
import com.ixam97.carStatsViewer.compose.components.CarSwitchRow

@Composable
fun GeneralSettings(
    viewModel: SettingsViewModel
) {
    DefaultColumnScrollbar(
        modifier = Modifier
            .fillMaxSize()
    ) {
        CarSwitchRow(
            switchState = viewModel.settingsState.autoAppStart,
            onClick = { viewModel.setAutoAppStart(it) }
        ) {
            Text(text = stringResource(id = R.string.settings_autostart))
        }
        Divider(Modifier.padding(horizontal = 20.dp))
        CarSwitchRow(
            switchState = viewModel.settingsState.phoneNotification,
            onClick = { viewModel.setPhoneNotification(it) }
        ) {
            Text(text = stringResource(id = R.string.settings_phone_reminder))
        }
        Divider(Modifier.padding(horizontal = 20.dp))
        CarSwitchRow(
            switchState = viewModel.settingsState.detailedNotifications,
            onClick = { viewModel.setDetailedNotifications(it) }
        ) {
            Text(text = stringResource(id = R.string.settings_notifications))
        }
        Divider(Modifier.padding(horizontal = 20.dp))
    }
}