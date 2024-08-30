package com.ixam97.carStatsViewer.carApp

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.Header
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.carApp.utils.carIconFromRes

class ConfirmDeleteTripScreen(carContext: CarContext):
    Screen(carContext), DefaultLifecycleObserver
{
    init {
        lifecycle.addObserver(this)
    }

    override fun onGetTemplate(): Template {
        return confirmDeleteTripTemplate()
    }

    private fun confirmDeleteTripTemplate() = MessageTemplate.Builder(
        carContext.getString(R.string.history_dialog_delete_message)
    ).apply {
        setHeader(Header.Builder().apply {
            setTitle(carContext.getString(R.string.history_dialog_delete_title))
        }.build())
        setIcon(carContext.carIconFromRes(R.drawable.ic_delete))
        addAction(Action.Builder().apply {
            setTitle(carContext.getString(R.string.history_dialog_delete_confirm))
            setFlags(Action.FLAG_PRIMARY)
            setOnClickListener {
                setResult(true)
                screenManager.pop()
            }
        }.build())
        addAction(Action.Builder().apply {
            setTitle(carContext.getString(R.string.dialog_reset_cancel))
            setOnClickListener {
                setResult(false)
                screenManager.pop()
            }
        }.build())
    }.build()
}