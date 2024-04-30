package com.ixam97.carStatsViewer.carApp

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
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.carApp.renderer.CarDataSurfaceCallback
import com.ixam97.carStatsViewer.database.tripData.DrivingSession
import com.ixam97.carStatsViewer.utils.InAppLogger
import com.ixam97.carStatsViewer.utils.throttle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@ExperimentalCarApi
class CarStatsViewerScreen(
    carContext: CarContext,
    session: CarStatsViewerSession
) : Screen(carContext), DefaultLifecycleObserver {

    internal val TAG = "CarStatsViewerScreen"

    private val CID_TRIP_DATA = "cid_trip_data"
    private val CID_MENU = "cid_menu"
    private val CID_CANVAS = "cid_canvas"
    private val CID_STATUS = "cid_status"

    private val INVALIDATE_INTERVAL_MS = 500L

    internal var apiState: Map<String, Int> = mapOf()

    internal val carDataSurfaceCallback = CarDataSurfaceCallback(carContext)

    internal val appPreferences = CarStatsViewer.appPreferences

    internal var drivingSession: DrivingSession? = null

    internal val colorError = CarColor.createCustom(carContext.getColor(R.color.bad_red), carContext.getColor(R.color.bad_red))
    internal val colorDisconnected = CarColor.createCustom(carContext.getColor(R.color.inactive_text_color), carContext.getColor(R.color.disabled_tint))
    internal val colorConnected = CarColor.createCustom(carContext.getColor(R.color.connected_blue), carContext.getColor(R.color.connected_blue))
    internal val colorLimited = CarColor.createCustom(carContext.getColor(R.color.limited_yellow), carContext.getColor(R.color.limited_yellow))

    private var lastInvalidate: Long = 0L
    private var invalidateInQueue = false

    internal var selectedTabContentID = CID_TRIP_DATA
        set(value) {
            if (field != value) {
                carDataSurfaceCallback.invalidatePlot()
            }
            field = value

        }

    private var lifecycle = getLifecycle()


    init {
        lifecycle.addObserver(this)
        lifecycleScope.launch {
            CarStatsViewer.dataProcessor.realTimeDataFlow.throttle(100).collect {
                carDataSurfaceCallback.requestRenderFrame()
            }
        }
        lifecycleScope.launch {
            CarStatsViewer.dataProcessor.selectedSessionDataFlow.collect {
                carDataSurfaceCallback.updateSession()
                drivingSession = it
                if (selectedTabContentID != CID_CANVAS) {
                    invalidateTabView()
                    InAppLogger.v("[$TAG] Session data flow requested invalidate.")
                }
            }
        }
        lifecycleScope.launch {
            CarStatsViewer.watchdog.watchdogStateFlow.collect {
                apiState = it.apiState
                //invalidate()
                invalidateTabView()
                InAppLogger.v("[$TAG] Watchdog requested invalidate.")
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
        // carDataSurfaceCallback.pause()
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        // carDataSurfaceCallback.resume()
    }

    override fun onGetTemplate(): Template {
        return  createTabTemplate()
    }

    private fun createTabTemplate() = TabTemplate.Builder(object : TabCallback {
        override fun onTabSelected(tabContentId: String) {

            selectedTabContentID = tabContentId
            invalidateTabView()
            InAppLogger.v("[$TAG] Tab change requested invalidate.")
        }
    }).apply {
        val tripType = when (appPreferences.mainViewTrip + 1) {
            1 -> R.string.CurrentTripData
            2 -> R.string.SinceChargeData
            3 -> R.string.AutoTripData
            4 -> R.string.CurrentMonthData
            else -> R.string.car_app_unknown
        }
        setHeaderAction(Action.APP_ICON)
        addTab(createTab(tripType, CID_TRIP_DATA, R.drawable.ic_car_app_list))
        addTab(createTab(R.string.car_app_dashboard, CID_CANVAS, R.drawable.ic_car_app_dashboard))
        addTab(createTab(R.string.car_app_status, CID_STATUS, R.drawable.ic_car_app_status))
        addTab(createTab(R.string.car_app_menu, CID_MENU, R.drawable.ic_car_app_menu))
        setTabContents(TabContents.Builder(
            when (selectedTabContentID) {
                CID_TRIP_DATA -> {
                    carDataSurfaceCallback.pause()
                    TripDataList(drivingSession)
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

    internal fun invalidateTabView() {
        if (invalidateInQueue) return
        val nanoTime = System.nanoTime()
        if (lastInvalidate + 1_000_000_000 < nanoTime) {
            invalidate()
            InAppLogger.d("[$TAG] Invalidated")
            lastInvalidate = nanoTime
        } else {
            invalidateInQueue = true
            lifecycleScope.launch {
                val remainingDelay = INVALIDATE_INTERVAL_MS - (nanoTime - lastInvalidate) / 1_000_000
                delay(remainingDelay)
                invalidate()
                InAppLogger.d("[$TAG] Invalidated")
                lastInvalidate = System.nanoTime()
                invalidateInQueue = false
            }
        }
    }
}

