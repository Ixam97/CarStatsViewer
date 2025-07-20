package com.ixam97.carStatsViewer.carApp

import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
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
import com.ixam97.carStatsViewer.carApp.utils.carIconFromRes
import com.ixam97.carStatsViewer.database.tripData.DrivingSession
import com.ixam97.carStatsViewer.map.Mapbox
import com.ixam97.carStatsViewer.utils.StringFormatters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

class TripDetailsScreen(carContext: CarContext, private val pTrip: DrivingSession):
    Screen(carContext), DefaultLifecycleObserver
{

    private var trip: DrivingSession = pTrip
    private var startLocation: String = "Loading start location ..."
    private var endLocation: String = "Loading end location..."
    private val chargingLocations = mutableListOf<String?>()

    private var loading = true


    init {
        lifecycle.addObserver(this)
    }

    override fun onCreate(owner: LifecycleOwner) {
        lifecycleScope.launch(Dispatchers.IO) {
            trip = CarStatsViewer.tripDataSource.getFullDrivingSession(pTrip.driving_session_id)

            if (!trip.drivingPoints.isNullOrEmpty()) {
                val coordinates = trip.drivingPoints!!.filter { it.lat != null }
                if (coordinates.isNotEmpty()) {

                    startLocation = Mapbox.getAddress(
                        coordinates.first().lon!!.toDouble(),
                        coordinates.first().lat!!.toDouble()
                    )
                    endLocation = Mapbox.getAddress(
                        coordinates.last().lon!!.toDouble(),
                        coordinates.last().lat!!.toDouble()
                    )
                } else {
                    startLocation = "Start location not available"
                    endLocation = "End location not available"
                }
            } else {
                startLocation = "Start location not available"
                endLocation = "End location not available"
            }

            if (!trip.chargingSessions.isNullOrEmpty()) {
                trip.chargingSessions!!.forEach {
                    chargingLocations.add(if (it.lat != null && it.lon != null) {
                        Mapbox.getAddress(it.lon.toDouble(), it.lat.toDouble())
                    } else null )
                }
            }

            withContext(Dispatchers.Main) {
                loading = false
                invalidate()
            }
        }
        super.onCreate(owner)
    }

    override fun onGetTemplate(): Template {
        return tripDetailsTemplate(trip)
    }

    private fun tripDetailsTemplate(trip: DrivingSession?) = ListTemplate.Builder().apply {
        setHeader(Header.Builder().apply {
            setTitle("${carContext.resources.getStringArray(R.array.trip_type_names)[pTrip.session_type]}, ID: ${pTrip.driving_session_id}")
            if (loading) {
                addEndHeaderAction(Action.Builder().apply {
                    setTitle("Refresh")
                    setOnClickListener {
                        invalidate()
                    }
                }.build())
            } else {
                addEndHeaderAction(Action.Builder().apply {
                    setIcon(carContext.carIconFromRes(R.drawable.ic_upload))
                    setOnClickListener {
                        // CarToast.makeText(carContext, "Exporting trip", CarToast.LENGTH_SHORT)
                        //     .show()
                        CarToast.makeText(carContext, "Exporting not yet implemented", CarToast.LENGTH_SHORT)
                            .show()
                    }
                    setEnabled(false)
                }.build())
                if ((pTrip.end_epoch_time ?: 0) > 0) {
                    addEndHeaderAction(Action.Builder().apply {
                        setIcon(carContext.carIconFromRes(R.drawable.ic_delete))
                        setOnClickListener {
                            screenManager.pushForResult(ConfirmDeleteTripScreen(carContext)) { result ->
                                if (result == true) {
                                    // Delete selected Trip
                                    setResult("DeleteTrip")
                                    screenManager.pop()
                                }
                            }
                        }
                    }.build())
                }
            }
            setStartHeaderAction(Action.BACK)
        }.build())
        if (!loading) {
            trip?.let { trip ->
                addSectionedList(
                    SectionedItemList.create(
                        ItemList.Builder().apply {
                            addItem(Row.Builder().apply {
                                setTitle("Start: ${StringFormatters.getDateString(Date(trip.start_epoch_time))}")
                                addText(startLocation)
                            }.build())
                            if (trip.end_epoch_time != null && trip.end_epoch_time > 0) {
                                addItem(Row.Builder().apply {
                                    setTitle("End: ${StringFormatters.getDateString(Date(trip.end_epoch_time))}")
                                    addText(endLocation)
                                }.build())
                            }
                            addItem(Row.Builder().apply {
                                setTitle("${carContext.getString(R.string.summary_charging_sessions)}: ${trip.chargingSessions?.size ?: "loading..."}")
                                setImage(carContext.carIconFromRes(R.drawable.ic_charger))
                                setBrowsable(true)
                                setOnClickListener {
                                    screenManager.push(
                                        ChargingSessionListScreen(
                                            carContext,
                                            trip.chargingSessions ?: listOf(),
                                            chargingLocations
                                        )
                                    )
                                }
                                setEnabled((trip.chargingSessions?.size ?: 0) > 0)
                            }.build())
                        }.build(),
                        "Trip Info"
                    )
                )
                addSectionedList(
                    SectionedItemList.create(
                        ItemList.Builder().apply {
                            addItem(Row.Builder().apply {
                                setTitle(StringFormatters.getTraveledDistanceString(trip.driven_distance.toFloat()))
                                addText(carContext.getString(R.string.summary_traveled_distance))
                                setImage(carContext.carIconFromRes(R.drawable.ic_distance_large))
                            }.build())
                            addItem(Row.Builder().apply {
                                setTitle(StringFormatters.getEnergyString(trip.used_energy.toFloat()))
                                addText(carContext.getString(R.string.summary_used_energy))
                                setImage(carContext.carIconFromRes(R.drawable.ic_energy_large))
                            }.build())
                            addItem(Row.Builder().apply {
                                setTitle(
                                    StringFormatters.getAvgConsumptionString(
                                        trip.used_energy.toFloat(),
                                        trip.driven_distance.toFloat()
                                    )
                                )
                                addText(carContext.getString(R.string.summary_average_consumption))
                                setImage(carContext.carIconFromRes(R.drawable.ic_avg_consumption))
                            }.build())
                            addItem(Row.Builder().apply {
                                setTitle(
                                    StringFormatters.getAvgSpeedString(
                                        trip.driven_distance.toFloat(),
                                        trip.drive_time
                                    )
                                )
                                addText(carContext.getString(R.string.summary_speed))
                                setImage(carContext.carIconFromRes(R.drawable.ic_speed_large))
                            }.build())
                            addItem(Row.Builder().apply {
                                setTitle(
                                    StringFormatters.getElapsedTimeString(
                                        trip.drive_time,
                                        true
                                    )
                                )
                                addText(carContext.getString(R.string.summary_travel_time))
                                setImage(carContext.carIconFromRes(R.drawable.ic_time_large))
                            }.build())
                        }.build(),
                        "Trip Details"
                    )
                )
            }
        }
        setLoading(loading)
    }.build()

}