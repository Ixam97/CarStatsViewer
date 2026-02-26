package com.ixam97.carStatsViewer.carCompose.screens.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.carCompose.CarComposeGlobalViewModel
import com.ixam97.carStatsViewer.carCompose.deviceIsWideScreen
import com.ixam97.carStatsViewer.carCompose.screens.CarComposeMainScreenViewModel
import com.ixam97.carStatsViewer.carCompose.screens.settings.SettingsScreenNavKey
import com.ixam97.carStatsViewer.carCompose.theme.adaptiveIconPainterResource
import de.ixam97.carcompose.components.controls.CarIconButton
import de.ixam97.carcompose.components.layout.CarPaneLayout
import de.ixam97.carcompose.components.layout.CarTabLayout
import de.ixam97.carcompose.theme.CarTheme
import kotlinx.serialization.Serializable

@Serializable
data class MainScreenNavKey(
    val selectedTabKey: MainScreenTabKeys? = null
): NavKey

@Serializable
enum class MainScreenTabKeys {
    Dashboard, History, Settings
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun CarComposeMainScreen(
    initialTabKey: MainScreenTabKeys? = null,
    viewModel: CarComposeMainScreenViewModel = viewModel {
        CarComposeMainScreenViewModel(initialTabKey)
    },
    globalViewModel: CarComposeGlobalViewModel,
    backStack: NavBackStack<NavKey>,
    debugOnClose: (() -> Unit)? = null,
) {
    if (deviceIsWideScreen()) {
        CarComposeMainScreenWide(
            globalViewModel = globalViewModel,
            backStack = backStack,
            debugOnClose = debugOnClose,
            viewModel = viewModel
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
    globalViewModel: CarComposeGlobalViewModel,
    backStack: NavBackStack<NavKey>,
    debugOnClose: (() -> Unit)?,
    viewModel: CarComposeMainScreenViewModel
) {

    val tabsList = listOf(
        CarTabLayout.Tab(
            title = stringResource(R.string.car_app_dashboard),
            icon = painterResource(R.drawable.ic_grid_48),
            iconActive = painterResource(R.drawable.ic_grid_filled_48),
            key = MainScreenTabKeys.Dashboard
        ),
        CarTabLayout.Tab(
            title = stringResource(R.string.history_title),
            icon = painterResource(R.drawable.ic_carcompose_history),
            enabled = !globalViewModel.movingState,
            key = MainScreenTabKeys.History
        ),
        CarTabLayout.Tab(
            title = stringResource(R.string.settings_title),
            icon = painterResource(R.drawable.ic_carcompose_settings),
            enabled = !globalViewModel.movingState,
            key = MainScreenTabKeys.Settings
        )
    )

    CarTabLayout(
        headerTitle = stringResource(R.string.app_name),
        headerStartContent = { MainScreenAppIcon(debugOnClose) },
        tabs = tabsList,
        selectedKey = viewModel.selectedTabKey?:tabsList.first().key,
        onTabSelected = {
            if (it == MainScreenTabKeys.Settings)
                    backStack.add(SettingsScreenNavKey)
            else
                viewModel.setTabKey(it)
        }
    ) { key ->
        AnimatedVisibility(
            visible = key == MainScreenTabKeys.Dashboard,
            enter = fadeIn(),
            exit = fadeOut()
        ) {

        }
        AnimatedVisibility(
            visible = key == MainScreenTabKeys.History,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            TabHistory(
                globalViewModel,
                backStack
            )
        }
    }
}

@Composable
private fun CarComposeMainScreenSlim(
    globalViewModel: CarComposeGlobalViewModel,
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