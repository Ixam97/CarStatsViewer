package com.ixam97.carStatsViewer.compose.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.compose.DefaultColumnScrollbar
import com.ixam97.carStatsViewer.compose.SettingsViewModel
import com.ixam97.carStatsViewer.compose.components.CarRow
import com.ixam97.carStatsViewer.compose.components.CarSwitchRow
import com.ixam97.carStatsViewer.compose.screens.SettingsScreens
import com.ixam97.carStatsViewer.compose.theme.conConnected
import com.ixam97.carStatsViewer.compose.theme.conError
import com.ixam97.carStatsViewer.compose.theme.conLimited
import com.ixam97.carStatsViewer.compose.theme.conUnused
import com.ixam97.carStatsViewer.compose.theme.disabledTextColor
import com.ixam97.carStatsViewer.liveDataApi.ConnectionStatus

@Composable
fun ApiSettings(
    viewModel: SettingsViewModel,
    navController: NavController
) {

    val abrpApiKeyAvailable = LocalContext.current.resources.getIdentifier(
        "abrp_api_key",
        "string",
        LocalContext.current.packageName
    ) != 0

    DefaultColumnScrollbar(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Text(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp, bottom = 10.dp),
            color = MaterialTheme.colors.primary,
            text = stringResource(R.string.settings_external_apis)
        )
        Divider(modifier = Modifier.padding(horizontal = 24.dp))
        CarRow(
            title = if (abrpApiKeyAvailable) {
                stringResource(R.string.settings_apis_abrp)
            } else {
                stringResource(R.string.settings_apis_abrp) + " (API key unavailable!)"
            },
            // iconResId = R.mipmap.ic_abrp,
            browsable = true,
            enabled = abrpApiKeyAvailable,
            onClick = {
                navController.navigate(SettingsScreens.APIS_ABRP)
            },
            leadingContent = {
                Image(
                    modifier = Modifier
                        .width(80.dp)
                        .fillMaxHeight(),
                    painter = painterResource(R.mipmap.ic_abrp),
                    contentDescription = null
                )
            },
            trailingContent = {
                Icon(
                    painter = painterResource(R.drawable.ic_connected),
                    contentDescription = null,
                    tint = tintFromStatus(viewModel.apiSettingsState.abrpStatus)
                )
            }
        )
        Divider(Modifier.padding(horizontal = 20.dp))
        CarRow(
            title = stringResource(R.string.settings_apis_http),
            iconResId = R.drawable.ic_webhook,
            browsable = true,
            onClick = {
                navController.navigate(SettingsScreens.APIS_HTTP)
            },
            trailingContent = {
                Icon(
                    painter = painterResource(R.drawable.ic_connected),
                    contentDescription = null,
                    tint = tintFromStatus(viewModel.apiSettingsState.httpStatus)
                )
            }
        )
        if (viewModel.isDevEnabled) {
            Divider(Modifier.padding(horizontal = 24.dp))
            Text(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .padding(top = 24.dp, bottom = 10.dp),
                color = MaterialTheme.colors.primary,
                text = stringResource(R.string.settings_trip_export)
            )
            Divider(Modifier.padding(horizontal = 24.dp))
            CarRow(
                title = stringResource(R.string.settings_trip_export_hint)
            )
            Divider(Modifier.padding(horizontal = 24.dp))
            CarSwitchRow(
                enabled = false,
                switchState = false,
                onClick = { },
            ) { enabled ->
                Text(
                    text = stringResource(R.string.settings_trip_export_enable),
                    color = if (enabled) MaterialTheme.colors.onSurface else disabledTextColor
                )
            }
            Divider(Modifier.padding(horizontal = 20.dp))
            CarRow(
                enabled = false,
                title = stringResource(R.string.settings_trip_export_mail),
                customContent = {
                    TextField(
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                        value = "",
                        onValueChange = { newValue -> },
                    )
                }
            )
        }
    }
}

private fun tintFromStatus(status: ConnectionStatus): Color {
    return when (status) {
        ConnectionStatus.CONNECTED -> conConnected
        ConnectionStatus.ERROR -> conError
        ConnectionStatus.LIMITED -> conLimited
        ConnectionStatus.UNUSED -> conUnused
    }
}