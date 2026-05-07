package com.ixam97.carStatsViewer.carCompose.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.carCompose.deviceIsWideScreen
import com.ixam97.carStatsViewer.carCompose.theme.adaptiveIconPainterResource
import com.ixam97.carStatsViewer.carCompose.theme.polestar4ContentPadding
import com.ixam97.carStatsViewer.compose.ConnectionStatusIcon
import de.ixam97.carcompose.components.controls.CarIconButton
import de.ixam97.carcompose.components.controls.CarRow
import de.ixam97.carcompose.components.controls.CarRowSwitch
import de.ixam97.carcompose.components.controls.CarTextField
import de.ixam97.carcompose.components.layout.CarColumn
import de.ixam97.carcompose.components.layout.CarListItem
import de.ixam97.carcompose.components.layout.CarListSection
import de.ixam97.carcompose.components.layout.CarPaneLayout
import de.ixam97.carcompose.theme.CarTheme
import kotlinx.serialization.Serializable

@Serializable
object SettingsApisNavKey: MainSettingsNavKey

@Composable
fun SettingsApisScreen(
    backStack: NavBackStack<NavKey>,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    CarPaneLayout(
        headerTitle = stringResource(R.string.settings_apis_title),
        onBackAction = onBack
    ) {
        SettingsApisContent(
            modifier = Modifier.padding(start = if (deviceIsWideScreen()) polestar4ContentPadding else 0.dp),
            backStack = backStack,
            viewModel = viewModel
        )
    }
}

@Composable
fun SettingsApisContent(
    modifier: Modifier = Modifier,
    backStack: NavBackStack<NavKey>,
    viewModel: SettingsViewModel
) {
    val settingsApisState by viewModel.settingsApisState.collectAsState()

    CarColumn(
        modifier = modifier
    ) {
        CarListSection(
            sectionTitle = stringResource(R.string.settings_external_apis),
            dividerAtBottom = true,
            listItems = listOf(
                CarListItem {
                    CarRow(
                        title = stringResource(R.string.settings_apis_abrp),
                        leadingContent = {
                            Image(
                                modifier = Modifier.size(CarTheme.carDimensions.iconButtonSize),
                                painter = adaptiveIconPainterResource(R.mipmap.ic_abrp),
                                contentDescription = null
                            )
                        },
                        trailingContent = {
                            ConnectionStatusIcon(settingsApisState.abrpConnectionStatus)
                        },
                        browsable = true,
                        onBrowse = { backStack.add(SettingsApisAbrpScreenNavKey) }
                    )
                },
                CarListItem {
                    CarRow(
                        title = stringResource(R.string.settings_apis_http),
                        leadingContent = {
                            Icon(
                                modifier = Modifier.size(CarTheme.carDimensions.iconButtonSize),
                                painter = painterResource(R.drawable.ic_webhook),
                                contentDescription = null
                            )
                        },
                        trailingContent = {
                            ConnectionStatusIcon(settingsApisState.restConnectionStatus)
                        },
                        browsable = true,
                        onBrowse = { backStack.add(SettingsApisWebhookScreenNavKey) }
                    )
                },
            )
        )

        CarListSection(
            sectionTitle = stringResource(R.string.settings_trip_export),
            listItems = listOf(
                CarListItem {
                    CarRow(
                        title = stringResource(R.string.settings_trip_export_hint)
                    )
                },
                CarListItem {
                    CarRow(
                        title = stringResource(R.string.settings_trip_export_mail),
                        descriptionContent = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CarTextField(
                                    value = settingsApisState.exportMailAddress,
                                    onValueChange = { viewModel.setExportMailAddress(it) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    trailingIcon = {
                                        TextBoxCheckmark(settingsApisState.validExportMailAddress)
                                    }
                                )
                                Spacer(Modifier.size(CarTheme.carDimensions.defaultHorizontalPadding / 2f))
                                CarIconButton(imageVector = Icons.Outlined.Delete) { viewModel.setExportMailAddress("") }
                            }
                        }
                    )
                },
                CarListItem {
                    CarRowSwitch(
                        title = stringResource(R.string.settings_trip_export_enable),
                        state = settingsApisState.exportEnabled,
                        onStateChange = { viewModel.setExportEnabled(it) },
                        enabled = settingsApisState.validExportMailAddress == true
                    )
                },
            )
        )
    }
}

@Composable
internal fun TextBoxCheckmark(valid: Boolean?) {
    if (valid != null) {
        Icon(
            modifier = Modifier.size(40.dp),
            imageVector = if (valid) Icons.Default.Check else Icons.Default.ErrorOutline,
            tint = if (valid) Color.Green else Color.Red,
            contentDescription = null
        )
    } else {
        Box(Modifier.size(40.dp))
    }
}