package com.ixam97.carStatsViewer.carApp

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.Header
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.model.Toggle
import androidx.lifecycle.DefaultLifecycleObserver
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R

class TripHistoryFilterScreen(carContext: CarContext): Screen(carContext), DefaultLifecycleObserver {

    private var showManualTrips: Boolean = CarStatsViewer.appPreferences.tripFilterManual
    private var showSinceChargeTrips: Boolean = CarStatsViewer.appPreferences.tripFilterCharge
    private var showAutomaticTrips: Boolean = CarStatsViewer.appPreferences.tripFilterAuto
    private var showMonthlyTrips: Boolean = CarStatsViewer.appPreferences.tripFilterMonth

    init {
        lifecycle.addObserver(this)
    }

    override fun onGetTemplate(): Template {
        return tripHistoryFilterTemplate()
    }

    private fun tripHistoryFilterTemplate() = PaneTemplate.Builder(Pane.Builder().apply {
        addRow(Row.Builder().apply {
            setToggle(Toggle.Builder {
                showManualTrips = it
            }.setChecked(showManualTrips).build())
            setTitle(carContext.getString(R.string.history_dialog_filters_manual))
        }.build())
        addRow(Row.Builder().apply {
            setToggle(Toggle.Builder {
                showSinceChargeTrips = it
            }.setChecked(showSinceChargeTrips).build())
            setTitle(carContext.getString(R.string.history_dialog_filters_charge))
        }.build())
        addRow(Row.Builder().apply {
            setToggle(Toggle.Builder {
                showAutomaticTrips = it
            }.setChecked(showAutomaticTrips).build())
            setTitle(carContext.getString(R.string.history_dialog_filters_auto))
        }.build())
        addRow(Row.Builder().apply {
            setToggle(Toggle.Builder {
                showMonthlyTrips = it
            }.setChecked(showMonthlyTrips).build())
            setTitle(carContext.getString(R.string.history_dialog_filters_month))
        }.build())
        addAction(Action.Builder().apply {
            setTitle(carContext.getString(R.string.dialog_apply))
            setFlags(Action.FLAG_PRIMARY)
            setOnClickListener {
                CarStatsViewer.appPreferences.tripFilterManual = showManualTrips
                CarStatsViewer.appPreferences.tripFilterCharge = showSinceChargeTrips
                CarStatsViewer.appPreferences.tripFilterAuto = showAutomaticTrips
                CarStatsViewer.appPreferences.tripFilterMonth = showMonthlyTrips
                screenManager.pop()
            }
        }.build())
    }.build()).apply {
        setHeader(Header.Builder().apply {
            setTitle(carContext.getString(R.string.history_dialog_filters_title))
            setStartHeaderAction(Action.BACK)
        }.build())
    }.build()
}