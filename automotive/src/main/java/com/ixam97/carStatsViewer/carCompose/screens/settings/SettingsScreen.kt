package com.ixam97.carStatsViewer.carCompose.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.carCompose.CarComposeGlobalViewModel
import com.ixam97.carStatsViewer.carCompose.deviceIsWideScreen
import com.ixam97.carStatsViewer.carCompose.theme.CarComposeIcon
import com.ixam97.carStatsViewer.carCompose.theme.polestar4ContentPadding
import de.ixam97.carcompose.components.controls.CarIconButton
import de.ixam97.carcompose.components.controls.CarRow
import de.ixam97.carcompose.components.layout.CarColumn
import de.ixam97.carcompose.components.layout.CarListItem
import de.ixam97.carcompose.components.layout.CarListSection
import de.ixam97.carcompose.components.layout.CarPaneLayout
import de.ixam97.carcompose.components.layout.CarTabLayout
import de.ixam97.carcompose.theme.CarTheme
import kotlinx.serialization.Serializable

@Serializable
object SettingsScreenNavKey: NavKey

enum class SettingsTabKeys {
    General, Appearance, Privacy, Apis, About, Dev, Data
}

@Composable
fun SettingsScreen(
    backStack: NavBackStack<NavKey>,
    globalViewModel: CarComposeGlobalViewModel,
    viewModel: SettingsViewModel = viewModel()
) {
    val globalState by globalViewModel.globalState.collectAsState()

    val generalTab = CarTabLayout.Tab(
        title = stringResource(R.string.settings_general),
        icon = rememberVectorPainter(Icons.Outlined.Settings),
        key = SettingsTabKeys.General
    )
    val appearanceTab = CarTabLayout.Tab(
        title = stringResource(R.string.settings_appearance),
        icon = rememberVectorPainter(Icons.Outlined.Palette),
        key = SettingsTabKeys.Appearance
    )
    val privacyTab = CarTabLayout.Tab(
        title = stringResource(R.string.settings_privacy_location),
        icon = rememberVectorPainter(Icons.Outlined.LocationOn),
        key = SettingsTabKeys.Privacy
    )
    val apisTab = CarTabLayout.Tab(
        title = stringResource(R.string.settings_apis_title),
        icon = rememberVectorPainter(Icons.Outlined.Share),
        key = SettingsTabKeys.Apis
    )
    val aboutTab = CarTabLayout.Tab(
        title = stringResource(R.string.settings_about),
        icon = rememberVectorPainter(Icons.Outlined.Info),
        key = SettingsTabKeys.About
    )
    val devTab = CarTabLayout.Tab(
        title = stringResource(R.string.settings_dev_settings),
        icon = rememberVectorPainter(Icons.Outlined.DataObject),
        key = SettingsTabKeys.Dev
    )

    val settingsTabs = mutableListOf(
        generalTab, appearanceTab, privacyTab, apisTab, aboutTab
    )

    if (globalState.devModeEnabled) {
        settingsTabs.add(devTab)
    }

    if (deviceIsWideScreen()) {
        CarTabLayout(
            headerTitle = stringResource(R.string.settings_title),
            headerStartContent = {
                CarIconButton(
                    painter = painterResource(R.drawable.ic_arrow_backwards_48),
                    tint = CarTheme.carColors.accent,
                    onClick = { backStack.removeAt(backStack.lastIndex) }
                )
            },
            tabOrientation = CarTabLayout.Orientation.VerticalCompact,
            tabs = settingsTabs,
            maxTabs = settingsTabs.size.coerceAtMost(7),
            selectedKey = viewModel.selectedSettingsTabKey,
            onTabSelected = { viewModel.setSettingsTabKey(it) }
        ) { key ->
            AnimatedVisibility(
                visible = key == SettingsTabKeys.General,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                CarComposeSettingsGeneralContent(Modifier, backStack, globalViewModel, viewModel)
            }
            AnimatedVisibility(
                visible = key == SettingsTabKeys.Appearance,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                SettingsAppearanceContent(Modifier, backStack, globalViewModel, viewModel)
            }
            AnimatedVisibility(
                visible = key == SettingsTabKeys.Privacy,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                CarComposeSettingsLocationContent(Modifier, globalViewModel, backStack)
            }
            AnimatedVisibility(
                visible = key == SettingsTabKeys.Apis,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                SettingsApisContent(Modifier, backStack, viewModel)
            }
            AnimatedVisibility(
                visible = key == SettingsTabKeys.About,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                SettingsAboutContent(Modifier, backStack, globalViewModel)
            }
        }
    } else {
        CarPaneLayout(
            headerTitle = stringResource(R.string.settings_title),
            headerStartContent = {
                CarIconButton(
                    painter = painterResource(R.drawable.ic_arrow_backwards_48),
                    tint = CarTheme.carColors.accent,
                    onClick = { backStack.removeAt(backStack.lastIndex) }
                )
            }
        ) {
            CarComposeSettingsContent(
                modifier = Modifier.padding(start = if (deviceIsWideScreen()) polestar4ContentPadding else 0.dp) ,
                globalViewModel = globalViewModel,
                backStack = backStack
            )
        }
    }
}

@Composable
fun CarComposeSettingsContent(
    modifier: Modifier = Modifier,
    globalViewModel: CarComposeGlobalViewModel,
    backStack: NavBackStack<NavKey>,
) {
    CarColumn(
        modifier = modifier
    ) {
        val globalState by globalViewModel.globalState.collectAsState()

        val settingsListItems = mutableListOf(
            CarListItem {
                CarRow(
                    leadingContent = { CarComposeIcon(Icons.Outlined.Settings) },
                    title = stringResource(R.string.settings_general),
                    browsable = true,
                    onBrowse = { backStack.add(SettingsGeneralScreenNavKey) }
                )
            },
            CarListItem {
                CarRow(
                    leadingContent = { CarComposeIcon(Icons.Outlined.Palette) },
                    title = stringResource(R.string.settings_appearance),
                    browsable = true,
                    onBrowse = { backStack.add(SettingsAppearanceScreenNavKey) }
                )
            },
            CarListItem {
                CarRow(
                    leadingContent = { CarComposeIcon(Icons.Outlined.LocationOn) },
                    title = stringResource(R.string.settings_privacy_location),
                    browsable = true,
                    onBrowse = { backStack.add(SettingsLocationScreenNavKey) }
                )
            },
            CarListItem {
                CarRow(
                    leadingContent = { CarComposeIcon(Icons.Outlined.Share) },
                    title = stringResource(R.string.settings_apis_title),
                    browsable = true,
                    onBrowse = { backStack.add(SettingsApisNavKey) }
                )
            },
            CarListItem {
                CarRow(
                    leadingContent = { CarComposeIcon(Icons.Outlined.Info) },
                    title = stringResource(R.string.settings_about),
                    browsable = true,
                    onBrowse = { backStack.add(AboutScreenNavKey) },
                )
            }
        )

        if (globalState.devModeEnabled) settingsListItems.add(CarListItem {
            CarRow(
                leadingContent = { CarComposeIcon(Icons.Outlined.DataObject) },
                title = stringResource(R.string.settings_dev_settings),
                browsable = true,
                onBrowse = { }
            )
        })

        CarListSection(
            listItems = settingsListItems
        )
    }
}