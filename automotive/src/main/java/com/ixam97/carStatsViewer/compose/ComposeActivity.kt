package com.ixam97.carStatsViewer.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.compose.screens.HelloWorldScreen
import com.ixam97.carStatsViewer.compose.screens.PolestarTestScreen
import com.ixam97.carStatsViewer.compose.theme.ComposeTestTheme
import com.ixam97.carStatsViewer.compose.theme.PolestarTheme

class ComposeActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)

        setContent {
            val composeViewModel: ComposeViewModel = viewModel()
            composeViewModel.finishActivityLiveData.observe(this) {
                if (it.consume() == true) finish()
            }

            val state = composeViewModel.composeActivityState.collectAsState()

            if (composeViewModel.vehicleBrand == "Polestar" && state.value.screenIndex == 1)
                PolestarTheme {
                    PolestarTestScreen(viewModel = composeViewModel)
                }
            else
                ComposeTestTheme(composeViewModel.vehicleBrand) {
                    HelloWorldScreen(viewModel = composeViewModel)
                }
        }
    }
}