package com.ixam97.carStatsViewer.carApp

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.Header
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import com.ixam97.carStatsViewer.database.tripData.ChargingSession
import com.ixam97.carStatsViewer.utils.StringFormatters
import java.util.Date

class ChargingSessionDetailsScreen(
    carContext: CarContext,
    private val chargingSession: ChargingSession,
    private val chargingLocation: String?
): Screen(carContext), DefaultLifecycleObserver {

    init {
        lifecycle.addObserver(this)
    }

    override fun onGetTemplate(): Template {
        return chargingSessionDetailsTemplate()
    }

    private fun chargingSessionDetailsTemplate() = PaneTemplate.Builder(
        Pane.Builder().apply {
            addRow(Row.Builder().apply {
                setTitle(StringFormatters.getDateString(Date(chargingSession.start_epoch_time)))
            }.build())
        }.build()
    ).apply {
        setHeader(Header.Builder().apply {
            setTitle("Charging Session")
            setStartHeaderAction(Action.BACK)
        }.build())
    }.build()
}