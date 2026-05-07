package com.ixam97.carStatsViewer.carCompose.screens.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.ixam97.carStatsViewer.BuildConfig
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.carCompose.CarComposeGlobalViewModel
import com.ixam97.carStatsViewer.carCompose.deviceIsWideScreen
import com.ixam97.carStatsViewer.carCompose.theme.polestar4ContentPadding
import de.ixam97.carcompose.components.controls.CarRow
import de.ixam97.carcompose.components.controls.CarRowSwitch
import de.ixam97.carcompose.components.controls.CarTextField
import de.ixam97.carcompose.components.layout.CarColumn
import de.ixam97.carcompose.components.layout.CarListItem
import de.ixam97.carcompose.components.layout.CarListSection
import de.ixam97.carcompose.components.layout.CarPaneLayout
import kotlinx.serialization.Serializable

@Serializable
data object SettingsApisAbrpScreenNavKey: NavKey

@Composable
fun SettingsApisAbrpScreen(
    backStack: NavBackStack<NavKey>,
    onBack: () -> Unit,
    globalViewModel: CarComposeGlobalViewModel,
    viewModel: SettingsApisAbrpViewModel = viewModel()
) {
    CarPaneLayout(
        headerTitle = stringResource(R.string.settings_apis_abrp),
        onBackAction = onBack
    ) {
        SettingsApisAbrpContent(
            modifier = Modifier.padding(start = if (deviceIsWideScreen()) polestar4ContentPadding else 0.dp),
            viewModel = viewModel
        )
    }
}

@Composable
private fun SettingsApisAbrpContent(
    modifier: Modifier = Modifier,
    viewModel: SettingsApisAbrpViewModel
) {
    val abrpConfigState by viewModel.abrpConfigState.collectAsState()

    val listItems = mutableListOf(
        CarListItem {
            CarRow(title = stringResource(R.string.abrp_description))
        },
        CarListItem {
            CarRow(
                title = stringResource(R.string.abrp_generic_token),
                descriptionContent = {
                    CarTextField(
                        value = abrpConfigState.genericToken,
                        onValueChange = { viewModel.setAbrpGenericToken(it) }
                    )
                }
            )
        },
        CarListItem {
            CarRowSwitch(
                title = stringResource(R.string.settings_apis_use),
                state = abrpConfigState.useApi,
                enabled = abrpConfigState.genericToken.isNotBlank()
            ) { viewModel.setAbrpEnabled(it) }
        },
        CarListItem {
            CarRowSwitch(
                title = stringResource(R.string.settings_use_location),
                state = abrpConfigState.locationTracking
            ) { viewModel.setAbrpUseLocation(it) }
        }
    )

    if (BuildConfig.FLAVOR_aaos != "carapp") {
        listItems.add(CarListItem {
            CarRowSwitch(
                title = stringResource(R.string.settings_apis_show_status_icon),
                state = abrpConfigState.showStatusIcon
            ) { }
        })
    }

    CarColumn(
        modifier = modifier
    ) {
        CarListSection(listItems = listItems)
    }
}