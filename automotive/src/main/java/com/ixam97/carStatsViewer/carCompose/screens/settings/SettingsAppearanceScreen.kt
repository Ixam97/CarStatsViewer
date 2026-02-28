package com.ixam97.carStatsViewer.carCompose.screens.settings

import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
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
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.carCompose.CarComposeGlobalViewModel
import com.ixam97.carStatsViewer.carCompose.UiBrightnessMode
import com.ixam97.carStatsViewer.carCompose.UiType
import com.ixam97.carStatsViewer.carCompose.deviceIsWideScreen
import com.ixam97.carStatsViewer.carCompose.theme.polestar4ContentPadding
import de.ixam97.carcompose.components.controls.CarIconButton
import de.ixam97.carcompose.components.controls.CarRow
import de.ixam97.carcompose.components.controls.CarRowSwitch
import de.ixam97.carcompose.components.controls.CarSegmentedButton
import de.ixam97.carcompose.components.layout.CarColumn
import de.ixam97.carcompose.components.layout.CarListItem
import de.ixam97.carcompose.components.layout.CarListSection
import de.ixam97.carcompose.components.layout.CarPaneLayout
import de.ixam97.carcompose.theme.CarTheme
import kotlinx.serialization.Serializable

@Serializable
object SettingsAppearanceScreenNavKey: MainSettingsNavKey

enum class PlotColorSegmentKeys {
    Green, White
}

@Composable
fun SettingsAppearanceScreen(
    backStack: NavBackStack<NavKey>,
    globalViewModel: CarComposeGlobalViewModel,
    viewModel: SettingsViewModel = viewModel()
) {
    CarPaneLayout(
        headerTitle = stringResource(R.string.settings_appearance),
        headerStartContent = {
            CarIconButton(
                painter = painterResource(R.drawable.ic_arrow_backwards_48),
                tint = CarTheme.carColors.accent,
                onClick = { backStack.removeAt(backStack.lastIndex) }
            )
        }
    ) {
        SettingsAppearanceContent(
            modifier = Modifier.padding( if (deviceIsWideScreen()) polestar4ContentPadding else 0.dp),
            backStack = backStack,
            globalViewModel = globalViewModel,
            viewModel = viewModel
        )
    }
}

@Composable
fun SettingsAppearanceContent(
    modifier: Modifier = Modifier,
    backStack: NavBackStack<NavKey>,
    globalViewModel: CarComposeGlobalViewModel,
    viewModel: SettingsViewModel
) {
    val globalState by globalViewModel.globalState.collectAsState()
    val settingsAppearanceState by viewModel.settingsAppearanceState.collectAsState()

    val uiThemeSegments = listOf(
        CarSegmentedButton.Segment(
            content = { Text("PS Classic") },
            key = UiType.Classic
        ),
        CarSegmentedButton.Segment(
            content = { Text("PS Modern") },
            key = UiType.Modern
        ),
        CarSegmentedButton.Segment(
            content = { Text("Club") },
            key = UiType.Club
        ),
        CarSegmentedButton.Segment(
            content = { Text("Volvo") },
            key = UiType.Volvo
        ),
        CarSegmentedButton.Segment(
            content = { Text("Generic") },
            key = UiType.Generic
        ),
    )

    val uiBrightnessModeSegments = listOf(
        CarSegmentedButton.Segment(
            content = {Text("Auto")},
            key = UiBrightnessMode.Auto
        ),
        CarSegmentedButton.Segment(
            content = {Text("Dark")},
            key = UiBrightnessMode.Dark
        ),
        CarSegmentedButton.Segment(
            content = {Text("Bright")},
            key = UiBrightnessMode.Bright
        ),
    )

    val plotColorSegments = listOf(
        CarSegmentedButton.Segment(
            content = { Text("Green")},
            key = PlotColorSegmentKeys.Green
        ),
        CarSegmentedButton.Segment(
            content = { Text("White")},
            key = PlotColorSegmentKeys.White
        ),
    )

    CarColumn(
        modifier = modifier
    ) {
        CarListSection(
            sectionTitle = "${stringResource(R.string.settings_general)}:",
            dividerAtBottom = true,
            listItems = listOf(
                CarListItem {
                    CarRow(
                        title = stringResource(R.string.settings_theme),
                        descriptionContent = {
                            CarSegmentedButton(
                                segments = uiThemeSegments,
                                selectedKey = globalState.uiType,
                                onSegmentChanged = { globalViewModel.setUiType(it) }
                            )
                            if (globalState.uiSupportsBrightMode) {
                                Spacer(Modifier.size(CarTheme.carDimensions.defaultVerticalPadding))
                                CarSegmentedButton(
                                    segments = uiBrightnessModeSegments,
                                    selectedKey = globalState.uiBrightnessMode,
                                    onSegmentChanged = { globalViewModel.setUiBrightnessMode(it?: UiBrightnessMode.Auto) }
                                )
                            }
                        }
                    )
                },
                CarListItem {
                    CarRowSwitch(
                        title = stringResource(R.string.settings_consumption_unit, CarStatsViewer.appPreferences.distanceUnit.unit()),
                        state = settingsAppearanceState.altConsumptionUnit,
                        onStateChange = { viewModel.setAltConsumptionUnit(it) }
                    )
                }
            )
        )

        CarListSection(
            sectionTitle = stringResource(R.string.settings_consumption_plot),
            dividerAtBottom = true,
            listItems = listOf(
                CarListItem {
                    CarRowSwitch(
                        title = stringResource(R.string.settings_visible_gages),
                        state = settingsAppearanceState.showPowerBar,
                        onStateChange = { viewModel.setShowPowerBar(it) }
                    )
                },
                CarListItem {
                    CarRow(
                        title = stringResource(R.string.settings_plot_secondary_color_2),
                        trailingContent = {
                            CarSegmentedButton(
                                modifier = Modifier
                                    .widthIn(min = CarTheme.carDimensions.buttonMinWidth * 3)
                                    .width(IntrinsicSize.Min),
                                segments = plotColorSegments,
                                selectedKey = if (settingsAppearanceState.altSecPowerPlotColor) PlotColorSegmentKeys.White else PlotColorSegmentKeys.Green,
                                onSegmentChanged = { viewModel.setAltSecPowerPlotColor(it == PlotColorSegmentKeys.White) }
                            )
                        }
                    )
                }
            )
        )

        CarListSection(
            sectionTitle = stringResource(R.string.settings_charge_plot),
            listItems = listOf(
                CarListItem {
                    CarRowSwitch(
                        title = stringResource(R.string.settings_visible_gages),
                        state = settingsAppearanceState.showChargeBar,
                        onStateChange = { viewModel.setShowChargeBar(it) }
                    )
                },
                CarListItem {
                    CarRow(
                        title = stringResource(R.string.settings_plot_secondary_color_2),
                        trailingContent = {
                            CarSegmentedButton(
                                modifier = Modifier
                                    .widthIn(min = CarTheme.carDimensions.buttonMinWidth * 3)
                                    .width(IntrinsicSize.Min),
                                segments = plotColorSegments,
                                selectedKey = if (settingsAppearanceState.altSecChargePlotColor) PlotColorSegmentKeys.White else PlotColorSegmentKeys.Green,
                                onSegmentChanged = { viewModel.setAltSecChargePlotColor(it == PlotColorSegmentKeys.White) }
                            )
                        }
                    )
                }
            )
        )
    }
}