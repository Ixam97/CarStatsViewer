package com.ixam97.carStatsViewer.compose

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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

        if (!intent.hasExtra("SessionId")) {
            finish()
        } else {
            val sessionId = intent.getLongExtra("SessionId", 0)
            setContent {
                val viewModel: TripDetailsViewModel = viewModel()

                // val tripDetailsState = viewModel.tripDetailsState

                val brand = when (CarStatsViewer.appPreferences.colorTheme) {
                    0 -> Build.BRAND
                    2 -> "Orange"
                    else -> null
                }

                CarTheme(brand) {
                    TripDetailsPortraitScreen(
                        viewModel = viewModel,
                        sessionId = sessionId
                    )
                    /*
                    if (tripDetailsState.drivingSession != null) {
                        TripDetailsPortraitScreen(
                            viewModel = viewModel
                        )
                    } else {
                        viewModel.loadDrivingSession(sessionId)
                        Box(
                            modifier = Modifier
                                .fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                     */
                }
            }
        }
    }
}