package com.ixam97.carStatsViewer.carCompose.screens.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import de.ixam97.carcompose.components.controls.CarIconButton
import de.ixam97.carcompose.components.controls.CarRowSwitch
import de.ixam97.carcompose.components.layout.CarColumn
import de.ixam97.carcompose.components.layout.CarListItem
import de.ixam97.carcompose.components.layout.CarListSection
import de.ixam97.carcompose.components.layout.CarPaneLayout
import de.ixam97.carcompose.theme.CarTheme
import kotlinx.serialization.Serializable

@Serializable
object SettingsGeneralScreenNavKey: NavKey

@Composable
fun CarComposeSettingsGeneralScreen(
    backStack: NavBackStack<NavKey>,
    globalViewModel: CarComposeGlobalViewModel,
    viewModel: SettingsViewModel = viewModel()
) {
    CarPaneLayout(
        headerTitle = stringResource(R.string.settings_general),
        headerStartContent = {
            CarIconButton(
                painter = painterResource(R.drawable.ic_arrow_backwards_48),
                tint = CarTheme.carColors.accent,
                onClick = { backStack.removeAt(backStack.lastIndex) }
            )
        }
    ) {
        CarComposeSettingsGeneralContent(
            modifier = Modifier.padding(start = if (deviceIsWideScreen()) polestar4ContentPadding else 0.dp) ,
            globalViewModel = globalViewModel,
            backStack = backStack,
            viewModel = viewModel
        )
    }
}

@Composable
fun CarComposeSettingsGeneralContent(
    modifier: Modifier = Modifier,
    backStack: NavBackStack<NavKey>,
    globalViewModel: CarComposeGlobalViewModel,
    viewModel: SettingsViewModel
) {
    val settingsGeneralState by viewModel.settingsGeneralState.collectAsState()

    CarColumn(
        modifier = modifier
    ) {
        CarListSection(
            listItems = listOf(
                CarListItem {
                    CarRowSwitch(
                        title = stringResource(R.string.settings_autostart),
                        state = settingsGeneralState.autoAppStartEnabled,
                        onStateChange = { viewModel.setAutoAppStartEnabled(it) }
                    )
                },
                CarListItem {
                    CarRowSwitch(
                        title = stringResource(R.string.settings_phone_reminder),
                        state = settingsGeneralState.phoneReminderEnabled,
                        onStateChange = { viewModel.setPhoneReminderEnabled(it) }
                    )
                },
                CarListItem {
                    CarRowSwitch(
                        title = stringResource(R.string.settings_notifications),
                        state = settingsGeneralState.detailedNotificationEnabled,
                        onStateChange = { viewModel.setDetailedNotificationEnabled(it) }
                    )
                },
            )
        )
    }
}