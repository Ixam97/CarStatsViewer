package com.ixam97.carStatsViewer.compose.screens.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ixam97.carStatsViewer.compose.DefaultColumnScrollbar
import com.ixam97.carStatsViewer.compose.SettingsViewModel
import com.ixam97.carStatsViewer.compose.components.CarGradientButton
import com.ixam97.carStatsViewer.compose.components.CarRow
import com.ixam97.carStatsViewer.compose.components.CarSegmentedButton
import com.ixam97.carStatsViewer.compose.components.CarSwitchRow
import com.ixam97.carStatsViewer.compose.screens.SettingsScreens
import com.ixam97.carStatsViewer.compose.theme.badRed
import com.ixam97.carStatsViewer.utils.DistanceUnitEnum

@Composable
fun DevSettings(
    navController: NavController,
    viewModel: SettingsViewModel
) {
    DefaultColumnScrollbar(
        modifier = Modifier
            .fillMaxSize()
    ) {
        CarSwitchRow(
            switchState = viewModel.devSettingsState.debugDelays,
            onClick = { newState ->
                viewModel.setDebugDelays(newState)
            }
        ) { Text("Enable debug loading delays") }
        Divider(Modifier.padding(horizontal = 20.dp))
        CarSwitchRow(
            switchState = viewModel.devSettingsState.debugColors,
            onClick = { newState ->
                viewModel.setDebugColors(newState)
            }
        ) { Text("Enable additional color themes") }
        Divider(Modifier.padding(horizontal = 20.dp))
        CarSwitchRow(
            switchState = (viewModel.devSettingsState.distanceUnit == DistanceUnitEnum.MILES),
            onClick = { newState ->
                viewModel.setDistanceUnit(newState)
            }
        ) { Text("Miles as Distance unit") }
        Divider(Modifier.padding(horizontal = 20.dp))
        CarSwitchRow(
            switchState = viewModel.devSettingsState.showScreenshotButton,
            onClick = { newState ->
                viewModel.setShowScreenshotButton(newState)
            }
        ) { Text("Show screenshot button") }
        Divider(Modifier.padding(horizontal = 20.dp))
        Text(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp, bottom = 10.dp),
            color = MaterialTheme.colors.primary,
            text = "Logging:"
        )
        Divider(Modifier.padding(horizontal = 20.dp))
        CarRow(
            title = "Logging level:",
            customContent = {
                CarSegmentedButton(
                    // modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                    options = listOf("Verbose", "Debug", "Info", "Warning", "Error"),
                    selectedIndex = viewModel.devSettingsState.loggingLevel,
                    contentPadding = PaddingValues(20.dp),
                    onSelectedIndexChanged = { index ->
                        viewModel.setLoggingLevel(index)
                    }
                )
            }
        )
        Divider(Modifier.padding(horizontal = 20.dp))
        CarRow(
            title = "Max log length:",
            customContent = {
                CarSegmentedButton(
                    // modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                    options = listOf("all", "500", "1.000", "2.000", "5.000", "10.000"),
                    selectedIndex = viewModel.devSettingsState.logLength,
                    contentPadding = PaddingValues(20.dp),
                    onSelectedIndexChanged = { index ->
                        viewModel.setLogLength(index)
                    }
                )
            }
        )
        Divider(Modifier.padding(horizontal = 20.dp))
        CarRow(
            title = "Log actions:",
            customContent = {
                Row {
                    CarGradientButton(
                        modifier = Modifier.weight(1f),
                        enabled = false,
                        onClick = {
                            viewModel.submitLog()
                        }
                    ) { Text("Submit log") }
                    Spacer(Modifier.size(20.dp))
                    CarGradientButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            viewModel.clearLog()
                        },
                        active = true,
                        gradient = Brush.horizontalGradient(listOf(badRed, badRed))
                    ) { Text("Delete log") }
                    Spacer(Modifier.size(20.dp))
                    CarGradientButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            viewModel.loadLog()
                            navController.navigate(SettingsScreens.DEV_LOG)
                        }
                    ) { Text("Show Log") }
                }
            }
        )
    }
}