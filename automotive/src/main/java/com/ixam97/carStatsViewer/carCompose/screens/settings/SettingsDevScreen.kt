package com.ixam97.carStatsViewer.carCompose.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.carCompose.CarComposeGlobalViewModel
import com.ixam97.carStatsViewer.carCompose.deviceIsWideScreen
import com.ixam97.carStatsViewer.carCompose.theme.polestar4ContentPadding
import com.ixam97.carStatsViewer.utils.DistanceUnitEnum
import de.ixam97.carcompose.components.controls.CarButton
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
data object SettingsDevScreenNavKey: MainSettingsNavKey

@Composable
fun SettingsDevScreen(
    backStack: NavBackStack<NavKey>,
    globalViewModel: CarComposeGlobalViewModel,
    viewModel: SettingsViewModel = viewModel()
) {
    CarPaneLayout(
        headerTitle = stringResource(R.string.settings_dev_settings),
        headerStartContent = {
            CarIconButton(
                painter = painterResource(R.drawable.ic_arrow_backwards_48),
                tint = CarTheme.carColors.accent,
                onClick = { backStack.removeAt(backStack.lastIndex) }
            )
        }
    ) {
        SettingsDevContent(
            modifier = Modifier.padding(start = if (deviceIsWideScreen()) polestar4ContentPadding else 0.dp) ,
            globalViewModel = globalViewModel,
            backStack = backStack,
            viewModel = viewModel
        )
    }
}

@Composable
fun SettingsDevContent(
    modifier: Modifier = Modifier,
    backStack: NavBackStack<NavKey>,
    globalViewModel: CarComposeGlobalViewModel,
    viewModel: SettingsViewModel
) {
    val settingsDevState by viewModel.settingsDevState.collectAsState()

    CarColumn(
        modifier = modifier
    ) {
        CarListSection(
            sectionTitle = "General Debugging",
            dividerAtBottom = true,
            listItems = listOf(
                CarListItem {
                    CarRowSwitch(
                        title = "Enable delay loading delays",
                        state = settingsDevState.loadingDelays,
                    ) { }
                },
                CarListItem {
                    CarRowSwitch(
                        title = "Enable additional color schemes",
                        state = settingsDevState.additionalColorSchemes
                    ) { }
                },
                CarListItem {
                    CarRowSwitch(
                        title = "Miles as distance unit",
                        state = settingsDevState.milesAsDistanceUnit == DistanceUnitEnum.MILES
                    ) { }
                },
                CarListItem {
                    CarRow(
                        title = "Debug actions",
                        descriptionContent = {
                            Row() {
                                CarButton(
                                    modifier = Modifier.weight(1f),
                                    onClick = {}
                                ) { Text("Debug Crash") }
                                Spacer(Modifier.size(CarTheme.carDimensions.defaultHorizontalPadding))
                                CarButton(
                                    modifier = Modifier.weight(1f),
                                    onClick = {}
                                ) { Text("Scan Fonts") }
                            }
                        }
                    )
                },
            )
        )
        CarListSection(
            sectionTitle = "User Setup:",
            dividerAtBottom = true,
            listItems = listOf(
                CarListItem {
                    CarRow(
                        title = "User ID",
                        descriptionContent = {
                            Text("This ID is used to identify the user when sending logs or screenshots.",
                                style = CarTheme.carTypography.rowContent,
                                color = LocalContentColor.current.copy(alpha =  0.7f)
                            )
                            Spacer(Modifier.size(CarTheme.carDimensions.defaultVerticalPadding))
                            CarTextField(
                                value = settingsDevState.userId,
                                onValueChange = {}
                            )
                        }
                    )
                },
                CarListItem {
                    CarRow(
                        title = "User Mail",
                        descriptionContent = {
                            Text("A copy of submitted debug data and screenshots will be sent to this address.",
                                style = CarTheme.carTypography.rowContent,
                                color = LocalContentColor.current.copy(alpha =  0.7f)
                            )
                            Spacer(Modifier.size(CarTheme.carDimensions.defaultVerticalPadding))
                            CarTextField(
                                value = settingsDevState.userMail,
                                onValueChange = {},
                                trailingIcon = {

                                }
                            )
                        }
                    )
                },
            )
        )
        CarListSection(
            sectionTitle = "Screenshot Service:",
            dividerAtBottom = true,
            listItems = listOf(
                CarListItem {
                    CarRow(
                        content = {
                            Column() {
                                Text(
                                    text = "This launches a screen capture as foreground service and allows CSV to take " +
                                            "screenshots of the infotainment system. Pull down the notification center " +
                                            "anywhere and press \"Take Screenshot\" to capture the current screen.\n\n" +
                                            "If you want to receive the screenshots yourself, add an additional Email " +
                                            "address in the user setup section. Otherwise, screenshots will be sent to " +
                                            "the developer directly as this is mainly a debugging tool.",
                                    style = CarTheme.carTypography.rowContent,
                                    color = LocalContentColor.current.copy(alpha =  0.7f)
                                )
                                Spacer(Modifier.size(CarTheme.carDimensions.defaultVerticalPadding))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CarButton(
                                        onClick = {},
                                        active = settingsDevState.screenshotServiceRunning
                                    ) { Text( "${if (settingsDevState.screenshotServiceRunning) "Stop" else "Start"} Screenshot Service") }
                                    Spacer(Modifier.size(CarTheme.carDimensions.defaultHorizontalPadding))
                                    Text("Screenshots taken: ${settingsDevState.numberOfScreenshots}",
                                        style = CarTheme.carTypography.rowTitle
                                    )
                                    Spacer(Modifier.weight(1f))
                                    CarIconButton(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        enabled = settingsDevState.numberOfScreenshots > 0
                                    ) { }
                                }
                            }
                        }
                    )
                }
            )
        )
    }
}
