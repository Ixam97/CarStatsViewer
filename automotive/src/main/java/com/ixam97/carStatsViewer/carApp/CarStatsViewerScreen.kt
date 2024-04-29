package com.ixam97.carStatsViewer.carApp

import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.car.app.AppManager
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
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.carApp.renderer.CarDataSurfaceCallback
import com.ixam97.carStatsViewer.carApp.renderer.DefaultRenderer
import com.ixam97.carStatsViewer.database.tripData.DrivingSession
import com.ixam97.carStatsViewer.ui.activities.HistoryActivity
import com.ixam97.carStatsViewer.ui.activities.MainActivity
import com.ixam97.carStatsViewer.ui.activities.SettingsActivity
import com.ixam97.carStatsViewer.utils.InAppLogger
import com.ixam97.carStatsViewer.utils.throttle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


@ExperimentalCarApi
class CarStatsViewerScreen(
    carContext: CarContext,
    session: CarStatsViewerSession
) : Screen(carContext), DefaultLifecycleObserver {

    private val CID_TRIP_DATA = "cid_trip_data"
    private val CID_MENU = "cid_menu"
    private val CID_CANVAS = "cid_nav_test"
    private val CID_STATUS = "cid_status"

    internal var dataUpdate = false
    internal var apiState: Map<String, Int> = mapOf()
    internal var session : DrivingSession? = null

    internal val carDataSurfaceCallback = CarDataSurfaceCallback(carContext)

    internal val appPreferences = CarStatsViewer.appPreferences

    internal val colorDisconnected = CarColor.createCustom(carContext.getColor(R.color.inactive_text_color), carContext.getColor(R.color.disabled_tint))
    internal val colorConnected = CarColor.createCustom(carContext.getColor(R.color.connected_blue), carContext.getColor(R.color.connected_blue))
    internal val colorLimited = CarColor.createCustom(carContext.getColor(R.color.limited_yellow), carContext.getColor(R.color.limited_yellow))
    internal val colorError = CarColor.createCustom(carContext.getColor(R.color.bad_red), carContext.getColor(R.color.bad_red))

    internal var resetFlag = false

    internal var selectedTabContentID = CID_TRIP_DATA

    private val settingsActivityIntent = Intent(carContext, SettingsActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    private val mainActivityIntent = Intent(carContext, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    private val historyActivityIntent = Intent(carContext, HistoryActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    private var lifecycle = getLifecycle()


    init {
        lifecycle.addObserver(this)
        lifecycleScope.launch {
            CarStatsViewer.dataProcessor.realTimeDataFlow.throttle(100).collect {
                carDataSurfaceCallback.renderFrame()
            }
        }
        lifecycleScope.launch {
            CarStatsViewer.dataProcessor.selectedSessionDataFlow.throttle(1000).collect {
                carDataSurfaceCallback.updateSession()
            }
        }
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
            delay(250)
            Handler(Looper.getMainLooper()).post {
                // invalidate()
            }
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        carContext.getCarService(AppManager::class.java)
            .setSurfaceCallback(carDataSurfaceCallback)
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        carDataSurfaceCallback.pause()
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        carDataSurfaceCallback.resume()
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
            // when (tabContentId) {
            //     "settings" -> carContext.startActivity(settingsActivityIntent)
            //     "dashboard" -> carContext.startActivity(mainActivityIntent)
            //     "trip_history" -> carContext.startActivity(historyActivityIntent)
            // }

            selectedTabContentID = tabContentId
            invalidate()
        }
    }).apply {
        setHeaderAction(Action.APP_ICON)
        addTab(createTab(R.string.car_app_trip_data, CID_TRIP_DATA, R.drawable.ic_car_app_list))
        addTab(createTab(R.string.car_app_status, CID_STATUS, R.drawable.ic_car_app_status))
        addTab(createTab(R.string.car_app_canvas, CID_CANVAS, R.drawable.ic_car_app_canvas))
        addTab(createTab(R.string.car_app_menu, CID_MENU, R.drawable.ic_car_app_menu))
        setTabContents(TabContents.Builder(
            // TripDataList()
            // createTripDataPane()
            when (selectedTabContentID) {
                CID_TRIP_DATA -> {
                    carDataSurfaceCallback.pause()
                    TripDataList()
                }
                CID_STATUS -> {
                    carDataSurfaceCallback.pause()
                    CarStatsList()
                }
                CID_CANVAS -> {
                    carDataSurfaceCallback.resume()
                    NavigationTest()
                }
                CID_MENU -> {
                    carDataSurfaceCallback.pause()
                    MenuList()
                }
                else -> throw Exception("Unsupported Content ID!")
            }
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

