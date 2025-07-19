package com.ixam97.carStatsViewer.compose.screens.settings

import android.content.Intent
import android.media.projection.MediaProjectionManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.navigation.NavController
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.compose.DefaultColumnScrollbar
import com.ixam97.carStatsViewer.compose.SettingsViewModel
import com.ixam97.carStatsViewer.compose.components.CarGradientButton
import com.ixam97.carStatsViewer.compose.components.CarIconButton
import com.ixam97.carStatsViewer.compose.components.CarRow
import com.ixam97.carStatsViewer.compose.components.CarSegmentedButton
import com.ixam97.carStatsViewer.compose.components.CarSwitchRow
import com.ixam97.carStatsViewer.compose.screens.SettingsScreens
import com.ixam97.carStatsViewer.compose.theme.badRed
import com.ixam97.carStatsViewer.utils.DistanceUnitEnum
import com.ixam97.carStatsViewer.utils.ScreenshotService
import com.ixam97.carStatsViewer.utils.ScreenshotServiceConfig

@Composable
fun DevSettings(
    navController: NavController,
    viewModel: SettingsViewModel
) {

    val context = LocalContext.current

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

    DefaultColumnScrollbar(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Text(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp, bottom = 10.dp),
            color = MaterialTheme.colors.primary,
            text = "General Debugging:"
        )
        Divider(Modifier.padding(horizontal = 24.dp))
        CarSwitchRow(
            switchState = viewModel.devSettingsState.debugDelays,
            onClick = { newState ->
                viewModel.setDebugDelays(newState)
            }
        ) { Text("Enable debug loading delays") }
        Divider(Modifier.padding(horizontal = 24.dp))
        CarSwitchRow(
            switchState = viewModel.devSettingsState.debugColors,
            onClick = { newState ->
                viewModel.setDebugColors(newState)
            }
        ) { Text("Enable additional color themes") }
        Divider(Modifier.padding(horizontal = 24.dp))
        CarSwitchRow(
            switchState = (viewModel.devSettingsState.distanceUnit == DistanceUnitEnum.MILES),
            onClick = { newState ->
                viewModel.setDistanceUnit(newState)
            }
        ) { Text("Miles as Distance unit") }
        Divider(Modifier.padding(horizontal = 24.dp))
        CarRow(
            title = "Debug actions:",
            customContent = {
                Row {
                    CarGradientButton(
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.debugCrash() },
                        active = true,
                        gradient = Brush.horizontalGradient(listOf(badRed, badRed))
                    ) {
                        Text("Debug Crash")
                    }
                    Spacer(Modifier.size(20.dp))
                    CarGradientButton(
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.scanAvailableFonts() },
                    ) {
                        Text("Scan Fonts")
                    }
                }
            }
        )
        Divider(Modifier.padding(horizontal = 24.dp))
        CarRow(
            title = "User ID",
            customContent = {
                Column {
                    Text(
                        text = "This ID is used to identify the user when sending logs or screenshots.",
                        color = colorResource(id = R.color.secondary_text_color)
                    )
                    Spacer(modifier = Modifier.size(15.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            modifier = Modifier.weight(1f),
                            value = viewModel.devSettingsState.userID,
                            onValueChange = { viewModel.setUserID(it) },
                        )
                        IconButton(
                            modifier = Modifier.size(60.dp),
                            onClick = { viewModel.setUserID("Anonymous") }
                        ) {
                            Icon(
                                modifier = Modifier.size(50.dp),
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colors.onSurface
                            )
                        }
                    }
                }
            }
        )
        Divider(Modifier.padding(horizontal = 24.dp))
        Text(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp, bottom = 10.dp),
            color = MaterialTheme.colors.primary,
            text = "Screenshots:"
        )
        Divider(Modifier.padding(horizontal = 24.dp))
        CarRow(
            title = "Screenshot Service",
            text =  "This launches a screen capture as foreground service and allows CSV to take " +
                    "screenshots of the infotainment system. Pull down the notification center " +
                    "anywhere and press \"Take Screenshot\" to capture the current screen.\n\n" +
                    "If you want to receive the screenshots yourself, add an additional Email " +
                    "address below. Otherwise, screenshots will be sent to the developer directly " +
                    "as this is mainly a debugging tool."
        )
        Divider(Modifier.padding(horizontal = 24.dp))
        CarRow(
            title = "Additional Email address:",
            customContent = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        modifier = Modifier.weight(1f),
                        value = viewModel.devSettingsState.screenshotReceiver,
                        onValueChange = { viewModel.setScreenshotReceiver(it) },
                        isError = (viewModel.devSettingsState.validReceiverAddress == false),
                        trailingIcon = {
                            if (viewModel.devSettingsState.validReceiverAddress == true) {
                                Icon(
                                    modifier = Modifier
                                        .padding(horizontal = 10.dp)
                                        .size(40.dp),
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.Green
                                )
                            } else if (viewModel.devSettingsState.validReceiverAddress == false) {
                                Icon(
                                    modifier = Modifier
                                        .padding(horizontal = 10.dp)
                                        .size(40.dp),
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = badRed
                                )
                            }
                        }
                    )
                    IconButton(
                        modifier = Modifier.size(60.dp),
                        onClick = {viewModel.setScreenshotReceiver("")}
                    ) {
                        Icon(
                            modifier = Modifier.size(50.dp),
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colors.onSurface
                        )
                    }
                }
            }
        )
        Divider(Modifier.padding(horizontal = 24.dp))
        Row(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CarGradientButton(
                modifier = Modifier
                    .width(350.dp),
                active = viewModel.devSettingsState.isScreenshotServiceRunning,
                onClick = {
                    if (viewModel.devSettingsState.isScreenshotServiceRunning) {
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
            ) {
                if (viewModel.devSettingsState.isScreenshotServiceRunning) {
                    Text("Stop Screenshot Service")
                } else {
                    Text("Start Screenshot Service")
                }
            }
            Spacer(modifier = Modifier.size(40.dp))
            Text("Screenshots taken: ${viewModel.devSettingsState.numberOfScreenshots}")
            Spacer(modifier =  Modifier.weight(1f))
            CarIconButton(
                onClick = { viewModel.submitScreenshots(context) },
                iconResId = R.drawable.ic_send,
                enabled = (viewModel.devSettingsState.numberOfScreenshots > 0)
            )
        }
        Divider(Modifier.padding(horizontal = 24.dp))
        Text(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp, bottom = 10.dp),
            color = MaterialTheme.colors.primary,
            text = "Logging:"
        )
        Divider(Modifier.padding(horizontal = 24.dp))
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
        Divider(Modifier.padding(horizontal = 24.dp))
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
        Divider(Modifier.padding(horizontal = 24.dp))
        CarRow(
            title = "Log actions:",
            customContent = {
                Row {
                    CarGradientButton(
                        modifier = Modifier.weight(1f),
                        enabled = true,
                        onClick = {
                            viewModel.submitLog(context)
                        }
                    ) { Text("Submit log") }
                    Spacer(Modifier.size(20.dp))
                    CarGradientButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            viewModel.clearLog(context)
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