package com.ixam97.carStatsViewer.compose.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.MaterialTheme.typography
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.compose.SettingsViewModel
import com.ixam97.carStatsViewer.compose.components.CarIconButton
import com.ixam97.carStatsViewer.compose.components.SideTab
import com.ixam97.carStatsViewer.compose.components.SideTabLayout
import com.ixam97.carStatsViewer.compose.screens.settingsScreens.AboutScreen
import com.ixam97.carStatsViewer.compose.screens.settingsScreens.AppearanceScreen
import com.ixam97.carStatsViewer.compose.screens.settingsScreens.ChangelogScreen
import com.ixam97.carStatsViewer.compose.screens.settingsScreens.GeneralSettingsScreen
import com.ixam97.carStatsViewer.compose.screens.settingsScreens.WebViewScreen
import com.ixam97.carStatsViewer.compose.theme.CarTheme

object SettingsScreens {
    const val GENERAL = "General"
    const val APPEARANCE = "Appearance"
    const val ABOUT = "About"
    const val ABOUT_CHANGELOG = "About_Changelog"
    const val WEBVIEW = "WebView"
}

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {

    val settingsState = viewModel.settingsStateFlow.collectAsState()

    val tabsList = listOf(
        SideTab(
            tabTitle = "General",
            route = SettingsScreens.GENERAL,
            type = SideTab.Type.Tab,
            content = { GeneralSettingsScreen(settingsState = settingsState.value, viewModel = viewModel) }
        ),
        SideTab(
            tabTitle = "Appearance",
            route = SettingsScreens.APPEARANCE,
            type = SideTab.Type.Tab,
            content = { AppearanceScreen(viewModel = viewModel) }
        ),
        SideTab(
            tabTitle = "About Car Stats Viewer",
            route = SettingsScreens.ABOUT,
            type = SideTab.Type.Tab,
            content = { navController -> AboutScreen(navController = navController) }
        ),
        SideTab(
            tabTitle = "Changelog",
            route = SettingsScreens.ABOUT_CHANGELOG,
            type = SideTab.Type.Detail,
            content = { navController -> ChangelogScreen(navController = navController) }
        ),
        SideTab(
            tabTitle = "WebView Test",
            route = SettingsScreens.WEBVIEW,
            type = SideTab.Type.Tab,
            content = { WebViewScreen() }
    )
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            // .padding(10.dp)
            // .clip(RoundedCornerShape(25.dp))
            .background(MaterialTheme.colors.background)
    ){
        SideTabLayout(
            tabs = tabsList,
            topLevelTitle = stringResource(id = R.string.settings_title),
            topLevelBackAction = {viewModel.finishActivity()}
        ) //, tabsColumnBackground = Color.Black)
    }

}

