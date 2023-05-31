package com.ixam97.carStatsViewer.activities

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.DatePicker
import android.widget.TextView
import com.airbnb.paris.extensions.style
import com.google.gson.GsonBuilder
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.database.tripData.TripType
import com.ixam97.carStatsViewer.utils.InAppLogger
import com.ixam97.carStatsViewer.views.TripHistoryRowWidget
import kotlinx.android.synthetic.main.activity_history.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HistoryActivity  : Activity() {

    private lateinit var context : Context
    private val appPreferences = CarStatsViewer.appPreferences

    override fun startActivity(intent: Intent?) {
        super.startActivity(intent)
        if (intent?.hasExtra("noTransition") == true) {
            if (!intent.getBooleanExtra("noTransition", false)) {
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        } else {
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        context = applicationContext

        setContentView(R.layout.activity_history)

        val tripTypeStringArray = resources.getStringArray(R.array.trip_type_names)

        history_button_back.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.stay_still, R.anim.slide_out_right)
        }

        history_button_filters.setOnClickListener {
            val filtersDialog = AlertDialog.Builder(this@HistoryActivity).apply {
                val layout = LayoutInflater.from(this@HistoryActivity)
                    .inflate(R.layout.dialog_history_filters, null)

                val dateCheckbox = layout.findViewById<CheckBox>(R.id.history_filter_checkbox_date)
                val datePicker = layout.findViewById<DatePicker>(R.id.history_filter_date_picker)

                setView(layout)

                setPositiveButton("Apply") { dialog, _ ->
                }
                setTitle("Trip filters")
                setCancelable(true)
            }
            filtersDialog.create()
            filtersDialog.show()
        }

        history_button_reset.setOnClickListener {
            CoroutineScope(Dispatchers.Default).launch {
                val newIntent = Intent(intent)
                newIntent.putExtra("noTransition", true)
                runOnUiThread {
                    history_linear_layout.removeAllViews()
                }
                (applicationContext as CarStatsViewer).tripDataManager.resetTrip(TripType.MANUAL, (applicationContext as CarStatsViewer).dataProcessor.realTimeData.drivingState)
                finish();
                startActivity(newIntent);
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            runOnUiThread {
                val currentSessionsHeadLine = TextView(this@HistoryActivity)
                currentSessionsHeadLine.text = "Current trips:"
                currentSessionsHeadLine.style(R.style.menu_section_title_style)
                history_linear_layout.addView(currentSessionsHeadLine)
                val divider = View(this@HistoryActivity)
                divider.style(R.style.menu_divider_style)
                history_linear_layout.addView(divider)
            }
            val currentDrivingSessions = CarStatsViewer.tripDataSource.getActiveDrivingSessionsIds().reversed()
            currentDrivingSessions.forEach { drivingSessionId ->
                val drivingSession = CarStatsViewer.tripDataSource.getDrivingSession(drivingSessionId)
                if (drivingSession != null) {
                    val rowView = TripHistoryRowWidget(this@HistoryActivity, session = drivingSession)

                    rowView.setOnDeleteClickListener {
                        CoroutineScope(Dispatchers.IO).launch {
                            CarStatsViewer.tripDataSource.deleteDrivingSessionById(drivingSession.driving_session_id)
                            runOnUiThread {
                                val newIntent = Intent(intent)
                                newIntent.putExtra("noTransition", true)
                                finish();
                                startActivity(newIntent);
                            }
                        }
                    }

                    rowView.setOnMainClickListener {
                        CoroutineScope(Dispatchers.IO).launch {
                            val fullDrivingSession = CarStatsViewer.tripDataSource.getFullDrivingSession(drivingSessionId)
                            // InAppLogger.v("[NEO]" + GsonBuilder().setPrettyPrinting().create().toJson(fullDrivingSession))
                        }
                    }

                    runOnUiThread {
                        history_linear_layout.addView(rowView)
                    }
                }
            }

            runOnUiThread {
                val pastSessionsHeadLine = TextView(this@HistoryActivity)
                pastSessionsHeadLine.text = "Past trips:"
                pastSessionsHeadLine.style(R.style.menu_section_title_style)
                history_linear_layout.addView(pastSessionsHeadLine)
                val divider = View(this@HistoryActivity)
                divider.style(R.style.menu_divider_style)
                history_linear_layout.addView(divider)
            }

            val pastDrivingSessions = CarStatsViewer.tripDataSource.getPastDrivingSessions().sortedBy { it.start_epoch_time }.reversed()
            pastDrivingSessions.forEach { drivingSession ->
                val rowView = TripHistoryRowWidget(this@HistoryActivity, session = drivingSession)

                rowView.setOnDeleteClickListener {
                    CoroutineScope(Dispatchers.IO).launch {
                        CarStatsViewer.tripDataSource.deleteDrivingSessionById(drivingSession.driving_session_id)
                        runOnUiThread {
                            val newIntent = Intent(intent)
                            newIntent.putExtra("noTransition", true)
                            finish();
                            startActivity(newIntent);
                        }
                    }
                }

                rowView.setOnMainClickListener {
                    CoroutineScope(Dispatchers.IO).launch {
                        val fullDrivingSession = CarStatsViewer.tripDataSource.getFullDrivingSession(drivingSession.driving_session_id)
                        // InAppLogger.v("[NEO]" + GsonBuilder().setPrettyPrinting().create().toJson(fullDrivingSession))
                    }
                }

                runOnUiThread {
                    history_linear_layout.addView(rowView)
                }
            }

            if (pastDrivingSessions.isEmpty()) {
                runOnUiThread {
                    val noTripsTextView = TextView(this@HistoryActivity)
                    // noTripsTextView.style(R.style.menu_row_content_text)
                    noTripsTextView.textAlignment = View.TEXT_ALIGNMENT_CENTER
                    noTripsTextView.text = "No past trips have been saved."
                    history_linear_layout.addView(noTripsTextView)
                }
            }
        }
    }

}