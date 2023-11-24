package com.ixam97.carStatsViewer.ui.activities

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.DatePicker
import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.database.tripData.DrivingSession
import com.ixam97.carStatsViewer.ui.fragments.SummaryFragment
import com.ixam97.carStatsViewer.utils.InAppLogger
import com.ixam97.carStatsViewer.adapters.TripHistoryAdapter
import com.ixam97.carStatsViewer.database.tripData.DrivingPoint
import com.ixam97.carStatsViewer.liveDataApi.LiveDataApi
import com.ixam97.carStatsViewer.liveDataApi.http.HttpLiveData
import com.ixam97.carStatsViewer.ui.views.SnackbarWidget
import com.ixam97.carStatsViewer.ui.views.TripHistoryRowWidget
import com.ixam97.carStatsViewer.utils.applyTypeface
import com.ixam97.carStatsViewer.utils.setContentViewAndTheme
import kotlinx.android.synthetic.main.activity_history.*
import kotlinx.coroutines.*
import java.util.*
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.roundToInt

class HistoryActivity  : FragmentActivity() {

    private lateinit var context : Context
    private val appPreferences = CarStatsViewer.appPreferences

    private val tripsAdapter = TripHistoryAdapter(
        ::openSummary,
        ::rowEndButtonClick,
        ::rowEndButtonLongClick,
        ::createDeleteDialog,
        this
    )

    var multiSelectMode = false
        set(value) {
            field = value
            setMultiSelectVisibility()
            InAppLogger.d("[Trip History] Multi select mode: $field")
        }

    val selectedIds = mutableListOf<Long>()

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
        lifecycleScope.launch { withContext(Dispatchers.IO) {
            runOnUiThread { trip_history_progress_bar.visibility = View.VISIBLE }
            tripsAdapter.reloadDataBase()
            runOnUiThread { trip_history_progress_bar.visibility = View.GONE }
        }}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        context = applicationContext

        setContentViewAndTheme(this, R.layout.activity_history)

        history_trips_recycler_view.adapter = tripsAdapter
        history_trips_recycler_view.layoutManager = LinearLayoutManager(this@HistoryActivity)

        appPreferences.run {
            if (!tripFilterManual || !tripFilterAuto || !tripFilterCharge || !tripFilterMonth || tripFilterTime > 0) {
                history_button_filters.setImageDrawable(getDrawable(R.drawable.ic_filter_active))
            } else {
                history_button_filters.setImageDrawable(getDrawable(R.drawable.ic_filter))
            }
        }

        history_button_back.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.stay_still, R.anim.slide_out_right)
        }

        history_button_filters.setOnClickListener {
            createFilterDialog()
        }

        history_multi_delete.setOnClickListener {
            createMultiDeleteDialog()
        }

        CarStatsViewer.typefaceMedium?.let {
            applyTypeface(history_activity)
        }

        history_button_upload.setOnClickListener {
            openUploadDialog()
        }

    }

    private fun openSummary(sessionId: Long) {

        CoroutineScope(Dispatchers.IO).launch {
            val session = CarStatsViewer.tripDataSource.getDrivingSession(sessionId) ?: return@launch
            if ((appPreferences.mainViewTrip + 1) != session.session_type && (session.end_epoch_time?:0) <= 0) {
                appPreferences.mainViewTrip = session.session_type - 1
                CarStatsViewer.dataProcessor.changeSelectedTrip(session.session_type)
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
                    add(R.id.history_fragment_container, SummaryFragment(session))
                }
            }
        }
    }

    private fun startMultiSelectMode(sessionId: Long, widget: TripHistoryRowWidget) {
        multiSelectMode = true
        selectTrip(sessionId, widget)
    }

    private fun setMultiSelectVisibility() {
        history_multi_container?.let {
            if (multiSelectMode) {
                it.visibility = View.VISIBLE
                history_button_filters.visibility = View.GONE
            } else {
                it.visibility = View.GONE
                history_button_filters.visibility = View.VISIBLE
            }
        }
    }

    private fun selectTrip(sessionId: Long, widget: TripHistoryRowWidget) {
        if (!selectedIds.contains(sessionId)) {
            widget.setDeleteMarker(true)
            tripsAdapter.selectTrip(sessionId, true)
            selectedIds.add(sessionId)
        } else {
            widget.setDeleteMarker(false)
            tripsAdapter.selectTrip(sessionId, false)
            selectedIds.remove(sessionId)
        }
        history_multi_info.text = "${getString(R.string.history_selected)} ${selectedIds.size}"
        InAppLogger.d("[Trip History] selected IDs: $selectedIds")
    }

    private fun rowEndButtonClick(widget: TripHistoryRowWidget, position: Int) {
        val session = widget.getSession() ?: return
        if (multiSelectMode && (session.end_epoch_time?:0)  > 0) {
            selectTrip(session.driving_session_id, widget)
            if (selectedIds.size <= 0) multiSelectMode = false
        } else {
            if ((session.end_epoch_time?:0)  <= 0) {
                createResetDialog(session.session_type)
            } else {
                createDeleteDialog(session, position)
            }
        }
    }

    private fun rowEndButtonLongClick(widget: TripHistoryRowWidget, position: Int) {
        val session = widget.getSession() ?: return
        if ((session.end_epoch_time?:0) > 0) {
            startMultiSelectMode(session.driving_session_id, widget)
        } else {
            createResetDialog(session.session_type)
        }
    }

    private fun createResetDialog(tripType: Int) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.dialog_reset_title))
            .setMessage(getString(R.string.dialog_reset_message))
            .setCancelable(true)
            .setPositiveButton(getString(R.string.dialog_reset_confirm)) { _, _ ->
                lifecycleScope.launch{ withContext(Dispatchers.IO) {
                    runOnUiThread { trip_history_progress_bar.visibility = View.VISIBLE }
                    tripsAdapter.resetTrip(tripType).join()
                    runOnUiThread { trip_history_progress_bar.visibility = View.GONE }
                }}
            }
            .setNegativeButton(getString(R.string.dialog_reset_cancel)) { dialog, _ ->
                dialog.cancel()
            }
        val alert = builder.create()
        alert.show()
    }

    private fun createDeleteDialog(session: DrivingSession, position: Int) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.history_dialog_delete_title))
            .setCancelable(true)
            .setPositiveButton(getString(R.string.history_dialog_delete_confirm)) {_,_->
                tripsAdapter.deleteTrip(session, position)
                SnackbarWidget.Builder(this@HistoryActivity, "Trip has been deleted.")
                    .setDuration(3000)
                    .setButton("OK")
                    .setStartDrawable(R.drawable.ic_delete)
                    .show()
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

    private fun createMultiDeleteDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.history_dialog_multi_delete_title))
            .setCancelable(true)
            .setPositiveButton(getString(R.string.history_dialog_multi_delete_delete, selectedIds.size.toString())) {_,_->
                lifecycleScope.launch { withContext(Dispatchers.IO) {
                    val numSelected = selectedIds.size
                    runOnUiThread { trip_history_progress_bar.visibility = View.VISIBLE }
                    selectedIds.forEach {
                        CarStatsViewer.tripDataSource.deleteDrivingSessionById(it)
                    }
                    selectedIds.clear()
                    tripsAdapter.reloadDataBase()
                    runOnUiThread {
                        multiSelectMode = false
                        trip_history_progress_bar.visibility = View.GONE
                        SnackbarWidget.Builder(this@HistoryActivity, "$numSelected trips have been deleted.")
                            .setDuration(3000)
                            .setButton("OK")
                            .setStartDrawable(R.drawable.ic_delete)
                            .show()
                    }
                }}
            }
            .setNeutralButton(getString(R.string.history_dialog_multi_delete_deselect)) { dialog, _ ->
                lifecycleScope.launch { withContext(Dispatchers.IO) {
                    runOnUiThread { trip_history_progress_bar.visibility = View.VISIBLE }
                    selectedIds.forEach {
                        tripsAdapter.selectTrip(it, false)
                    }
                    selectedIds.clear()
                    tripsAdapter.reloadDataBase()
                    runOnUiThread {
                        tripsAdapter.notifyDataSetChanged()
                        multiSelectMode = false
                        trip_history_progress_bar.visibility = View.GONE
                    }
                }}
                dialog.cancel()
            }
            .setNegativeButton(getString(R.string.dialog_reset_cancel)) { dialog, _ ->
                dialog.cancel()
            }
            .setMessage(getString(R.string.history_dialog_multi_delete_message, selectedIds.size.toString()))
        val alert = builder.create()
        alert.show()
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

            setPositiveButton(getString(R.string.dialog_apply)) { _, _ ->
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
            }
            setTitle(getString(R.string.history_dialog_filters_title))
            setCancelable(true)
        }
        filtersDialog.create()
        filtersDialog.show()
    }

    private fun applyFilters(
        filterManual: Boolean,
        filterCharge: Boolean,
        filterAuto: Boolean,
        filterMonth: Boolean,
        filterTime: Long
    ) {
        appPreferences.tripFilterManual = filterManual
        appPreferences.tripFilterCharge = filterCharge
        appPreferences.tripFilterAuto = filterAuto
        appPreferences.tripFilterMonth = filterMonth
        appPreferences.tripFilterTime = filterTime

        lifecycleScope.launch { withContext(Dispatchers.IO) {
            runOnUiThread { trip_history_progress_bar.visibility = View.VISIBLE }
            tripsAdapter.reloadDataBase()
            runOnUiThread { trip_history_progress_bar.visibility = View.GONE }
        }}

        appPreferences.run {
            if (!tripFilterManual || !tripFilterAuto || !tripFilterCharge || !tripFilterMonth || tripFilterTime > 0) {
                history_button_filters.setImageDrawable(getDrawable(R.drawable.ic_filter_active))
            } else {
                history_button_filters.setImageDrawable(getDrawable(R.drawable.ic_filter))
            }
        }
    }

    private fun openUploadDialog() {
        var uploadDialog = AlertDialog.Builder(this@HistoryActivity).apply {
            setTitle(R.string.history_dialog_upload_title)
            // setMessage("You are about to upload the entire local database to the API endpoint"+
            //         " specified in the HTTP webhook settings!\n\nMake sure you have reset the"+
            //         " data on the server before this to prevent data duplication. If supported by" +
            //         " the API endpoint this will happen automatically \n\n" +
            //         " This action may take a long time to finish depending on the database size." +
            //         " Please remain on this page until a notification is shown!")
            setMessage(R.string.history_dialog_upload_message)
            setNegativeButton(R.string.dialog_reset_cancel) { _,_ ->

            }
            setPositiveButton(R.string.history_dialog_upload_upload_button) { _, _ ->
                uploadDatabase()
            }
        }
        uploadDialog.show()
    }

    private fun uploadDatabase() {
        val chunkSize = 250
        CoroutineScope(Dispatchers.IO).launch {
            val waitSnack = SnackbarWidget.Builder(this@HistoryActivity, "Upload in progress, 0%")
                .build()
            runOnUiThread {
                this@HistoryActivity.window.findViewById<FrameLayout>(android.R.id.content).addView(waitSnack)
            }
            val drivingPoints = CarStatsViewer.tripDataSource.getAllDrivingPoints()
            InAppLogger.i("[HIST] Done loading ${drivingPoints.size} driving points")
            val chargingSessions = CarStatsViewer.tripDataSource.getAllChargingSessions()
            InAppLogger.i("[HIST] Done loading ${chargingSessions.size} charging sessions")

            val drivingPointsChunks = ceil(drivingPoints.size.toFloat() / chunkSize).roundToInt()
            InAppLogger.i("Divided ${drivingPoints.size} driving points into to $drivingPointsChunks chunks")
            val chargingSessionsSize = chargingSessions.size
            val totalParts = drivingPointsChunks + chargingSessionsSize

            var result: LiveDataApi.ConnectionStatus? = null

            for (i in 0 until drivingPointsChunks) {
                result = (CarStatsViewer.liveDataApis[1] as HttpLiveData).sendWithDrivingPoint(
                    CarStatsViewer.dataProcessor.realTimeData,
                    drivingPoints.slice(chunkSize * i..min(chunkSize * (i+1), drivingPoints.size - 1))
                )
                val percentage = (((i + 1).toFloat() / totalParts) * 100).roundToInt()
                if (result == LiveDataApi.ConnectionStatus.CONNECTED) {
                    InAppLogger.v("Chunk $i transferred, $percentage%")
                    runOnUiThread {
                        waitSnack.updateMessage("Upload in progress, $percentage%")
                        waitSnack.setProgressBarPercent(percentage)
                    }
                } else {
                    InAppLogger.e("Chunk $i failed..")
                    break
                }
            }

            for (i in 0 until chargingSessionsSize) {
                result = (CarStatsViewer.liveDataApis[1] as HttpLiveData).sendWithDrivingPoint(
                    CarStatsViewer.dataProcessor.realTimeData,
                    chargingSessions = chargingSessions.slice(setOf(i))
                )
                val percentage = (((i + 1 + drivingPointsChunks).toFloat() / totalParts) * 100).roundToInt()
                if (result == LiveDataApi.ConnectionStatus.CONNECTED) {
                    InAppLogger.v("Charging session $i transferred, $percentage%")
                    runOnUiThread {
                        waitSnack.updateMessage("Upload in progress, $percentage%")
                        waitSnack.setProgressBarPercent(percentage)
                    }
                } else {
                    InAppLogger.e("Charging session $i failed..")
                    break
                }
            }
/*
            val result = (CarStatsViewer.liveDataApis[1] as HttpLiveData).sendWithDrivingPoint(
                CarStatsViewer.dataProcessor.realTimeData,
                drivingPoints,
                chargingSessions
            )

 */

            runOnUiThread {
                this@HistoryActivity.window.findViewById<FrameLayout>(android.R.id.content).removeView(waitSnack)
                when (result) {
                    LiveDataApi.ConnectionStatus.CONNECTED, LiveDataApi.ConnectionStatus.LIMITED -> {
                        SnackbarWidget.Builder(this@HistoryActivity, "Database uploaded successfully.")
                            .setIsError(false)
                            .setDuration(3000)
                            .setStartDrawable(R.drawable.ic_upload)
                            .show()
                    }
                    else ->{
                        SnackbarWidget.Builder(this@HistoryActivity, "Database upload failed.")
                            .setIsError(true)
                            .setDuration(3000)
                            .show()
                    }
                }
            }

        }

    }
}