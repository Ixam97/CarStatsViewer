package com.ixam97.carStatsViewer.activities

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.DatePicker
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.database.tripData.DrivingSession
import com.ixam97.carStatsViewer.database.tripData.TripType
import com.ixam97.carStatsViewer.fragments.SummaryFragment
import com.ixam97.carStatsViewer.utils.InAppLogger
import com.ixam97.carStatsViewer.views.TripHistoryAdapter
import kotlinx.android.synthetic.main.activity_history.*
import kotlinx.android.synthetic.main.activity_log.*
import kotlinx.coroutines.*
import java.util.*

class HistoryActivity  : FragmentActivity() {

    private lateinit var context : Context
    private val appPreferences = CarStatsViewer.appPreferences

    private var listAccess = false

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

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch { withContext(Dispatchers.IO) { reloadDataBase(true) }}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        context = applicationContext

        setContentView(R.layout.activity_history)

        history_trips_recycler_view.adapter = tripsAdapter
        history_trips_recycler_view.layoutManager = LinearLayoutManager(this@HistoryActivity)

        appPreferences.run {
            if (!tripFilterManual || !tripFilterAuto || !tripFilterCharge || !tripFilterMonth || tripFilterTime > 0) {
                history_button_filters.setColorFilter(CarStatsViewer.primaryColor, PorterDuff.Mode.SRC_IN)
            } else {
                history_button_filters.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
            }
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

    private fun createDeleteDialog(session: DrivingSession, position: Int? = null) {
        val builder = AlertDialog.Builder(this@HistoryActivity)
        builder.setTitle(getString(R.string.history_dialog_delete_title))
            .setCancelable(true)
            .setPositiveButton(getString(R.string.history_dialog_delete_confirm)) {_,_->
                deleteTrip(session, position)
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

    private fun deleteTrip(session: DrivingSession, position: Int? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            CarStatsViewer.tripDataSource.deleteDrivingSessionById(session.driving_session_id)
            (applicationContext as CarStatsViewer).dataProcessor.checkTrips()
            if ((session.end_epoch_time?:0) <= 0) {
                reloadDataBase(position == null)
                position?.let {
                    runOnUiThread { tripsAdapter.notifyItemChanged(it) }
                }
            } else {
                runOnUiThread {
                    if (position == null) {
                        drivingSessions.remove(session)
                        tripsAdapter.notifyDataSetChanged()
                    }
                    else {
                        tripsAdapter.removeAt(position)
                        if (drivingSessions.size <= 6) {
                            tripsAdapter.removeAt(5)
                        }
                    }
                }
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
            val sessionId = CarStatsViewer.tripDataSource.getActiveDrivingSessionsIdsMap()[tripType]
            val currentPosition = drivingSessions.indices.find {sessionId == drivingSessions[it].driving_session_id}
            (applicationContext as CarStatsViewer).dataProcessor.resetTrip(tripType, (applicationContext as CarStatsViewer).dataProcessor.realTimeData.drivingState)
            reloadDataBase(false)
            val pastPosition = drivingSessions.indices.find {sessionId == drivingSessions[it].driving_session_id}
            runOnUiThread {
                if (currentPosition == null || pastPosition == null) {
                    tripsAdapter.notifyDataSetChanged()
                } else {
                    tripsAdapter.notifyItemChanged(currentPosition)
                    tripsAdapter.notifyItemInserted(pastPosition)
                }
            }
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
                // if (appPreferences.tripFilterManual != manualCheckBox.isChecked && !manualCheckBox.isChecked) {
                //     val indexList = drivingSessions.indices.filter { drivingSessions[it].session_type == TripType.MANUAL && (drivingSessions[it].end_epoch_time?:0) > 0}.toList()
                //     drivingSessions.removeIf { it.session_type == TripType.MANUAL }
                //     indexList.reversed().forEach {
                //         tripsAdapter.notifyItemRemoved(it)
                //     }
                // }
                val filterTime = if (dateCheckbox.isChecked) {
                    val date = Calendar.getInstance()
                    date.set(datePicker.year, datePicker.month, datePicker.dayOfMonth)
                    date.set(Calendar.HOUR_OF_DAY, 0)
                    date.set(Calendar.MINUTE, 0)
                    date.set(Calendar.SECOND, 0)
                    date.timeInMillis
                } else 0L

                applyFilters(
                    manualCheckBox.isChecked,
                    chargeCheckBox.isChecked,
                    autoCheckBox.isChecked,
                    monthCheckBox.isChecked,
                    filterTime
                )
                // appPreferences.run {
                //     if (!tripFilterManual || !tripFilterAuto || !tripFilterCharge || !tripFilterMonth || tripFilterTime > 0) {
                //         history_button_filters.setColorFilter(CarStatsViewer.primaryColor, PorterDuff.Mode.SRC_IN)
                //     } else {
                //         history_button_filters.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
                //     }
                // }
            }
            setTitle(getString(R.string.history_dialog_filters_title))
            setCancelable(true)
        }
        filtersDialog.create()
        filtersDialog.show()
    }

    private suspend fun reloadDataBase(notify: Boolean = true) {
        InAppLogger.d("[Trip History] Reloading data base in trip history")
        val currentDrivingSessions = CarStatsViewer.tripDataSource.getActiveDrivingSessions().sortedBy { it.session_type }.toMutableList()
        val pastDrivingSessions = getFilteredPastTrips().toMutableList()

        /** Add dummy sessions to the beginning of each list to generate list dividers */
        currentDrivingSessions.add(0, currentDrivingSessions[0].copy(
            session_type = 5,
            note = getString(R.string.history_current_trips),
            driving_session_id = 0
        ))
        if (pastDrivingSessions.isNotEmpty())
            pastDrivingSessions.add(0, pastDrivingSessions[0].copy(
                session_type = 5,
                note = getString(R.string.history_past_trips),
                driving_session_id = 0
            ))

        drivingSessions.clear()
        drivingSessions.addAll(currentDrivingSessions + pastDrivingSessions)


        runOnUiThread {
            tripsAdapter.notifyItemChanged(5)
            if (notify) tripsAdapter.notifyDataSetChanged()

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



    private fun applyFilters(
        filterManual: Boolean,
        filterCharge: Boolean,
        filterAuto: Boolean,
        filterMonth: Boolean,
        filterTime: Long
    ) {

        val completeRemoveIndexList = mutableListOf<Int>()
        val completeAddIndexList = mutableListOf<Int>()

        fun checkFilterApplied(prefFilter: Boolean, filter: Boolean, type: Int) {
            if (prefFilter != filter && !filter) {
                val indexList = drivingSessions.indices.filter {
                    drivingSessions[it].session_type == type
                    && (drivingSessions[it].end_epoch_time?:0) > 0
                }.toList()
                completeRemoveIndexList.addAll(indexList)
            }
        }

        fun checkFilterRemoved(prefFilter: Boolean, filter: Boolean, type: Int) {
            if (prefFilter != filter && filter) {
                val indexList = drivingSessions.indices.filter {
                    drivingSessions[it].session_type == type
                    && (drivingSessions[it].end_epoch_time?:0) > 0
                }.toList()
                completeAddIndexList.addAll(indexList)
            }
        }

        val prefFilterManual = appPreferences.tripFilterManual
        val prefFilterCharge = appPreferences.tripFilterCharge
        val prefFilterAuto = appPreferences.tripFilterAuto
        val prefFilterMonth = appPreferences.tripFilterMonth
        val prefFilterTime = appPreferences.tripFilterTime

        appPreferences.tripFilterManual = filterManual
        appPreferences.tripFilterCharge = filterCharge
        appPreferences.tripFilterAuto = filterAuto
        appPreferences.tripFilterMonth = filterMonth
        appPreferences.tripFilterTime = filterTime

        checkFilterApplied(prefFilterManual, filterManual, TripType.MANUAL)
        checkFilterApplied(prefFilterCharge, filterCharge, TripType.SINCE_CHARGE)
        checkFilterApplied(prefFilterAuto, filterAuto, TripType.AUTO)
        checkFilterApplied(prefFilterMonth, filterMonth, TripType.MONTH)

        if (prefFilterTime < filterTime) {
            val indexList = drivingSessions.indices.filter {drivingSessions[it].start_epoch_time < filterTime && (drivingSessions[it].end_epoch_time?:0) > 0 && drivingSessions[it].session_type != 5}.toList()
            completeRemoveIndexList.addAll(indexList)
        }

        InAppLogger.d("[Trip History] Remove index list: ${completeRemoveIndexList.sortedBy { it }.reversed().distinct()}")
            completeRemoveIndexList.sortedBy { it }.reversed().distinct().forEach {
                InAppLogger.d("[Trip History] Removing index $it")
                drivingSessions.removeAt(it)
                tripsAdapter.notifyItemRemoved(it)
                if (it == 5 || it == 0) throw Exception("[Trip History] Trip history index must not be 0 or 5!")
            }

        lifecycleScope.launch { withContext(Dispatchers.IO) {

            InAppLogger.d("[Trip History] Animation duration: ${history_trips_recycler_view.itemAnimator?.removeDuration?:500}")
            delay((history_trips_recycler_view.itemAnimator?.removeDuration?:500) + 100)

            reloadDataBase(false)

            checkFilterRemoved(prefFilterManual, filterManual, TripType.MANUAL)
            checkFilterRemoved(prefFilterCharge, filterCharge, TripType.SINCE_CHARGE)
            checkFilterRemoved(prefFilterAuto, filterAuto, TripType.AUTO)
            checkFilterRemoved(prefFilterMonth, filterMonth, TripType.MONTH)

            if (prefFilterTime > filterTime) {
                val indexList = drivingSessions.indices.filter { drivingSessions[it].start_epoch_time in filterTime until prefFilterTime && (drivingSessions[it].end_epoch_time?:0) > 0 && drivingSessions[it].session_type != 5}.toList()
                completeAddIndexList.addAll(indexList)
            }

            runOnUiThread {
                InAppLogger.d("[Trip history] Added index list: ${completeAddIndexList.sortedBy { it }.distinct()}")
                completeAddIndexList.sortedBy { it }.distinct().forEach {
                    InAppLogger.d("[Trip history] Adding index $it")
                    tripsAdapter.notifyItemInserted(it)
                }
            }

        }}

        appPreferences.run {
            if (!tripFilterManual || !tripFilterAuto || !tripFilterCharge || !tripFilterMonth || tripFilterTime > 0) {
                history_button_filters.setColorFilter(CarStatsViewer.primaryColor, PorterDuff.Mode.SRC_IN)
            } else {
                history_button_filters.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
            }
        }
    }

}