package com.ixam97.carStatsViewer.carApp

import androidx.annotation.OptIn
import androidx.car.app.annotations.ExperimentalCarApi
import androidx.car.app.model.CarIcon
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.SectionedItemList
import androidx.core.graphics.drawable.IconCompat
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.liveDataApi.ConnectionStatus

@OptIn(ExperimentalCarApi::class)
internal fun CarStatsViewerScreen.CarStatsList() = ListTemplate.Builder().apply {
    addSectionedList(SectionedItemList.create(
        ItemList.Builder().apply {
            addItem(Row.Builder().apply {
                setTitle("Not yet supported!")
                setImage(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_construction)).build())
            }.build())
        }.build(),
        "Real time data"
    ))
    addSectionedList(SectionedItemList.create(
        ItemList.Builder().apply {

            apiState.forEach { (apiName, status) ->
                val apiIconColor = when (ConnectionStatus.fromInt(status)) {
                    ConnectionStatus.UNUSED -> colorDisconnected
                    ConnectionStatus.CONNECTED -> colorConnected
                    ConnectionStatus.LIMITED -> colorLimited
                    else -> colorError
                }
                addItem(Row.Builder().apply {
                    setTitle(apiName)
                    addText(when (ConnectionStatus.fromInt(status)) {
                        ConnectionStatus.UNUSED -> "Disabled"
                        ConnectionStatus.CONNECTED -> "Connected"
                        ConnectionStatus.LIMITED -> "Limited"
                        else -> "Error"
                    })
                    setImage(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_connected)).setTint(apiIconColor).build())
                }.build())
            }

            if (apiState.isEmpty()) {
                addItem(Row.Builder().apply {
                    setTitle("No API available!")
                    setImage(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_connected)).setTint(colorDisconnected).build())
                }.build())
            }
        }.build(),
        "APIs"
    ))
}.build()