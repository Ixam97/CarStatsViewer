package com.ixam97.carStatsViewer.carApp

import android.car.Car
import androidx.annotation.OptIn
import androidx.car.app.annotations.ExperimentalCarApi
import androidx.car.app.model.CarIcon
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.SectionedItemList
import androidx.core.graphics.drawable.IconCompat
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R

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
                val apiIconColor = when (status) {
                    0 -> colorDisconnected
                    1 -> colorConnected
                    2 -> colorLimited
                    else -> colorError
                }
                addItem(Row.Builder().apply {
                    setTitle(apiName)
                    addText(when (status) {
                        0 -> "Disabled"
                        1 -> "Connected"
                        2 -> "Limited"
                        else -> "Error"
                    })
                    setImage(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_connected)).setTint(apiIconColor).build())
                }.build())
            }
        }.build(),
        "APIs"
    ))
}.build()