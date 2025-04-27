package com.ixam97.carStatsViewer.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
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

                val brand = brandSelector(CarStatsViewer.appPreferences.colorTheme)

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