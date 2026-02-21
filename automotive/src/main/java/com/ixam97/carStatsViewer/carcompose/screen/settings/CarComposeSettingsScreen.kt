package com.ixam97.carStatsViewer.carcompose.screen.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.carcompose.CarComposeViewModel
import com.ixam97.carStatsViewer.carcompose.VehicleModel
import com.ixam97.carStatsViewer.carcompose.theme.CarComposeIcon
import com.ixam97.carStatsViewer.carcompose.theme.polestar4ContentPadding
import de.ixam97.carcompose.components.controls.CarIconButton
import de.ixam97.carcompose.components.controls.CarRow
import de.ixam97.carcompose.components.layout.CarColumn
import de.ixam97.carcompose.components.layout.CarListDivider
import de.ixam97.carcompose.components.layout.CarPaneLayout
import de.ixam97.carcompose.theme.CarTheme
import kotlinx.serialization.Serializable

@Serializable
object SettingsScreenNavKey: NavKey

@Composable
fun CarComposeSettingsScreen(
    globalViewModel: CarComposeViewModel,
    backStack: NavBackStack<NavKey>,
) {
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
            modifier = Modifier.padding(start = if (globalViewModel.carComposeState.vehicleModel == VehicleModel.Polestar4) polestar4ContentPadding else 0.dp) ,
            globalViewModel = globalViewModel,
            backStack = backStack
        )
    }
}

@Composable
fun CarComposeSettingsContent(
    modifier: Modifier = Modifier,
    globalViewModel: CarComposeViewModel,
    backStack: NavBackStack<NavKey>,
) {
    CarColumn(
        modifier = modifier
    ) {
        CarRow(
            leadingContent = { CarComposeIcon(Icons.Outlined.Settings) },
            title = stringResource(R.string.settings_general),
            browsable = true,
            onBrowse = {  }
        )
        CarListDivider()
        CarRow(
            leadingContent = { CarComposeIcon(Icons.Outlined.Edit) },
            title = stringResource(R.string.settings_appearance),
            browsable = true,
            onBrowse = { backStack.add(AppearanceScreenNavKey) }
        )
        CarListDivider()
        CarRow(
            leadingContent = { CarComposeIcon(Icons.Outlined.LocationOn) },
            title = stringResource(R.string.settings_privacy_location),
            browsable = true,
            onBrowse = {  }
        )
        CarListDivider()
        CarRow(
            leadingContent = { CarComposeIcon(Icons.Outlined.Share) },
            title = stringResource(R.string.settings_apis_title),
            browsable = true,
            onBrowse = {  }
        )
        CarListDivider()
        CarRow(
            leadingContent = { CarComposeIcon(Icons.Outlined.Info) },
            title = stringResource(R.string.settings_about),
            browsable = true,
            onBrowse = { backStack.add(AboutScreenNavKey) }
        )
        CarListDivider()
        CarRow(
            leadingContent = { CarComposeIcon(Icons.Outlined.DataObject) },
            title = stringResource(R.string.settings_dev_settings),
            browsable = true,
            onBrowse = {  }
        )
    }
}