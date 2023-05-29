package com.ixam97.carStatsViewer.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.airbnb.paris.extensions.style
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.utils.StringFormatters
import com.ixam97.carStatsViewer.views.MultiLineRowButtonWidget
import kotlinx.android.synthetic.main.activity_history.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

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

        history_button_reset.setOnClickListener {
            (applicationContext as CarStatsViewer).dataProcessor.resetManualTrip()
            CoroutineScope(Dispatchers.Default).launch {
                val newIntent = Intent(intent)
                newIntent.putExtra("noTransition", true)
                runOnUiThread {
                    history_linear_layout.removeAllViews()
                }
                delay(500)
                finish();
                startActivity(newIntent);
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            runOnUiThread {
                val currentSessionsHeadLine = TextView(this@HistoryActivity)
                currentSessionsHeadLine.text = "Current sessions:"
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
                    val rowView = MultiLineRowButtonWidget(this@HistoryActivity)
                    rowView.topText = "${StringFormatters.getDateString(Date(drivingSession.start_epoch_time))}, Type: ${tripTypeStringArray[drivingSession.session_type]}, ID: ${drivingSession.driving_session_id}"
                    rowView.bottomText = String.format("%.1f km, %.1f kWh", drivingSession.driven_distance / 1000f, drivingSession.used_energy / 1000f)
                    rowView.setOnClickListener {
                        CoroutineScope(Dispatchers.IO).launch {
                            //InAppLogger.v(GsonBuilder().setPrettyPrinting().create().toJson(CarStatsViewer.tripDataSource.getFullDrivingSession(drivingSession.driving_session_id)))
                            CarStatsViewer.tripDataSource.deleteDrivingSessionById(drivingSession.driving_session_id)
                            runOnUiThread {
                                val newIntent = Intent(intent)
                                newIntent.putExtra("noTransition", true)
                                finish();
                                startActivity(newIntent);
                            }
                        }
                    }
                    runOnUiThread {
                        history_linear_layout.addView(rowView)
                    }
                }
            }

            runOnUiThread {
                val pastSessionsHeadLine = TextView(this@HistoryActivity)
                pastSessionsHeadLine.text = "Past sessions:"
                pastSessionsHeadLine.style(R.style.menu_section_title_style)
                history_linear_layout.addView(pastSessionsHeadLine)
                val divider = View(this@HistoryActivity)
                divider.style(R.style.menu_divider_style)
                history_linear_layout.addView(divider)
            }

            val pastDrivingSessions = CarStatsViewer.tripDataSource.getPastDrivingSessions().sortedBy { it.start_epoch_time }.reversed()
            pastDrivingSessions.forEach { drivingSession ->
                val rowView = MultiLineRowButtonWidget(this@HistoryActivity)
                rowView.topText = "${StringFormatters.getDateString(Date(drivingSession.start_epoch_time))}, Type: ${tripTypeStringArray[drivingSession.session_type]}, ID: ${drivingSession.driving_session_id}"
                rowView.bottomText = String.format("%.1f km, %.1f kWh", drivingSession.driven_distance / 1000f, drivingSession.used_energy / 1000f)
                rowView.setOnClickListener {
                    CoroutineScope(Dispatchers.IO).launch {
                        //InAppLogger.v(GsonBuilder().setPrettyPrinting().create().toJson(CarStatsViewer.tripDataSource.getFullDrivingSession(drivingSession.driving_session_id)))
                        CarStatsViewer.tripDataSource.deleteDrivingSessionById(drivingSession.driving_session_id)
                        runOnUiThread {
                            val newIntent = Intent(intent)
                            newIntent.putExtra("noTransition", true)
                            finish();
                            startActivity(newIntent);
                        }
                    }
                }
                runOnUiThread {
                    history_linear_layout.addView(rowView)
                }
            }
        }
    }

}