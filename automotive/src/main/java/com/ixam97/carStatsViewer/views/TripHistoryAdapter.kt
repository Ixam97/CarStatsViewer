package com.ixam97.carStatsViewer.views

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.database.tripData.DrivingSession
import com.ixam97.carStatsViewer.utils.InAppLogger

class TripHistoryAdapter(
    var sessions: MutableList<DrivingSession>,
    val openSummary: (sessionId: Long) -> Unit,
    val deleteTrip: (session: DrivingSession, position: Int?) -> Unit,
    val resetTrip: (tripType: Int) -> Unit
    ):
    RecyclerView.Adapter<TripHistoryAdapter.TripHistoryViewHolder>() {

    inner class TripHistoryViewHolder(val tripView: TripHistoryRowWidget): RecyclerView.ViewHolder(tripView)

    fun removeAt(index: Int) {
        sessions.removeAt(index)
        notifyItemRemoved(index)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripHistoryViewHolder {
        val tripView = TripHistoryRowWidget(parent.context)
        tripView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return TripHistoryViewHolder(tripView)
    }

    override fun onBindViewHolder(holder: TripHistoryViewHolder, position: Int) {
        val session = sessions[position]

        holder.tripView.setSession(session)

        holder.tripView.setOnMainClickListener {
            openSummary(session.driving_session_id)
        }

        if ((session.end_epoch_time?:0) > 0) {
            holder.tripView.setOnDeleteClickListener {
                deleteTrip(session, sessions.lastIndexOf(session))
            }
        } else {
            holder.tripView.setOnMainLongClickListener {
                deleteTrip(session, position)
            }
            holder.tripView.setOnDeleteClickListener {
                resetTrip(session.session_type)
            }
        }
    }

    override fun getItemCount() = sessions.size
}