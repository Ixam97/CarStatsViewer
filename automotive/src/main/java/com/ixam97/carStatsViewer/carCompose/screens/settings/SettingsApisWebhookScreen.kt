package com.ixam97.carStatsViewer.carCompose.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.ixam97.carStatsViewer.BuildConfig
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.carCompose.CarComposeGlobalViewModel
import com.ixam97.carStatsViewer.carCompose.deviceIsWideScreen
import com.ixam97.carStatsViewer.carCompose.theme.polestar4ContentPadding
import de.ixam97.carcompose.components.controls.CarIconButton
import de.ixam97.carcompose.components.controls.CarRow
import de.ixam97.carcompose.components.controls.CarRowSwitch
import de.ixam97.carcompose.components.controls.CarSegmentedButton
import de.ixam97.carcompose.components.controls.CarTextField
import de.ixam97.carcompose.components.layout.CarColumn
import de.ixam97.carcompose.components.layout.CarListItem
import de.ixam97.carcompose.components.layout.CarListSection
import de.ixam97.carcompose.components.layout.CarPaneLayout
import de.ixam97.carcompose.theme.CarTheme
import kotlinx.serialization.Serializable

@Serializable
data object SettingsApisWebhookScreenNavKey: NavKey

@Composable
fun SettingsApisWebhookScreen(
    backStack: NavBackStack<NavKey>,
    onBack: () -> Unit,
    globalViewModel: CarComposeGlobalViewModel,
    viewModel: SettingsApisWebhookViewModel = viewModel()
) {
    CarPaneLayout(
        headerTitle = stringResource(R.string.settings_apis_http),
        onBackAction = onBack
    ) {
        SettingsApisWebhookContent(
            modifier = Modifier.padding(start = if (deviceIsWideScreen()) polestar4ContentPadding else 0.dp),
            viewModel = viewModel
        )
    }
}

@Composable
fun SettingsApisWebhookContent(
    modifier: Modifier = Modifier,
    viewModel: SettingsApisWebhookViewModel
) {
    val webhookConfigState by viewModel.webhookConfigState.collectAsState()

    val listItems = mutableListOf(
        CarListItem {
            CarRow(title = stringResource(R.string.http_description))
        },
        CarListItem {
            CarRow(
                title = "Endpoint URL",
                descriptionContent = {
                    CarTextField(
                        value = webhookConfigState.endpointUrl,
                        singleLine = true,
                        onValueChange = {
                            viewModel.setWebhookEndpointUrl(it)
                        },
                        trailingIcon = {
                            TextBoxCheckmark(webhookConfigState.endpointUrlValid)
                        }
                    )
                }
            )
        },
        CarListItem {
            CarRow(
                title = stringResource(R.string.http_username),
                descriptionContent = {
                    CarTextField(
                        value = webhookConfigState.username,
                        singleLine = true,
                        onValueChange = { viewModel.setWebhookUserName(it)}
                    )
                }
            )
        },
        CarListItem {
            CarRow(
                title = stringResource(R.string.http_password),
                descriptionContent = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(CarTheme.carDimensions.defaultHorizontalPadding / 2)
                    ) {
                        CarTextField(
                            modifier = Modifier.weight(1f),
                            value = webhookConfigState.password,
                            singleLine = true,
                            onValueChange = { viewModel.setWebhookPassword(it)},
                            visualTransformation = if (webhookConfigState.passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
                        )
                        CarIconButton(
                            imageVector = Icons.Outlined.VisibilityOff,
                            activeImageVector = Icons.Filled.Visibility,
                            activeTint = CarTheme.carColors.accent,
                            active = webhookConfigState.passwordVisible
                        ) { viewModel.setWebhookPasswordVisible(!webhookConfigState.passwordVisible) }
                    }
                }
            )
        },
        CarListItem {
            CarRowSwitch(
                title = stringResource(R.string.settings_apis_use),
                state = webhookConfigState.useApi,
                enabled = webhookConfigState.run {
                    (endpointUrlValid == true) && username.isNotBlank() && password.isNotBlank()
                }
            ) { viewModel.setWebhookEnabled(it) }
        },
        CarListItem {
            CarRowSwitch(
                title = stringResource(R.string.settings_use_location),
                state = webhookConfigState.locationTracking
            ) { viewModel.setWebhookLocationTracking(it) }
        },
        CarListItem {
            CarRow(
                title = "Telemetry type",
                descriptionContent = {
                    CarSegmentedButton(
                        segments = listOf(
                            CarSegmentedButton.Segment(
                                key = SettingsWebhookTelemetryType.RealTime,
                                content = {
                                    Text("Real time")
                                }
                            ),
                            CarSegmentedButton.Segment(
                                key = SettingsWebhookTelemetryType.DrivePoints,
                                content = {
                                    Text("Drive points")
                                }
                            ),
                            CarSegmentedButton.Segment(
                                key = SettingsWebhookTelemetryType.Both,
                                content = {
                                    Text("Both")
                                }
                            ),
                        ),
                        selectedKey = webhookConfigState.telemetryType,
                        onSegmentChanged = {
                            viewModel.setWebhookTelemetryType(it?: SettingsWebhookTelemetryType.RealTime)
                        }
                    )
                }
            )
        }
    )

    if (BuildConfig.FLAVOR_aaos != "carapp") {
        listItems.add(CarListItem {
            CarRowSwitch(
                title = stringResource(R.string.settings_apis_show_status_icon),
                state = webhookConfigState.showStatusIcon
            ) { viewModel.setWebhookStatusIcon(it) }
        })
    }

    CarColumn(
        modifier = modifier
    ) {
        CarListSection(
            listItems = listItems
        )
    }
}