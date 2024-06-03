package com.ixam97.carStatsViewer.carApp

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.LongMessageTemplate
import androidx.car.app.model.ParkedOnlyOnClickListener
import androidx.car.app.model.Template
import com.ixam97.carStatsViewer.R

class DevNoticeScreen(carContext: CarContext): Screen(carContext) {

    override fun onGetTemplate(): Template {
        return LongMessageTemplate.Builder(carContext.getString(R.string.car_app_dev_notice_message)).apply {
            setTitle(carContext.getString(R.string.car_app_dev_notice_title))
            setHeaderAction(Action.APP_ICON)
            addAction(Action.Builder().apply {
                setTitle(carContext.getString(R.string.car_app_dev_notice_confirm))
                setFlags(Action.FLAG_PRIMARY)
                setOnClickListener(ParkedOnlyOnClickListener.create {
                    screenManager.pop()
                })
            }.build())
        }.build()
    }
}