package com.ixam97.carStatsViewer.compose.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ixam97.carStatsViewer.compose.components.CarGradientButton
import com.ixam97.carStatsViewer.compose.components.CarRow
import com.ixam97.carStatsViewer.compose.components.CarSegmentedButton
import com.ixam97.carStatsViewer.compose.components.CarSwitchRow

@Composable
fun DevSettings() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        CarSwitchRow(
            switchState = false,
            onClick = {}
        ) { Text("Miles as Distance unit") }
        Divider(Modifier.padding(horizontal = 20.dp))
        CarSwitchRow(
            switchState = false,
            onClick = {}
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
                    selectedIndex = 0,
                    contentPadding = PaddingValues(20.dp),
                    onSelectedIndexChanged = { index -> }
                )
            }
        )
        Divider(Modifier.padding(horizontal = 20.dp))
        CarRow(
            title = "Max log length:",
            customContent = {
                CarSegmentedButton(
                    // modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                    options = listOf("all", "1.000", "5.000", "10.000"),
                    selectedIndex = 0,
                    contentPadding = PaddingValues(20.dp),
                    onSelectedIndexChanged = { index -> }
                )
            }
        )
        Divider(Modifier.padding(horizontal = 20.dp))
        CarRow(
            title = "Log actions:",
            trailingContent = {
                Row {
                    CarGradientButton(
                        onClick = {}
                    ) { Text("Submit log") }
                    Spacer(Modifier.size(20.dp))
                    CarGradientButton(
                        onClick = {}
                    ) { Text("Delete log") }
                    Spacer(Modifier.size(20.dp))
                    CarGradientButton(
                        onClick = {}
                    ) { Text("Show Log") }
                }
            }
        )
    }
}