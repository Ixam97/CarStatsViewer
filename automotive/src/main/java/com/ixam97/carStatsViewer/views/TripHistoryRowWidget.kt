package com.ixam97.carStatsViewer.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.ixam97.carStatsViewer.R
import com.ixam97.carStatsViewer.database.tripData.DrivingSession
import com.ixam97.carStatsViewer.database.tripData.TripType
import com.ixam97.carStatsViewer.utils.StringFormatters
import java.util.*

class TripHistoryRowWidget(context: Context, private val attrs: AttributeSet? = null, defStyleAttr: Int = 0,private val session: DrivingSession) :
    LinearLayout(context, attrs, defStyleAttr) {
/*
    var topText = "Lorem Ipsum"
        set(value) {
            field = value
            init()
        }
    var bottomText = "Lorem Ipsum"
        set(value) {
            field = value
            init()
        }
*/
    private var mainClickListener: OnMainClickListener? = null
    private var deleteClickListener: OnDeleteClickListener? = null

    init {
        init()
    }

    fun interface OnMainClickListener {
        fun onMainClicked()
    }

    fun interface OnDeleteClickListener {
        fun onDeleteClicked()
    }

    fun setOnMainClickListener(listener: OnMainClickListener) {
        mainClickListener = listener
    }

    fun setOnDeleteClickListener(listener: OnDeleteClickListener) {
        deleteClickListener = listener
    }

    private fun init() {
        this.removeAllViews()
        View.inflate(context, R.layout.widget_trip_history_row, this)

        val rowIcon: ImageView = findViewById(R.id.row_start_icon)

        val topText: TextView = findViewById(R.id.row_top_text)
        val distanceText: TextView = findViewById(R.id.distance_text)
        val energyText: TextView = findViewById(R.id.energy_text)
        val consText: TextView = findViewById(R.id.consumption_text)
        val timeText: TextView = findViewById(R.id.time_text)

        val rowDelete: ImageView = findViewById((R.id.row_end_icon))
        val rowBody: ConstraintLayout = findViewById(R.id.row_container_body)

        rowDelete.setOnClickListener {
            deleteClickListener?.onDeleteClicked()
        }

        rowBody.setOnClickListener {
            mainClickListener?.onMainClicked()
        }

        when (session.session_type) {
            TripType.SINCE_CHARGE -> rowIcon.setImageResource(R.drawable.ic_charger_2)
            TripType.MONTH -> rowIcon.setImageResource(R.drawable.ic_month)
            TripType.AUTO -> rowIcon.setImageResource(R.drawable.ic_day)
            TripType.MANUAL -> rowIcon.setImageResource(R.drawable.ic_hand)
            else -> rowIcon.setImageResource(R.drawable.ic_help)
        }


        topText.text = StringFormatters.getDateString(Calendar.getInstance().apply { timeInMillis = session.start_epoch_time })
        distanceText.text = StringFormatters.getTraveledDistanceString(session.driven_distance.toFloat())
        energyText.text = StringFormatters.getEnergyString(session.used_energy.toFloat())
        consText.text = StringFormatters.getAvgConsumptionString(session.used_energy.toFloat(), session.driven_distance.toFloat())
        timeText.text = StringFormatters.getElapsedTimeString(session.drive_time, minutes = true)
    }
}