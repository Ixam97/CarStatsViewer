package com.ixam97.carStatsViewer.compose.screens.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.compose.DefaultColumnScrollbar
import com.ixam97.carStatsViewer.compose.SettingsViewModel
import com.ixam97.carStatsViewer.compose.components.CarRow
import com.ixam97.carStatsViewer.compose.components.CarSegmentedButton
import com.ixam97.carStatsViewer.compose.components.CarSwitchRow

@Composable
fun AppearanceSettings(viewModel: SettingsViewModel) {

    val themeSetting = viewModel.themeSettingStateFLow.collectAsState()

    DefaultColumnScrollbar(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Text(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp, bottom = 10.dp),
            color = MaterialTheme.colors.primary,
            text = "${stringResource(R.string.settings_general)}:"
        )
        Divider(Modifier.padding(horizontal = 20.dp))
        CarRow(
            title = stringResource(R.string.settings_theme),
            customContent = {
                CarSegmentedButton(
                    // modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                    options = if (CarStatsViewer.appPreferences.debugColors) listOf("OEM", "Club", "Orange", "Blue")
                        else listOf("OEM", "Club"),// "Simple"),
                    selectedIndex = themeSetting.value,
                    onSelectedIndexChanged = { index ->
                        viewModel.setTheme(index)
                    }
                )
            }
        )
        Divider(Modifier.padding(horizontal = 20.dp))
        CarSwitchRow(
            switchState = viewModel.settingsState.altConsumptionUnit,
            onClick = { newState -> viewModel.setAltConsumptionUnit(newState) }
        ) {
            Text(text = stringResource(id = R.string.settings_consumption_unit, CarStatsViewer.appPreferences.distanceUnit.unit()))
        }
        Divider(Modifier.padding(horizontal = 20.dp))
        Text(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp, bottom = 10.dp),
            color = MaterialTheme.colors.primary,
            text = stringResource(id = R.string.settings_consumption_plot)
        )
        Divider(Modifier.padding(horizontal = 20.dp))
        CarSwitchRow(
            switchState = viewModel.settingsState.showConsumptionGages,
            onClick = { newState ->
                viewModel.setShowConsumptionGages(newState)
            }
        ) {
            Text(text = stringResource(id = R.string.settings_visible_gages))
        }
        Divider(Modifier.padding(horizontal = 20.dp))
        CarRow(
            title = stringResource(R.string.settings_plot_secondary_color_2),
            customContent = {
                CarSegmentedButton(
                    // modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                    options = listOf(stringResource(R.string.settings_plot_green), stringResource(R.string.settings_plot_white)),
                    selectedIndex = viewModel.settingsState.secondaryConsumptionPlotColor,
                    onSelectedIndexChanged = { index ->
                        viewModel.setSecondaryConsumptionPlotColor(index)
                    }
                )
            }
        )
        Divider(Modifier.padding(horizontal = 20.dp))
        Text(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp, bottom = 10.dp),
            color = MaterialTheme.colors.primary,
            text = stringResource(id = R.string.settings_charge_plot)
        )
        Divider(Modifier.padding(horizontal = 20.dp))
        CarSwitchRow(
            switchState = viewModel.settingsState.showChargingGages,
            onClick = { newState ->
                viewModel.setShowChargingGages(newState)
            }
        ) {
            Text(text = stringResource(id = R.string.settings_visible_gages))
        }
        Divider(Modifier.padding(horizontal = 20.dp))
        CarRow(
            title = stringResource(R.string.settings_plot_secondary_color_2),
            customContent = {
                CarSegmentedButton(
                    // modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                    options = listOf(stringResource(R.string.settings_plot_green), stringResource(R.string.settings_plot_white)),
                    selectedIndex = viewModel.settingsState.secondaryChargePlotColor,
                    onSelectedIndexChanged = { index ->
                        viewModel.setSecondaryChargePlotColor(index)
                    }
                )
            }
        )
    }
}