package com.ixam97.carStatsViewer.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.utils.StringFormatters
import com.ixam97.carStatsViewer.views.MultiLineRowButtonWidget
import kotlinx.android.synthetic.main.activity_history.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class HistoryActivity  : Activity() {

    private lateinit var context : Context
    private val appPreferences = CarStatsViewer.appPreferences

    override fun startActivity(intent: Intent?) {
        super.startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
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

        CoroutineScope(Dispatchers.IO).launch {
            val drivingSessions = CarStatsViewer.tripDataSource.getPastDrivingSessions().reversed()
            drivingSessions.forEach {
                val rowView = MultiLineRowButtonWidget(this@HistoryActivity)
                rowView.topText = "${StringFormatters.getDateString(Date(it.start_epoch_time))}, Type: ${tripTypeStringArray[it.session_type]}, ID: ${it.driving_session_id}"
                rowView.bottomText = String.format("%.1f km, %.1f kWh", it.driven_distance / 1000f, it.used_energy / 1000f)
                runOnUiThread {
                    history_linear_layout.addView(rowView)
                }
            }
        }
    }

}