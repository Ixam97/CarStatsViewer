package com.ixam97.carStatsViewer.carApp

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.SectionedItemList
import androidx.car.app.model.Template
import androidx.car.app.model.Toggle
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.lifecycleScope
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TripDataSettingsScreen(carContext: CarContext) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        return tripDataSettingsList()
    }

    private fun tripDataSettingsList() = ListTemplate.Builder().apply {
        setTitle(carContext.getString(R.string.car_app_strip_selection))
        setHeaderAction(Action.BACK)
        if (CarStatsViewer.appPreferences.mainViewTrip == 0) {
            setActionStrip(ActionStrip.Builder().apply {
                addAction(Action.Builder().apply {
                    setTitle(carContext.getString(R.string.dialog_reset_confirm))
                    setOnClickListener {
                        screenManager.pushForResult(ConfirmResetScreen(carContext)) {
                            if (it == true) {
                                setResult(true)
                                screenManager.pop()
                            }
                        }
                    }
                }.build())
            }.build())
        }
        addSectionedList(SectionedItemList.create(ItemList.Builder().apply {
            setSelectedIndex(CarStatsViewer.appPreferences.mainViewTrip)
            addItem(tripTypeRow(0))
            addItem(tripTypeRow(1))
            addItem(tripTypeRow(2))
            addItem(tripTypeRow(3))
            setOnSelectedListener{ changeSelectedTrip(it) }
        }.build(), carContext.getString(R.string.settings_select_trip)))
    }.build()

    private fun changeSelectedTrip(index: Int) {
        lifecycleScope.launch {
            delay(100)
            setResult(index)
            screenManager.pop()
        }

        // invalidate()
    }
    private fun tripTypeRow(tripType: Int) = Row.Builder().apply {
        when (tripType) {
            0 -> {
                setTitle(carContext.getString(R.string.CurrentTripData))
                setImage(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_hand)).build())
            }
            1 -> {
                setTitle(carContext.getString(R.string.SinceChargeData))
                setImage(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_charger)).build())
            }
            2 -> {
                setTitle(carContext.getString(R.string.AutoTripData))
                setImage(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_day)).build())
            }
            3 -> {
                setTitle(carContext.getString(R.string.CurrentMonthData))
                setImage(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_month)).build())
            }
            else -> throw Exception("Unknown Trip Type!")
        }
    }.build()
}