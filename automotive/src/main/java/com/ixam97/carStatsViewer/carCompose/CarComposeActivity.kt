package com.ixam97.carStatsViewer.carCompose

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.rememberNavBackStack
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.carCompose.screens.main.MainScreenNavKey
import com.ixam97.carStatsViewer.carCompose.screens.settings.SettingsApisNavKey
import com.ixam97.carStatsViewer.carCompose.screens.settings.SettingsScreenNavKey
import com.ixam97.carStatsViewer.carCompose.screens.tripHistory.TripHistoryScreenNavKey
import com.ixam97.carStatsViewer.carCompose.theme.ClubThemeConfig
import de.ixam97.carcompose.theme.CarTheme
import de.ixam97.carcompose.theme.GenericCarThemeConfig
import de.ixam97.carcompose.theme.GenericCarTypography
import de.ixam97.carcompose.theme.themes.PolestarCarTypography
import de.ixam97.carcompose.theme.themes.PolestarClassicThemeConfig
import de.ixam97.carcompose.theme.themes.PolestarModernThemeConfig
import de.ixam97.carcompose.theme.themes.VolvoCarUxThemeConfig
import de.ixam97.carcompose.theme.themes.VolvoTypograph
import de.ixam97.carcompose.utils.calculateWindowInsets

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


            window.decorView.systemUiVisibility =
                if (globalState.overrideWindowInsets) View.SYSTEM_UI_FLAG_FULLSCREEN
                else View.SYSTEM_UI_FLAG_VISIBLE

            val carThemeConfig = when (globalState.uiType) {
                UiType.Club -> ClubThemeConfig.copy(
                    carTypography = when(globalState.vehicleModel) {
                        VehicleModel.Polestar2, VehicleModel.Polestar3, VehicleModel.Polestar4 -> PolestarCarTypography
                        VehicleModel.Volvo -> VolvoTypograph
                        else -> GenericCarTypography
                    }
                )
                UiType.Modern -> PolestarModernThemeConfig
                UiType.Classic -> PolestarClassicThemeConfig
                UiType.Volvo -> VolvoCarUxThemeConfig
                else -> GenericCarThemeConfig
            }

            viewModel.setUiSupportsBrightMode(carThemeConfig.carBrightColors != null)

            val windowInsets =
                if (globalState.overrideWindowInsets) PaddingValues(top = 64.dp, bottom = 138.dp)
                else calculateWindowInsets()

            Box(modifier = Modifier.fillMaxSize()) {
                CarTheme(
                    carThemeConfig = carThemeConfig,
                    darkTheme = when (globalState.uiBrightnessMode) {
                        UiBrightnessMode.Dark -> true
                        UiBrightnessMode.Bright -> false
                        UiBrightnessMode.Auto -> isSystemInDarkTheme()
                    },
                    windowInsets = windowInsets
                ) {
                    CarComposeNavigationRoot(
                        globalViewModel = viewModel,
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
                if (globalState.overrideWindowInsets) {
                    Image(
                        painter = painterResource(R.drawable.img_p4_top_bar),
                        modifier = Modifier
                            .height(64.dp)
                            .fillMaxWidth()
                            .background(Color.Black)
                            .align(Alignment.TopStart),
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds
                    )
                    Image(
                        painter = painterResource(R.drawable.img_p4_bottom_bar),
                        modifier = Modifier
                            .height(138.dp)
                            .fillMaxWidth()
                            .background(Color.Black)
                            .align(Alignment.BottomStart),
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds
                    )
                }
            }
        }

        super.onCreate(savedInstanceState)
    }
}