package com.ixam97.carStatsViewer.carApp.tabsScreenTabs

import android.content.Intent
import androidx.annotation.OptIn
import androidx.car.app.annotations.ExperimentalCarApi
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.SectionedItemList
import androidx.car.app.model.Toggle
import androidx.core.graphics.drawable.IconCompat
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.carApp.TabsScreen
import com.ixam97.carStatsViewer.compose.ComposeSettingsActivity

@OptIn(ExperimentalCarApi::class)
internal fun TabsScreen.settingsList() = ListTemplate.Builder().apply {

    val composeSettingsActivityIntent = Intent(carContext, ComposeSettingsActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    val quickSettingsItemList = ItemList.Builder().apply {
        addItem(Row.Builder().apply {
            setTitle(carContext.getString(R.string.settings_use_location))
            addText("Track your location when recording a trip.") // + "\n \n" + carContext.getString(R.string.car_app_show_real_time_data_hint))
            setToggle(Toggle.Builder {
                appPreferences.useLocation = it
                invalidateTabView()
            }.setChecked(appPreferences.useLocation).build())
        }.build())
        addItem(Row.Builder().apply {
            setTitle(carContext.getString(R.string.car_app_show_real_time_data_title))
            addText(carContext.getString(R.string.car_app_show_real_time_data_subtitle)) // + "\n \n" + carContext.getString(R.string.car_app_show_real_time_data_hint))
            setToggle(Toggle.Builder {
                appPreferences.carAppRealTimeData = it
                invalidateTabView()
            }.setChecked(appPreferences.carAppRealTimeData).build())
        }.build())
        addItem(Row.Builder().apply {
            setTitle("Dev Notice")
            addText("Please provide feedback to the developer on what settings should also be available in the quick settings for easy access while driving.")
            setImage(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_car_app_feedback))
                .setTint(
                    CarColor.createCustom(
                        carContext.getColor(R.color.polestar_orange),
                        carContext.getColor(R.color.polestar_orange)
                    )
                )
                .build())
        }.build())
    }.build()

    val advancedSettingsItemList = ItemList.Builder().apply {
        addItem(Row.Builder().apply{
            setTitle(carContext.getString(R.string.car_app_advanced_settings))
            setImage(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_car_app_settings)).build())
            setBrowsable(true)
            setOnClickListener {
                carContext.startActivity(composeSettingsActivityIntent)
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