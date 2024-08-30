package com.ixam97.carStatsViewer.compose.screens.settings.apis

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.compose.components.CarRow
import com.ixam97.carStatsViewer.compose.components.CarSwitchRow

@Composable
fun ABRPSettings() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        CarRow(
            title = stringResource(R.string.abrp_description)
        )
        Divider(Modifier.padding(horizontal = 20.dp))
        CarRow(
            title = stringResource(R.string.abrp_generic_token),
            customContent = {
                TextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = "",
                    onValueChange = { newValue -> },
                )
            }
        )
        Divider(Modifier.padding(horizontal = 20.dp))
        CarSwitchRow(
            switchState = false,
            onClick = { }
        ) {
            Text(stringResource(R.string.settings_apis_use))
        }
        Divider(Modifier.padding(horizontal = 20.dp))
        CarSwitchRow(
            switchState = false,
            onClick = { }
        ) {
            Text(stringResource(R.string.settings_use_location))
        }
    }
}