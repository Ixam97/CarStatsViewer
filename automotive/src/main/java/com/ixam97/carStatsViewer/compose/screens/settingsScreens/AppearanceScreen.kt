package com.ixam97.carStatsViewer.compose.screens.settingsScreens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.ixam97.carStatsViewer.compose.SettingsViewModel
import com.ixam97.carStatsViewer.compose.components.CarRow
import com.ixam97.carStatsViewer.compose.components.CarSegmentedButton

@Composable
fun AppearanceScreen(viewModel: SettingsViewModel) {

    val themeSetting = viewModel.themeSettingStateFLow.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        var selectedIndex by remember { mutableStateOf(0) }

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