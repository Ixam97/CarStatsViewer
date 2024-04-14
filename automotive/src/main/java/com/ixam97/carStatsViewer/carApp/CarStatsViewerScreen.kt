package com.ixam97.carStatsViewer.carApp

import android.content.Intent
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.annotations.ExperimentalCarApi
import androidx.car.app.model.Action
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Tab
import androidx.car.app.model.TabContents
import androidx.car.app.model.TabTemplate
import androidx.car.app.model.TabTemplate.TabCallback
import androidx.car.app.model.Template
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.database.tripData.DrivingSession
import com.ixam97.carStatsViewer.ui.activities.HistoryActivity
import com.ixam97.carStatsViewer.ui.activities.MainActivity
import com.ixam97.carStatsViewer.ui.activities.SettingsActivity
import com.ixam97.carStatsViewer.utils.InAppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@ExperimentalCarApi
class CarStatsViewerScreen(carContext: CarContext) : Screen(carContext) {

    internal var dataUpdate = false
    internal var apiState: Map<String, Int> = mapOf()
    internal var session : DrivingSession? = null

    internal val colorDisconnected = CarColor.createCustom(carContext.getColor(R.color.inactive_text_color), carContext.getColor(R.color.disabled_tint))
    internal val colorConnected = CarColor.createCustom(carContext.getColor(R.color.connected_blue), carContext.getColor(R.color.connected_blue))
    internal val colorLimited = CarColor.createCustom(carContext.getColor(R.color.limited_yellow), carContext.getColor(R.color.limited_yellow))
    internal val colorError = CarColor.createCustom(carContext.getColor(R.color.bad_red), carContext.getColor(R.color.bad_red))

    internal var resetFlag = false

    internal var selectedTabContentID = "trip_data"

    private val settingsActivityIntent = Intent(carContext, SettingsActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    private val mainActivityIntent = Intent(carContext, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    private val historyActivityIntent = Intent(carContext, HistoryActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    init {
        setupListeners()
    }

    private fun setupListeners() {
        val exec = ContextCompat.getMainExecutor(carContext)
        CarStatsViewer.watchdog.setAaosCallback(exec) { externalInvalidate() }
        CarStatsViewer.dataProcessor.setAaosCallback(exec) { externalInvalidate() }
    }
    private fun externalInvalidate() {
        CoroutineScope(Dispatchers.IO).launch {
            dataUpdate = true
            delay(100)
            invalidate()
        }
    }
    override fun onGetTemplate(): Template {

        // updateLiveData()
        if (dataUpdate) {
            InAppLogger.i("[AAOS] Data Update")
            this.apiState = CarStatsViewer.watchdog.getCurrentWatchdogState().apiState
            session = CarStatsViewer.dataProcessor.selectedSessionData
            dataUpdate = false
        }

        return  createTabTemplate() // listTemplate
    }

    private fun createTabTemplate() = TabTemplate.Builder(object : TabCallback {
        override fun onTabSelected(tabContentId: String) {
            when (tabContentId) {
                "settings" -> carContext.startActivity(settingsActivityIntent)
                "dashboard" -> carContext.startActivity(mainActivityIntent)
                "trip_history" -> carContext.startActivity(historyActivityIntent)
            }
            selectedTabContentID = "trip_data"
            invalidate()
        }
    }).apply {
        setHeaderAction(Action.APP_ICON)
        addTab(createTab(R.string.car_app_trip_data, "trip_data", R.drawable.ic_list))
        addTab(createTab(R.string.history_title, "trip_history", R.drawable.ic_history))
        addTab(createTab(R.string.car_app_dashboard, "dashboard", R.drawable.ic_diagram))
        addTab(createTab(R.string.settings_title, "settings", R.drawable.ic_settings))
        setTabContents(TabContents.Builder(
            // createTripDataList()
            createTripDataPane()
        ).build())
        setActiveTabContentId(selectedTabContentID)
    }.build()

    private fun createTab(labelResId: Int, contentId: String, iconResId: Int) = Tab.Builder().apply {
        setTitle(carContext.getString(labelResId))
        setIcon(CarIcon.Builder(
            IconCompat.createWithResource(carContext, iconResId)
        ).build())
        setContentId(contentId)
    }.build()


}

