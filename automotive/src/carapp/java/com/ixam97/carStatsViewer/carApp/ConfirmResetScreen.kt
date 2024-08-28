package com.ixam97.carStatsViewer.carApp

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.CarIcon
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat
import com.ixam97.carStatsViewer.R

class ConfirmResetScreen(carContext: CarContext) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        return dialogTemplate()
    }

    private fun dialogTemplate() = MessageTemplate.Builder(
        carContext.getString(R.string.dialog_reset_message)
    ).apply {
        setHeaderAction(Action.BACK)
        setTitle(carContext.getString(R.string.dialog_reset_title))
        setIcon(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_reset)).build())
        addAction(Action.Builder().apply {
            setTitle(carContext.getString(R.string.dialog_reset_confirm))
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