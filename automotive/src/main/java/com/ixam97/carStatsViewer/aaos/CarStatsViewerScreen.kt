package com.ixam97.carStatsViewer.aaos

import android.content.Intent
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.SectionedItemList
import androidx.car.app.model.Template
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.ui.activities.HistoryActivity
import com.ixam97.carStatsViewer.ui.activities.MainActivity
import com.ixam97.carStatsViewer.ui.activities.SettingsActivity
import com.ixam97.carStatsViewer.utils.InAppLogger
import com.ixam97.carStatsViewer.utils.StringFormatters


class CarStatsViewerScreen(carContext: CarContext) : Screen(carContext) {

    private var apiStateString = "No Data"
    private var speed : Float? = 0f
    private var power : Float? = 0f
    init {
        setupListeners()
    }

    private fun setupListeners() {
        val exec = ContextCompat.getMainExecutor(carContext)
        CarStatsViewer.watchdog.setAaosCallback(exec) { updateLiveData() }
        CarStatsViewer.dataProcessor.setAaosCallback(exec) { updateLiveData() }
    }
    private fun updateLiveData() {
        val apiState = CarStatsViewer.watchdog.getCurrentWatchdogState().apiState
        val realTimeData = CarStatsViewer.dataProcessor.realTimeData
        InAppLogger.d("[AAOS] Updating live data: $apiState")
        apiStateString = apiState.toString()
        speed = realTimeData.speed
        power = realTimeData.power
        invalidate()
    }
    override fun onGetTemplate(): Template {
        val settingsActivityIntent = Intent(carContext, SettingsActivity::class.java)
        settingsActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val mainActivityIntent = Intent(carContext, MainActivity::class.java)
        mainActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val historyActivityIntent = Intent(carContext, HistoryActivity::class.java)
        historyActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val dashboardRow = Row.Builder()
            .setTitle("Open Dashboard")
            .setOnClickListener {
                carContext.startActivity(mainActivityIntent)
            }
            .setImage(
                CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_diagram))
                    .setTint(CarColor.DEFAULT).build()
            )
            .setBrowsable(true)
            .build()
        val historyRow = Row.Builder()
            .setTitle("Open Trip History")
            .setOnClickListener {
                carContext.startActivity(historyActivityIntent)
            }
            .setImage(
                CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_history))
                    .setTint(CarColor.DEFAULT).build()
            )
            .setBrowsable(true)
            .build()

        return ListTemplate.Builder().apply {
            setTitle(carContext.getString(R.string.app_name))
            setHeaderAction(Action.APP_ICON)
            setActionStrip(ActionStrip.Builder().apply {
                addAction(Action.Builder().apply {
                    setIcon(CarIcon.Builder(
                        IconCompat.createWithResource(carContext, R.drawable.ic_settings)
                    ).setTint(CarColor.DEFAULT).build())
                    setOnClickListener {
                        carContext.startActivity(settingsActivityIntent)
                    }
                }.build())
            }.build())

            addSectionedList(SectionedItemList.create(ItemList.Builder().apply {
                addItem(dashboardRow)
                addItem(historyRow)
            }.build(), "Main Functions"))

            addSectionedList(SectionedItemList.create(ItemList.Builder().apply {
                addItem(Row.Builder().apply {
                    setTitle(apiStateString)
                }.build())
            }.build(), "API Status"))

            addSectionedList(SectionedItemList.create(ItemList.Builder().apply {
                addItem(Row.Builder().apply {
                    setTitle("${((speed?:0f) * 3.6f).toInt()} km/h")
                    addText("Speed")
                }.build())
                addItem(Row.Builder().apply {
                    setTitle("${((power?:0f) / 1_000_000).toInt()} kW")
                    addText("Power")
                }.build())
            }.build(), "Car Data"))

        }.build()
    }
}

