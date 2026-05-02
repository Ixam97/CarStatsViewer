package com.ixam97.carStatsViewer.carCompose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.rememberNavBackStack
import com.ixam97.carStatsViewer.carCompose.screens.main.MainScreenNavKey
import com.ixam97.carStatsViewer.carCompose.screens.settings.SettingsApisNavKey
import com.ixam97.carStatsViewer.carCompose.screens.settings.SettingsScreenNavKey
import com.ixam97.carStatsViewer.carCompose.screens.tripHistory.TripHistoryScreenNavKey
import de.ixam97.carcompose.theme.CarTheme
import de.ixam97.carcompose.theme.GenericCarThemeConfig
import de.ixam97.carcompose.theme.themes.PolestarClassicThemeConfig
import de.ixam97.carcompose.theme.themes.PolestarModernThemeConfig
import de.ixam97.carcompose.theme.themes.VolvoCarUxThemeConfig

class CarComposeActivity: ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        val initialNavKey = intent.getStringExtra("NavKey").let { key ->
            when (key) {
                SettingsScreenNavKey.toContentKey() -> SettingsScreenNavKey
                TripHistoryScreenNavKey.toContentKey() -> TripHistoryScreenNavKey
                SettingsApisNavKey.toContentKey() -> SettingsApisNavKey
                else -> MainScreenNavKey()
            }
        }

        enableEdgeToEdge()
        setContent {


            val viewModel: CarComposeGlobalViewModel = viewModel()
            val backStack = rememberNavBackStack(initialNavKey)

            val globalState by viewModel.globalState.collectAsState()

            val carThemeConfig = when (globalState.uiType) {
                // UiType.Club -> ClubThemeConfig
                UiType.Modern -> PolestarModernThemeConfig
                UiType.Classic -> PolestarClassicThemeConfig
                UiType.Volvo -> VolvoCarUxThemeConfig
                else -> GenericCarThemeConfig
            }

            viewModel.setUiSupportsBrightMode(carThemeConfig.carBrightColors != null)

            CarTheme(
                carThemeConfig = carThemeConfig,
                darkTheme = when(globalState.uiBrightnessMode) {
                    UiBrightnessMode.Dark -> true
                    UiBrightnessMode.Bright -> false
                    UiBrightnessMode.Auto -> isSystemInDarkTheme()
                }
            ) {
                CarComposeNavigationRoot(
                    globalViewModel =  viewModel,
                    backStack = backStack,
                    onBack = {
                        if (backStack.size > 1) backStack.removeLastOrNull()
                        else finish()
                    },
                    debugOnClose = { finish() }
                )
                // Polestar4MainScreen(
                //     viewModel = viewModel,
                //     onBackClick = { finish() }
                // )
                // MainScreenLandscape(
                //     viewModel = viewModel,
                //     onBackClick = { finish() }
                // )

                // if (Build.MODEL == "PS4" || Build.DEVICE == "lemon_x86_64")
                //     Polestar4MainScreen(
                //         viewModel = viewModel,
                //         onBackClick = { finish() }
                //     )
                // else
                //     MainScreenLandscape(
                //         viewModel = viewModel,
                //         onBackClick = { finish() }
                //     )
            }
        }

        super.onCreate(savedInstanceState)
    }
}