package com.ixam97.carStatsViewer.carcompose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.rememberNavBackStack
import com.ixam97.carStatsViewer.carcompose.screen.MainScreenNavKey
import com.ixam97.carStatsViewer.carcompose.theme.ClubCarTheme
import de.ixam97.carcompose.theme.CarComposeTheme

class CarComposeActivity: ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        enableEdgeToEdge()
        setContent {

            val viewModel: CarComposeViewModel = viewModel()
            val backStack = rememberNavBackStack(MainScreenNavKey)

            val theme = when (viewModel.carComposeState.uiTypeIndex) {
                0 -> ClubCarTheme
                1 -> CarComposeTheme.PolestarModern
                2 -> CarComposeTheme.PolestarClassic
                else -> CarComposeTheme.Generic
            }

            theme.CarTheme {
                CarComposeNavigationRoot(
                    globalViewModel =  viewModel,
                    backStack = backStack,
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