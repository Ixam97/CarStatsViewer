package com.ixam97.carStatsViewer.compose

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.compose.screens.SettingsScreen
import com.ixam97.carStatsViewer.compose.theme.CarTheme
import com.ixam97.carStatsViewer.utils.InAppLogger

class ComposeSettingsActivity: ComponentActivity() {

    private var reinitSates: () -> Unit = {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val themeSetting = settingsViewModel.themeSettingStateFLow.collectAsState()

            reinitSates = { settingsViewModel.initStates() }

            settingsViewModel.finishActivityLiveData.observe(this) {
                if (it.consume() == true) finish()
            }

            InAppLogger.i("Device Info: Brand: ${Build.BRAND}, model: ${Build.MODEL}, device: ${Build.DEVICE}")

            val brand = when (themeSetting.value) {
                0 -> if (Build.MODEL == "Polestar 2") Build.MODEL else Build.BRAND
                2 -> "Orange"
                else -> null
            }

            CarTheme(brand) {
                SettingsScreen(viewModel = settingsViewModel)
            }
        }
    }

    override fun onResume() {
        reinitSates.invoke()
        super.onResume()
    }
}