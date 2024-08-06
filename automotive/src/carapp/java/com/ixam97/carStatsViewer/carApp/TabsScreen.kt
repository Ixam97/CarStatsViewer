package com.ixam97.carStatsViewer.carApp

import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.annotations.ExperimentalCarApi
import androidx.car.app.model.Action
import androidx.car.app.model.Badge
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.GridItem
import androidx.car.app.model.GridTemplate
import androidx.car.app.model.ItemList
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Tab
import androidx.car.app.model.TabContents
import androidx.car.app.model.TabTemplate
import androidx.car.app.model.TabTemplate.TabCallback
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.ixam97.carStatsViewer.BuildConfig
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.carApp.tabsScreenTabs.apiStatusList
import com.ixam97.carStatsViewer.carApp.tabsScreenTabs.miscList
import com.ixam97.carStatsViewer.carApp.tabsScreenTabs.settingsList
import com.ixam97.carStatsViewer.carApp.utils.Gauge
import com.ixam97.carStatsViewer.carApp.utils.asCarIcon
import com.ixam97.carStatsViewer.dataProcessor.RealTimeData
import com.ixam97.carStatsViewer.database.tripData.DrivingSession
import com.ixam97.carStatsViewer.utils.InAppLogger
import com.ixam97.carStatsViewer.utils.throttle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt


@ExperimentalCarApi
class TabsScreen(
    carContext: CarContext,
    val session: CarStatsViewerSession
) : Screen(carContext), DefaultLifecycleObserver {

    internal val TAG = "TabsScreen"

    private val CID_TRIP_DATA = "cid_trip_data"
    private val CID_MISC = "cid_menu"
    private val CID_DASHBOARD = "cid_canvas"
    private val CID_STATUS = "cid_status"
    private val CID_SETTINGS = "cid_settings"

    private val INVALIDATE_INTERVAL_MS = 1000L

    internal var apiState: Map<String, Int> = mapOf()

    internal val appPreferences = CarStatsViewer.appPreferences

    internal var drivingSession: DrivingSession? = null
    private var realTimeData: RealTimeData? = null

    internal val colorError = CarColor.createCustom(carContext.getColor(R.color.bad_red), carContext.getColor(R.color.bad_red))
    internal val colorDisconnected = CarColor.createCustom(carContext.getColor(R.color.inactive_text_color), carContext.getColor(R.color.disabled_tint))
    internal val colorConnected = CarColor.createCustom(carContext.getColor(R.color.connected_blue), carContext.getColor(R.color.connected_blue))
    internal val colorLimited = CarColor.createCustom(carContext.getColor(R.color.limited_yellow), carContext.getColor(R.color.limited_yellow))

    private var lastInvalidate: Long = 0L
    private var invalidateInQueue = false
    private var invalidateTabViewEnabled = false

    private val gauge = Gauge(carContext)

    private val realTimeDataTemplate = RealTimeDataTemplate(
        carContext = carContext,
        session = session,
        backButton = false,
        navigationOnly = true,
        invalidateCallback = this::invalidateTabView
    )
    private val tripDataTemplate = TripDataTemplate(carContext)

    internal var selectedTabContentID = CID_TRIP_DATA
        set(value) {
            if (field != value) {
                session.carDataSurfaceCallback.invalidatePlot()
            }
            field = value

        }

    // private var lifecycle = getLifecycle()


    init {
        lifecycle.addObserver(this)
        lifecycleScope.launch {
            CarStatsViewer.dataProcessor.realTimeDataFlow.throttle(250).collect {
                realTimeData = it
                val realTimeDataOnTripData = selectedTabContentID == CID_TRIP_DATA && appPreferences.carAppRealTimeData
                val realTimeDataOnDashboard = selectedTabContentID == CID_DASHBOARD && appPreferences.carAppRealTimeData && !(carContext.carAppApiLevel >= 7 && BuildConfig.FLAVOR_version == "dev")
                if (realTimeDataOnDashboard || realTimeDataOnTripData) {
                    invalidateTabView()
                    InAppLogger.v("[$TAG] Real time data flow requested invalidate.")
                }
            }
        }
        lifecycleScope.launch {
            CarStatsViewer.dataProcessor.selectedSessionDataFlow.collect {
                session.carDataSurfaceCallback.updateSession()
                drivingSession = it
                if (selectedTabContentID == CID_TRIP_DATA) {
                    invalidateTabView()
                    // InAppLogger.v("[$TAG] Session data flow requested invalidate.")
                }
            }
        }
        lifecycleScope.launch {
            CarStatsViewer.watchdog.watchdogStateFlow.collect {
                apiState = it.apiState
                //invalidate()
                if (selectedTabContentID == CID_STATUS || selectedTabContentID == CID_MISC)
                    invalidateTabView()
                // InAppLogger.v("[$TAG] Watchdog requested invalidate.")
            }
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        if (BuildConfig.FLAVOR_version == "dev") {
            carContext.getCarService(AppManager::class.java)
                .setSurfaceCallback(session.carDataSurfaceCallback)
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        invalidateTabViewEnabled = false
        InAppLogger.d("[$TAG] Pausing Screen Lifecycle")
        // session.carDataSurfaceCallback.pause()
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        invalidateTabViewEnabled = true
        invalidate() // TabView()
        InAppLogger.d("[$TAG] Resuming Screen Lifecycle")
        // session.carDataSurfaceCallback.resume()
    }

    override fun onGetTemplate(): Template {
        return  createTabTemplate()
    }

    private fun createTabTemplate() = TabTemplate.Builder(object : TabCallback {
        override fun onTabSelected(tabContentId: String) {
            selectedTabContentID = tabContentId
            invalidate() // TabView()
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
        if (BuildConfig.FLAVOR_version == "dev") addTab(createTab(R.string.car_app_dashboard, CID_DASHBOARD, R.drawable.ic_car_app_dashboard))
        else addTab(createTab(R.string.car_app_status, CID_STATUS, R.drawable.ic_connected))
        addTab(createTab(R.string.settings_title, CID_SETTINGS, R.drawable.ic_car_app_settings))
        addTab(createTab(R.string.car_app_menu, CID_MISC, R.drawable.ic_car_app_menu))
        setTabContents(TabContents.Builder(
            when (selectedTabContentID) {
                CID_TRIP_DATA -> {
                    session.carDataSurfaceCallback.pause()

                    InAppLogger.v("[$TAG] Refreshing trip data template")
                    tripDataTemplate.tripDataPaneTemplate(drivingSession, if (appPreferences.carAppRealTimeData) realTimeData else null)
                }
                CID_STATUS -> {
                    session.carDataSurfaceCallback.pause()
                    apiStatusList()
                }
                CID_DASHBOARD -> {
                    if (carContext.carAppApiLevel >= 7 && BuildConfig.FLAVOR_version == "dev") {
                        session.carDataSurfaceCallback.resume()
                        realTimeDataTemplate.getTemplate()
                    } else {
                        LowApiLevelMessage()
                    }
                }
                CID_SETTINGS -> {
                    session.carDataSurfaceCallback.pause()
                    settingsList()
                }
                CID_MISC -> {
                    session.carDataSurfaceCallback.pause()
                    miscList()
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
        if (invalidateInQueue || !invalidateTabViewEnabled) return
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

    private fun LowApiLevelMessage() = MessageTemplate.Builder("API level 7 required for embedded view").apply {
        addAction(Action.Builder().apply {
            setTitle("Open external map view")
            setOnClickListener {
                screenManager.push(RealTimeDataScreen(carContext, session))
            }
        }.build())
    }.build()

    private fun liveDataDisabledMessage() = MessageTemplate.Builder(
        "Live Data is disabled in the settings."
    ).build()

    private fun realTimeDataGridTemplate() = GridTemplate.Builder().apply {
        // setItemSize(GridTemplate.ITEM_SIZE_LARGE)
        setSingleList(ItemList.Builder().apply {
            addItem(GridItem.Builder().apply {
                // val selected = appPreferences.carAppSelectedRealTimeData == 1
                setTitle("${(((realTimeData?.power?:0f)/1_000_000) * 10).toInt() / 10f } kW")
                setText("Power")
                setImage(
                // setImageWithBadge(
                    gauge.draw(128, (realTimeData?.power?:0f)/1_000_000, min = -150f, max = 300f, selected = false /* selected */).asCarIcon(),
                    // appPreferences.carAppSelectedRealTimeData == 1
                )
                InAppLogger.v("[$TAG] Refreshing image")
                setItemSize(GridTemplate.ITEM_SIZE_LARGE)
                // setOnClickListener {
                //     appPreferences.carAppSelectedRealTimeData = if (appPreferences.carAppSelectedRealTimeData == 1) 0 else 1
                //     invalidate() // TabView()
                // }
            }.build())
            addItem(GridItem.Builder().apply {
                // val selected = appPreferences.carAppSelectedRealTimeData == 2

                var instCons = realTimeData?.instConsumption
                val instConsVal: Number? = if (instCons != null && (realTimeData?.speed?:0f) * 3.6 > 3) {
                    if (appPreferences.consumptionUnit) {
                        appPreferences.distanceUnit.asUnit(instCons).roundToInt()
                    } else {
                        appPreferences.distanceUnit.asUnit(instCons).roundToInt() / 10
                    }
                } else {
                    null
                }
                val instUnit = if (appPreferences.consumptionUnit) {
                    "Wh/${appPreferences.distanceUnit.unit()}"
                } else {
                    "kWh/100${appPreferences.distanceUnit.unit()}"
                }

                if ((realTimeData?.speed?:0f) * 3.6 < 3) instCons = null

                setTitle("${instConsVal?: "∞"} $instUnit")
                setText("Consumption")
                setImage( //WithBadge(
                    gauge.draw(128, instCons?:0f, -300f, 600f, selected = false /* selected */).asCarIcon(),
                    // selected
                )
                setItemSize(GridTemplate.ITEM_SIZE_MEDIUM)
                // setOnClickListener {
                //     appPreferences.carAppSelectedRealTimeData = if (appPreferences.carAppSelectedRealTimeData == 2) 0 else 2
                //     invalidate() // TabView()
                // }
            }.build())
        }.build())
    }.build()

    private fun realTimeDataPaneTemplate() = PaneTemplate.Builder(Pane.Builder().apply {
        setImage(gauge.draw(480, 240f).asCarIcon(), )
        addRow(Row.Builder().apply {
            setTitle("Test Title")
        }.build())
    }.build()).build()

    private fun checkedBadge() = Badge.Builder().apply {
        setIcon(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_car_app_check)).build())
    }.build()

    private fun GridItem.Builder.setImageWithBadge(carIcon: CarIcon, checked: Boolean = false) {
        when (checked) {
            true -> setImage(carIcon, checkedBadge())
            false -> setImage(carIcon)
        }
    }
}

