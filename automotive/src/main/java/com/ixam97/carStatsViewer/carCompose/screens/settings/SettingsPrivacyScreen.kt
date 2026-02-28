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
import de.ixam97.carcompose.components.controls.CarRow
import de.ixam97.carcompose.components.controls.CarRowBrowsableType
import de.ixam97.carcompose.components.controls.CarRowSwitch
import de.ixam97.carcompose.components.layout.CarColumn
import de.ixam97.carcompose.components.layout.CarListItem
import de.ixam97.carcompose.components.layout.CarListSection
import de.ixam97.carcompose.components.layout.CarPaneLayout
import de.ixam97.carcompose.theme.CarTheme
import kotlinx.serialization.Serializable

@Serializable
object SettingsLocationScreenNavKey: MainSettingsNavKey

@Composable
fun SettingsPrivacyScreen(
    backStack: NavBackStack<NavKey>,
    globalViewModel: CarComposeGlobalViewModel,
    viewModel: SettingsViewModel = viewModel()
) {
    CarPaneLayout(
        headerTitle = stringResource(R.string.settings_privacy_location),
        headerStartContent = {
            CarIconButton(
                painter = painterResource(R.drawable.ic_arrow_backwards_48),
                tint = CarTheme.carColors.accent,
                onClick = { backStack.removeAt(backStack.lastIndex) }
            )
        }
    ) {
        SettingsPrivacyContent(
            modifier = Modifier.padding(start = if (deviceIsWideScreen()) polestar4ContentPadding else 0.dp) ,
            backStack = backStack,
            globalViewModel = globalViewModel,
            viewModel = viewModel
        )
    }
}

@Composable
fun SettingsPrivacyContent(
    modifier: Modifier = Modifier,
    backStack: NavBackStack<NavKey>,
    globalViewModel: CarComposeGlobalViewModel,
    viewModel: SettingsViewModel
) {
    val settingsPrivacyState by viewModel.settingsPrivacyState.collectAsState()

    CarColumn(
        modifier = modifier
    ) {
        CarListSection(
            listItems = listOf(
                CarListItem {
                    CarRowSwitch(
                        title = stringResource(R.string.settings_use_location),
                        state = settingsPrivacyState.locationTracking,
                        onStateChange = { viewModel.setLocationTracking(it) }
                    )
                },
                CarListItem {
                    settingsPrivacyState.analytics?.let { analytics ->
                        CarRowSwitch(
                            title = stringResource(R.string.settings_analytics),
                            state = analytics,
                            onStateChange = { viewModel.setAnalytics(it) }
                        )
                    }
                    if (settingsPrivacyState.analytics == null)
                        CarRow(
                            title = stringResource(R.string.settings_firebase_note)
                        )
                },
                CarListItem {
                    CarRow(
                        title = stringResource(R.string.settings_privacy),
                        browsable = true,
                        browsableType = CarRowBrowsableType.External,
                        onBrowse = { }
                    )
                },
            )
        )
    }
}