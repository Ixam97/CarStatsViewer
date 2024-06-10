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
import com.ixam97.carStatsViewer.compose.components.SideTabLayout
import com.ixam97.carStatsViewer.compose.screens.settingsScreens.AboutTab
import com.ixam97.carStatsViewer.compose.screens.settingsScreens.AppearanceTab
import com.ixam97.carStatsViewer.compose.screens.settingsScreens.GeneralSettingsTab
import com.ixam97.carStatsViewer.compose.theme.CarTheme

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    
    val settingsState = viewModel.settingsStateFlow.collectAsState()
    
    val tabsList = listOf(
        GeneralSettingsTab(settingsState = settingsState.value, viewModel = viewModel),
        AppearanceTab(viewModel),
        AboutTab()
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            // .padding(10.dp)
            // .clip(RoundedCornerShape(25.dp))
            .background(MaterialTheme.colors.background)
    ){
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = MaterialTheme.colors.background),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CarIconButton(
                onCLick = { viewModel.finishActivity() },
                iconResId = R.drawable.ic_arrow_back,
                tint = MaterialTheme.colors.secondary
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = stringResource(id = R.string.settings_title), style = typography.h1)
        }
        Box(
            modifier = Modifier
                .height(3.dp)
                .fillMaxWidth()
                .background(
                    brush = CarTheme.headerLineBrush
                ),
        )
        SideTabLayout(tabs = tabsList) //, tabsColumnBackground = Color.Black)
    }

}