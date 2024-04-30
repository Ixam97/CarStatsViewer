package com.ixam97.carStatsViewer.carApp

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.LongMessageTemplate
import androidx.car.app.model.ParkedOnlyOnClickListener
import androidx.car.app.model.Template
import com.ixam97.carStatsViewer.R

class PermissionScreen(
    carContext: CarContext,
    val session: CarStatsViewerSession
): Screen(carContext) {
    override fun onGetTemplate(): Template {
        return LongMessageTemplate.Builder(carContext.getString(R.string.car_app_permissions_message)).apply {
            setTitle(carContext.getString(R.string.car_app_permissions_title))
            setHeaderAction(Action.APP_ICON)
            addAction(Action.Builder().apply {
                setTitle(carContext.getString(R.string.car_app_permissions_deny))
                setOnClickListener(ParkedOnlyOnClickListener.create {
                    carContext.finishCarApp()
                })
            }.build())
            addAction(Action.Builder().apply {
                setTitle(carContext.getString(R.string.car_app_permissions_grant))
                setFlags(Action.FLAG_PRIMARY)
                setOnClickListener(ParkedOnlyOnClickListener.create {
                    requestPermissions()
                })
            }.build())
        }.build()
    }

    private fun requestPermissions() {
        carContext.requestPermissions(session.permissions) {granted,_ ->
            if (granted.containsAll(session.permissions)) {
                session.startService()
                screenManager.pop()
            }
        }
    }
}