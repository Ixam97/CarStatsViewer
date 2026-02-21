package com.ixam97.carStatsViewer.carcompose.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowDpSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.carcompose.CarComposeViewModel
import com.ixam97.carStatsViewer.carcompose.MainScreenTab
import com.ixam97.carStatsViewer.carcompose.screen.settings.CarComposeSettingsContent
import com.ixam97.carStatsViewer.carcompose.screen.settings.SettingsScreenNavKey
import com.ixam97.carStatsViewer.carcompose.theme.adaptiveIconPainterResource
import de.ixam97.carcompose.components.controls.CarIconButton
import de.ixam97.carcompose.components.layout.CarPaneLayout
import de.ixam97.carcompose.components.layout.CarTabLayout
import de.ixam97.carcompose.theme.CarTheme
import kotlinx.serialization.Serializable

@Serializable
object MainScreenNavKey: NavKey

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun CarComposeMainScreen(
    globalViewModel: CarComposeViewModel,
    backStack: NavBackStack<NavKey>,
    debugOnClose: (() -> Unit)? = null
) {
    val windowWith = currentWindowDpSize().width

    if (windowWith > 1300.dp) {
        CarComposeMainScreenWide(
            globalViewModel = globalViewModel,
            backStack = backStack,
            debugOnClose = debugOnClose
        )
    } else {
        CarComposeMainScreenSlim(
            globalViewModel = globalViewModel,
            backStack = backStack,
            debugOnClose = debugOnClose
        )
    }
}

@Composable
private fun CarComposeMainScreenWide(
    globalViewModel: CarComposeViewModel,
    backStack: NavBackStack<NavKey>,
    debugOnClose: (() -> Unit)?
) {

    val tabsList = listOf(
        CarTabLayout.Tab(
            title = stringResource(R.string.car_app_dashboard),
            icon = painterResource(R.drawable.ic_grid_48),
            iconActive = painterResource(R.drawable.ic_grid_filled_48)
        ),
        CarTabLayout.Tab(
            title = stringResource(R.string.history_title),
            icon = painterResource(R.drawable.ic_carcompose_history),
            enabled = !globalViewModel.movingState
        ),
        CarTabLayout.Tab(
            title = stringResource(R.string.settings_title),
            icon = painterResource(R.drawable.ic_carcompose_settings),
            enabled = !globalViewModel.movingState
        )
    )

    CarTabLayout(
        headerTitle = stringResource(R.string.app_name),
        headerStartContent = { MainScreenAppIcon(debugOnClose) },
        tabs = tabsList,
        tabSelectedIndex = globalViewModel.carComposeState.selectedMainScreenTab.ordinal,
        tabOnIndexChanged = { globalViewModel.setSelectedMainScreenTabIndex(it) }
    ) {
        AnimatedVisibility(
            visible =globalViewModel.carComposeState.selectedMainScreenTab == MainScreenTab.Dashboard,
            enter = fadeIn(),
            exit = fadeOut()
        ) {

        }
        AnimatedVisibility(
            visible =globalViewModel.carComposeState.selectedMainScreenTab == MainScreenTab.History,
            enter = fadeIn(),
            exit = fadeOut()
        ) {

        }
        AnimatedVisibility(
            visible = globalViewModel.carComposeState.selectedMainScreenTab == MainScreenTab.Settings,
            enter = fadeIn(animationSpec = spring(stiffness = 3000f)),
            exit = fadeOut(animationSpec = spring(stiffness = 3000f))
        ) {
            CarComposeSettingsContent(
                globalViewModel = globalViewModel,
                backStack = backStack
            )
        }
    }
}

@Composable
private fun CarComposeMainScreenSlim(
    globalViewModel: CarComposeViewModel,
    backStack: NavBackStack<NavKey>,
    debugOnClose: (() -> Unit)?
) {
    val headerButtonsList = mutableListOf<@Composable (() -> Unit)>(
        {
            CarIconButton(
                onClick = { backStack.add(SettingsScreenNavKey) },
                painter = painterResource(R.drawable.ic_carcompose_settings)
            )
        },
        {
            CarIconButton(
                onClick = {},
                painter = painterResource(R.drawable.ic_carcompose_history)
            )
        },
    )
    CarPaneLayout(
        headerStartContent = { MainScreenAppIcon(debugOnClose) },
        headerTitle = stringResource(R.string.app_name),
        headerIconButtons = headerButtonsList
    ) {
    }
}

@Composable
private fun MainScreenAppIcon(
    debugOnClose: (() -> Unit)?
)
{
    Image(
        modifier = Modifier
            .size(CarTheme.carDimensions.iconButtonSize)
            .clickable(
                onClick = if (debugOnClose != null) debugOnClose else {{}},
                enabled = debugOnClose != null
            ),
        painter = adaptiveIconPainterResource(R.mipmap.ic_launcher),
        contentDescription = null
    )
}