package com.ixam97.carStatsViewer.carApp

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.annotation.OptIn
import androidx.car.app.CarContext
import androidx.car.app.ScreenManager
import androidx.car.app.annotations.ExperimentalCarApi
import androidx.car.app.model.Action
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.core.graphics.drawable.IconCompat
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.carApp.utils.Gauge
import com.ixam97.carStatsViewer.carApp.utils.asCarIcon
import com.ixam97.carStatsViewer.dataProcessor.RealTimeData
import com.ixam97.carStatsViewer.database.tripData.DrivingSession
import com.ixam97.carStatsViewer.database.tripData.TripType
import com.ixam97.carStatsViewer.liveDataApi.ConnectionStatus
import com.ixam97.carStatsViewer.utils.StringFormatters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TripDataTemplate(val carContext: CarContext) {

    private val screenManager = carContext.getCarService(ScreenManager::class.java)

    private val colorError = CarColor.createCustom(carContext.getColor(R.color.bad_red), carContext.getColor(R.color.bad_red))
    private val colorDisconnected = CarColor.createCustom(carContext.getColor(R.color.inactive_text_color), carContext.getColor(R.color.disabled_tint))
    private val colorConnected = CarColor.createCustom(carContext.getColor(R.color.connected_blue), carContext.getColor(R.color.connected_blue))
    private val colorLimited = CarColor.createCustom(carContext.getColor(R.color.limited_yellow), carContext.getColor(R.color.limited_yellow))

    private val gauge = Gauge(carContext)

    fun tripDataListTemplate(session: DrivingSession?) = ListTemplate.Builder().apply {

        if (session == null) {
            setLoading(true)
        } else {
            setSingleList(tripDataItemList(session))
            addAction(configAction())

            // Adding second FAB, only working on API Level 7!
            if (session.session_type == 1 && carContext.carAppApiLevel >= 7) {
                addAction(resetAction())
            }
        }
    }.build()

    fun tripDataPaneTemplate(session: DrivingSession?, realTimeData: RealTimeData?) = PaneTemplate.Builder(Pane.Builder().apply {
        if (session == null) {
            setLoading(true)
        } else {
            session.let {
                addRow(createDataRow(
                    StringFormatters.getTraveledDistanceString(it.driven_distance.toFloat()),
                    carContext.getString(R.string.summary_traveled_distance),
                    R.drawable.ic_distance_large
                ))
                addRow(createDataRow(
                    StringFormatters.getAvgConsumptionString(it.used_energy.toFloat(), it.driven_distance.toFloat()),
                    carContext.getString(R.string.summary_average_consumption),
                    R.drawable.ic_diagram
                ))
                addRow(createDataRow(
                    StringFormatters.getAvgSpeedString(it.driven_distance.toFloat(), it.drive_time),
                    carContext.getString(R.string.summary_speed),
                    R.drawable.ic_speed_large
                ))
                addRow(createDataRow(
                    StringFormatters.getElapsedTimeString(it.drive_time, true),
                    carContext.getString(R.string.summary_travel_time),
                    R.drawable.ic_time_large
                ))
                addRow(createDataRow(
                    StringFormatters.getEnergyString(it.used_energy.toFloat()),
                    carContext.getString(R.string.summary_used_energy),
                    R.drawable.ic_energy_large
                ))
            }
            addAction(configAction(showTitle = true))
            if (session.session_type == 1) {
                addAction(resetAction(showTitle = true))
            }
            // val selectedGaugeIndex = CarStatsViewer.appPreferences.carAppSelectedRealTimeData

            if (realTimeData != null) {

                val useLocation = CarStatsViewer.appPreferences.useLocation

                val gaugeBitmap = Bitmap.createBitmap(480, 480, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(gaugeBitmap)
                canvas.drawBitmap(gauge.drawPowerGauge(480, if (useLocation) 170 else 235, realTimeData.power?:0f), 0f, 0f ,null)
                canvas.drawBitmap(gauge.drawConsumptionGauge(480, if (useLocation) 170 else 235, realTimeData.instConsumption, realTimeData.speed), 0f, if (useLocation) 180f else 245f, null)
                if (useLocation) canvas.drawBitmap(gauge.draw(width = 480, height = 140, value = realTimeData.alt?:0f, valueString = realTimeData.alt?.toInt().toString(), unitString = "m", textOnly = true), 0f, 360f, null)

                setImage(gaugeBitmap.asCarIcon())
            }

            // if (realTimeData != null) when (selectedGaugeIndex) {
            //     1 -> setImage(.asCarIcon())
            //     2 -> setImage(.asCarIcon())
            //     else -> { /* Don't show an image */ }
            // }

        }
    }.build()).build()

    @OptIn(ExperimentalCarApi::class)
    private fun createDataRow(value: String, name: String? = null, iconResId: Int, iconColor: CarColor = CarColor.DEFAULT) = Row.Builder().apply {
        setTitle(value)
        setImage(CarIcon.Builder(IconCompat.createWithResource(carContext, iconResId)).setTint(iconColor).build())
        name?.let{addText(it)}
    }.build()

    private fun tripDataItemList(session: DrivingSession) = ItemList.Builder().apply {
        session.let {

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
            var apiIconColor = colorDisconnected

            CarStatsViewer.liveDataApis.forEachIndexed { apiIndex, liveDataApi ->
                if (liveDataApi.connectionStatus.status == 0) return@forEachIndexed
                val name = liveDataApi.apiIdentifier
                val status = liveDataApi.connectionStatus
                val statusName = when (status) {
                    ConnectionStatus.UNUSED -> "Disabled"
                    ConnectionStatus.CONNECTED -> "Connected"
                    ConnectionStatus.LIMITED -> "Limited"
                    else -> "Error"
                }
                if (statusString.isNotEmpty()) statusString += "\n"
                statusString += "$name: $statusName"
                if (apiIndex == CarStatsViewer.appPreferences.mainViewConnectionApi) {
                    apiIconColor = when (status) {
                        ConnectionStatus.UNUSED -> colorDisconnected
                        ConnectionStatus.CONNECTED -> colorConnected
                        ConnectionStatus.LIMITED -> colorLimited
                        else -> colorError
                    }
                }
            }

            if (statusString.isEmpty() || statusString == "") statusString = "No API available"

            addItem(createDataRow(
                statusString,
                null,
                R.drawable.ic_connected,
                apiIconColor
            ))
        }
    }.build()

    private fun configAction(showTitle: Boolean = false) = Action.Builder().apply {
        val backgroundColor = carContext.getColor(R.color.default_button_color)
        setIcon(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_car_app_tune)).build())
        if (showTitle) setTitle(carContext.getString(R.string.car_app_strip_selection))
        setBackgroundColor(CarColor.createCustom(backgroundColor, backgroundColor))
        setOnClickListener {
            screenManager.pushForResult(TripDataSettingsScreen(carContext)) {result ->
                if (result is Int) {
                    CarStatsViewer.appPreferences.mainViewTrip = result
                    CarStatsViewer.dataProcessor.changeSelectedTrip(result + 1)
                }
                else if (result is Boolean && result == true) {
                    CoroutineScope(Dispatchers.IO).launch {
                        CarStatsViewer.dataProcessor.resetTrip(
                            TripType.MANUAL,
                            CarStatsViewer.dataProcessor.realTimeData.drivingState
                        )
                    }
                }
            }
        }
    }.build()

    private fun resetAction(showTitle: Boolean = false) = Action.Builder().apply {
        val backgroundColor = carContext.getColor(R.color.default_button_color)
        setIcon(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_car_app_reset)).build())
        if (showTitle) setTitle(carContext.getString(R.string.dialog_reset_confirm))
        setBackgroundColor(CarColor.createCustom(backgroundColor, backgroundColor))
        setOnClickListener {
            screenManager.pushForResult(ConfirmResetScreen(carContext)) {result ->
                if (result == true) {
                    CoroutineScope(Dispatchers.IO).launch {
                        CarStatsViewer.dataProcessor.resetTrip(
                            TripType.MANUAL,
                            CarStatsViewer.dataProcessor.realTimeData.drivingState
                        )
                    }
                }
            }
        }
    }.build()
}