package com.ixam97.carStatsViewer.compose.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.compose.SettingsViewModel
import com.ixam97.carStatsViewer.compose.components.CarRow
import com.ixam97.carStatsViewer.compose.components.CarSegmentedButton
import com.ixam97.carStatsViewer.compose.components.CarSwitchRow

@Composable
fun AppearanceSettings(viewModel: SettingsViewModel) {

    val themeSetting = viewModel.themeSettingStateFLow.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        var selectedIndex by remember { mutableStateOf(0) }

        CarSwitchRow(
            switchState = false,
            onClick = {  }
        ) {
            Text(text = stringResource(id = R.string.settings_consumption_unit, CarStatsViewer.appPreferences.distanceUnit.unit()))
        }
        Divider(Modifier.padding(horizontal = 20.dp))
        CarSwitchRow(
            switchState = false,
            onClick = {  }
        ) {
            Text(text = stringResource(id = R.string.settings_visible_gages))
        }
        Divider(Modifier.padding(horizontal = 20.dp))
        CarRow(
            title = "Secondary plot color",
            customContent = {
                CarSegmentedButton(
                    // modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                    options = listOf("Green", "White"),
                    selectedIndex = 0,
                    onSelectedIndexChanged = { index ->

                    }
                )
            }
        )
        Divider(Modifier.padding(horizontal = 20.dp))
        CarRow(
            title = "Theme",
            customContent = {
                CarSegmentedButton(
                    // modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                    options = listOf("OEM", "Club", "Simple"),
                    selectedIndex = themeSetting.value,
                    onSelectedIndexChanged = { index ->
                        viewModel.setTheme(index)
                    }
                )
            }
        )
    }
}