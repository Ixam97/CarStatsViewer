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
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
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
                Text(text = stringResource(R.string.settings_analytics))
            }
        } else {
            CarRow(title = stringResource(R.string.settings_firebase_note))
        }
        Divider(modifier = Modifier.padding(horizontal = 24.dp))
        CarRow(
            title = stringResource(R.string.settings_privacy),
            browsable = true,
            external = true,
            onClick = { viewModel.openPrivacyLink(context) }
        )
    }
}