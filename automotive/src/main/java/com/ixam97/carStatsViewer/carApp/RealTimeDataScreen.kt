package com.ixam97.carStatsViewer.carApp

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Template
import com.ixam97.carStatsViewer.carApp.renderer.CarDataSurfaceCallback

class RealTimeDataScreen(
    carContext: CarContext,
    val session: CarStatsViewerSession
): Screen(carContext) {
    override fun onGetTemplate(): Template {
        return RealTimeDateTemplate(
            carContext,
            session,
            backButton = true
        ) {
            invalidate()
        }
    }
}