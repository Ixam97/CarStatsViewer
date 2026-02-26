package com.ixam97.carStatsViewer.carCompose.screens.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
object SettingsLocationScreenNavKey: NavKey

@Composable
fun CarComposeSettingsLocationScreen(
    globalViewModel: CarComposeGlobalViewModel,
    backStack: NavBackStack<NavKey>,
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
        CarComposeSettingsLocationContent(
            modifier = Modifier.padding(start = if (deviceIsWideScreen()) polestar4ContentPadding else 0.dp) ,
            globalViewModel = globalViewModel,
            backStack = backStack
        )
    }
}

@Composable
fun CarComposeSettingsLocationContent(
    modifier: Modifier = Modifier,
    globalViewModel: CarComposeGlobalViewModel,
    backStack: NavBackStack<NavKey>,
) {
    CarColumn(
        modifier = modifier
    ) {
        CarListSection(
            listItems = listOf(
                CarListItem {
                    CarRowSwitch(
                        title = stringResource(R.string.settings_use_location),
                        state = false,
                        onStateChange = { }
                    )
                },
                CarListItem {
                    CarRowSwitch(
                        title = stringResource(R.string.settings_analytics),
                        state = false,
                        onStateChange = { }
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