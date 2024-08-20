package com.ixam97.carStatsViewer.carApp

import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.ScreenManager
import androidx.car.app.constraints.ConstraintManager
import androidx.car.app.model.Action
import androidx.car.app.model.Header
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.SectionedItemList
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.carApp.utils.autoTripIcon
import com.ixam97.carStatsViewer.carApp.utils.carIconFromRes
import com.ixam97.carStatsViewer.carApp.utils.getContentLimit
import com.ixam97.carStatsViewer.carApp.utils.manualTripIcon
import com.ixam97.carStatsViewer.carApp.utils.monthTripIcon
import com.ixam97.carStatsViewer.carApp.utils.sinceChargeTripIcon
import com.ixam97.carStatsViewer.database.tripData.DrivingSession
import com.ixam97.carStatsViewer.database.tripData.TripType
import com.ixam97.carStatsViewer.utils.StringFormatters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

class TripHistoryScreen(carContext: CarContext):
    Screen(carContext), DefaultLifecycleObserver
{
    private var showManualTrips: Boolean = CarStatsViewer.appPreferences.tripFilterManual
    private var showSinceChargeTrips: Boolean = CarStatsViewer.appPreferences.tripFilterCharge
    private var showAutomaticTrips: Boolean = CarStatsViewer.appPreferences.tripFilterAuto
    private var showMonthlyTrips: Boolean = CarStatsViewer.appPreferences.tripFilterMonth

    private val maxListLength = carContext.getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_LIST)
    private val screenManager = carContext.getCarService(ScreenManager::class.java)

    private var contentLoaded = false

    private val activeDrivingSessions = mutableListOf<DrivingSession>()
    private val pastDrivingSessions = mutableListOf<DrivingSession>()

    init {
        lifecycle.addObserver(this)
    }

    override fun onGetTemplate(): Template {
        return TripHistoryListTemplate()
    }

    override fun onCreate(owner: LifecycleOwner) {
        loadTripsFromDataSource()
        super.onCreate(owner)
    }

    private fun TripHistoryListTemplate() = ListTemplate.Builder().apply {
        setHeader(Header.Builder().apply {
            setTitle(carContext.getString(R.string.history_title))
            setStartHeaderAction(Action.BACK)
            addEndHeaderAction(Action.Builder().apply {
                setTitle("Refresh")
                setOnClickListener { invalidate() }
            }.build())
            addEndHeaderAction(Action.Builder().apply {
                setIcon(carContext.carIconFromRes(R.drawable.ic_filter))
                setOnClickListener {
                    screenManager.pushForResult(TripHistoryFilterScreen(carContext)) { result ->
                        loadTripsFromDataSource()
                    }
                }
                setEnabled(false)
            }.build())
        }.build())
        if (contentLoaded) {
            addSectionedList(
                SectionedItemList.create(
                    activeTripsItemList(),
                    carContext.getString(R.string.history_current_trips)
                )
            )
            if (pastDrivingSessions.size > 0) {
                addSectionedList(
                    SectionedItemList.create(
                        pastTripsItemList(),
                        carContext.getString(R.string.history_past_trips)
                    )
                )
            }
        }
        setLoading(!contentLoaded)
    }.build()

    private fun activeTripsItemList() = ItemList.Builder().apply {
        activeDrivingSessions.forEach { trip ->
            addItem(Row.Builder().apply {
                when (trip.session_type) {
                    1 -> setImage(manualTripIcon(carContext))
                    2 -> setImage(sinceChargeTripIcon(carContext))
                    3 -> setImage(autoTripIcon(carContext))
                    4 -> setImage(monthTripIcon(carContext))
                }
                setTitle("${ StringFormatters.getDateString(Date(trip.start_epoch_time)) }, ID: ${trip.driving_session_id}")
                addText(tripDataString(trip))
                setOnClickListener {
                    screenManager.push(TripDetailsScreen(carContext, trip))
                }
            }.build())
        }
    }.build()

    private fun pastTripsItemList() = ItemList.Builder().apply {
        pastDrivingSessions.forEach { trip ->
            addItem(Row.Builder().apply {
                when (trip.session_type) {
                    1 -> setImage(manualTripIcon(carContext))
                    2 -> setImage(sinceChargeTripIcon(carContext))
                    3 -> setImage(autoTripIcon(carContext))
                    4 -> setImage(monthTripIcon(carContext))
                }
                setTitle("${ StringFormatters.getDateString(Date(trip.start_epoch_time)) }, ID: ${trip.driving_session_id}")
                addText(tripDataString(trip))
                setOnClickListener {
                    screenManager.pushForResult(TripDetailsScreen(carContext, trip)) { result ->
                        if (result == "DeleteTrip" && (trip.end_epoch_time?:0) > 0) {
                            lifecycleScope.launch(Dispatchers.IO) {
                                CarStatsViewer.tripDataSource.deleteDrivingSessionById(trip.driving_session_id)
                            }
                            pastDrivingSessions.remove(trip)
                            invalidate()
                        }
                    }
                }
                if (carContext.carAppApiLevel >= 6) {
                    addAction(Action.Builder().apply {
                        setIcon(carContext.carIconFromRes(R.drawable.ic_delete))
                        setOnClickListener {
                            screenManager.pushForResult(ConfirmDeleteTripScreen(carContext)) { result ->
                                if (result == true && (trip.end_epoch_time?:0) > 0) {
                                    // delete corresponding trip and reload list.
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        CarStatsViewer.tripDataSource.deleteDrivingSessionById(trip.driving_session_id)
                                    }
                                    pastDrivingSessions.remove(trip)
                                    invalidate()
                                }
                            }
                        }
                    }.build())
                }
            }.build())
        }
    }.build()

    private fun tripDataString(trip: DrivingSession): String {
        val distance = StringFormatters.getTraveledDistanceString(trip.driven_distance.toFloat())
        val energy = StringFormatters.getEnergyString(trip.used_energy.toFloat())
        val consumption = StringFormatters.getAvgConsumptionString(trip.used_energy.toFloat(), trip.driven_distance.toFloat())
        val time = StringFormatters.getElapsedTimeString(trip.drive_time, true)

        return "$distance, $energy, $consumption, $time"
    }

    private fun loadTripsFromDataSource() {
        lifecycleScope.launch(Dispatchers.IO) {
            contentLoaded = false
            invalidate()
            showManualTrips = CarStatsViewer.appPreferences.tripFilterManual
            showSinceChargeTrips = CarStatsViewer.appPreferences.tripFilterCharge
            showAutomaticTrips = CarStatsViewer.appPreferences.tripFilterAuto
            showMonthlyTrips = CarStatsViewer.appPreferences.tripFilterMonth

            activeDrivingSessions.clear()
            activeDrivingSessions.addAll(CarStatsViewer.tripDataSource.getActiveDrivingSessions())
            activeDrivingSessions.sortBy { it.session_type }

            pastDrivingSessions.clear()
            pastDrivingSessions.addAll(CarStatsViewer.tripDataSource.getPastDrivingSessions())
            pastDrivingSessions.sortByDescending { it.driving_session_id }

            pastDrivingSessions.apply {
                clear()
                val trips = CarStatsViewer.tripDataSource.getPastDrivingSessions()
                addAll(trips.subList(0, (trips.size - 1).coerceAtMost(49)))
                sortByDescending { it.driving_session_id }

                if (!showManualTrips) { removeIf { it.session_type == TripType.MANUAL } }
                if (!showSinceChargeTrips) { removeIf { it.session_type == TripType.SINCE_CHARGE } }
                if (!showAutomaticTrips) { removeIf { it.session_type == TripType.AUTO } }
                if (!showMonthlyTrips) { removeIf { it.session_type == TripType.MONTH } }
            }

            contentLoaded = true
            withContext(Dispatchers.Main) {
                invalidate()
            }
        }
    }

}