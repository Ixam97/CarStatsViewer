package com.ixam97.carStatsViewer.compose

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.compose.screens.SettingsScreen
import com.ixam97.carStatsViewer.compose.screens.TripDetailsPortraitScreen
import com.ixam97.carStatsViewer.compose.theme.CarTheme

class ComposeTripDetailsActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)

        setContent {

            CarTheme(Build.BRAND) {
                TripDetailsPortraitScreen(CarStatsViewer.dataProcessor.selectedSessionData)
            }
        }
    }
}