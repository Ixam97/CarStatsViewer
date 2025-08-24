package com.ixam97.carStatsViewer.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.compose.screens.SettingsScreen
import com.ixam97.carStatsViewer.compose.theme.CarTheme

class ComposeSettingsActivity: ComponentActivity() {

    private var reinitSates: () -> Unit = {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)

        val targetScreen: String? = if (intent.hasExtra("TargetRoute")) {
            intent.getStringExtra("TargetRoute")
        } else {
            null
        }

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val themeSetting = settingsViewModel.themeSettingStateFLow.collectAsState()

            reinitSates = { settingsViewModel.initStates() }

            settingsViewModel.finishActivityLiveData.observe(this) {
                if (it.consume() == true) finish()
            }

            val brand = brandSelector(themeSetting.value)

            CarTheme(brand) {
                SettingsScreen(viewModel = settingsViewModel, targetRoute = targetScreen)
            }
        }
    }

    override fun onResume() {
        reinitSates.invoke()
        super.onResume()
    }
}