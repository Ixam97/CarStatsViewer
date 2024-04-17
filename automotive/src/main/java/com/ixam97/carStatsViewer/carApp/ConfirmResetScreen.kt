package com.ixam97.carStatsViewer.carApp

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.database.tripData.TripType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ConfirmResetScreen(carContext: CarContext) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        return dialogTemplate()
    }

    private fun dialogTemplate() = MessageTemplate.Builder(
        carContext.getString(R.string.dialog_reset_message)
    ).apply {
        // addAction(Action.BACK)
        setHeaderAction(Action.BACK)
        setTitle(carContext.getString(R.string.dialog_reset_title))
        setIcon(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_reset)).build())
        addAction(Action.Builder().apply {
            setTitle(carContext.getString(R.string.dialog_reset_confirm))
            setFlags(Action.FLAG_PRIMARY)
            setOnClickListener {
                CoroutineScope(Dispatchers.IO).launch {
                    CarStatsViewer.dataProcessor.resetTrip(
                        TripType.MANUAL,
                        CarStatsViewer.dataProcessor.realTimeData.drivingState
                    )
                }
                screenManager.popToRoot()
            }
        }.build())
        addAction(Action.Builder().apply {
            setTitle(carContext.getString(R.string.dialog_reset_cancel))
            setOnClickListener {
                screenManager.pop()
            }
        }.build())
    }.build()
}