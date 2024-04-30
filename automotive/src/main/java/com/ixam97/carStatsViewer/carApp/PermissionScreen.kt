package com.ixam97.carStatsViewer.carApp

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.Header
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.ParkedOnlyOnClickListener
import androidx.car.app.model.Template

class PermissionScreen(
    carContext: CarContext,
    val session: CarStatsViewerSession
): Screen(carContext) {
    override fun onGetTemplate(): Template {
        val message = "PERMISSIONS_MESSAGE"
        return MessageTemplate.Builder(message).apply {
            setHeader(Header.Builder().apply {
                setTitle("REQUEST_PERMISSIONS_TITLE")
                setStartHeaderAction(Action.APP_ICON)
            }.build())
            addAction(Action.Builder().apply {
                setTitle("PERMISSIONS_CANCEL")
                setOnClickListener {
                    carContext.finishCarApp()
                }
            }.build())
            addAction(Action.Builder().apply {
                setTitle("PERMISSIONS_GRANT")
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