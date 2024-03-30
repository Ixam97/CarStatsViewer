package com.ixam97.carStatsViewer.carApp

import android.app.AlertDialog
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.annotations.ExperimentalCarApi
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.Alert
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.CarText
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.ParkedOnlyOnClickListener
import androidx.car.app.model.Row
import androidx.car.app.model.SectionedItemList
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
import com.ixam97.carStatsViewer.database.tripData.TripType
import com.ixam97.carStatsViewer.ui.activities.HistoryActivity
import com.ixam97.carStatsViewer.ui.activities.MainActivity
import com.ixam97.carStatsViewer.ui.activities.SettingsActivity
import com.ixam97.carStatsViewer.utils.InAppLogger
import com.ixam97.carStatsViewer.utils.StringFormatters
import com.ixam97.carStatsViewer.utils.setContentViewAndTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@ExperimentalCarApi
class CarStatsViewerScreen(carContext: CarContext) : Screen(carContext) {

    private var apiStateString = "No Data"
    private var speed : Float? = 0f
    private var power : Float? = 0f
    private var session : DrivingSession? = null

    private var resetFlag = false

    private var selectedTabContentID = "trip_data"

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
        CarStatsViewer.watchdog.setAaosCallback(exec) { updateLiveData() }
        CarStatsViewer.dataProcessor.setAaosCallback(exec) { updateLiveData() }
    }
    private fun updateLiveData() {
        val apiState = CarStatsViewer.watchdog.getCurrentWatchdogState().apiState
        val realTimeData = CarStatsViewer.dataProcessor.realTimeData
        session = CarStatsViewer.dataProcessor.selectedSessionData
        apiStateString = apiState.toString()
        speed = realTimeData.speed
        power = realTimeData.power
        InAppLogger.d("[Car App] Data Update")
        invalidate()
    }
    override fun onGetTemplate(): Template {

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
        addTab(Tab.Builder().apply {
            setTitle(carContext.getString(R.string.car_app_trip_data))
            setIcon(CarIcon.Builder(
                IconCompat.createWithResource(carContext, R.drawable.ic_list)
            ).setTint(CarColor.BLUE).build())
            setContentId("trip_data")
        }.build())
        addTab(Tab.Builder().apply {
            setTitle(carContext.getString(R.string.history_title))
            setIcon(CarIcon.Builder(
                IconCompat.createWithResource(carContext, R.drawable.ic_history)
            ).setTint(CarColor.DEFAULT).build())
            setContentId("trip_history")
        }.build())
        addTab(Tab.Builder().apply {
            setTitle(carContext.getString(R.string.car_app_dashboard))
            setIcon(CarIcon.Builder(
                IconCompat.createWithResource(carContext, R.drawable.ic_diagram)
            ).setTint(CarColor.DEFAULT).build())
            setContentId("dashboard")
        }.build())
        addTab(Tab.Builder().apply {
            setTitle(carContext.getString(R.string.settings_title))
            setIcon(CarIcon.Builder(
                IconCompat.createWithResource(carContext, R.drawable.ic_settings)
            ).setTint(CarColor.DEFAULT).build())
            setContentId("settings")
        }.build())
        setTabContents(TabContents.Builder(
            // createListTemplate()
            createPaneTemplate()
        ).build())
        setActiveTabContentId(selectedTabContentID)
    }.build()

    private fun createPaneTemplate() = PaneTemplate.Builder(Pane.Builder().apply {
        session?.let {
            val tripType = when (it.session_type) {
                1 -> carContext.getString(R.string.CurrentTripData)
                2 -> carContext.getString(R.string.SinceChargeData)
                3 -> carContext.getString(R.string.AutoTripData)
                4 -> carContext.getString(R.string.CurrentMonthData)
                else -> "unknown"
            }
            val tripTypeIcon = when (it.session_type) {
                1 -> CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_hand)).build()
                2 -> CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_charger)).build()
                3 -> CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_day)).build()
                4 -> CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_month)).build()
                else -> CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_help)).build()
            }
            addRow(Row.Builder().apply {
                setTitle(StringFormatters.getTraveledDistanceString(it.driven_distance.toFloat()))
                setImage(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_distance_large)).build())
                addText(carContext.getString(R.string.summary_traveled_distance))
            }.build())
            addRow(Row.Builder().apply {
                setTitle(StringFormatters.getEnergyString(it.used_energy.toFloat()))
                setImage(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_energy_large)).build())
                addText(carContext.getString(R.string.summary_used_energy))
            }.build())
            addRow(Row.Builder().apply {
                setTitle(StringFormatters.getAvgConsumptionString(it.used_energy.toFloat(), it.driven_distance.toFloat()))
                setImage(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_diagram)).build())
                addText(carContext.getString(R.string.summary_average_consumption))
            }.build())
            addRow(Row.Builder().apply {
                setTitle(StringFormatters.getAvgSpeedString(it.driven_distance.toFloat(), it.drive_time))
                setImage(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_speed_large)).build())
                addText(carContext.getString(R.string.summary_speed))
            }.build())
            addRow(Row.Builder().apply {
                setTitle(StringFormatters.getElapsedTimeString(it.drive_time, true))
                setImage(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_time_large)).build())
                addText(carContext.getString(R.string.summary_travel_time))
            }.build())

            if (it.session_type == 1) {
                addAction(Action.Builder().apply {
                    setIcon(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_reset)).build())
                    setTitle("Reset")
                    setOnClickListener {
                        if (resetFlag) {
                            CoroutineScope(Dispatchers.IO).launch {
                                CarStatsViewer.dataProcessor.resetTrip(
                                    TripType.MANUAL,
                                    CarStatsViewer.dataProcessor.realTimeData.drivingState
                                )
                            }
                            resetFlag = false
                            CarToast.makeText(carContext, "Manual Trip has ben reset.", CarToast.LENGTH_LONG).show()
                        } else {
                            CoroutineScope(Dispatchers.IO).launch {
                                delay(4_000)
                                resetFlag = false
                                invalidate()
                            }
                            resetFlag = true
                            CarToast.makeText(carContext, "Press again to reset.", CarToast.LENGTH_LONG).show()
                        }
                        invalidate()
                    }
                    if (resetFlag) {
                        setFlags(Action.FLAG_PRIMARY)
                    }
                }.build())
            }

            addAction(Action.Builder().apply {
                setIcon(tripTypeIcon)
                setTitle(tripType)
                setOnClickListener {
                    val newTripType = if (it.session_type >= 4 ) {
                        1
                    } else {
                        it.session_type + 1
                    }
                    CarStatsViewer.dataProcessor.changeSelectedTrip(newTripType)
                    CarStatsViewer.appPreferences.mainViewTrip = newTripType - 1
                    session = null
                    // invalidate()
                }
                // setFlags(Action.FLAG_PRIMARY)
            }.build())

        } ?: setLoading(true)
    }.build()).apply {
        setTitle("Dummy")
    }.build()

    private fun createListTemplate() = ListTemplate.Builder().apply {

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
            addSectionedList(SectionedItemList.create(ItemList.Builder().apply {
                session?.let {
                    addItem(Row.Builder().apply {
                        setTitle(StringFormatters.getTraveledDistanceString(it.driven_distance.toFloat()))
                        setImage(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_distance_large)).build())
                        // addText(carContext.getString(R.string.summary_traveled_distance))
                    }.build())
                    addItem(Row.Builder().apply {
                        setTitle(StringFormatters.getEnergyString(it.used_energy.toFloat()))
                        setImage(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_energy_large)).build())
                        // addText(carContext.getString(R.string.summary_used_energy))
                    }.build())
                    addItem(Row.Builder().apply {
                        setTitle(StringFormatters.getAvgConsumptionString(it.used_energy.toFloat(), it.driven_distance.toFloat()))
                        setImage(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_diagram)).build())
                        // addText(carContext.getString(R.string.summary_average_consumption))
                    }.build())
                    addItem(Row.Builder().apply {
                        setTitle(StringFormatters.getAvgSpeedString(it.driven_distance.toFloat(), it.drive_time))
                        setImage(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_speed_large)).build())
                        // addText(carContext.getString(R.string.summary_speed))
                    }.build())
                    addItem(Row.Builder().apply {
                        setTitle(StringFormatters.getElapsedTimeString(it.drive_time, true))
                        setImage(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_time_large)).build())
                        // addText(carContext.getString(R.string.summary_travel_time))
                    }.build())
                }
            }.build(), tripType))
        }

    }.build()
}

