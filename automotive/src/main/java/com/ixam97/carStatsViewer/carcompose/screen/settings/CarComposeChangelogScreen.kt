package com.ixam97.carStatsViewer.carcompose.screen.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.carcompose.CarComposeViewModel
import com.ixam97.carStatsViewer.carcompose.VehicleModel
import com.ixam97.carStatsViewer.carcompose.theme.polestar4ContentPadding
import com.ixam97.carStatsViewer.utils.ChangeLogCreator
import de.ixam97.carcompose.components.controls.CarIconButton
import de.ixam97.carcompose.components.controls.CarRow
import de.ixam97.carcompose.components.layout.CarLazyColumn
import de.ixam97.carcompose.components.layout.CarPaneLayout
import de.ixam97.carcompose.theme.CarTheme
import kotlinx.serialization.Serializable

@Serializable
object ChangelogScreenNavKey: NavKey

@Composable
fun CarComposeChangelogScreen(
    globalViewModel: CarComposeViewModel,
    backStack: NavBackStack<NavKey>
) {
    CarPaneLayout(
        headerTitle = stringResource(R.string.dialog_changes_title_changelog),
        headerStartContent = {
            CarIconButton(
                painter = painterResource(R.drawable.ic_arrow_backwards_48),
                tint = CarTheme.carColors.accent,
                onClick = { backStack.removeAt(backStack.lastIndex) }
            )
        }
    ) {
        CarComposeChangelogContent(
            modifier = Modifier.padding(start = if (globalViewModel.carComposeState.vehicleModel == VehicleModel.Polestar4) polestar4ContentPadding else 0.dp) ,
            globalViewModel = globalViewModel,
            backStack = backStack
        )
    }
}

@Composable
fun CarComposeChangelogContent(
    modifier: Modifier = Modifier,
    globalViewModel: CarComposeViewModel,
    backStack: NavBackStack<NavKey>
) {
    val changelogMap = ChangeLogCreator.createChangelog(LocalContext.current).toList()

    CarLazyColumn(
        modifier = modifier
    ) {
        items(items = changelogMap) { (version, text) ->
            CarRow(
                title = version,
                description = text
            )
        }
    }
}