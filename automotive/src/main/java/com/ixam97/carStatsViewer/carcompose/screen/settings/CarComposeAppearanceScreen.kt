package com.ixam97.carStatsViewer.carcompose.screen.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
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
import com.ixam97.carStatsViewer.carcompose.theme.polestar4ContentPadding
import de.ixam97.carcompose.components.controls.CarIconButton
import de.ixam97.carcompose.components.controls.CarRow
import de.ixam97.carcompose.components.controls.CarSegmentedButton
import de.ixam97.carcompose.components.layout.CarColumn
import de.ixam97.carcompose.components.layout.CarListSectionTitle
import de.ixam97.carcompose.components.layout.CarPaneLayout
import de.ixam97.carcompose.theme.CarTheme
import kotlinx.serialization.Serializable

@Serializable
object AppearanceScreenNavKey: NavKey

@Composable
fun CarComposeAppearanceScreen(
    globalViewModel: CarComposeViewModel,
    backStack: NavBackStack<NavKey>
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
        CarComposeAppearanceContent(
            modifier = Modifier.padding(start = if (globalViewModel.carComposeState.vehicleModel == VehicleModel.Polestar4) polestar4ContentPadding else 0.dp) ,
            globalViewModel = globalViewModel,
            backStack = backStack
        )
    }
}

@Composable
fun CarComposeAppearanceContent(
    modifier: Modifier = Modifier,
    globalViewModel: CarComposeViewModel,
    backStack: NavBackStack<NavKey>
) {
    CarColumn(
        modifier = modifier
    ) {
        CarListSectionTitle("${stringResource(R.string.settings_general)}:")
        CarRow(
            title = stringResource(R.string.settings_theme),
            descriptionContent = {
                CarSegmentedButton(
                    buttonContents = listOf(
                        {
                            Text("Club")
                        },{
                            Text("PS Modern")
                        },{
                            Text("PS Classic")
                        },{
                            Text("Generic")
                        },
                    ),
                    selectedIndex = globalViewModel.carComposeState.uiTypeIndex,
                    onIndexChanged = {
                        globalViewModel.setUiTypeIndex(it)
                    }
                )
            }
        )
    }
}