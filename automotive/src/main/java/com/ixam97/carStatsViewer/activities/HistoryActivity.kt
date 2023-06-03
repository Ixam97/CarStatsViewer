package com.ixam97.carStatsViewer.activities

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.DatePicker
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import com.airbnb.paris.extensions.style
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.fragments.SummaryFragment
import com.ixam97.carStatsViewer.views.TripHistoryRowWidget
import kotlinx.android.synthetic.main.activity_history.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HistoryActivity  : FragmentActivity() {

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

                setPositiveButton(getString(R.string.dialog_apply)) { dialog, _ ->
                }
                setTitle(getString(R.string.history_dialog_filters_title))
                setCancelable(true)
            }
            filtersDialog.create()
            filtersDialog.show()
        }

        CoroutineScope(Dispatchers.IO).launch {
            runOnUiThread {
                addSection(getString(R.string.history_current_trips))
            }
            val currentDrivingSessions = CarStatsViewer.tripDataSource.getActiveDrivingSessions().sortedBy { it.session_type }
            currentDrivingSessions.forEach { drivingSession ->
                val rowView = TripHistoryRowWidget(this@HistoryActivity, session = drivingSession)

                rowView.setOnDeleteClickListener {
                    // deleteTrip(drivingSession.driving_session_id)
                    resetTrip(drivingSession.session_type)
                }

                rowView.setOnMainClickListener {
                    openSummary(drivingSession.driving_session_id)
                }

                runOnUiThread {
                    history_linear_layout.addView(rowView)
                }
            }

            runOnUiThread {
                addSection(getString(R.string.history_past_trips))
            }

            val pastDrivingSessions = CarStatsViewer.tripDataSource.getPastDrivingSessions().sortedBy { it.start_epoch_time }.reversed()
            pastDrivingSessions.forEach { drivingSession ->
                val rowView = TripHistoryRowWidget(this@HistoryActivity, session = drivingSession)

                rowView.setOnDeleteClickListener {
                    deleteTrip(drivingSession.driving_session_id)
                }

                rowView.setOnMainClickListener {
                    openSummary(drivingSession.driving_session_id)
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

    private fun openSummary(sessionId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            val session = CarStatsViewer.tripDataSource.getFullDrivingSession(sessionId)
            if ((appPreferences.mainViewTrip + 1) != session.session_type) {
                appPreferences.mainViewTrip = session.session_type - 1
                (applicationContext as CarStatsViewer).dataProcessor.changeSelectedTrip(session.session_type)
            }
            runOnUiThread {
                history_fragment_container.visibility = View.VISIBLE
                supportFragmentManager.commit {
                    setCustomAnimations(
                        R.anim.slide_in_up,
                        R.anim.stay_still,
                        R.anim.stay_still,
                        R.anim.slide_out_down
                    )
                    add(R.id.history_fragment_container, SummaryFragment(session, R.id.history_fragment_container))
                }
            }
        }
    }

    private fun addSection(title: String) {
        val sectionTitle = TextView(this@HistoryActivity)
        sectionTitle.text = title
        sectionTitle.style(R.style.menu_section_title_style)
        history_linear_layout.addView(sectionTitle)
        val divider = View(this@HistoryActivity)
        divider.style(R.style.menu_divider_style)
        history_linear_layout.addView(divider)
    }

    private fun deleteTrip(sessionId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            CarStatsViewer.tripDataSource.deleteDrivingSessionById(sessionId)
            runOnUiThread {
                val newIntent = Intent(intent)
                newIntent.putExtra("noTransition", true)
                finish();
                startActivity(newIntent);
            }
        }
    }

    private fun resetTrip(tripType: Int) {
        CoroutineScope(Dispatchers.Default).launch {
            val newIntent = Intent(intent)
            newIntent.putExtra("noTransition", true)
            runOnUiThread {
                history_linear_layout.removeAllViews()
            }
            (applicationContext as CarStatsViewer).dataProcessor.resetTrip(tripType, (applicationContext as CarStatsViewer).dataProcessor.realTimeData.drivingState)
            finish();
            startActivity(newIntent);
        }
    }

}