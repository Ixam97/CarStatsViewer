package com.ixam97.carStatsViewer.carApp

import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.car.app.CarToast
import androidx.car.app.annotations.ExperimentalCarApi
import androidx.car.app.model.Action
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.SectionedItemList
import androidx.core.graphics.drawable.IconCompat
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.database.tripData.DrivingSession
import com.ixam97.carStatsViewer.database.tripData.TripType
import com.ixam97.carStatsViewer.utils.InAppLogger
import com.ixam97.carStatsViewer.utils.StringFormatters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalCarApi::class)
internal fun CarStatsViewerScreen.TripDataList(session: DrivingSession?) = ListTemplate.Builder().apply {

    if (session == null) {
        setLoading(true)
    } else {
        val tripType = when (session?.session_type) {
            1 -> carContext.getString(R.string.CurrentTripData)
            2 -> carContext.getString(R.string.SinceChargeData)
            3 -> carContext.getString(R.string.AutoTripData)
            4 -> carContext.getString(R.string.CurrentMonthData)
            else -> "unknown"
        }
        setSingleList(ItemList.Builder().apply {
            session?.let {

                addItem(createDataRow(
                    StringFormatters.getTraveledDistanceString(it.driven_distance.toFloat()),
                    carContext.getString(R.string.summary_traveled_distance),
                    R.drawable.ic_distance_large
                ))
                addItem(createDataRow(
                    StringFormatters.getEnergyString(it.used_energy.toFloat()),
                    carContext.getString(R.string.summary_used_energy),
                    R.drawable.ic_energy_large
                ))
                addItem(createDataRow(
                    StringFormatters.getAvgConsumptionString(it.used_energy.toFloat(), it.driven_distance.toFloat()),
                    carContext.getString(R.string.summary_average_consumption),
                    R.drawable.ic_diagram
                ))
                addItem(createDataRow(
                    StringFormatters.getAvgSpeedString(it.driven_distance.toFloat(), it.drive_time),
                    carContext.getString(R.string.summary_speed),
                    R.drawable.ic_speed_large
                ))
                addItem(createDataRow(
                    StringFormatters.getElapsedTimeString(it.drive_time, true),
                    carContext.getString(R.string.summary_travel_time),
                    R.drawable.ic_time_large
                ))

                var statusString = ""
                var index = 0
                var apiIconColor = colorDisconnected

                apiState.forEach { (apiName, status) ->
                    statusString += "$apiName: $status    "
                    if (index == CarStatsViewer.appPreferences.mainViewConnectionApi) {
                        apiIconColor = when (status) {
                            0 -> colorDisconnected
                            1 -> colorConnected
                            2 -> colorLimited
                            else -> colorError
                        }
                    }

                    index++
                }

                addItem(createDataRow(
                    statusString,
                    "Api Status",
                    R.drawable.ic_connected,
                    apiIconColor
                ))
            }
        }.build())

        addAction(Action.Builder().apply {
            val backgroundColor = carContext.getColor(R.color.default_button_color)
            setIcon(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_car_app_tune)).build())
            setBackgroundColor(CarColor.createCustom(backgroundColor, backgroundColor))
            setOnClickListener {
                screenManager.pushForResult(TripDataSettingsScreen(carContext)) {
                    Handler(Looper.getMainLooper()).post {
                        invalidate()
                    }
                }
            }
        }.build())

        // Adding second FAB, only working in Emulator right now!
        if (session?.session_type == 1) {
            addAction(Action.Builder().apply {
                val backgroundColor = carContext.getColor(R.color.default_button_color)
                setIcon(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_car_app_reset)).build())
                setBackgroundColor(CarColor.createCustom(backgroundColor, backgroundColor))
                setOnClickListener {
                    screenManager.pushForResult(ConfirmResetScreen(carContext)) {
                        Handler(Looper.getMainLooper()).post {
                            invalidate()
                        }
                    }
                }
            }.build())
        }
    }

}.build()

@OptIn(ExperimentalCarApi::class) private
fun CarStatsViewerScreen.createDataRow(value: String, name: String, iconResId: Int, iconColor: CarColor = CarColor.DEFAULT) = Row.Builder().apply {
    setTitle(value)
    setImage(CarIcon.Builder(IconCompat.createWithResource(carContext, iconResId)).setTint(iconColor).build())
    addText(name)
    InAppLogger.v("[AAOS] Data row created")
}.build()