package com.ixam97.carStatsViewer.carApp

import android.content.Intent
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.CarIcon
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.ui.activities.HistoryActivity
import com.ixam97.carStatsViewer.ui.activities.MainActivity
import com.ixam97.carStatsViewer.ui.activities.SettingsActivity

internal fun CarStatsViewerScreen.MenuList() = ListTemplate.Builder().apply {

    val settingsActivityIntent = Intent(carContext, SettingsActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
     val mainActivityIntent = Intent(carContext, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
     val historyActivityIntent = Intent(carContext, HistoryActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    setSingleList(ItemList.Builder().apply {
        addItem(Row.Builder().apply{
            setTitle(carContext.getString(R.string.car_app_dashboard))
            setImage(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_diagram)).build())
            setBrowsable(true)
            setOnClickListener {
                carContext.startActivity(mainActivityIntent)
            }
        }.build())
        addItem(Row.Builder().apply{
            setTitle(carContext.getString(R.string.history_title))
            setImage(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_history)).build())
            setBrowsable(true)
            setOnClickListener {
                carContext.startActivity(historyActivityIntent)
            }
        }.build())
        addItem(Row.Builder().apply{
            setTitle(carContext.getString(R.string.settings_title))
            setImage(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_settings)).build())
            setBrowsable(true)
            setOnClickListener {
                carContext.startActivity(settingsActivityIntent)
            }
        }.build())
    }.build())
}.build()