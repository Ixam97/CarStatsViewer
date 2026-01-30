package com.ixam97.carStatsViewer.carcompose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ixam97.carStatsViewer.carcompose.screen.main.MainScreenLandscape
import com.ixam97.carStatsViewer.carcompose.theme.ClubCarTheme
import de.ixam97.carcompose.theme.CarComposeTheme

class CarComposeActivity: ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        enableEdgeToEdge()
        setContent {

            val viewModel: CarComposeViewModel = viewModel()

            val theme = when (viewModel.carComposeState.uiTypeIndex) {
                0 -> ClubCarTheme
                1 -> CarComposeTheme.PolestarModern
                2 -> CarComposeTheme.PolestarClassic
                else -> CarComposeTheme.Generic
            }

            theme.CarTheme {
                MainScreenLandscape(
                    viewModel = viewModel,
                    onBackClick = { finish() }
                )
            }
        }

        super.onCreate(savedInstanceState)
    }
}