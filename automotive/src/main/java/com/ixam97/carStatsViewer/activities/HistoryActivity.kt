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
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.paris.extensions.style
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.database.tripData.DrivingSession
import com.ixam97.carStatsViewer.database.tripData.TripType
import com.ixam97.carStatsViewer.fragments.SummaryFragment
import com.ixam97.carStatsViewer.utils.InAppLogger
import com.ixam97.carStatsViewer.views.TripHistoryAdapter
import kotlinx.android.synthetic.main.activity_history.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class HistoryActivity  : FragmentActivity() {

    private lateinit var context : Context
    private val appPreferences = CarStatsViewer.appPreferences

    private var drivingSessions = mutableListOf<DrivingSession>()
    private val tripsAdapter = TripHistoryAdapter(
        drivingSessions,
        ::openSummary,
        ::createDeleteDialog,
        ::createResetDialog
    )

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

        history_trips_recycler_view.adapter = tripsAdapter
        history_trips_recycler_view.layoutManager = LinearLayoutManager(this@HistoryActivity)

        CoroutineScope(Dispatchers.IO).launch {
            reloadDataBase()
        }

        history_button_back.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.stay_still, R.anim.slide_out_right)
        }

        history_button_filters.setOnClickListener {
            createFilterDialog()
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

    private fun createDeleteDialog(session: DrivingSession) {
        val builder = AlertDialog.Builder(this@HistoryActivity)
        builder.setTitle(getString(R.string.history_dialog_delete_title))
            .setCancelable(true)
            .setPositiveButton(getString(R.string.history_dialog_delete_confirm)) {_,_->
                deleteTrip(session)
            }
            .setNegativeButton(getString(R.string.dialog_reset_cancel)) { dialog, _ ->
                dialog.cancel()
            }
        if ((session.end_epoch_time?:0) <= 0) {
            builder.setMessage("You are about to delete an active Trip! This may cause unwanted behaviour and should only be used for bugged trips!")
        } else {
            builder.setMessage(R.string.history_dialog_delete_message)
        }
        val alert = builder.create()
        alert.show()
    }

    private fun deleteTrip(session: DrivingSession) {
        CoroutineScope(Dispatchers.IO).launch {
            CarStatsViewer.tripDataSource.deleteDrivingSessionById(session.driving_session_id)
            (applicationContext as CarStatsViewer).dataProcessor.checkTrips()
            if ((session.end_epoch_time?:0) > 0){
                drivingSessions.remove(session)
                runOnUiThread {
                    tripsAdapter.notifyDataSetChanged()
                }
            }
            else {
                reloadDataBase()
            }
        }
    }

    private fun createResetDialog(tripType: Int) {
        val builder = AlertDialog.Builder(this@HistoryActivity)
        builder.setTitle(getString(R.string.dialog_reset_title))
            .setMessage(getString(R.string.dialog_reset_message))
            .setCancelable(true)
            .setPositiveButton(getString(R.string.dialog_reset_confirm)) { _, _ ->
                resetTrip(tripType)
            }
            .setNegativeButton(getString(R.string.dialog_reset_cancel)) { dialog, _ ->
                dialog.cancel()
            }
        val alert = builder.create()
        alert.show()
    }

    private fun resetTrip(tripType: Int) {
        CoroutineScope(Dispatchers.Default).launch {
            (applicationContext as CarStatsViewer).dataProcessor.resetTrip(tripType, (applicationContext as CarStatsViewer).dataProcessor.realTimeData.drivingState)
            reloadDataBase()
        }
    }

    private fun createFilterDialog() {

        val filtersDialog = AlertDialog.Builder(this@HistoryActivity).apply {
            val layout = LayoutInflater.from(this@HistoryActivity)
                .inflate(R.layout.dialog_history_filters, null)

            val manualCheckBox = layout.findViewById<CheckBox>(R.id.history_filter_checkbox_manual)
            val chargeCheckBox = layout.findViewById<CheckBox>(R.id.history_filter_checkbox_charge)
            val autoCheckBox = layout.findViewById<CheckBox>(R.id.history_filter_checkbox_auto)
            val monthCheckBox = layout.findViewById<CheckBox>(R.id.history_filter_checkbox_month)
            val dateCheckbox = layout.findViewById<CheckBox>(R.id.history_filter_checkbox_date)
            val datePicker = layout.findViewById<DatePicker>(R.id.history_filter_date_picker)

            manualCheckBox.isChecked = appPreferences.tripFilterManual
            chargeCheckBox.isChecked = appPreferences.tripFilterCharge
            autoCheckBox.isChecked = appPreferences.tripFilterAuto
            monthCheckBox.isChecked = appPreferences.tripFilterMonth
            dateCheckbox.isChecked = appPreferences.tripFilterTime > 0

            if (appPreferences.tripFilterTime > 0) {
                val date = Calendar.getInstance().apply{timeInMillis = appPreferences.tripFilterTime}

                datePicker.updateDate(
                    date.get(Calendar.YEAR),
                    date.get(Calendar.MONTH),
                    date.get(Calendar.DAY_OF_MONTH)
                )
            }

            setView(layout)

            setPositiveButton(getString(R.string.dialog_apply)) { dialog, _ ->
                appPreferences.tripFilterManual = manualCheckBox.isChecked
                appPreferences.tripFilterCharge = chargeCheckBox.isChecked
                appPreferences.tripFilterAuto = autoCheckBox.isChecked
                appPreferences.tripFilterMonth = monthCheckBox.isChecked
                if (dateCheckbox.isChecked) {
                    val date = Calendar.getInstance()
                    date.set(datePicker.year, datePicker.month, datePicker.dayOfMonth)
                    appPreferences.tripFilterTime = date.timeInMillis
                } else appPreferences.tripFilterTime = 0L
                CoroutineScope(Dispatchers.IO).launch {
                    reloadDataBase()
                }
            }
            setTitle(getString(R.string.history_dialog_filters_title))
            setCancelable(true)
        }
        filtersDialog.create()
        filtersDialog.show()
    }

    private suspend fun reloadDataBase() {
        val currentDrivingSessions = CarStatsViewer.tripDataSource.getActiveDrivingSessions().sortedBy { it.session_type }
        val pastDrivingSessions = getFilteredPastTrips()
        drivingSessions.clear()
        drivingSessions.addAll(currentDrivingSessions + pastDrivingSessions)
        runOnUiThread {
            tripsAdapter.notifyDataSetChanged()
        }
    }

    private suspend fun getFilteredPastTrips(): List<DrivingSession> {
        val pastDrivingSessions = CarStatsViewer.tripDataSource.getPastDrivingSessions().sortedBy { it.start_epoch_time }.reversed().toMutableList()
        return pastDrivingSessions.run {
            if (!appPreferences.tripFilterManual) removeIf { it.session_type == TripType.MANUAL }
            if (!appPreferences.tripFilterCharge) removeIf { it.session_type == TripType.SINCE_CHARGE }
            if (!appPreferences.tripFilterAuto) removeIf { it.session_type == TripType.AUTO }
            if (!appPreferences.tripFilterMonth) removeIf { it.session_type == TripType.MONTH }
            if (appPreferences.tripFilterTime > 0) removeIf { it.start_epoch_time < appPreferences.tripFilterTime }
            this
        }

    }

}