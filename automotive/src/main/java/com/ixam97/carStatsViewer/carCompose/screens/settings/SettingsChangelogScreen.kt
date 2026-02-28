package com.ixam97.carStatsViewer.carCompose.screens.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.carCompose.deviceIsWideScreen
import com.ixam97.carStatsViewer.carCompose.theme.polestar4ContentPadding
import com.ixam97.carStatsViewer.utils.ChangeLogCreator
import de.ixam97.carcompose.components.controls.CarIconButton
import de.ixam97.carcompose.components.controls.CarRow
import de.ixam97.carcompose.components.layout.CarLazyColumn
import de.ixam97.carcompose.components.layout.CarListItem
import de.ixam97.carcompose.components.layout.CarPaneLayout
import de.ixam97.carcompose.components.layout.carListSection
import de.ixam97.carcompose.theme.CarTheme
import kotlinx.serialization.Serializable

@Serializable
object SettingsChangelogScreenNavKey: NavKey

@Composable
fun SettingsChangelogScreen(
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
        SettingsChangelogContent(
            modifier = Modifier.padding(start = if (deviceIsWideScreen()) polestar4ContentPadding else 0.dp)
        )
    }
}

@Composable
fun SettingsChangelogContent(
    modifier: Modifier = Modifier
) {
    val changelogMap = ChangeLogCreator.createChangelog(LocalContext.current).toList()

    CarLazyColumn(
        modifier = modifier
    ) {
        changelogMap.forEach { (version, text) ->
            carListSection(
                listItems = listOf(
                    CarListItem {
                        CarRow(
                            title = version,
                            description = text
                        )
                    }
                )
            )
        }
    }
}