package com.ixam97.carStatsViewer.carApp.tabsScreenTabs

import android.content.Intent
import androidx.annotation.OptIn
import androidx.car.app.annotations.ExperimentalCarApi
import androidx.car.app.model.CarIcon
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.ParkedOnlyOnClickListener
import androidx.car.app.model.Row
import androidx.car.app.model.SectionedItemList
import androidx.core.graphics.drawable.IconCompat
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.carApp.TabsScreen
import com.ixam97.carStatsViewer.carApp.TripHistoryScreen
import com.ixam97.carStatsViewer.liveDataApi.ConnectionStatus
import com.ixam97.carStatsViewer.ui.activities.SettingsApisActivity

@OptIn(ExperimentalCarApi::class)
internal fun TabsScreen.apiStatusList() = ListTemplate.Builder().apply {

    val settingsApisActivityIntent = Intent(carContext, SettingsApisActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    /* addSectionedList(SectionedItemList.create(
        ItemList.Builder().apply {
            addItem(Row.Builder().apply {
                setTitle("Not yet supported!")
                setImage(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_construction)).build())
            }.build())
        }.build(),
        "Real time data"
    )) */
    addSectionedList(SectionedItemList.create(
        ItemList.Builder().apply {
            addItem(Row.Builder().apply{
                setTitle(carContext.getString(R.string.history_title))
                addText(carContext.getString(R.string.car_app_legacy_history_desc))
                setImage(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_car_app_history)).build())
                setBrowsable(true)
                setOnClickListener(ParkedOnlyOnClickListener.create {
                    // carContext.startActivity(historyActivityIntent)
                    screenManager.push(TripHistoryScreen(carContext))
                })
            }.build())
        }.build(),
        carContext.getString(R.string.history_title)
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

            addItem(Row.Builder().apply {
                setTitle(carContext.getString(R.string.settings_apis_title))
                setImage(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_api)).build())
                setBrowsable(true)
                setOnClickListener(ParkedOnlyOnClickListener.create {
                    carContext.startActivity(settingsApisActivityIntent)
                })
            }.build())

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