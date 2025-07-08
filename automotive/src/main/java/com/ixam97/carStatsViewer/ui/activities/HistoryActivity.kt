package com.ixam97.carStatsViewer.ui.activities

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.DatePicker
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ixam97.carStatsViewer.BuildConfig
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.adapters.TripHistoryAdapter
import com.ixam97.carStatsViewer.compose.ComposeTripDetailsActivity
import com.ixam97.carStatsViewer.database.tripData.DrivingSession
import com.ixam97.carStatsViewer.databinding.ActivityHistoryBinding
import com.ixam97.carStatsViewer.liveDataApi.ConnectionStatus
import com.ixam97.carStatsViewer.liveDataApi.uploadService.UploadService
import com.ixam97.carStatsViewer.ui.views.SnackbarWidget
import com.ixam97.carStatsViewer.ui.views.TripHistoryRowWidget
import com.ixam97.carStatsViewer.utils.InAppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class HistoryActivity  : FragmentActivity() {

    private lateinit var context : Context
    private lateinit var binding: ActivityHistoryBinding
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
                if (BuildConfig.FLAVOR_aaos != "carapp")
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        } else {
            if (BuildConfig.FLAVOR_aaos != "carapp")
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    override fun onResume() {
        super.onResume()
        with(binding){
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    runOnUiThread { tripHistoryProgressBar.visibility = View.VISIBLE }
                    tripsAdapter.reloadDataBase()
                    runOnUiThread { tripHistoryProgressBar.visibility = View.GONE }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (CarStatsViewer.appPreferences.colorTheme > 0) setTheme(R.style.ColorTestTheme)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        val view = binding.root

        context = applicationContext

        setContentView(view)

        with(binding){
            historyTripsRecyclerView.adapter = tripsAdapter
            historyTripsRecyclerView.layoutManager = LinearLayoutManager(this@HistoryActivity)

            appPreferences.run {
                if (!tripFilterManual || !tripFilterAuto || !tripFilterCharge || !tripFilterMonth || tripFilterTime > 0) {
                    historyButtonFilters.setImageDrawable(getDrawable(R.drawable.ic_filter_active))
                } else {
                    historyButtonFilters.setImageDrawable(getDrawable(R.drawable.ic_filter))
                }
            }

            historyButtonBack.setOnClickListener {
                finish()
                if (BuildConfig.FLAVOR_aaos != "carapp")
                    overridePendingTransition(R.anim.stay_still, R.anim.slide_out_right)
            }

            historyButtonFilters.setOnClickListener {
                createFilterDialog()
            }

            historyMultiDelete.setOnClickListener {
                createMultiDeleteDialog()
            }

            if (CarStatsViewer.liveDataApis[1].connectionStatus == ConnectionStatus.UNUSED) {
                historyButtonUpload.isEnabled = false
                historyButtonUpload.setColorFilter(
                    getColor(R.color.disabled_tint),
                    PorterDuff.Mode.SRC_IN
                )
            }

            historyButtonUpload.setOnClickListener {
                openUploadDialog()
            }
        }

    }

    private fun openSummary(sessionId: Long) {

         val summaryIntent =
             Intent(this, ComposeTripDetailsActivity::class.java)
         summaryIntent.putExtra("SessionId", sessionId)
         startActivity(summaryIntent)
    }

    private fun startMultiSelectMode(sessionId: Long, widget: TripHistoryRowWidget) {
        multiSelectMode = true
        selectTrip(sessionId, widget)
    }

    private fun setMultiSelectVisibility() {
        binding.historyMultiContainer?.let {
            if (multiSelectMode) {
                it.visibility = View.VISIBLE
                binding.historyButtonFilters.visibility = View.GONE
            } else {
                it.visibility = View.GONE
                binding.historyButtonFilters.visibility = View.VISIBLE
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
        binding.historyMultiInfo.text = "${getString(R.string.history_selected)} ${selectedIds.size}"
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
                    runOnUiThread { binding.tripHistoryProgressBar.visibility = View.VISIBLE }
                    tripsAdapter.resetTrip(tripType).join()
                    runOnUiThread { binding.tripHistoryProgressBar.visibility = View.GONE }
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
                    runOnUiThread { binding.tripHistoryProgressBar.visibility = View.VISIBLE }
                    selectedIds.forEach {
                        CarStatsViewer.tripDataSource.deleteDrivingSessionById(it)
                    }
                    selectedIds.clear()
                    tripsAdapter.reloadDataBase()
                    runOnUiThread {
                        multiSelectMode = false
                        binding.tripHistoryProgressBar.visibility = View.GONE
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
                    runOnUiThread { binding.tripHistoryProgressBar.visibility = View.VISIBLE }
                    selectedIds.forEach {
                        tripsAdapter.selectTrip(it, false)
                    }
                    selectedIds.clear()
                    tripsAdapter.reloadDataBase()
                    runOnUiThread {
                        tripsAdapter.notifyDataSetChanged()
                        multiSelectMode = false
                        binding.tripHistoryProgressBar.visibility = View.GONE
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
            runOnUiThread { binding.tripHistoryProgressBar.visibility = View.VISIBLE }
            tripsAdapter.reloadDataBase()
            runOnUiThread { binding.tripHistoryProgressBar.visibility = View.GONE }
        }}

        appPreferences.run {
            if (!tripFilterManual || !tripFilterAuto || !tripFilterCharge || !tripFilterMonth || tripFilterTime > 0) {
                binding.historyButtonFilters.setImageDrawable(getDrawable(R.drawable.ic_filter_active))
            } else {
                binding.historyButtonFilters.setImageDrawable(getDrawable(R.drawable.ic_filter))
            }
        }
    }

    private fun openUploadDialog() {
        val uploadDialog = AlertDialog.Builder(this@HistoryActivity).apply {
            setTitle(R.string.history_dialog_upload_title)
            setMessage(R.string.history_dialog_upload_message_2)
            setNegativeButton(R.string.dialog_reset_cancel) { _,_ ->

            }
            setPositiveButton(R.string.history_dialog_upload_upload_button) { _, _ ->
                uploadDatabase()
            }
        }
        uploadDialog.show()
    }

    private fun uploadDatabase() {
        val uploadIntent = Intent(context, UploadService::class.java)
        uploadIntent.putExtra("type", "full_db")
        startService(uploadIntent)
    }
}