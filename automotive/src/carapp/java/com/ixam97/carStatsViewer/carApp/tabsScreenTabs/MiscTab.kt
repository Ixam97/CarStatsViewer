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
import com.ixam97.carStatsViewer.BuildConfig
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.carApp.TabsScreen
import com.ixam97.carStatsViewer.carApp.RealTimeDataScreen
import com.ixam97.carStatsViewer.carApp.TripHistoryScreen
import com.ixam97.carStatsViewer.ui.activities.DebugActivity
import com.ixam97.carStatsViewer.ui.activities.HistoryActivity
import com.ixam97.carStatsViewer.ui.activities.MainActivity

import com.ixam97.carStatsViewer.compose.ComposeSettingsActivity
import com.ixam97.carStatsViewer.compose.ComposeTripDetailsActivity
import com.ixam97.carStatsViewer.liveDataApi.ConnectionStatus

@OptIn(ExperimentalCarApi::class)
internal fun TabsScreen.miscList() = ListTemplate.Builder().apply {

    val composeSettingsActivityIntent = Intent(carContext, ComposeSettingsActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    val mainActivityIntent = Intent(carContext, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    val historyActivityIntent = Intent(carContext, HistoryActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    val debugActivityIntent = Intent(carContext, DebugActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    val legacyViewsList = ItemList.Builder().apply {
        addItem(Row.Builder().apply{
            setTitle(carContext.getString(R.string.car_app_legacy_dashboard))
            setImage(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_car_app_dashboard)).build())
            addText(carContext.getString(R.string.car_app_legacy_dashboard_desc))
            setBrowsable(true)
            setOnClickListener(ParkedOnlyOnClickListener.create {
                carContext.startActivity(mainActivityIntent)
            })
        }.build())
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
    }.build()

    val debugList = ItemList.Builder().apply {
        addItem(Row.Builder().apply {
            setTitle("Compose Settings")
            setImage(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_car_app_debug)).build())
            setBrowsable(true)
            setOnClickListener(ParkedOnlyOnClickListener.create {
                carContext.startActivity(composeSettingsActivityIntent)
            })
        }.build())
        addItem(Row.Builder().apply{
            setTitle("Debug")
            setImage(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_car_app_debug)).build())
            setBrowsable(true)
            setOnClickListener {
                carContext.startActivity(debugActivityIntent)
            }
        }.build())
        addItem(Row.Builder().apply{
            setTitle("Open dashboard in map view")
            setImage(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_car_app_canvas)).build())
            setBrowsable(true)
            setOnClickListener {
                screenManager.push(RealTimeDataScreen(carContext, session))
            }
        }.build())

        addItem(Row.Builder().apply{
            setTitle("Throw Exception")
            setImage(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_kill))
                .setTint(colorError)
                .build()
            )
            setBrowsable(true)
            setOnClickListener {
                throw RuntimeException("Debug Exception")
            }
        }.build())
    }.build()

    addSectionedList(SectionedItemList.create(
        legacyViewsList,
        carContext.getString(R.string.car_app_menu_other_views)
    ))

    if (BuildConfig.FLAVOR_version == "dev") {
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
            }.build(),
            carContext.getString(R.string.car_app_status)
        ))
        addSectionedList(SectionedItemList.create(
            debugList,
            "Debug Views"
        ))
    }
}.build()