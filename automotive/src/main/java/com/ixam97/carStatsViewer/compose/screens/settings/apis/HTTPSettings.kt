package com.ixam97.carStatsViewer.compose.screens.settings.apis

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.compose.WebhookSettingsViewModel
import com.ixam97.carStatsViewer.compose.components.CarRow
import com.ixam97.carStatsViewer.compose.components.CarSegmentedButton
import com.ixam97.carStatsViewer.compose.components.CarSwitchRow

@Composable
fun HTTPSettings() {
    val webhookSettingsViewModel: WebhookSettingsViewModel = viewModel()

    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        CarRow(
            title = stringResource(R.string.http_description)
        )
        Divider(Modifier.padding(horizontal = 20.dp))
        CarRow(
            title = "URL",
            customContent = {
                TextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = webhookSettingsViewModel.webhookSettingsState.endpointUrl,
                    onValueChange = { newValue ->
                        webhookSettingsViewModel.setUrl(newValue)
                    },
                )
            }
        )
        Divider(Modifier.padding(horizontal = 20.dp))
        CarRow(
            title = stringResource(R.string.http_username),
            customContent = {
                TextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = webhookSettingsViewModel.webhookSettingsState.userName,
                    onValueChange = { newValue ->
                        webhookSettingsViewModel.setUserName(newValue)
                    },
                )
            }
        )
        Divider(Modifier.padding(horizontal = 20.dp))
        CarRow(
            title = stringResource(R.string.http_password),
            customContent = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        modifier = Modifier.weight(1f),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        value = webhookSettingsViewModel.webhookSettingsState.userPassword,
                        onValueChange = { newValue ->
                            webhookSettingsViewModel.setUserPassword(newValue)
                        },
                    )
                    IconButton(
                        modifier = Modifier.size(60.dp),
                        onClick = {
                            passwordVisible = !passwordVisible
                        }
                    ) {
                        Icon(
                            modifier = Modifier.size(50.dp),
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = if (passwordVisible) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface
                        )
                    }
                }
            }
        )
        Divider(Modifier.padding(horizontal = 20.dp))
        CarSwitchRow(
            switchState = webhookSettingsViewModel.webhookSettingsState.enabled,
            onClick = { newState ->
                webhookSettingsViewModel.setEnabled(newState)
            }
        ) {
            Text(stringResource(R.string.settings_apis_use))
        }
        Divider(Modifier.padding(horizontal = 20.dp))
        CarSwitchRow(
            switchState = webhookSettingsViewModel.webhookSettingsState.useLocation,
            onClick = { newState ->
                webhookSettingsViewModel.setUseLocation(newState)
            }
        ) {
            Text(stringResource(R.string.settings_use_location))
        }
        Divider(Modifier.padding(horizontal = 20.dp))
        CarSwitchRow(
            switchState = webhookSettingsViewModel.webhookSettingsState.debugAbrp,
            onClick = { newState ->
                webhookSettingsViewModel.setDebugAbrp(newState)
            }
        ) {
            Text("Debug ABRP")
        }
        Divider(Modifier.padding(horizontal = 20.dp))
        CarRow(
            title = "Telemetry type",
            customContent = {
                CarSegmentedButton(
                    // modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                    options = listOf("Real time", "Drive points", "Both"),
                    selectedIndex = webhookSettingsViewModel.webhookSettingsState.telemetryType,
                    onSelectedIndexChanged = { index ->
                        webhookSettingsViewModel.setTelemetryType(index)
                    }
                )
            }
        )
    }
}