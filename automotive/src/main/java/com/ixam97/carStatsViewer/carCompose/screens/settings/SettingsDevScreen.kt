package com.ixam97.carStatsViewer.carCompose.screens.settings

import android.content.Intent
import android.media.projection.MediaProjectionManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.carCompose.CarComposeGlobalViewModel
import com.ixam97.carStatsViewer.carCompose.deviceIsWideScreen
import com.ixam97.carStatsViewer.carCompose.theme.badRed
import com.ixam97.carStatsViewer.carCompose.theme.polestar4ContentPadding
import com.ixam97.carStatsViewer.compose.RowContentText
import com.ixam97.carStatsViewer.compose.TextFieldWithValidation
import com.ixam97.carStatsViewer.utils.DistanceUnitEnum
import com.ixam97.carStatsViewer.utils.ScreenshotService
import com.ixam97.carStatsViewer.utils.ScreenshotServiceConfig
import de.ixam97.carcompose.components.controls.CarButton
import de.ixam97.carcompose.components.controls.CarButtonDefaults
import de.ixam97.carcompose.components.controls.CarIconButton
import de.ixam97.carcompose.components.controls.CarRow
import de.ixam97.carcompose.components.controls.CarRowSwitch
import de.ixam97.carcompose.components.controls.CarSegmentedButton
import de.ixam97.carcompose.components.controls.CarTextField
import de.ixam97.carcompose.components.layout.CarColumn
import de.ixam97.carcompose.components.layout.CarListItem
import de.ixam97.carcompose.components.layout.CarListSection
import de.ixam97.carcompose.components.layout.CarPaneLayout
import de.ixam97.carcompose.components.layout.LocalCarSnackBarState
import de.ixam97.carcompose.theme.CarTheme
import de.ixam97.carcompose.utils.buildGradientBrush
import kotlinx.serialization.Serializable

@Serializable
data object SettingsDevScreenNavKey: MainSettingsNavKey

@Composable
fun SettingsDevScreen(
    backStack: NavBackStack<NavKey>,
    onBack: () -> Unit,
    globalViewModel: CarComposeGlobalViewModel,
    viewModel: SettingsViewModel = viewModel()
) {
    CarPaneLayout(
        headerTitle = stringResource(R.string.settings_dev_settings),
        onBackAction = onBack
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
    val context = LocalContext.current
    val snackBarState = LocalCarSnackBarState.current
    val settingsDevState by viewModel.settingsDevState.collectAsState()
    val mediaProjectionManager by lazy {
        context.getSystemService<MediaProjectionManager>()!!
    }
    val screenshotServiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val intent = result.data?:return@rememberLauncherForActivityResult
        val config = ScreenshotServiceConfig(
            resultCode = result.resultCode,
            data = intent
        )

        val serviceIntent = Intent(context, ScreenshotService::class.java).apply {
            action = ScreenshotService.START_SCREENSHOT_SERVICE
            putExtra(ScreenshotService.KEY_SCREENSHOT_CONFIG, config)
        }
        context.startForegroundService(serviceIntent)
    }

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
                    ) { viewModel.setLoadingDelays(it) }
                },
                CarListItem {
                    CarRowSwitch(
                        title = "Enable additional color schemes",
                        state = settingsDevState.additionalColorSchemes
                    ) { viewModel.setAdditionalColorSchemes(it)}
                },
                CarListItem {
                    val distanceUnitSegments = listOf(
                        CarSegmentedButton.Segment(
                            content = { Text(DistanceUnitEnum.MILES.unit()) },
                            key = DistanceUnitEnum.MILES
                        ),
                        CarSegmentedButton.Segment(
                            content = { Text(DistanceUnitEnum.KM.unit()) },
                            key = DistanceUnitEnum.KM
                        ),
                    )

                    CarRow(
                        title = "Override distance unit",
                        trailingContent = {
                            CarSegmentedButton(
                                modifier = Modifier
                                    .widthIn(min = CarTheme.carDimensions.buttonMinWidth * 3)
                                    .width(IntrinsicSize.Min),
                                segments = distanceUnitSegments,
                                selectedKey = settingsDevState.distanceUnit,
                                onSegmentChanged = { it?.let { viewModel.setDistanceUnit(it) } }
                            )
                        }
                    )
                },
                CarListItem {
                    CarRow(
                        title = "Debug actions",
                        descriptionContent = {
                            Row() {
                                CarButton(
                                    modifier = Modifier.weight(1f),
                                    onClick = { viewModel.debugCrash(context) },
                                    colors = CarButtonDefaults.colors.copy(
                                        backgroundBrush = buildGradientBrush(listOf(badRed)),
                                        textColor = Color.White
                                    )
                                ) { Text("Debug Crash") }
                                Spacer(Modifier.size(CarTheme.carDimensions.defaultHorizontalPadding))
                                CarButton(
                                    modifier = Modifier.weight(1f),
                                    onClick = { viewModel.scanAvailableFonts() }
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
                            RowContentText("This ID is used to identify the user when sending logs or screenshots.",)
                            Spacer(Modifier.size(CarTheme.carDimensions.defaultVerticalPadding))
                            CarTextField(
                                value = settingsDevState.userId,
                                onValueChange = { viewModel.setDevUserId(it) }
                            )
                        }
                    )
                },
                CarListItem {
                    CarRow(
                        title = "User Mail",
                        descriptionContent = {
                            RowContentText("A copy of submitted debug data and screenshots will be sent to this address.",)
                            Spacer(Modifier.size(CarTheme.carDimensions.defaultVerticalPadding))
                            TextFieldWithValidation(
                                value = settingsDevState.userMail,
                                validAddress = settingsDevState.userMailValid,
                                onValueChange = { viewModel.setDevUserMail(it) }
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
                                RowContentText(
                                    text = "This launches a screen capture as foreground service and allows CSV to take " +
                                            "screenshots of the infotainment system. Pull down the notification center " +
                                            "anywhere and press \"Take Screenshot\" to capture the current screen.\n\n" +
                                            "If you want to receive the screenshots yourself, add an additional Email " +
                                            "address in the user setup section. Otherwise, screenshots will be sent to " +
                                            "the developer directly as this is mainly a debugging tool.",
                                )
                                Spacer(Modifier.size(CarTheme.carDimensions.defaultVerticalPadding))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CarButton(
                                        onClick = {
                                            if (settingsDevState.screenshotServiceRunning) {
                                                Intent(context, ScreenshotService::class.java).also {
                                                    it.action = ScreenshotService.STOP_SCREENSHOT_SERVICE
                                                    context.startForegroundService(it)
                                                }
                                            } else {
                                                screenshotServiceLauncher.launch(
                                                    mediaProjectionManager.createScreenCaptureIntent()
                                                )
                                            }
                                        },
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
                                    ) { viewModel.submitScreenshots(snackBarState) }
                                }
                            }
                        }
                    )
                }
            )
        )

        CarListSection(
            sectionTitle = "Logging:",
            listItems = listOf(
                CarListItem {
                    
                    val logLevelSegments = listOf(
                        CarSegmentedButton.Segment(
                            content = {Text("Verbose")},
                            key = LogLevelKey.Verbose
                        ),
                        CarSegmentedButton.Segment(
                            content = {Text("Debug")},
                            key = LogLevelKey.Debug
                        ),
                        CarSegmentedButton.Segment(
                            content = {Text("Info")},
                            key = LogLevelKey.Info
                        ),
                        CarSegmentedButton.Segment(
                            content = {Text("Warning")},
                            key = LogLevelKey.Warning
                        ),
                        CarSegmentedButton.Segment(
                            content = {Text("Error")},
                            key = LogLevelKey.Error
                        ),
                    )
                    
                    CarRow(
                        title = "Logging Level",
                        descriptionContent = {
                            CarSegmentedButton(
                                segments = logLevelSegments,
                                selectedKey = settingsDevState.logLevelKey,
                                onSegmentChanged = { viewModel.setLogLevel(it) },
                            )
                        }
                    )
                },
                CarListItem {

                    val logLengthSegments = listOf(
                        CarSegmentedButton.Segment(
                            content = {Text("All")},
                            key = LogLengthKey.All
                        ),
                        CarSegmentedButton.Segment(
                            content = {Text("500")},
                            key = LogLengthKey.L500
                        ),
                        CarSegmentedButton.Segment(
                            content = {Text("1.000")},
                            key = LogLengthKey.L1000
                        ),
                        CarSegmentedButton.Segment(
                            content = {Text("2.000")},
                            key = LogLengthKey.L2000
                        ),
                        CarSegmentedButton.Segment(
                            content = {Text("5.000")},
                            key = LogLengthKey.L5000
                        ),
                        CarSegmentedButton.Segment(
                            content = {Text("10.000")},
                            key = LogLengthKey.L10000
                        ),
                    )

                    CarRow(
                        title = "Logging length",
                        descriptionContent = {
                            CarSegmentedButton(
                                segments = logLengthSegments,
                                selectedKey = settingsDevState.logLengthKey,
                                onSegmentChanged = { viewModel.setLogLength(it) }
                            )
                        }
                    )
                },
                CarListItem {
                    CarRow(
                        title = "Log Actions",
                        descriptionContent = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(CarTheme.carDimensions.defaultHorizontalPadding)
                            ) {
                                CarButton(
                                    modifier = Modifier.weight(1f),
                                    onClick = { viewModel.submitLog(snackBarState) }
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Send,
                                        null,
                                        Modifier.size(CarTheme.carDimensions.iconButtonSize)
                                    )
                                    Text("Submit Log")
                                }
                                CarButton(
                                    modifier = Modifier.weight(1f),
                                    onClick = { viewModel.clearLog(context) },
                                    colors = CarButtonDefaults.colors.copy(
                                        backgroundBrush = buildGradientBrush(listOf(badRed)),
                                        textColor = Color.White
                                    )
                                ) {
                                    Icon(
                                        Icons.Outlined.DeleteOutline,
                                        null,
                                        Modifier.size(CarTheme.carDimensions.iconButtonSize)
                                    )
                                    Text("Delete Log")
                                }
                                CarButton(
                                    modifier = Modifier.weight(1f),
                                    onClick = { backStack.add(SettingsDevLogScreenNavKey) }
                                ) { Text("Show Log") }
                            }
                        }
                    )
                }
            )
        )
    }
}
