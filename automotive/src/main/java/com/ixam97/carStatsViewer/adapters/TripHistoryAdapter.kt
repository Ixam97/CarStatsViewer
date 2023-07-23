package com.ixam97.carStatsViewer.adapters

import android.app.ActionBar.LayoutParams
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.ixam97.carStatsViewer.CarStatsViewer
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.ui.activities.HistoryActivity
import com.ixam97.carStatsViewer.database.tripData.DrivingSession
import com.ixam97.carStatsViewer.database.tripData.TripType
import com.ixam97.carStatsViewer.ui.views.TripHistoryRowWidget
import com.ixam97.carStatsViewer.utils.InAppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class TripHistoryAdapter(
    val openSummary: (sessionId: Long) -> Unit,
    val endClickFun: (widget: TripHistoryRowWidget, position: Int) -> Unit,
    val endLongClickFun: (widget: TripHistoryRowWidget, position: Int) -> Unit,
    val mainLongClickFun: (session: DrivingSession, position: Int) -> Unit,
    val activity: HistoryActivity
    ):
    RecyclerView.Adapter<TripHistoryAdapter.TripHistoryViewHolder>() {

    var sessions: List<DrivingSession> = listOf()
    val appPreferences = CarStatsViewer.appPreferences

    inner class TripHistoryViewHolder(val tripView: TripHistoryRowWidget): RecyclerView.ViewHolder(tripView)

    private val differCallback = object: DiffUtil.ItemCallback<DrivingSession>() {
        override fun areItemsTheSame(
            oldItem: DrivingSession,
            newItem: DrivingSession
        ): Boolean {
            return oldItem.driving_session_id == newItem.driving_session_id
        }

        override fun areContentsTheSame(
            oldItem: DrivingSession,
            newItem: DrivingSession
        ): Boolean {
            return oldItem == newItem
        }

    }

    private val differ = AsyncListDiffer(this, differCallback)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripHistoryViewHolder {
        val tripView = TripHistoryRowWidget(parent.context)
        tripView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return TripHistoryViewHolder(tripView)
    }

    override fun onBindViewHolder(holder: TripHistoryViewHolder, position: Int) {
        val session = differ.currentList[position]

        if (activity.selectedIds.contains(session.driving_session_id))
            session.deleteMarker = true

        holder.tripView.setSession(session)
        if (position == 0) {
            val params = LayoutParams(holder.tripView.layoutParams)
            params.topMargin = 10
            holder.tripView.layoutParams = params
        }

        holder.tripView.setOnMainClickListener { openSummary(session.driving_session_id) }
        holder.tripView.setOnMainLongClickListener { mainLongClickFun(session, position) }

        holder.tripView.setOnRowEndClickListener { endClickFun(holder.tripView, position) }
        holder.tripView.setOnRowEndLongClickListener { endLongClickFun(holder.tripView, position) }
    }

    override fun getItemCount() = differ.currentList.size // = sessions.size

    fun selectTrip(sessionId: Long, isSelected: Boolean) {
        val position = differ.currentList.indexOfFirst { it.driving_session_id == sessionId }
        differ.currentList[position].deleteMarker = isSelected
    }

    fun deleteTrip(session: DrivingSession, position: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            CarStatsViewer.tripDataSource.deleteDrivingSessionById(session.driving_session_id)
            CarStatsViewer.dataProcessor.checkTrips()

            if ((session.end_epoch_time?:0) > 0) {
                val newSessions = sessions.toMutableList()
                newSessions.removeAt(position)
                if (newSessions.size <= 6) newSessions.removeAt(5)
                sessions = newSessions
                activity.runOnUiThread { differ.submitList(newSessions); InAppLogger.d("Submitting to differ") }
            } else {
                // val newSession = CarStatsViewer.tripDataSource.getActiveDrivingSessions().find { it.session_type == session.session_type }
                reloadDataBase()

                // if (newSession != null) {
                //     val newSessions = sessions.toMutableList()
                //     newSessions[position] = newSession
                //     sessions = newSessions
                //     activity.runOnUiThread { differ.submitList(sessions) }
                // } else {
                //     reloadDataBase()
                // }
            }
        }
    }

    fun resetTrip(tripType: Int): Job {
        return CoroutineScope(Dispatchers.Default).launch {
            CarStatsViewer.dataProcessor.resetTrip(tripType, CarStatsViewer.dataProcessor.realTimeData.drivingState)
            reloadDataBase()
        }
    }

    suspend fun reloadDataBase() {
        InAppLogger.v("[Trip History] Reloading data base in trip history")
        val currentDrivingSessions = CarStatsViewer.tripDataSource.getActiveDrivingSessions().sortedBy { it.session_type }.toMutableList()
        val pastDrivingSessions = getFilteredPastTrips().toMutableList()

        /** Add dummy sessions to the beginning of each list to generate list dividers */
        currentDrivingSessions.add(0, currentDrivingSessions[0].copy(
            session_type = 5,
            note = activity.getString(R.string.history_current_trips),
            driving_session_id = 0
        ))
        if (pastDrivingSessions.isNotEmpty())
            pastDrivingSessions.add(0, pastDrivingSessions[0].copy(
                session_type = 5,
                note = activity.getString(R.string.history_past_trips),
                driving_session_id = 0
            ))
        sessions = (currentDrivingSessions + pastDrivingSessions)

        activity.runOnUiThread { differ.submitList(sessions) }
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