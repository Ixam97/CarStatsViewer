package com.ixam97.carStatsViewer.carApp.tabsScreenTabs

import android.content.Intent
import androidx.annotation.OptIn
import androidx.car.app.annotations.ExperimentalCarApi
import androidx.car.app.model.CarIcon
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.SectionedItemList
import androidx.car.app.model.Toggle
import androidx.core.graphics.drawable.IconCompat
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.carApp.TabsScreen
import com.ixam97.carStatsViewer.ui.activities.SettingsActivity

@OptIn(ExperimentalCarApi::class)
internal fun TabsScreen.settingsList() = ListTemplate.Builder().apply {

    val settingsActivityIntent = Intent(carContext, SettingsActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    val quickSettingsItemList = ItemList.Builder().apply {
        addItem(Row.Builder().apply {
            setTitle(carContext.getString(R.string.car_app_show_real_time_data_title))
            addText(carContext.getString(R.string.car_app_show_real_time_data_hint))
            setToggle(Toggle.Builder {
                appPreferences.carAppRealTimeData = it
                invalidateTabView()
            }.setChecked(appPreferences.carAppRealTimeData).build())
        }.build())
    }.build()

    val advancedSettingsItemList = ItemList.Builder().apply {
        addItem(Row.Builder().apply{
            setTitle(carContext.getString(R.string.car_app_advanced_settings))
            setImage(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_car_app_settings)).build())
            setBrowsable(true)
            setOnClickListener {
                carContext.startActivity(settingsActivityIntent)
            }
        }.build())
    }.build()

    addSectionedList(SectionedItemList.create(
        quickSettingsItemList,
        carContext.getString(R.string.car_app_quick_settings)
    ))
    addSectionedList(SectionedItemList.create(
        advancedSettingsItemList,
        carContext.getString(R.string.car_app_advanced_settings)
    ))

}.build()